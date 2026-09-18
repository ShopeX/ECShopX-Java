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

import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import cn.shopex.ecshopx.orders.service.payment.OrderPrescriptionPaymentGuardService;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrderAdminPaymentService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final OrderPrescriptionPaymentGuardService orderPrescriptionPaymentGuardService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OrdersPaymentDoPaymentService ordersPaymentDoPaymentService;

	public NormalOrderAdminPaymentService(
			OrderAssociationsMapper orderAssociationsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			OrderPrescriptionPaymentGuardService orderPrescriptionPaymentGuardService,
			NormalOrdersMapper normalOrdersMapper,
			OrdersPaymentDoPaymentService ordersPaymentDoPaymentService) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.orderPrescriptionPaymentGuardService = orderPrescriptionPaymentGuardService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.ordersPaymentDoPaymentService = ordersPaymentDoPaymentService;
	}

	public Map<String, Object> payment(HttpServletRequest request, Map<String, Object> mergedInput) {
		@SuppressWarnings("unchecked")
		Map<String, Object> authInfo = readAuthInfoMap(request);
		long companyId = readCompanyIdFromAuth(authInfo);

		long orderIdLong;
		String orderIdCanonicalStr;
		try {
			orderIdLong = parseOrderIdLong(mergedInput.get("order_id"));
			orderIdCanonicalStr = Long.toString(orderIdLong);
		} catch (BadRequestException e) {
			throw e;
		}

		Object payTypeParam = mergedInput.get("pay_type");
		Object authCodeRaw = mergedInput.get("auth_code");
		boolean authCodePresent = isAuthCodePresent(authCodeRaw);
		boolean payTypePresent = !isBlankOrZeroLike(payTypeParam);
		if (!authCodePresent && !payTypePresent) {
			throw new BadRequestException("支付方式必填");
		}

		String payType = payTypePresent ? String.valueOf(payTypeParam).trim() : "wxpayh5";
		String orderType = !isBlankOrZeroLike(mergedInput.get("order_type"))
				? String.valueOf(mergedInput.get("order_type")).trim()
				: "normal";

		if (authCodePresent) {
			String ac = String.valueOf(authCodeRaw).trim();
			if (ac.matches("^1[0-5][0-9]{16}$")) {
				payType = "wxpaypos";
			} else if (ac.matches("^(25|26|27|28|29|30)[0-9]{14,22}$")) {
				payType = "alipaypos";
			}
		}

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdLong)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("当前订单不存在");
		}
		String assocOrderType = assoc.getOrderType() == null ? "" : assoc.getOrderType().trim();
		if ("supplier_order".equalsIgnoreCase(assocOrderType)) {
			throw new ResourceException("暂不支持该订单类型的支付");
		}
		String orderStatus = assoc.getOrderStatus() == null ? "" : assoc.getOrderStatus().trim();
		if (!"NOTPAY".equals(orderStatus) && !"PART_PAYMENT".equals(orderStatus)) {
			throw new ResourceException("当前订单不需要支付");
		}

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if (effective == null || effective.isEmpty() || !supportsPaymentBundleForPayment(effective)) {
			throw new ResourceException("无此类型订单！");
		}

		Map<String, Object> bundle;
		try {
			bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdCanonicalStr, false);
		} catch (ResourceException e) {
			throw new ResourceException("当前订单不存在");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo =
				bundle.get("orderInfo") instanceof Map<?, ?> m
						? (Map<String, Object>) m
						: null;
		if (orderInfo == null) {
			throw new ResourceException("当前订单不存在");
		}

		if (parseFeeStringToInt(orderInfo.get("total_fee")) == 0 && intFrom(orderInfo.get("point")) > 0) {
			payType = "point";
		}

		updatePayTypeIfApplicable(companyId, orderIdLong, effective, payType, mergedInput.get("pay_channel"));

		orderPrescriptionPaymentGuardService.assertOrderPrescriptionAllowsPay(companyId, orderIdCanonicalStr, orderInfo);

		if ("deposit".equals(payType)) {
			Object card = authInfo.get("user_card_code");
			if (!StringUtils.hasText(card == null ? null : String.valueOf(card).trim())) {
				throw new BadRequestException("请先登录");
			}
		}
		if ("alipaymini".equals(payType)) {
			Object aliUid = authInfo.get("alipay_user_id");
			if (!StringUtils.hasText(aliUid == null ? null : String.valueOf(aliUid).trim())) {
				throw new BadRequestException("请在支付宝小程序授权登录");
			}
		}

		Map<String, Object> data = buildPaymentData(mergedInput, authInfo, companyId, orderIdCanonicalStr, orderIdLong, orderType, payType, orderInfo);
		if (authCodePresent) {
			data.put("auth_code", String.valueOf(authCodeRaw).trim());
		}

		Map<String, Object> payResult = ordersPaymentDoPaymentService.doPayment(authInfo, data, false);

		orderInfo.put("pay_type", payType);
		payResult.put("pay_type", payType);
		payResult.put("order_id", orderIdCanonicalStr);
		payResult.put("team_id", orderInfo.get("team_id"));
		payResult.put("order_info", orderInfo);

		return payResult;
	}

	private void updatePayTypeIfApplicable(
			long companyId, long orderIdLong, String effective, String payType, Object payChannelObj) {
		if (!supportsPaymentBundleForPayment(effective)) {
			return;
		}
		var uw = new LambdaUpdateWrapper<NormalOrders>()
				.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderIdLong)
				.set(NormalOrders::getPayType, payType);
		if (payChannelObj != null) {
			String pc = String.valueOf(payChannelObj).trim();
			if (!pc.isEmpty() && !"0".equals(pc)) {
				uw.set(NormalOrders::getPayChannel, pc);
			}
		}
		normalOrdersMapper.update(null, uw);
	}

	private Map<String, Object> buildPaymentData(
			Map<String, Object> mergedInput,
			Map<String, Object> authInfo,
			long companyId,
			String orderIdCanonicalStr,
			long orderIdLong,
			String orderType,
			String payType,
			Map<String, Object> orderInfo) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("order_id", orderIdCanonicalStr);
		data.put("order_id_numeric", orderIdLong);
		data.put("pay_type", payType);
		data.put("trade_source_type", orderType);
		Object uid = orderInfo.get("user_id");
		data.put("user_id", uid instanceof Number ? ((Number) uid).longValue() : longOrZero(uid));
		Object dist = orderInfo.get("distributor_id");
		data.put("distributor_id", dist instanceof Number ? ((Number) dist).longValue() : longOrZero(dist));
		Object shop = orderInfo.get("shop_id");
		data.put("shop_id", shop instanceof Number ? ((Number) shop).longValue() : longOrZero(shop));
		data.put("mobile", str(orderInfo.get("mobile")));
		String openId = str(authInfo.get("open_id"));
		if (!StringUtils.hasText(openId)) {
			openId = str(mergedInput.get("open_id"));
		}
		data.put("open_id", openId);
		data.put("authorizer_appid", str(authInfo.get("woa_appid")));
		data.put("wxa_appid", str(authInfo.get("wxapp_appid")));
		data.put("body", str(orderInfo.get("title")));
		data.put("detail", str(orderInfo.get("title")));
		data.put("return_url", str(mergedInput.get("return_url")));
		data.put("source", str(mergedInput.get("source")));
		data.put("client_ip", clientIpFrom(mergedInput));
		data.put("remark", str(mergedInput.get("remark")));

		int payFee = intFrom(orderInfo.get("cny_fee"));
		if (payFee <= 0) {
			payFee = parseFeeStringToInt(orderInfo.get("total_fee"));
		}
		int pointAmt = intFrom(orderInfo.get("point"));
		if ("point".equals(payType) && pointAmt > 0) {
			payFee = pointAmt;
		}
		data.put("pay_fee", payFee);
		data.put("total_fee", parseFeeStringToInt(orderInfo.get("total_fee")));
		data.put("fee_rate", orderInfo.get("fee_rate"));
		data.put("fee_type", str(orderInfo.get("fee_type")));
		data.put("fee_symbol", str(orderInfo.get("fee_symbol")));

		data.put("point_amount", pointAmt);

		int ps = intFrom(orderInfo.get("prescription_status"));
		data.put("prescription_requires_full_pay", ps != 0);
		data.put("auto_cancel_time", orderInfo.get("auto_cancel_time"));

		if (payChannelFromRequest(payType)) {
			Object pc = mergedInput.get("pay_channel");
			if (pc != null) {
				data.put("pay_channel", String.valueOf(pc).trim());
			} else {
				data.put("pay_channel", "");
			}
		} else {
			data.put("pay_channel", "");
		}

		return data;
	}

	private static String clientIpFrom(Map<String, Object> mergedInput) {
		String ip = str(mergedInput.get("client_ip"));
		if (StringUtils.hasText(ip)) {
			return ip;
		}
		return str(mergedInput.get("spbill_create_ip"));
	}

	private static boolean payChannelFromRequest(String payType) {
		String p = payType == null ? "" : payType.toLowerCase(Locale.ROOT);
		return "adapay".equals(p)
				|| "bspay".equals(p)
				|| "offline_pay".equals(p)
				|| "paypal".equals(p);
	}

	private static long longOrZero(Object o) {
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

	private static int intFrom(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return (int) Double.parseDouble(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int parseFeeStringToInt(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return new java.math.BigDecimal(String.valueOf(raw).trim())
					.setScale(0, java.math.RoundingMode.HALF_UP)
					.intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static boolean isBlankOrZeroLike(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		String t = String.valueOf(raw).trim();
		return t.isEmpty() || "0".equals(t);
	}

	private static boolean isAuthCodePresent(Object authCodeRaw) {
		if (authCodeRaw == null) {
			return false;
		}
		String t = String.valueOf(authCodeRaw).trim();
		return !t.isEmpty() && !"0".equals(t);
	}

	private static long parseOrderIdLong(Object raw) {
		if (raw == null) {
			throw new BadRequestException("订单号不存在");
		}
		if (raw instanceof CharSequence cs && cs.toString().trim().isEmpty()) {
			throw new BadRequestException("订单号不存在");
		}
		if (raw instanceof Number n && n.longValue() == 0L) {
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

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readAuthInfoMap(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		return (Map<String, Object>) attr;
	}

	private static long readCompanyIdFromAuth(Map<String, Object> authInfo) {
		Object companyIdObj = authInfo.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static boolean supportsPaymentBundleForPayment(String effective) {
		if (effective == null || effective.isEmpty()) {
			return false;
		}
		if ("membercard".equals(effective) || "supplier_order".equals(effective)) {
			return false;
		}
		if ("normal".equals(effective)
				|| "normal_shopadmin".equals(effective)
				|| "service".equals(effective)
				|| effective.startsWith("service_")
				|| "bargain".equals(effective)
				|| "normal_bargain".equals(effective)) {
			return true;
		}
		return effective.startsWith("normal_");
	}
}
