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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.AftersalesRefundJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundAbnormalChannelPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AftersalesRefundService {

	/** Lower bound for order creation time (Unix epoch seconds); scheduled refunds only include orders created at or after this instant. */
	public static final int SCHEDULE_REFUND_MIN_CREATE_TIME_EPOCH_SEC = 1607616000;

	/** TradeFinish 逾期支付待人工审批退款（Spec §2.1） */
	public static final String REFUND_STATUS_PAY_AFTER_CANCEL = "PAY_AFTER_CANCEL";

	/** TradeFinish 重复支付待人工审批退款（Spec §2.1） */
	public static final String REFUND_STATUS_DUP_PAY = "DUP_PAY";

	private static final List<String> ABNORMAL_TRADE_REFUND_IDEMPOTENT_STATUSES =
			List.of(
					REFUND_STATUS_PAY_AFTER_CANCEL,
					REFUND_STATUS_DUP_PAY,
					"PROCESSING",
					"SUCCESS");

	private static final List<String> ABNORMAL_TRADE_REFUND_CAS_FROM =
			List.of(REFUND_STATUS_PAY_AFTER_CANCEL, REFUND_STATUS_DUP_PAY);

	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final AftersalesRefundJobDispatchPublisher aftersalesRefundJobDispatchPublisher;
	private final AftersalesRefundAbnormalChannelPort aftersalesRefundAbnormalChannelPort;
	private final ObjectProvider<OrderNormalOrderHeaderReadPort> orderNormalOrderHeaderReadPort;

	public AftersalesRefundService(
			AftersalesRefundMapper aftersalesRefundMapper,
			AftersalesRefundJobDispatchPublisher aftersalesRefundJobDispatchPublisher,
			AftersalesRefundAbnormalChannelPort aftersalesRefundAbnormalChannelPort,
			ObjectProvider<OrderNormalOrderHeaderReadPort> orderNormalOrderHeaderReadPort) {
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.aftersalesRefundJobDispatchPublisher = aftersalesRefundJobDispatchPublisher;
		this.aftersalesRefundAbnormalChannelPort = aftersalesRefundAbnormalChannelPort;
		this.orderNormalOrderHeaderReadPort = orderNormalOrderHeaderReadPort;
	}

	/**
	 * TradeFinish 异常退款建单工厂：绑定本笔 {@code trade_id}，状态为 {@link #REFUND_STATUS_PAY_AFTER_CANCEL} 或
	 * {@link #REFUND_STATUS_DUP_PAY}。同 {@code company_id + trade_id} 在待审/处理中/已成功时幂等返回已有单。
	 */
	public AftersalesRefund createAbnormalTradeRefund(
			Map<String, Object> order, Map<String, Object> tradeRow, String refundStatus) {
		if (order == null || tradeRow == null) {
			throw new IllegalArgumentException("order and tradeRow required");
		}
		String status = str(refundStatus);
		if (!REFUND_STATUS_PAY_AFTER_CANCEL.equals(status) && !REFUND_STATUS_DUP_PAY.equals(status)) {
			throw new IllegalArgumentException("abnormal refund_status must be PAY_AFTER_CANCEL or DUP_PAY");
		}
		long companyId = longVal(order.get("company_id"));
		String tradeId = str(tradeRow.get("trade_id"));
		if (companyId <= 0L || tradeId.isEmpty()) {
			throw new IllegalArgumentException("company_id and trade_id required");
		}
		AftersalesRefund existing = findAbnormalTradeRefundIdempotent(companyId, tradeId);
		if (existing != null) {
			return existing;
		}

		String tradePayType = str(tradeRow.get("pay_type"));
		int payFee = intVal(tradeRow.get("pay_fee"));
		int refundFee = "point".equalsIgnoreCase(tradePayType) ? 0 : payFee;
		int refundPoint = "point".equalsIgnoreCase(tradePayType) ? payFee : 0;
		String refundChannel =
				"offline_pay".equalsIgnoreCase(tradePayType) ? "offline" : "original";

		long userId = longVal(order.get("user_id"));
		long orderId = longVal(order.get("order_id"));
		long shopId = longVal(order.get("shop_id"));
		long distributorId = longVal(order.get("distributor_id"));
		long merchantId = longVal(tradeRow.get("merchant_id"));
		String freightType = str(order.get("freight_type"));
		if (freightType.isEmpty()) {
			freightType = "cash";
		}
		String curFeeType = str(tradeRow.get("cur_fee_type"));
		if (curFeeType.isEmpty()) {
			curFeeType = "CNY";
		}
		BigDecimal curFeeRateBd = toBigDecimal(tradeRow.get("cur_fee_rate"));
		String curFeeSymbol = str(tradeRow.get("cur_fee_symbol"));
		if (curFeeSymbol.isEmpty()) {
			curFeeSymbol = "￥";
		}
		String curPayFee =
				"point".equalsIgnoreCase(tradePayType)
						? String.valueOf(refundPoint)
						: BigDecimal.valueOf(refundFee)
								.multiply(curFeeRateBd)
								.setScale(0, RoundingMode.HALF_UP)
								.toPlainString();

		int now = (int) (System.currentTimeMillis() / 1000L);
		AftersalesRefund row = new AftersalesRefund();
		row.setRefundBn(genRefundBnLong());
		row.setOrderId(orderId);
		row.setTradeId(tradeId);
		row.setCompanyId(companyId);
		row.setSupplierId(0L);
		row.setUserId(userId);
		row.setShopId(shopId);
		row.setDistributorId(distributorId);
		row.setRefundType("1");
		row.setRefundChannel(refundChannel);
		row.setRefundStatus(status);
		row.setRefundFee(refundFee);
		row.setRefundPoint(refundPoint);
		row.setReturnFreight(0);
		row.setFreight(0);
		row.setFreightType(freightType);
		row.setPayType(tradePayType);
		row.setCurrency("point".equalsIgnoreCase(tradePayType) ? "" : str(tradeRow.get("fee_type")));
		row.setCurFeeType(
				"point".equalsIgnoreCase(tradePayType) ? "" : curFeeType);
		row.setCurFeeRate(curFeeRateBd.setScale(4, RoundingMode.HALF_UP).doubleValue());
		row.setCurFeeSymbol(
				"point".equalsIgnoreCase(tradePayType) ? "" : curFeeSymbol);
		row.setCurPayFee(curPayFee);
		row.setReturnPoint(0);
		row.setMerchantId(merchantId);
		row.setCreateTime(now);
		row.setUpdateTime(now);
		aftersalesRefundMapper.insert(row);
		return row;
	}

	AftersalesRefund findAbnormalTradeRefundIdempotent(long companyId, String tradeId) {
		LambdaQueryWrapper<AftersalesRefund> w =
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getTradeId, tradeId)
						.in(AftersalesRefund::getRefundStatus, ABNORMAL_TRADE_REFUND_IDEMPOTENT_STATUSES)
						.orderByDesc(AftersalesRefund::getCreateTime)
						.last("LIMIT 1");
		return aftersalesRefundMapper.selectOne(w);
	}

	/**
	 * TradeFinish 异常退款实退（Spec §4.3）：CAS 待审异常态 → 渠道打款 → SUCCESS 或回原状态；无订单收尾。
	 */
	public AftersalesRefund refundAbnormalTrade(long companyId, long refundBn) {
		AftersalesRefund refund = loadRefundByBn(companyId, refundBn);
		if (refund == null) {
			throw new ResourceException("退款单不存在");
		}
		String orderStatus = resolveOrderStatusForAbnormalLog(companyId, refund.getOrderId());
		String refundStatus = str(refund.getRefundStatus());
		if ("SUCCESS".equals(refundStatus)) {
			logRefundAbnormal(
					refund,
					orderStatus,
					refundStatus,
					"already_success");
			return refund;
		}
		if ("PROCESSING".equals(refundStatus)) {
			throw new ResourceException("该退款单处理中");
		}
		if (!ABNORMAL_TRADE_REFUND_CAS_FROM.contains(refundStatus)) {
			throw new ResourceException("退款单状态不支持异常实退");
		}

		String priorStatus = refundStatus;
		int now = (int) (System.currentTimeMillis() / 1000L);
		int claimed =
				aftersalesRefundMapper.update(
						null,
						new LambdaUpdateWrapper<AftersalesRefund>()
								.eq(AftersalesRefund::getCompanyId, companyId)
								.eq(AftersalesRefund::getRefundBn, refundBn)
								.eq(AftersalesRefund::getRefundStatus, priorStatus)
								.set(AftersalesRefund::getRefundStatus, "PROCESSING")
								.set(AftersalesRefund::getUpdateTime, now));
		if (claimed <= 0) {
			return refundAbnormalTradeCasMiss(companyId, refundBn, orderStatus);
		}

		Map<String, Object> payRes;
		try {
			payRes =
					aftersalesRefundAbnormalChannelPort.dispatchAbnormalRefundChannel(
							companyId, refundBn);
		} catch (RuntimeException ex) {
			restoreAbnormalRefundPriorStatus(companyId, refundBn, priorStatus, now);
			AftersalesRefund restored = loadRefundByBn(companyId, refundBn);
			logRefundAbnormal(
					restored != null ? restored : refund,
					orderStatus,
					priorStatus,
					"channel_throw");
			throw ex;
		}
		String payStatus = String.valueOf(payRes.getOrDefault("status", ""));
		int effectiveRefundFee = refund.getRefundFee() == null ? 0 : refund.getRefundFee();
		int effectiveRefundPoint = refund.getRefundPoint() == null ? 0 : refund.getRefundPoint();

		if ("SUCCESS".equals(payStatus) || "PROCESSING".equals(payStatus)) {
			String refundId = payRes.get("refund_id") == null ? "" : String.valueOf(payRes.get("refund_id"));
			aftersalesRefundMapper.update(
					null,
					new LambdaUpdateWrapper<AftersalesRefund>()
							.eq(AftersalesRefund::getCompanyId, companyId)
							.eq(AftersalesRefund::getRefundBn, refundBn)
							.eq(AftersalesRefund::getRefundStatus, "PROCESSING")
							.set(AftersalesRefund::getRefundId, refundId)
							.set(AftersalesRefund::getRefundStatus, "SUCCESS")
							.set(AftersalesRefund::getRefundedFee, effectiveRefundFee)
							.set(AftersalesRefund::getRefundedPoint, effectiveRefundPoint)
							.set(AftersalesRefund::getRefundSuccessTime, (long) now)
							.set(AftersalesRefund::getUpdateTime, now));
			AftersalesRefund fresh = loadRefundByBn(companyId, refundBn);
			logRefundAbnormal(
					fresh != null ? fresh : refund,
					orderStatus,
					"SUCCESS",
					"channel_success");
			return fresh != null ? fresh : refund;
		}

		restoreAbnormalRefundPriorStatus(companyId, refundBn, priorStatus, now);
		AftersalesRefund restored = loadRefundByBn(companyId, refundBn);
		logRefundAbnormal(
				restored != null ? restored : refund,
				orderStatus,
				priorStatus,
				"channel_fail");
		Object err = payRes.get("error_desc");
		if (err == null) {
			err = payRes.get("error_msg");
		}
		String msg = err == null ? "退款失败" : String.valueOf(err);
		throw new ResourceException(msg.isBlank() ? "退款失败" : msg);
	}

	private void restoreAbnormalRefundPriorStatus(
			long companyId, long refundBn, String priorStatus, int now) {
		aftersalesRefundMapper.update(
				null,
				new LambdaUpdateWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getRefundBn, refundBn)
						.eq(AftersalesRefund::getRefundStatus, "PROCESSING")
						.set(AftersalesRefund::getRefundStatus, priorStatus)
						.set(AftersalesRefund::getUpdateTime, now));
	}

	private AftersalesRefund refundAbnormalTradeCasMiss(
			long companyId, long refundBn, String orderStatus) {
		AftersalesRefund again = loadRefundByBn(companyId, refundBn);
		if (again == null) {
			throw new ResourceException("退款单不存在");
		}
		String rs = str(again.getRefundStatus());
		if ("SUCCESS".equals(rs)) {
			logRefundAbnormal(again, orderStatus, rs, "already_success");
			return again;
		}
		if ("PROCESSING".equals(rs)) {
			throw new ResourceException("该退款单处理中");
		}
		throw new ResourceException("退款单状态已变化，请重试");
	}

	private AftersalesRefund loadRefundByBn(long companyId, long refundBn) {
		return aftersalesRefundMapper.selectOne(
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getRefundBn, refundBn)
						.last("LIMIT 1"));
	}

	private String resolveOrderStatusForAbnormalLog(long companyId, Long orderId) {
		if (orderId == null || orderId <= 0L) {
			return "";
		}
		OrderNormalOrderHeaderReadPort port =
				orderNormalOrderHeaderReadPort == null ? null : orderNormalOrderHeaderReadPort.getIfAvailable();
		if (port == null) {
			return "";
		}
		return port.getHeader(companyId, orderId)
				.map(h -> str(h.get("order_status")))
				.orElse("");
	}

	private void logRefundAbnormal(
			AftersalesRefund refund, String orderStatus, String refundStatus, String result) {
		log.info(
				"refund_abnormal_refund order_id={} company_id={} trade_id={} order_status={} refund_status={} refund_bn={} result={}",
				refund.getOrderId(),
				refund.getCompanyId(),
				refund.getTradeId(),
				orderStatus,
				refundStatus,
				refund.getRefundBn(),
				result);
	}

	/**
	 * 售前取消等场景直接创建退款申请单（对位仓储 create + 字段组装）。
	 * 写库后回写 {@code params}：{@code refund_bn}（{@link Long}）、若存在则 {@code refund_id}。
	 */
	public void createRefund(Map<String, Object> params) {
		if (params == null) {
			return;
		}
		long refundBn = genRefundBnLong();
		long companyId = longVal(params.get("company_id"));
		long userId = longVal(params.get("user_id"));
		long orderId = longVal(params.get("order_id"));
		String tradeId = str(params.get("trade_id"));
		long shopId = longVal(params.get("shop_id"));
		long distributorId = longVal(params.get("distributor_id"));
		long supplierId = longVal(params.get("supplier_id"));
		int refundFee = intVal(params.get("refund_fee"));
		int refundPoint = intVal(params.get("refund_point"));
		int returnFreight = intVal(params.get("return_freight"));
		int freight = intVal(params.get("freight"));
		String freightType = str(params.get("freight_type"));
		if (freightType.isEmpty()) {
			freightType = "cash";
		}
		Long aftersalesBnOpt = parseLongOrNull(params.get("aftersales_bn"));
		String refundType = str(params.get("refund_type"));
		if (refundType.isEmpty()) {
			refundType = aftersalesBnOpt != null ? "0" : "1";
		}
		String refundChannel = str(params.get("refund_channel"));
		String refundStatus = str(params.get("refund_status"));
		if (refundStatus.isEmpty()) {
			refundStatus = "READY";
		}
		String payType = str(params.get("pay_type"));
		String currency = str(params.get("currency"));
		String curFeeType = str(params.get("cur_fee_type"));
		if (curFeeType.isEmpty()) {
			curFeeType = "CNY";
		}
		BigDecimal curFeeRateBd = toBigDecimal(params.get("cur_fee_rate"));
		String curFeeSymbol = str(params.get("cur_fee_symbol"));
		if (curFeeSymbol.isEmpty()) {
			curFeeSymbol = "￥";
		}
		String curPayFee;
		if (params.containsKey("cur_pay_fee") && params.get("cur_pay_fee") != null) {
			curPayFee = str(params.get("cur_pay_fee"));
		} else {
			curPayFee =
					BigDecimal.valueOf(refundFee)
							.multiply(curFeeRateBd)
							.setScale(0, RoundingMode.HALF_UP)
							.toPlainString();
		}
		BigDecimal curFeeRateStoredBd = curFeeRateBd.setScale(4, RoundingMode.HALF_UP);
		long merchantId = longVal(params.get("merchant_id"));

		int now = (int) (System.currentTimeMillis() / 1000L);
		AftersalesRefund row = new AftersalesRefund();
		row.setRefundBn(refundBn);
		row.setAftersalesBn(aftersalesBnOpt);
		row.setOrderId(orderId);
		row.setTradeId(tradeId);
		row.setCompanyId(companyId);
		row.setSupplierId(supplierId);
		row.setUserId(userId);
		row.setShopId(shopId);
		row.setDistributorId(distributorId);
		row.setRefundType(refundType);
		row.setRefundChannel(refundChannel);
		row.setRefundStatus(refundStatus);
		row.setRefundFee(refundFee);
		row.setRefundPoint(refundPoint);
		row.setReturnFreight(returnFreight);
		row.setFreight(freight);
		row.setFreightType(freightType);
		row.setPayType(payType);
		row.setCurrency(currency);
		row.setCurFeeType(curFeeType);
		row.setCurFeeRate(curFeeRateStoredBd.doubleValue());
		row.setCurFeeSymbol(curFeeSymbol);
		row.setCurPayFee(curPayFee);
		row.setMerchantId(merchantId);
		if (params.containsKey("return_point")) {
			row.setReturnPoint(intVal(params.get("return_point")));
		}
		row.setCreateTime(now);
		row.setUpdateTime(now);
		aftersalesRefundMapper.insert(row);
		params.put("refund_bn", refundBn);
		if (row.getRefundId() != null && !row.getRefundId().isEmpty()) {
			params.put("refund_id", row.getRefundId());
		}
	}

	private static long genRefundBnLong() {
		String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		long rnd = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
		return Long.parseLong("2" + day + rnd);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static BigDecimal toBigDecimal(Object o) {
		if (o == null) {
			return BigDecimal.ONE;
		}
		if (o instanceof BigDecimal b) {
			return b;
		}
		if (o instanceof BigInteger bi) {
			return new BigDecimal(bi);
		}
		if (o instanceof Byte || o instanceof Short || o instanceof Integer || o instanceof Long) {
			return BigDecimal.valueOf(((Number) o).longValue());
		}
		if (o instanceof Number n) {
			String t = n.toString();
			if (t == null || t.isBlank()) {
				return BigDecimal.ONE;
			}
			try {
				return new BigDecimal(t.trim());
			} catch (NumberFormatException e) {
				return BigDecimal.ONE;
			}
		}
		try {
			String t = String.valueOf(o).trim();
			if (t.isEmpty()) {
				return BigDecimal.ONE;
			}
			return new BigDecimal(t);
		} catch (NumberFormatException e) {
			return BigDecimal.ONE;
		}
	}

	private static Long parseLongOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s && s.isBlank()) {
			return null;
		}
		long v = longVal(raw);
		return v > 0L ? v : null;
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	public AftersalesRefund findSingleForConfirmCancel(
			long companyId,
			long orderId,
			long supplierId,
			Object refundBnRaw,
			Collection<String> refundStatusIn) {
		Long refundBn = parseRefundBnOrNull(refundBnRaw);
		LambdaQueryWrapper<AftersalesRefund> w =
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId);
		if (refundBn != null) {
			w.eq(AftersalesRefund::getRefundBn, refundBn);
		} else {
			w.eq(AftersalesRefund::getRefundType, "1");
			// 挂售后单的部分取消售前退款走售后审核，不进订单 confirmcancel
			w.isNull(AftersalesRefund::getAftersalesBn);
			if (supplierId > 0L) {
				w.eq(AftersalesRefund::getSupplierId, supplierId);
			}
			// 未指定 refund_bn 时只取待审 READY，避免 create_time DESC 命中已审过的最新单
			w.eq(AftersalesRefund::getRefundStatus, "READY");
		}
		w.orderByDesc(AftersalesRefund::getCreateTime).last("LIMIT 1");
		return aftersalesRefundMapper.selectOne(w);
	}

	public long countReadySupplierSubCancelRefunds(long companyId, long orderId) {
		return aftersalesRefundMapper.selectCount(
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId)
						.eq(AftersalesRefund::getRefundType, "1")
						.eq(AftersalesRefund::getRefundStatus, "READY")
						.isNull(AftersalesRefund::getAftersalesBn)
						.gt(AftersalesRefund::getSupplierId, 0L));
	}

	public boolean hasApprovedFullOrderCancelRefund(long companyId, long orderId) {
		return aftersalesRefundMapper.selectCount(
						new LambdaQueryWrapper<AftersalesRefund>()
								.eq(AftersalesRefund::getCompanyId, companyId)
								.eq(AftersalesRefund::getOrderId, orderId)
								.eq(AftersalesRefund::getRefundType, "1")
								.eq(AftersalesRefund::getSupplierId, 0L)
								.isNull(AftersalesRefund::getAftersalesBn)
								.in(
										AftersalesRefund::getRefundStatus,
										List.of("AUDIT_SUCCESS", "SUCCESS", "PROCESSING")))
				> 0L;
	}

	public List<AftersalesRefund> listReadyCancelRefundsAsc(long companyId, long orderId, long supplierId) {
		LambdaQueryWrapper<AftersalesRefund> w =
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId)
						.eq(AftersalesRefund::getRefundType, "1")
						.eq(AftersalesRefund::getRefundStatus, "READY")
						.isNull(AftersalesRefund::getAftersalesBn);
		if (supplierId > 0L) {
			w.eq(AftersalesRefund::getSupplierId, supplierId);
		}
		w.orderByAsc(AftersalesRefund::getCreateTime);
		return aftersalesRefundMapper.selectList(w);
	}

	public List<AftersalesRefund> listReadyCancelRefundsForSupplier(
			long companyId, long orderId, long supplierId) {
		return aftersalesRefundMapper.selectList(
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId)
						.eq(AftersalesRefund::getRefundType, "1")
						.eq(AftersalesRefund::getRefundStatus, "READY")
						.isNull(AftersalesRefund::getAftersalesBn)
						.eq(AftersalesRefund::getSupplierId, supplierId)
						.orderByAsc(AftersalesRefund::getCreateTime));
	}

	public List<AftersalesRefund> listApprovedCancelRefundsForSupplier(
			long companyId, long orderId, long supplierId) {
		return aftersalesRefundMapper.selectList(
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId)
						.eq(AftersalesRefund::getRefundType, "1")
						.isNull(AftersalesRefund::getAftersalesBn)
						.eq(AftersalesRefund::getSupplierId, supplierId)
						.in(
								AftersalesRefund::getRefundStatus,
								List.of("AUDIT_SUCCESS", "SUCCESS", "PROCESSING")));
	}

	public List<AftersalesRefund> listForPointsmallConfirmCancel(
			long companyId,
			long orderId,
			Object refundBnRaw,
			Collection<String> refundStatusIn) {
		Long refundBn = parseRefundBnOrNull(refundBnRaw);
		LambdaQueryWrapper<AftersalesRefund> w =
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId);
		if (refundBn != null) {
			w.eq(AftersalesRefund::getRefundBn, refundBn);
		} else {
			w.eq(AftersalesRefund::getRefundType, "1");
			w.isNull(AftersalesRefund::getAftersalesBn);
			if (refundStatusIn != null && !refundStatusIn.isEmpty()) {
				w.in(AftersalesRefund::getRefundStatus, refundStatusIn);
			}
		}
		w.orderByDesc(AftersalesRefund::getCreateTime);
		return aftersalesRefundMapper.selectList(w);
	}

	public int updateRefundByConfirmFilter(
			long companyId,
			long orderId,
			Long refundBn,
			Long supplierId,
			Map<String, Object> updateFields) {
		if (updateFields == null || updateFields.isEmpty()) {
			return 0;
		}
		LambdaUpdateWrapper<AftersalesRefund> u =
				new LambdaUpdateWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId);
		if (refundBn != null && refundBn > 0L) {
			u.eq(AftersalesRefund::getRefundBn, refundBn);
		}
		if (supplierId != null && supplierId > 0L) {
			u.eq(AftersalesRefund::getSupplierId, supplierId);
		}
		applyRefundUpdateFields(u, updateFields);
		return aftersalesRefundMapper.update(null, u);
	}

	private static void applyRefundUpdateFields(
			LambdaUpdateWrapper<AftersalesRefund> u, Map<String, Object> updateFields) {
		for (Map.Entry<String, Object> e : updateFields.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			if (k == null || k.isBlank()) {
				continue;
			}
			switch (k) {
				case "refund_status" -> u.set(AftersalesRefund::getRefundStatus, v == null ? null : String.valueOf(v));
				case "update_time" -> u.set(
						AftersalesRefund::getUpdateTime,
						v instanceof Number n ? n.intValue() : intVal(v));
				case "refunds_memo" -> u.set(AftersalesRefund::getRefundsMemo, v == null ? null : String.valueOf(v));
				default -> {
					// 仅支持确认取消审核涉及的列，避免误传任意列名
				}
			}
		}
	}

	private static Long parseRefundBnOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return null;
			}
		}
		long v = longVal(raw);
		return v > 0L ? v : null;
	}

	public long getTotalRefundFee(long companyId, long orderId) {
		QueryWrapper<AftersalesRefund> qw = new QueryWrapper<>();
		qw.select("COALESCE(SUM(refunded_fee),0) AS total_refunded");
		qw.eq("company_id", companyId);
		qw.eq("order_id", orderId);
		qw.eq("refund_status", "SUCCESS");
		List<Map<String, Object>> maps = aftersalesRefundMapper.selectMaps(qw);
		if (maps == null || maps.isEmpty()) {
			return 0L;
		}
		return longFromSum(maps.get(0).get("total_refunded"));
	}

	private static long longFromSum(Object raw) {
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

	public AftersalesRefund findRefundByAftersalesBn(long companyId, long aftersalesBn) {
		LambdaQueryWrapper<AftersalesRefund> w =
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getAftersalesBn, aftersalesBn);
		return aftersalesRefundMapper.selectOne(w);
	}

	public List<AftersalesRefund> listByCompanyAndAftersalesBns(long companyId, List<Long> aftersalesBns) {
		if (aftersalesBns == null || aftersalesBns.isEmpty()) {
			return List.of();
		}
		return aftersalesRefundMapper.selectList(
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.in(AftersalesRefund::getAftersalesBn, aftersalesBns));
	}

	public int updateRefundByAftersalesKeys(long companyId, long aftersalesBn, Map<String, Object> fields) {
		if (fields == null || fields.isEmpty()) {
			return 0;
		}
		LambdaUpdateWrapper<AftersalesRefund> u =
				new LambdaUpdateWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getAftersalesBn, aftersalesBn);
		for (Map.Entry<String, Object> e : fields.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			if (k == null || k.isBlank()) {
				continue;
			}
			switch (k) {
				case "refund_status" -> u.set(AftersalesRefund::getRefundStatus, v == null ? null : String.valueOf(v));
				case "refund_channel" -> u.set(AftersalesRefund::getRefundChannel, v == null ? null : String.valueOf(v));
				case "refunds_memo" -> u.set(AftersalesRefund::getRefundsMemo, v == null ? null : String.valueOf(v));
				case "refund_fee" -> u.set(
						AftersalesRefund::getRefundFee,
						v instanceof Number n ? n.intValue() : intVal(v));
				case "refund_point" -> u.set(
						AftersalesRefund::getRefundPoint,
						v instanceof Number n ? n.intValue() : intVal(v));
				case "freight" -> u.set(
						AftersalesRefund::getFreight,
						v instanceof Number n ? n.intValue() : intVal(v));
				case "return_freight" -> u.set(
						AftersalesRefund::getReturnFreight,
						v instanceof Number n ? n.intValue() : intVal(v));
				case "update_time" -> u.set(
						AftersalesRefund::getUpdateTime,
						v instanceof Number n ? n.intValue() : intVal(v));
				default -> {
					// 仅允许计划列出的列
				}
			}
		}
		return aftersalesRefundMapper.update(null, u);
	}

	/**
	 * 审核成功且达到时间下界的退款申请向 <code>slow</code> 队列排程；返回本趟成功投递数。
	 * <p>
	 * 在本趟遍历顺序下，每个非空订单标识首次出现时，对应投递使用零秒延迟；同一标识在本趟内再次出现时使用递增秒级延迟以错峰。
	 */
	public int scheduleRefund() {
		log.info("aftersalesRefundService::schedule_refund: 开始执行审核成功退款单退款初始化脚本");
		List<AftersalesRefund> refunds = aftersalesRefundMapper.selectList(buildScheduleRefundCandidateQuery());
		log.info(
				"aftersalesRefundService::schedule_refund: 查询条数 size={} sampleBnMask={} companyIdMaskCount={}",
				refunds.size(),
				briefMaskRefundBns(refunds),
				uniqueCompanyIdCount(refunds));
		if (refunds.isEmpty()) {
			log.info("aftersalesRefundService::schedule_refund: 无待排程记录，本趟完成 dispatched=0");
			return 0;
		}
		Set<Long> orderHas = new HashSet<>();
		Map<Long, Integer> orderHasNum = new HashMap<>();
		int dispatched = 0;
		for (AftersalesRefund v : refunds) {
			Long orderId = v.getOrderId();
			int delaySec;
			if (orderId != null && orderHas.contains(orderId)) {
				int before = orderHasNum.getOrDefault(orderId, 0);
				delaySec = 60 * before + 1;
				orderHasNum.put(orderId, before + 1);
			} else {
				if (orderId != null) {
					orderHas.add(orderId);
					orderHasNum.put(orderId, 1);
				}
				delaySec = 0;
			}
			aftersalesRefundJobDispatchPublisher.publish(
					v.getRefundBn(), v.getCompanyId(), v.getOrderId(), delaySec);
			dispatched++;
		}
		log.info("aftersalesRefundService::schedule_refund: 本趟完成 dispatched={}", dispatched);
		return dispatched;
	}

	/**
	 * 与单测共享的查询形状；包内可见，避免业务外误用。
	 */
	QueryWrapper<AftersalesRefund> buildScheduleRefundCandidateQuery() {
		QueryWrapper<AftersalesRefund> w = new QueryWrapper<>();
		w.select("refund_bn", "company_id", "order_id");
		w.eq("refund_status", "AUDIT_SUCCESS");
		w.ge("create_time", SCHEDULE_REFUND_MIN_CREATE_TIME_EPOCH_SEC);
		return w;
	}

	private static String briefMaskRefundBns(List<AftersalesRefund> refunds) {
		if (refunds == null || refunds.isEmpty()) {
			return "-";
		}
		return refunds.stream()
				.map(AftersalesRefund::getRefundBn)
				.filter(bn -> bn != null)
				.limit(3L)
				.map(AftersalesRefundService::maskRefundBn)
				.collect(Collectors.joining(","));
	}

	private static String maskRefundBn(long bn) {
		String s = String.valueOf(bn);
		if (s.length() <= 4) {
			return "****";
		}
		return s.charAt(0) + "******" + s.substring(s.length() - 2);
	}

	private static int uniqueCompanyIdCount(List<AftersalesRefund> refunds) {
		if (refunds == null) {
			return 0;
		}
		return (int) refunds.stream().map(AftersalesRefund::getCompanyId).distinct().count();
	}
}
