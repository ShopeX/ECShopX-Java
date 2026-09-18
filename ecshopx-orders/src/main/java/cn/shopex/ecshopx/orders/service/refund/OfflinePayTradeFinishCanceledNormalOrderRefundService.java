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

package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OfflinePayTradeFinishCanceledNormalOrderRefundService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final OrderSuccessTradeReadPort orderSuccessTradeReadPort;
	private final AftersalesRefundService aftersalesRefundService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	public OfflinePayTradeFinishCanceledNormalOrderRefundService(
			NormalOrdersMapper normalOrdersMapper,
			SupplierOrderMapper supplierOrderMapper,
			OrderSuccessTradeReadPort orderSuccessTradeReadPort,
			AftersalesRefundService aftersalesRefundService,
			OrderProcessLogPublishPort orderProcessLogPublishPort) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.orderSuccessTradeReadPort = orderSuccessTradeReadPort;
		this.aftersalesRefundService = aftersalesRefundService;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public void executeIfApplicable(Map<String, Object> tradeFinishRow) {
		if (!isOfflinePayTradeSuccess(tradeFinishRow)) {
			return;
		}
		long companyId = longFromRow(tradeFinishRow.get("company_id"));
		long orderId = longFromRow(tradeFinishRow.get("order_id"));
		if (companyId <= 0 || orderId <= 0) {
			return;
		}
		if (!isCanceledNormalOrderEligible(companyId, orderId)) {
			return;
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (order == null) {
			return;
		}
		long userId = order.getUserId() == null ? 0L : order.getUserId();
		Optional<Map<String, Object>> tradeOpt = orderSuccessTradeReadPort.primarySuccessTrade(companyId, orderId);
		if (tradeOpt.isEmpty()) {
			return;
		}
		Map<String, Object> trade = tradeOpt.get();
		String tradeId = String.valueOf(trade.get("trade_id"));
		String tradePayType = safe(String.valueOf(trade.get("pay_type")));

		long shopId = order.getShopId() == null ? 0L : order.getShopId();
		long distributorId = order.getDistributorId() == null ? 0L : order.getDistributorId();
		String refundChannel = "offline_pay".equalsIgnoreCase(tradePayType) ? "offline" : "original";

		List<SupplierOrder> suppliers =
				supplierOrderMapper.selectList(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getCompanyId, companyId)
								.eq(SupplierOrder::getOrderId, orderId)
								.eq(SupplierOrder::getUserId, userId));
		if (suppliers.isEmpty()) {
			createSingleRefundThenOpl(companyId, userId, orderId, order, trade, tradeId, shopId, distributorId, refundChannel);
		} else {
			for (SupplierOrder so : suppliers) {
				Map<String, Object> p = baseRefundParams(companyId, userId, orderId, trade, tradeId, shopId, distributorId);
				long sid = so.getSupplierId() == null ? 0L : so.getSupplierId();
				p.put("supplier_id", sid);
				p.put("refund_fee", parseMoneyLong(so.getTotalFee()));
				p.put("refund_point", so.getPoint() == null ? 0 : so.getPoint());
				p.put("return_freight", 1);
				p.put("freight", so.getFreightFee() == null ? 0 : so.getFreightFee());
				p.put("freight_type", safe(so.getFreightType()));
				p.put("pay_type", safe(so.getPayType()));
				p.put("refund_type", "1");
				p.put("refund_channel", refundChannel);
				p.put("refund_status", "READY");
				p.put("currency", "point".equalsIgnoreCase(tradePayType) ? "" : String.valueOf(trade.get("fee_type")));
				p.put("cur_fee_type", "point".equalsIgnoreCase(tradePayType) ? "" : String.valueOf(trade.get("cur_fee_type")));
				p.put("cur_fee_rate", trade.get("cur_fee_rate"));
				p.put("cur_fee_symbol", "point".equalsIgnoreCase(tradePayType) ? "" : String.valueOf(trade.get("cur_fee_symbol")));
				p.put(
						"cur_pay_fee",
						"point".equalsIgnoreCase(tradePayType)
								? String.valueOf(p.get("refund_point"))
								: String.valueOf(
										(int)
												Math.round(
														intVal(p.get("refund_fee")) * doubleVal(trade.get("cur_fee_rate")))));
				aftersalesRefundService.createRefund(p);
			}
			publishRefundOpl(companyId, orderId, userId);
		}
	}

	private void createSingleRefundThenOpl(
			long companyId,
			long userId,
			long orderId,
			NormalOrders normalOrder,
			Map<String, Object> trade,
			String tradeId,
			long shopId,
			long distributorId,
			String refundChannel) {
		int totalFee = parseMoneyInt(normalOrder.getTotalFee());
		int freightFee = normalOrder.getFreightFee() == null ? 0 : normalOrder.getFreightFee();
		int refundFee = Math.max(0, totalFee - freightFee);
		int refundPoint = normalOrder.getPoint() == null ? 0 : normalOrder.getPoint();
		String tradePayType = safe(String.valueOf(trade.get("pay_type")));
		String payType = safe(normalOrder.getPayType());

		Map<String, Object> p = baseRefundParams(companyId, userId, orderId, trade, tradeId, shopId, distributorId);
		p.put("supplier_id", 0L);
		p.put("refund_fee", refundFee);
		p.put("refund_point", refundPoint);
		p.put("return_freight", 1);
		p.put("freight", freightFee);
		p.put("freight_type", safe(normalOrder.getFreightType()));
		p.put("pay_type", payType);
		p.put("refund_type", "1");
		p.put("refund_channel", refundChannel);
		p.put("refund_status", "READY");
		p.put("currency", "point".equalsIgnoreCase(tradePayType) ? "" : String.valueOf(trade.get("fee_type")));
		p.put("cur_fee_type", "point".equalsIgnoreCase(tradePayType) ? "" : String.valueOf(trade.get("cur_fee_type")));
		p.put("cur_fee_rate", trade.get("cur_fee_rate"));
		p.put("cur_fee_symbol", "point".equalsIgnoreCase(tradePayType) ? "" : String.valueOf(trade.get("cur_fee_symbol")));
		p.put(
				"cur_pay_fee",
				"point".equalsIgnoreCase(tradePayType)
						? String.valueOf(refundPoint)
						: String.valueOf((int) Math.round(refundFee * doubleVal(trade.get("cur_fee_rate")))));
		aftersalesRefundService.createRefund(p);
		publishRefundOpl(companyId, orderId, userId);
	}

	private void publishRefundOpl(long companyId, long orderId, long userId) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("order_id", orderId);
		params.put("user_id", userId);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", orderId);
		entities.put("company_id", companyId);
		entities.put("operator_type", "system");
		entities.put("operator_id", 0L);
		entities.put("remarks", "订单退款");
		entities.put("detail", "订单号：" + orderId + "，系统自动同意退款");
		entities.put("params", params);
		entities.put("is_show", Boolean.FALSE);
		orderProcessLogPublishPort.publish(entities);
	}

	private static Map<String, Object> baseRefundParams(
			long companyId,
			long userId,
			long orderId,
			Map<String, Object> trade,
			String tradeId,
			long shopId,
			long distributorId) {
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("company_id", companyId);
		p.put("user_id", userId);
		p.put("order_id", orderId);
		p.put("trade_id", tradeId);
		p.put("shop_id", shopId);
		p.put("distributor_id", distributorId);
		p.put("merchant_id", longVal(trade.get("merchant_id")));
		return p;
	}

	private boolean isOfflinePayTradeSuccess(Map<String, Object> tradeFinishRow) {
		String tradeState = safe(String.valueOf(tradeFinishRow.get("trade_state")));
		if (!"SUCCESS".equals(tradeState)) {
			return false;
		}
		String payType = safe(String.valueOf(tradeFinishRow.get("pay_type")));
		return isPayTypeAllowedForCanceledRefundPath(payType);
	}

	/**
	 * Trade-finish rows allowed into the canceled normal-order refund + refund OPL path (mirrors legacy
	 * offline audit and Alipay notify success semantics). Do not broaden beyond analyzed pay types.
	 */
	private boolean isPayTypeAllowedForCanceledRefundPath(String payType) {
		return "offline_pay".equalsIgnoreCase(payType) || "alipay".equalsIgnoreCase(payType);
	}

	private boolean isCanceledNormalOrderEligible(long companyId, long orderId) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (order == null) {
			return false;
		}
		String orderClass = order.getOrderClass() == null ? "normal" : safe(order.getOrderClass());
		if (!"normal".equalsIgnoreCase(orderClass)) {
			return false;
		}
		String orderStatus = safe(order.getOrderStatus());
		if (!"CANCEL".equalsIgnoreCase(orderStatus)) {
			return false;
		}
		return !"NOTPAY".equalsIgnoreCase(orderStatus) && !"PART_PAYMENT".equalsIgnoreCase(orderStatus);
	}

	private static long longFromRow(Object raw) {
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

	private static double doubleVal(Object o) {
		if (o == null) {
			return 1.0;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 1.0;
		}
	}

	private static long parseMoneyLong(String raw) {
		return parseMoneyInt(raw);
	}

	private static int parseMoneyInt(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
