/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.orders.service.tradefinish;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * TradeFinish 状态机（Spec §3）：可收付推进、逾期 {@link AftersalesRefundService#REFUND_STATUS_PAY_AFTER_CANCEL}、
 * 重复 {@link AftersalesRefundService#REFUND_STATUS_DUP_PAY}。绑定事件中的本笔 {@code trade_id}。
 */
@Service
public class OrdersTradeFinishReconcileService {

	private static final Logger log = LoggerFactory.getLogger(OrdersTradeFinishReconcileService.class);

	static final String BRANCH_PAYABLE = "payable";
	static final String BRANCH_PAY_AFTER_CANCEL = "pay_after_cancel";
	static final String BRANCH_DUP_PAY = "dup_pay";
	static final String BRANCH_SAME_TRADE_IGNORE = "same_trade_ignore";
	static final String BRANCH_UNHANDLED = "unhandled";

	private static final int MAX_CAS_ATTEMPTS = 3;

	private static final Set<String> POST_PAY_ORDER_STATUSES =
			Set.of("PAYED", "WAIT_GROUPS_SUCCESS", "WAIT_BUYER_CONFIRM", "DONE");

	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final OrdersTradeFinishNormalOrderPaySuccessApplyService paySuccessApplyService;
	private final AftersalesRefundService aftersalesRefundService;

	public OrdersTradeFinishReconcileService(
			NormalOrdersMapper normalOrdersMapper,
			TradeMapper tradeMapper,
			OrdersTradeFinishNormalOrderPaySuccessApplyService paySuccessApplyService,
			AftersalesRefundService aftersalesRefundService) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.tradeMapper = tradeMapper;
		this.paySuccessApplyService = paySuccessApplyService;
		this.aftersalesRefundService = aftersalesRefundService;
	}

	/**
	 * @return PaySuccess 异步 payload；异常/忽略分支返回 {@link Optional#empty()}
	 */
	public Optional<Map<String, Object>> reconcile(Map<String, Object> tradeFinishPayload) {
		if (tradeFinishPayload == null || tradeFinishPayload.isEmpty()) {
			return Optional.empty();
		}
		if (!"SUCCESS".equals(trim(tradeFinishPayload.get("trade_state")))) {
			return Optional.empty();
		}
		long companyId = longVal(tradeFinishPayload.get("company_id"));
		long orderId = longVal(tradeFinishPayload.get("order_id"));
		String tradeId = trim(tradeFinishPayload.get("trade_id"));
		if (companyId <= 0L || orderId <= 0L || tradeId.isEmpty()) {
			return Optional.empty();
		}

		Optional<Map<String, Object>> tradeRowOpt = loadTradeForReconcile(companyId, tradeId);
		if (tradeRowOpt.isEmpty()) {
			log.warn(
					"trade_finish_reconcile_missing_trade order_id={} company_id={} trade_id={}",
					orderId,
					companyId,
					tradeId);
			return Optional.empty();
		}
		Map<String, Object> tradeRow = tradeRowOpt.get();

		for (int attempt = 0; attempt < MAX_CAS_ATTEMPTS; attempt++) {
			NormalOrders order =
					normalOrdersMapper.selectOne(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderId)
									.last("LIMIT 1"));
			if (order == null) {
				log.debug("trade_finish_reconcile_order_missing order_id={} company_id={}", orderId, companyId);
				return Optional.empty();
			}

			long nowSec = System.currentTimeMillis() / 1000L;
			String branch = resolveReconcileBranch(order, tradeId, nowSec);
			if (BRANCH_PAYABLE.equals(branch)) {
				Optional<Map<String, Object>> paySuccess =
						paySuccessApplyService.markPayedIfNeeded(tradeFinishPayload);
				if (paySuccess.isPresent()) {
					log.info(
							"trade_finish_reconcile_pay_success order_id={} company_id={} trade_id={} order_status={} result=pay_success",
							orderId,
							companyId,
							tradeId,
							order.getOrderStatus());
					return paySuccess;
				}
				log.info(
						"trade_finish_reconcile_cas_miss order_id={} company_id={} trade_id={} order_status={} result=cas_retry",
						orderId,
						companyId,
						tradeId,
						order.getOrderStatus());
				continue;
			}

			handleNonPayableBranch(branch, order, tradeRow, companyId, orderId, tradeId);
			return Optional.empty();
		}

		log.info(
				"trade_finish_reconcile_cas_exhausted order_id={} company_id={} trade_id={}",
				orderId,
				companyId,
				tradeId);
		return Optional.empty();
	}

	String resolveReconcileBranch(NormalOrders order, String currentTradeId, long nowEpochSec) {
		String status = trim(order.getOrderStatus());
		if ("PART_PAYMENT".equals(status)) {
			return BRANCH_PAYABLE;
		}
		if ("NOTPAY".equals(status)) {
			return isNotPayTimedOut(order, nowEpochSec) ? BRANCH_PAY_AFTER_CANCEL : BRANCH_PAYABLE;
		}
		if ("CANCEL".equals(status)) {
			return BRANCH_PAY_AFTER_CANCEL;
		}
		if (POST_PAY_ORDER_STATUSES.contains(status)) {
			long companyId = order.getCompanyId() == null ? 0L : order.getCompanyId();
			long orderId = order.getOrderId() == null ? 0L : order.getOrderId();
			return hasOtherSuccessfulNonPointTrade(companyId, orderId, currentTradeId)
					? BRANCH_DUP_PAY
					: BRANCH_SAME_TRADE_IGNORE;
		}
		return BRANCH_UNHANDLED;
	}

	boolean isNotPayTimedOut(NormalOrders order, long nowEpochSec) {
		if (!"NOTPAY".equals(trim(order.getOrderStatus()))) {
			return false;
		}
		int autoCancel = intVal(order.getAutoCancelTime());
		return autoCancel > 0 && autoCancel <= nowEpochSec;
	}

	boolean hasOtherSuccessfulNonPointTrade(long companyId, long orderId, String excludeTradeId) {
		if (companyId < 1L || orderId < 1L || excludeTradeId == null || excludeTradeId.isEmpty()) {
			return false;
		}
		List<Trade> tradeList =
				tradeMapper.selectList(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getOrderId, String.valueOf(orderId))
								.eq(Trade::getTradeState, "SUCCESS")
								.last("LIMIT 100"));
		for (Trade row : tradeList) {
			String rowTradeId = trim(row.getTradeId());
			if (!rowTradeId.isEmpty() && rowTradeId.equals(excludeTradeId)) {
				continue;
			}
			if (!"point".equalsIgnoreCase(trim(row.getPayType()))) {
				return true;
			}
		}
		return false;
	}

	private void handleNonPayableBranch(
			String branch,
			NormalOrders order,
			Map<String, Object> tradeRow,
			long companyId,
			long orderId,
			String tradeId) {
		Map<String, Object> orderMap = orderToMap(order);
		String orderStatus = trim(order.getOrderStatus());

		if (BRANCH_PAY_AFTER_CANCEL.equals(branch)) {
			AftersalesRefund refund =
					aftersalesRefundService.createAbnormalTradeRefund(
							orderMap, tradeRow, AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL);
			log.info(
					"trade_finish_reconcile_pay_after_cancel order_id={} company_id={} trade_id={} order_status={} refund_status={} refund_bn={} result=created",
					orderId,
					companyId,
					tradeId,
					orderStatus,
					refund.getRefundStatus(),
					refund.getRefundBn());
			return;
		}
		if (BRANCH_DUP_PAY.equals(branch)) {
			AftersalesRefund refund =
					aftersalesRefundService.createAbnormalTradeRefund(
							orderMap, tradeRow, AftersalesRefundService.REFUND_STATUS_DUP_PAY);
			log.info(
					"trade_finish_reconcile_dup_pay order_id={} company_id={} trade_id={} order_status={} refund_status={} refund_bn={} result=created",
					orderId,
					companyId,
					tradeId,
					orderStatus,
					refund.getRefundStatus(),
					refund.getRefundBn());
			return;
		}
		if (BRANCH_SAME_TRADE_IGNORE.equals(branch)) {
			log.info(
					"trade_finish_reconcile_same_trade_ignore order_id={} company_id={} trade_id={} order_status={} result=ignored",
					orderId,
					companyId,
					tradeId,
					orderStatus);
			return;
		}
		log.info(
				"trade_finish_reconcile_unhandled order_id={} company_id={} trade_id={} order_status={} result=skipped",
				orderId,
				companyId,
				tradeId,
				orderStatus);
	}

	private Optional<Map<String, Object>> loadTradeForReconcile(long companyId, String tradeId) {
		Trade t =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getTradeId, tradeId)
								.last("LIMIT 1"));
		if (t == null) {
			return Optional.empty();
		}
		return Optional.of(tradeToMap(t));
	}

	private static Map<String, Object> tradeToMap(Trade t) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("trade_id", t.getTradeId());
		m.put("pay_type", t.getPayType());
		m.put("fee_type", t.getFeeType());
		m.put("cur_fee_type", t.getCurFeeType());
		m.put("cur_fee_rate", t.getCurFeeRate());
		m.put("cur_fee_symbol", t.getCurFeeSymbol());
		m.put("pay_fee", t.getPayFee());
		m.put("merchant_id", t.getMerchantId());
		return m;
	}

	private static Map<String, Object> orderToMap(NormalOrders order) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", order.getCompanyId());
		m.put("order_id", order.getOrderId());
		m.put("user_id", order.getUserId());
		m.put("shop_id", order.getShopId());
		m.put("distributor_id", order.getDistributorId());
		m.put("freight_type", order.getFreightType());
		m.put("order_status", order.getOrderStatus());
		m.put("auto_cancel_time", order.getAutoCancelTime());
		return m;
	}

	private static String trim(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim();
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}