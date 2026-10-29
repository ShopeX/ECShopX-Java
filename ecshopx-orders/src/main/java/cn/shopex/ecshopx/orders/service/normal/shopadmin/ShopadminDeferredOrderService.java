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

package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import cn.shopex.ecshopx.common.dispatch.NormalOrderAddDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentService;
import cn.shopex.ecshopx.orders.service.tradefinish.OrdersTradeFinishNormalOrderPaySuccessApplyService;
import cn.shopex.ecshopx.payment.service.orderrefund.AlipayTradeAftersalesRefundRunner;
import cn.shopex.ecshopx.payment.service.orderrefund.WxpaySecapiAftersalesRefundRunner;
import cn.shopex.ecshopx.supplier.service.SupplierOrderSplitOnNormalOrderAddService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopadminDeferredOrderService {

	private static final Set<String> DEFERRED_PAY_TYPES = Set.of("pos", "wxpaypos", "alipaypos");

	private static final ObjectMapper SNAKE_ROW =
			new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

	private final ShopadminDeferredOrderCache shopadminDeferredOrderCache;
	private final ShopadminNormalOrderCreateTransactionalRunner shopadminNormalOrderCreateTransactionalRunner;
	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final OrdersPaymentDoPaymentService ordersPaymentDoPaymentService;
	private final OrdersTradeFinishNormalOrderPaySuccessApplyService ordersTradeFinishNormalOrderPaySuccessApplyService;
	private final SupplierOrderSplitOnNormalOrderAddService supplierOrderSplitOnNormalOrderAddService;
	private final NormalOrderAddDispatchPublisher normalOrderAddDispatchPublisher;
	private final WxpaySecapiAftersalesRefundRunner wxpaySecapiAftersalesRefundRunner;
	private final AlipayTradeAftersalesRefundRunner alipayTradeAftersalesRefundRunner;
	private final PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort;

	public ShopadminDeferredOrderService(
			ShopadminDeferredOrderCache shopadminDeferredOrderCache,
			ShopadminNormalOrderCreateTransactionalRunner shopadminNormalOrderCreateTransactionalRunner,
			NormalOrdersMapper normalOrdersMapper,
			TradeMapper tradeMapper,
			OrdersPaymentDoPaymentService ordersPaymentDoPaymentService,
			OrdersTradeFinishNormalOrderPaySuccessApplyService ordersTradeFinishNormalOrderPaySuccessApplyService,
			SupplierOrderSplitOnNormalOrderAddService supplierOrderSplitOnNormalOrderAddService,
			NormalOrderAddDispatchPublisher normalOrderAddDispatchPublisher,
			WxpaySecapiAftersalesRefundRunner wxpaySecapiAftersalesRefundRunner,
			AlipayTradeAftersalesRefundRunner alipayTradeAftersalesRefundRunner,
			PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort) {
		this.shopadminDeferredOrderCache = shopadminDeferredOrderCache;
		this.shopadminNormalOrderCreateTransactionalRunner = shopadminNormalOrderCreateTransactionalRunner;
		this.normalOrdersMapper = normalOrdersMapper;
		this.tradeMapper = tradeMapper;
		this.ordersPaymentDoPaymentService = ordersPaymentDoPaymentService;
		this.ordersTradeFinishNormalOrderPaySuccessApplyService = ordersTradeFinishNormalOrderPaySuccessApplyService;
		this.supplierOrderSplitOnNormalOrderAddService = supplierOrderSplitOnNormalOrderAddService;
		this.normalOrderAddDispatchPublisher = normalOrderAddDispatchPublisher;
		this.wxpaySecapiAftersalesRefundRunner = wxpaySecapiAftersalesRefundRunner;
		this.alipayTradeAftersalesRefundRunner = alipayTradeAftersalesRefundRunner;
		this.paymentSubjectDistributorIdPort = paymentSubjectDistributorIdPort;
	}

	public Map<String, Object> payDeferredOrder(
			long companyId,
			Map<String, Object> authInfo,
			HttpServletRequest request,
			Map<String, Object> mergedInput) {
		long orderId = parseOrderIdLong(mergedInput.get("order_id"));
		ShopadminDeferredOrderSnapshot snap = shopadminDeferredOrderCache.get(companyId, orderId);
		if (snap == null) {
			throw new ResourceException("当前订单不存在");
		}
		if (ShopadminDeferredOrderStatus.REFUNDED.equals(snap.getStatus())) {
			throw new ResourceException("订单已退款");
		}
		String payType = resolveDeferredPayType(mergedInput, snap);
		return shopadminDeferredOrderCache.withLock(
				orderId, () -> executeDeferredPaymentLocked(companyId, authInfo, mergedInput, orderId, snap, payType));
	}

	public void supplementAfterPaymentQuery(long companyId, Map<String, Object> authInfo, Trade trade) {
		if (trade == null || !"SUCCESS".equalsIgnoreCase(safe(trade.getTradeState()))) {
			return;
		}
		long orderId = longVal(trade.getOrderId(), 0L);
		if (orderId <= 0L) {
			return;
		}
		if (findNormalOrder(companyId, orderId) != null) {
			recoverExistingOrder(companyId, orderId);
			return;
		}
		ShopadminDeferredOrderSnapshot snap = shopadminDeferredOrderCache.get(companyId, orderId);
		if (snap == null) {
			refundWithoutSnapshot(companyId, trade, orderId);
			return;
		}
		if (ShopadminDeferredOrderStatus.REFUNDED.equals(snap.getStatus())) {
			return;
		}
		if (ShopadminDeferredOrderStatus.REFUND_FAILED.equals(snap.getStatus())) {
			shopadminDeferredOrderCache.withLock(
					orderId, () -> {
						retryRefundLocked(companyId, snap, trade);
						return null;
					});
			return;
		}
		shopadminDeferredOrderCache.withLock(
				orderId,
				() -> {
					materializeFromSnapshotAfterPaid(companyId, authInfo, snap, trade);
					return null;
				});
	}

	private Map<String, Object> executeDeferredPaymentLocked(
			long companyId,
			Map<String, Object> authInfo,
			Map<String, Object> mergedInput,
			long orderId,
			ShopadminDeferredOrderSnapshot snap,
			String payType) {
		NormalOrders existing = findNormalOrder(companyId, orderId);
		if (existing != null) {
			recoverExistingOrder(companyId, orderId);
			return buildPaidResponseForExistingOrder(companyId, orderId, existing, mergedInput, authInfo);
		}

		Trade paidTrade = findPaidTrade(companyId, orderId);
		if (paidTrade != null) {
			if ("REFUND".equalsIgnoreCase(safe(paidTrade.getTradeState()))) {
				throw new ResourceException("订单已退款");
			}
			materializeFromSnapshotAfterPaid(companyId, authInfo, freshSnap(companyId, orderId, snap), paidTrade);
			return buildPayResultFromTrade(paidTrade, mergedInput, authInfo, payType);
		}

		Map<String, Object> payData = buildPaymentDataFromSnapshot(snap, payType, mergedInput, authInfo, companyId, orderId);
		Map<String, Object> payResult;
		try {
			payResult = ordersPaymentDoPaymentService.doPayment(authInfo, payData, false);
		} catch (RuntimeException e) {
			throw e;
		}
		if (!Boolean.TRUE.equals(payResult.get("pay_status"))) {
			return payResult;
		}
		String tradeId = extractTradeId(payResult);
		Trade trade =
				StringUtils.hasText(tradeId)
						? tradeMapper.selectById(tradeId)
						: findPaidTrade(companyId, orderId);
		if (trade == null) {
			throw new ResourceException("支付单不存在");
		}
		ShopadminDeferredOrderSnapshot current = freshSnap(companyId, orderId, snap);
		current.setTradeId(trade.getTradeId());
		try {
			persistAndFinishAfterPay(companyId, current, trade);
		} catch (ResourceException e) {
			refundAfterBusinessFailure(companyId, current, trade);
			throw e;
		} catch (RuntimeException e) {
			markPaidWaitOrder(companyId, current, trade);
			throw new ResourceException("订单创建失败，请稍后查询");
		}
		enrichPayResult(payResult, payType, orderId, companyId);
		return payResult;
	}

	private void materializeFromSnapshotAfterPaid(
			long companyId, Map<String, Object> authInfo, ShopadminDeferredOrderSnapshot snap, Trade trade) {
		if (findNormalOrder(companyId, snap.getOrderId()) != null) {
			recoverExistingOrder(companyId, snap.getOrderId());
			return;
		}
		if (ShopadminDeferredOrderStatus.REFUNDED.equals(snap.getStatus())) {
			return;
		}
		try {
			persistAndFinishAfterPay(companyId, snap, trade);
		} catch (ResourceException e) {
			refundAfterBusinessFailure(companyId, snap, trade);
			throw e;
		} catch (RuntimeException e) {
			markPaidWaitOrder(companyId, snap, trade);
			throw new ResourceException("订单创建失败，请稍后查询");
		}
	}

	private void persistAndFinishAfterPay(long companyId, ShopadminDeferredOrderSnapshot snap, Trade trade) {
		NormalOrderCreateState state = snap.toCreateState();
		shopadminNormalOrderCreateTransactionalRunner.persistPreparedSnapshot(state);
		applyMarkPayedIfNeeded(trade);
		splitAndPublishOnce(state);
		snap.setStatus(ShopadminDeferredOrderStatus.ORDERED);
		snap.setTradeId(trade.getTradeId());
		shopadminDeferredOrderCache.delete(companyId, snap.getOrderId());
	}

	private void recoverExistingOrder(long companyId, long orderId) {
		NormalOrders order = findNormalOrder(companyId, orderId);
		if (order == null) {
			return;
		}
		if ("NOTPAY".equals(safe(order.getOrderStatus()))) {
			Trade trade = findPaidTrade(companyId, orderId);
			if (trade != null) {
				applyMarkPayedIfNeeded(trade);
			}
		}
		supplierOrderSplitOnNormalOrderAddService.split(companyId, orderId);
	}

	private void splitAndPublishOnce(NormalOrderCreateState state) {
		Map<String, Object> insert = state.getOrdersInsertResult();
		Long companyId = longObject(insert.get("company_id"));
		Long orderId = longObject(insert.get("order_id"));
		if (companyId == null || orderId == null) {
			return;
		}
		supplierOrderSplitOnNormalOrderAddService.split(companyId, orderId);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		Object payType = insert.get("pay_type");
		if (payType == null && state.getOrderData() != null) {
			payType = state.getOrderData().get("pay_type");
		}
		payload.put("pay_type", payType);
		normalOrderAddDispatchPublisher.publish(payload);
	}

	private void markPaidWaitOrder(long companyId, ShopadminDeferredOrderSnapshot snap, Trade trade) {
		snap.setStatus(ShopadminDeferredOrderStatus.PAID_WAIT_ORDER);
		snap.setTradeId(trade.getTradeId());
		shopadminDeferredOrderCache.updateSnapshot(snap, ShopadminDeferredOrderStatus.COMPENSATION_TTL_SECONDS);
	}

	private void refundAfterBusinessFailure(long companyId, ShopadminDeferredOrderSnapshot snap, Trade trade) {
		boolean ok = executeChannelRefund(companyId, snap.getOrderId(), trade);
		if (ok) {
			markTradeRefunded(trade.getTradeId());
			snap.setStatus(ShopadminDeferredOrderStatus.REFUNDED);
			shopadminDeferredOrderCache.updateSnapshot(snap, ShopadminDeferredOrderStatus.COMPENSATION_TTL_SECONDS);
		} else {
			snap.setStatus(ShopadminDeferredOrderStatus.REFUND_FAILED);
			snap.setTradeId(trade.getTradeId());
			shopadminDeferredOrderCache.updateSnapshot(snap, ShopadminDeferredOrderStatus.COMPENSATION_TTL_SECONDS);
		}
	}

	private void retryRefundLocked(long companyId, ShopadminDeferredOrderSnapshot snap, Trade trade) {
		if (findNormalOrder(companyId, snap.getOrderId()) != null) {
			recoverExistingOrder(companyId, snap.getOrderId());
			return;
		}
		boolean ok = executeChannelRefund(companyId, snap.getOrderId(), trade);
		if (ok) {
			markTradeRefunded(trade.getTradeId());
			snap.setStatus(ShopadminDeferredOrderStatus.REFUNDED);
			shopadminDeferredOrderCache.updateSnapshot(snap, ShopadminDeferredOrderStatus.COMPENSATION_TTL_SECONDS);
		}
	}

	private void refundWithoutSnapshot(long companyId, Trade trade, long orderId) {
		if (!"SUCCESS".equalsIgnoreCase(safe(trade.getTradeState()))) {
			return;
		}
		boolean ok = executeChannelRefund(companyId, orderId, trade);
		if (ok) {
			markTradeRefunded(trade.getTradeId());
		}
	}

	private boolean executeChannelRefund(long companyId, long orderId, Trade trade) {
		String payType = safe(trade.getPayType()).toLowerCase(Locale.ROOT);
		int payFee = trade.getPayFee() == null ? 0 : trade.getPayFee();
		long refundBn = orderId;
		if ("pos".equals(payType)) {
			return true;
		}
		long distributorId = longVal(trade.getDistributorId(), 0L);
		long distributorForSetting =
				paymentSubjectDistributorIdPort.resolveActualDistributorId(companyId, distributorId);
		if (payType.startsWith("wxpay")) {
			Map<String, Object> res =
					wxpaySecapiAftersalesRefundRunner.refund(
							companyId,
							distributorForSetting,
							"",
							trade.getTradeId(),
							refundBn,
							payFee,
							payFee);
			return "SUCCESS".equals(String.valueOf(res.get("status")));
		}
		if (payType.startsWith("alipay")) {
			Map<String, Object> res =
					alipayTradeAftersalesRefundRunner.refund(
							companyId, distributorForSetting, trade.getTradeId(), refundBn, payFee, payFee);
			String status = String.valueOf(res.get("status"));
			return "SUCCESS".equals(status) || "PROCESSING".equals(status);
		}
		return false;
	}

	private void markTradeRefunded(String tradeId) {
		tradeMapper.update(
				null,
				new LambdaUpdateWrapper<Trade>()
						.eq(Trade::getTradeId, tradeId)
						.eq(Trade::getTradeState, "SUCCESS")
						.set(Trade::getTradeState, "REFUND"));
	}

	private void applyMarkPayedIfNeeded(Trade trade) {
		try {
			Map<String, Object> tradeRow =
					SNAKE_ROW.convertValue(trade, new TypeReference<Map<String, Object>>() {});
			ordersTradeFinishNormalOrderPaySuccessApplyService.markPayedIfNeeded(tradeRow);
		} catch (IllegalArgumentException e) {
			throw new ResourceException("支付状态更新失败");
		}
	}

	private Trade findPaidTrade(long companyId, long orderId) {
		return tradeMapper.selectOne(
				new LambdaQueryWrapper<Trade>()
						.eq(Trade::getCompanyId, String.valueOf(companyId))
						.eq(Trade::getOrderId, String.valueOf(orderId))
						.in(Trade::getTradeState, "SUCCESS", "REFUND")
						.orderByDesc(Trade::getTradeId)
						.last("LIMIT 1"));
	}

	private NormalOrders findNormalOrder(long companyId, long orderId) {
		return normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.last("LIMIT 1"));
	}

	private ShopadminDeferredOrderSnapshot freshSnap(
			long companyId, long orderId, ShopadminDeferredOrderSnapshot fallback) {
		ShopadminDeferredOrderSnapshot loaded = shopadminDeferredOrderCache.get(companyId, orderId);
		return loaded != null ? loaded : fallback;
	}

	private static String resolveDeferredPayType(Map<String, Object> mergedInput, ShopadminDeferredOrderSnapshot snap) {
		Object authCodeRaw = mergedInput.get("auth_code");
		if (isAuthCodePresent(authCodeRaw)) {
			String ac = String.valueOf(authCodeRaw).trim();
			if (ac.matches("^1[0-5][0-9]{16}$")) {
				return "wxpaypos";
			}
			if (ac.matches("^(25|26|27|28|29|30)[0-9]{14,22}$")) {
				return "alipaypos";
			}
		}
		Object payTypeParam = mergedInput.get("pay_type");
		String payType =
				payTypeParam != null && StringUtils.hasText(String.valueOf(payTypeParam))
						? String.valueOf(payTypeParam).trim().toLowerCase(Locale.ROOT)
						: snap.getPayType();
		if (!DEFERRED_PAY_TYPES.contains(payType)) {
			throw new BadRequestException("暂不支持该支付方式");
		}
		return payType;
	}

	private Map<String, Object> buildPaymentDataFromSnapshot(
			ShopadminDeferredOrderSnapshot snap,
			String payType,
			Map<String, Object> mergedInput,
			Map<String, Object> authInfo,
			long companyId,
			long orderId) {
		Map<String, Object> od = snap.getOrderData();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("order_id", Long.toString(orderId));
		data.put("order_id_numeric", orderId);
		data.put("pay_type", payType);
		data.put("trade_source_type", "normal");
		data.put("user_id", longVal(od.get("user_id"), 0L));
		data.put("distributor_id", longVal(od.get("distributor_id"), 0L));
		data.put("shop_id", longVal(od.get("shop_id"), 0L));
		data.put("mobile", str(od.get("mobile")));
		data.put("open_id", str(mergedInput.get("open_id")));
		data.put("authorizer_appid", str(authInfo.get("woa_appid")));
		data.put("wxa_appid", str(authInfo.get("wxapp_appid")));
		data.put("body", str(od.get("title")));
		data.put("detail", str(od.get("title")));
		data.put("client_ip", clientIpFrom(mergedInput));
		int payFee = snap.getTotalFee();
		data.put("pay_fee", payFee);
		data.put("total_fee", payFee);
		data.put("point_amount", intVal(od.get("point_use"), 0));
		data.put("auto_cancel_time", od.get("auto_cancel_time"));
		if (mergedInput.get("auth_code") != null) {
			data.put("auth_code", String.valueOf(mergedInput.get("auth_code")).trim());
		}
		return data;
	}

	private static Map<String, Object> buildPayResultFromTrade(
			Trade trade, Map<String, Object> mergedInput, Map<String, Object> authInfo, String payType) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("pay_status", true);
		out.put("pay_type", payType);
		out.put("order_id", trade.getOrderId());
		return out;
	}

	private static Map<String, Object> buildPaidResponseForExistingOrder(
			long companyId,
			long orderId,
			NormalOrders order,
			Map<String, Object> mergedInput,
			Map<String, Object> authInfo) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("pay_status", true);
		out.put("order_id", Long.toString(orderId));
		out.put("pay_type", order.getPayType());
		return out;
	}

	private static void enrichPayResult(
			Map<String, Object> payResult, String payType, long orderId, long companyId) {
		payResult.put("pay_type", payType);
		payResult.put("order_id", Long.toString(orderId));
	}

	private static String extractTradeId(Map<String, Object> payResult) {
		Object ti = payResult.get("trade_info");
		if (ti instanceof Map<?, ?> m) {
			Object id = m.get("trade_id");
			if (id != null) {
				return String.valueOf(id);
			}
		}
		return "";
	}

	private static long parseOrderIdLong(Object raw) {
		if (raw == null) {
			throw new BadRequestException("订单号不存在");
		}
		String trimmed = String.valueOf(raw).trim();
		if (trimmed.isEmpty() || "0".equals(trimmed)) {
			throw new BadRequestException("订单号不存在");
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new BadRequestException("订单号不存在");
		}
	}

	private static boolean isAuthCodePresent(Object authCodeRaw) {
		if (authCodeRaw == null) {
			return false;
		}
		String t = String.valueOf(authCodeRaw).trim();
		return !t.isEmpty() && !"0".equals(t);
	}

	private static String clientIpFrom(Map<String, Object> mergedInput) {
		String ip = str(mergedInput.get("client_ip"));
		if (StringUtils.hasText(ip)) {
			return ip;
		}
		return str(mergedInput.get("spbill_create_ip"));
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}

	private static int intVal(Object o, int def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return (int) Double.parseDouble(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static long longVal(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static Long longObject(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
