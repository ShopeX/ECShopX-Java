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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.dispatch.SendPayOrdersRemindJobDispatchPublisher;
import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.distribution.DistributorWhiteListCheckUserValidPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.service.epidemic.OrderEpidemicRegisterValidateService;
import cn.shopex.ecshopx.orders.service.epidemic.OrderEpidemicRegisterWxappAfterOrderService;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOrderCreateService {

	private final WxappOrderSourceFromResolveService wxappOrderSourceFromResolveService;

	private final WxappOrderClientIpResolveService wxappOrderClientIpResolveService;

	private final OrderEpidemicRegisterValidateService orderEpidemicRegisterValidateService;

	private final OrderEpidemicRegisterWxappAfterOrderService orderEpidemicRegisterWxappAfterOrderService;

	private final WxappOrderSalespersonResolveService wxappOrderSalespersonResolveService;

	private final WxappOrderTypeRegistry wxappOrderTypeRegistry;

	private final MemberAccountService memberAccountService;

	private final OrdersPaymentDoPaymentService ordersPaymentDoPaymentService;

	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;

	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	private final DistributorWhiteListCheckUserValidPort distributorWhiteListCheckUserValidPort;

	private final ObjectMapper objectMapper;

	private final SendPayOrdersRemindJobDispatchPublisher sendPayOrdersRemindJobDispatchPublisher;

	private final OrderProcessLogPublishPort orderProcessLogPublishPort;

	public WxappOrderCreateService(
			WxappOrderSourceFromResolveService wxappOrderSourceFromResolveService,
			WxappOrderClientIpResolveService wxappOrderClientIpResolveService,
			OrderEpidemicRegisterValidateService orderEpidemicRegisterValidateService,
			OrderEpidemicRegisterWxappAfterOrderService orderEpidemicRegisterWxappAfterOrderService,
			WxappOrderSalespersonResolveService wxappOrderSalespersonResolveService,
			WxappOrderTypeRegistry wxappOrderTypeRegistry,
			MemberAccountService memberAccountService,
			OrdersPaymentDoPaymentService ordersPaymentDoPaymentService,
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			DistributorWhiteListCheckUserValidPort distributorWhiteListCheckUserValidPort,
			ObjectMapper objectMapper,
			SendPayOrdersRemindJobDispatchPublisher sendPayOrdersRemindJobDispatchPublisher,
			OrderProcessLogPublishPort orderProcessLogPublishPort) {
		this.wxappOrderSourceFromResolveService = wxappOrderSourceFromResolveService;
		this.wxappOrderClientIpResolveService = wxappOrderClientIpResolveService;
		this.orderEpidemicRegisterValidateService = orderEpidemicRegisterValidateService;
		this.orderEpidemicRegisterWxappAfterOrderService = orderEpidemicRegisterWxappAfterOrderService;
		this.wxappOrderSalespersonResolveService = wxappOrderSalespersonResolveService;
		this.wxappOrderTypeRegistry = wxappOrderTypeRegistry;
		this.memberAccountService = memberAccountService;
		this.ordersPaymentDoPaymentService = ordersPaymentDoPaymentService;
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.distributorWhiteListCheckUserValidPort = distributorWhiteListCheckUserValidPort;
		this.objectMapper = objectMapper;
		this.sendPayOrdersRemindJobDispatchPublisher = sendPayOrdersRemindJobDispatchPublisher;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
	}

	public Map<String, Object> createOrder(
			HttpServletRequest request, Map<String, Object> mergedParams, Map<String, Object> sessionAuth) {
		Map<String, Object> params = WxappOrderParamMergeSupport.applyDefaultsAndAuth(mergedParams, sessionAuth);
		long companyId = longVal(params.get("company_id"));
		long userId = longVal(params.get("user_id"));
		params.put("source_from", wxappOrderSourceFromResolveService.resolve(companyId, request, params));
		params.put("client_ip", wxappOrderClientIpResolveService.resolve(request));

		Map<String, Object> epidemicPayload = null;
		Object eraw = params.get("epidemic_register_info");
		if (eraw != null && StringUtils.hasText(eraw.toString())) {
			try {
				epidemicPayload = objectMapper.readValue(eraw.toString().trim(), new TypeReference<>() {});
			} catch (Exception e) {
				throw new BadRequestException("疫情登记信息格式错误");
			}
			orderEpidemicRegisterValidateService.validate(epidemicPayload);
		}

		String workUserId = stringVal(params.get("work_userid"));
		if (StringUtils.hasText(workUserId)) {
			wxappOrderSalespersonResolveService.applySalesmanId(params, workUserId);
		} else {
			params.putIfAbsent("salesman_id", 0L);
		}

		WxappOrderCreateContext ctx = new WxappOrderCreateContext();
		ctx.setRequest(request);
		ctx.setCompanyId(companyId);
		ctx.setUserId(userId);
		ctx.setSessionAuth(sessionAuth);
		ctx.getParams().putAll(params);
		ctx.setEpidemicRegisterPayload(epidemicPayload);

		Map<String, Object> result = wxappOrderTypeRegistry.create(ctx);

		if (epidemicPayload != null) {
			orderEpidemicRegisterWxappAfterOrderService.run(epidemicPayload, result);
		}

		publishWxappOrderCreatedOrderProcessLog(companyId, userId, result, params);

		String orderType = stringVal(params.get("order_type")).toLowerCase(Locale.ROOT);
		if ("normal_drug".equals(orderType)) {
			hydrateCurrency(companyId, result);
			publishPayOrdersRemindIfApplicable(companyId, userId, result, params, sessionAuth);
			return result;
		}

		hydrateCurrency(companyId, result);
		publishPayOrdersRemindIfApplicable(companyId, userId, result, params, sessionAuth);

		Map<String, Object> authInfo = buildAuthInfo(sessionAuth, userId, companyId);

		long distributorId = longVal(params.get("distributor_id"));
		Map<String, Object> distributorInfo = Map.of();
		if (distributorId > 0L) {
			distributorInfo = distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(companyId, distributorId);
		}

		String payType = stringVal(params.get("pay_type")).toLowerCase(Locale.ROOT);
		if (!StringUtils.hasText(payType)) {
			payType = stringVal(result.get("pay_type")).toLowerCase(Locale.ROOT);
		}
		if ("prepaid_point".equals(stringVal(result.get("purchase_mode")))
				|| "prepaid_point".equals(stringVal(result.get("pay_type")))) {
			payType = "prepaid_point";
		}
		if ("adapay".equals(payType)) {
			String pc = stringVal(params.get("pay_channel"));
			if (!StringUtils.hasText(pc)) {
				throw new BadRequestException("adapay支付方式  pay_channel必传");
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("user_id", userId);
		data.put("total_fee", parseFeeToInt(result.get("total_fee")));
		data.put("detail", stringVal(result.get("title")));
		data.put("body", stringVal(result.get("title")));
		data.put("order_id", String.valueOf(result.get("order_id")));
		data.put("order_id_numeric", longVal(result.get("order_id")));
		data.put("open_id", stringVal(authInfo.get("open_id")));
		data.put("wxa_appid", stringVal(authInfo.get("wxapp_appid")));
		data.put("mobile", stringVal(params.get("mobile")));
		data.put("pay_type", payType);
		int payFee =
				"point".equals(payType)
						? parseFeeToInt(result.get("point"))
						: parseFeeToInt(
								result.get("prepaid_payable_fee") != null
												&& parseFeeToInt(result.get("prepaid_payable_fee")) > 0
										? result.get("prepaid_payable_fee")
										: result.get("total_fee"));
		data.put("pay_fee", payFee);
		data.put("discount_fee", parseFeeToInt(result.get("discount_fee")));
		data.put("discount_info", result.get("discount_info"));
		data.put("fee_rate", result.get("fee_rate"));
		data.put("fee_type", stringVal(result.get("fee_type")));
		data.put("fee_symbol", stringVal(result.get("fee_symbol")));
		data.put("shop_id", longVal(result.get("shop_id")));
		data.put("distributor_id", longVal(result.get("distributor_id")));
		data.put("trade_source_type", orderType);
		data.put("return_url", stringVal(params.get("return_url")));
		data.put("distributor_info", distributorInfo);
		int pointAmt = parseFeeToInt(result.get("point"));
		data.put("point", pointAmt);
		data.put("point_amount", pointAmt);
		int ps = parseFeeToInt(result.get("prescription_status"));
		data.put("is_create_prescription_order", ps > 0 ? 1 : 0);
		data.put("client_ip", stringVal(params.get("client_ip")));
		data.put("pay_channel", stringVal(params.get("pay_channel")));

		if ("deposit".equals(payType)) {
			data.put("member_card_code", stringVal(authInfo.get("user_card_code")));
		}
		if ("alipaymini".equals(payType)) {
			data.put("alipay_user_id", stringVal(authInfo.get("alipay_user_id")));
		}

		Map<String, Object> payResult = ordersPaymentDoPaymentService.doPayment(authInfo, data, false);
		payResult.put("team_id", result.get("team_id"));
		payResult.put("order_created", result.get("create_time"));
		return payResult;
	}

	public Map<String, Object> createNewOrder(
			HttpServletRequest request,
			Map<String, Object> mergedParams,
			Map<String, Object> h5AuthClaims) {
		Map<String, Object> params =
				WxappOrderParamMergeSupport.applyDefaultsAndAuthForOrderNew(mergedParams, h5AuthClaims);
		long companyId = longVal(params.get("company_id"));
		long userId = longVal(params.get("user_id"));
		params.put("source_from", wxappOrderSourceFromResolveService.resolve(companyId, request, params));
		params.put("client_ip", wxappOrderClientIpResolveService.resolve(request));

		Object distributorIdRaw = mergedParams.get("distributor_id");
		if (distributorIdRaw != null) {
			String s = distributorIdRaw.toString().trim();
			if (!s.isEmpty() && !"0".equals(s) && longVal(distributorIdRaw) != 0L) {
				if (!distributorWhiteListCheckUserValidPort.checkUserValidCommon(
						longVal(distributorIdRaw), userId, companyId)) {
					throw new ResourceException("非本店铺会员无法下单");
				}
			}
		}

		String workUserId = stringVal(params.get("work_userid"));
		if (StringUtils.hasText(workUserId)) {
			wxappOrderSalespersonResolveService.applySalesmanId(params, workUserId);
		} else {
			params.putIfAbsent("salesman_id", 0L);
		}

		if (mergedParams.containsKey("promoter_user_id")) {
			long promoter = longVal(mergedParams.get("promoter_user_id"));
			if (promoter != 0L) {
				long authUid = longVal(h5AuthClaims != null ? h5AuthClaims.get("user_id") : null);
				if (authUid == promoter) {
					params.put("user_id", longVal(params.get("buy_user_id")));
					params.put("order_source", "salesperson");
					Object rm = params.get("receiver_mobile");
					params.put("mobile", rm == null ? 0L : longVal(rm));
					params.put("salesman_id", longVal(params.get("promoter_user_id")));
				}
			}
		}

		long orderUserId = longVal(params.get("user_id"));
		WxappOrderCreateContext ctx = new WxappOrderCreateContext();
		ctx.setRequest(request);
		ctx.setCompanyId(companyId);
		ctx.setUserId(orderUserId);
		ctx.setSessionAuth(h5AuthClaims);
		ctx.getParams().putAll(params);
		ctx.setEpidemicRegisterPayload(null);

		Map<String, Object> result = wxappOrderTypeRegistry.create(ctx);

		hydrateCurrency(companyId, result);
		publishWxappOrderCreatedOrderProcessLog(companyId, orderUserId, result, params);
		publishPayOrdersRemindIfApplicable(companyId, orderUserId, result, params, h5AuthClaims);

		Map<String, Object> authInfo = buildPaymentAuthInfoForOrderNew(h5AuthClaims, orderUserId, companyId);

		// Auto-pay gate follows PHP: only when the client intentionally chose pay_type=point.
		// Full 积分抵扣 may rewrite persisted order pay_type to point, but must not auto-pay here —
		// otherwise the subsequent /payment call (still offline_pay/wxpay) hits「当前订单不需要支付」.
		String requestPayType = stringVal(params.get("pay_type")).toLowerCase(Locale.ROOT);
		String effectivePayType = stringVal(result.get("pay_type"));
		if (!StringUtils.hasText(effectivePayType)) {
			effectivePayType = requestPayType;
		}
		effectivePayType = effectivePayType.toLowerCase(Locale.ROOT);
		if (parseFeeToInt(result.get("total_fee")) == 0
				&& "point".equals(requestPayType)
				&& parseFeeToInt(result.get("prescription_status")) == 0) {
			long distributorId = longVal(params.get("distributor_id"));
			Map<String, Object> distributorInfo = Map.of();
			if (distributorId > 0L) {
				distributorInfo =
						distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(
								companyId, distributorId);
			}

			String orderTypeKey = stringVal(params.get("order_type")).toLowerCase(Locale.ROOT);
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("company_id", companyId);
			data.put("user_id", longVal(params.get("user_id")));
			data.put("total_fee", parseFeeToInt(result.get("total_fee")));
			data.put("detail", stringVal(result.get("title")));
			data.put("body", stringVal(result.get("title")));
			data.put("order_id", String.valueOf(result.get("order_id")));
			data.put("order_id_numeric", longVal(result.get("order_id")));
			data.put("open_id", stringVal(authInfo.get("open_id")));
			data.put("wxa_appid", stringVal(authInfo.get("wxapp_appid")));
			data.put("mobile", stringVal(params.get("mobile")));
			data.put("pay_type", effectivePayType);
			int payFee =
					"point".equals(effectivePayType)
							? parseFeeToInt(result.get("point"))
							: parseFeeToInt(result.get("total_fee"));
			data.put("pay_fee", payFee);
			data.put("discount_fee", parseFeeToInt(result.get("discount_fee")));
			data.put("discount_info", result.get("discount_info"));
			data.put("fee_rate", result.get("fee_rate"));
			data.put("fee_type", stringVal(result.get("fee_type")));
			data.put("fee_symbol", stringVal(result.get("fee_symbol")));
			data.put("shop_id", longVal(result.get("shop_id")));
			data.put("distributor_id", longVal(result.get("distributor_id")));
			data.put("trade_source_type", orderTypeKey);
			data.put("return_url", stringVal(params.get("return_url")));
			data.put("distributor_info", distributorInfo);
			int pointAmt = parseFeeToInt(result.get("point"));
			data.put("point", pointAmt);
			data.put("point_amount", pointAmt);
			int ps = parseFeeToInt(result.get("prescription_status"));
			data.put("is_create_prescription_order", ps > 0 ? 1 : 0);
			data.put("pay_channel", stringVal(params.get("pay_channel")));

			Map<String, Object> payReturn = ordersPaymentDoPaymentService.doPayment(authInfo, data, false);
			if ("paypal".equals(stringVal(data.get("pay_type")).toLowerCase(Locale.ROOT))) {
				return payReturn;
			}
		}

		if (intInput(params.get("isSalesmanPage"), 0) == 1
				&& longVal(params.get("promoter_user_id")) > 0L
				&& "offline_pay".equals(stringVal(params.get("pay_type")).toLowerCase(Locale.ROOT))) {
			Map<String, Object> offlinePayData = buildSalesmanOfflinePayData(params, result, companyId);
			ordersPaymentDoPaymentService.doPayment(authInfo, offlinePayData, false);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("order_id", String.valueOf(result.get("order_id")));
		String outPayType = stringVal(result.get("pay_type"));
		if (!StringUtils.hasText(outPayType)) {
			outPayType = stringVal(params.get("pay_type"));
		}
		out.put("pay_type", outPayType);
		out.put("order_type", stringVal(params.get("order_type")));
		out.put("team_id", result.get("team_id"));
		out.put("total_fee", result.get("total_fee"));
		return out;
	}

	private Map<String, Object> buildSalesmanOfflinePayData(
			Map<String, Object> params, Map<String, Object> result, long companyId) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", longVal(params.get("company_id")));
		data.put("user_id", longVal(params.get("buy_user_id")));
		data.put("total_fee", result.get("total_fee"));
		data.put("detail", stringVal(result.get("title")));
		data.put("order_id", result.get("order_id"));
		data.put("body", stringVal(result.get("title")));
		data.put("open_id", "");
		long buyerId = longVal(params.get("buy_user_id"));
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(buyerId, companyId);
		data.put("wxa_appid", stringVal(memberInfo.get("wxapp_appid")));
		data.put("mobile", stringVal(memberInfo.get("mobile")));
		data.put("pay_type", params.get("pay_type"));
		Object payFee =
				"point".equals(stringVal(params.get("pay_type")))
						? result.get("point")
						: result.get("total_fee");
		data.put("pay_fee", payFee);
		data.put("discount_fee", result.get("discount_fee"));
		data.put("discount_info", result.get("discount_info"));
		data.put("fee_rate", result.get("fee_rate"));
		data.put("fee_type", result.get("fee_type"));
		data.put("fee_symbol", result.get("fee_symbol"));
		data.put("shop_id", longVal(result.get("shop_id")));
		if (result.containsKey("distributor_id")) {
			data.put("distributor_id", result.get("distributor_id"));
		} else {
			data.put("distributor_id", "");
		}
		data.put("trade_source_type", params.get("order_type"));
		data.put("return_url", stringVal(params.get("return_url")));
		Map<String, Object> distributorInfo = Map.of();
		if (longVal(result.get("distributor_id")) != 0L) {
			distributorInfo =
					distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(
							companyId, longVal(result.get("distributor_id")));
		}
		data.put("distributor_info", distributorInfo);
		int pointAmt = parseFeeToInt(result.get("point"));
		data.put("point", pointAmt);
		data.put("point_amount", pointAmt);
		data.put("order_id_numeric", longVal(result.get("order_id")));
		return data;
	}

	private void publishWxappOrderCreatedOrderProcessLog(
			long companyId,
			long operatorUserId,
			Map<String, Object> orderCreateResult,
			Map<String, Object> requestParams) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		Object rawOrderId = orderCreateResult.get("order_id");
		payload.put("order_id", rawOrderId == null ? null : String.valueOf(rawOrderId));
		payload.put("company_id", String.valueOf(companyId));
		payload.put("operator_type", "user");
		payload.put("is_show", Boolean.TRUE);
		payload.put("operator_id", operatorUserId);
		String orderClass = stringVal(requestParams.get("order_class"));
		String remarks;
		String detailMsg;
		if ("excard".equals(orderClass)) {
			remarks = "订单核销";
			detailMsg = "订单核销成功";
		} else {
			remarks = "订单创建";
			detailMsg = "订单创建";
		}
		String orderIdStr = rawOrderId == null ? "" : String.valueOf(rawOrderId);
		payload.put("remarks", remarks);
		payload.put("detail", "订单号：" + orderIdStr + "，" + detailMsg);
		payload.put("params", new LinkedHashMap<>(requestParams));
		orderProcessLogPublishPort.publish(payload);
	}

	private void publishPayOrdersRemindIfApplicable(
			long companyId,
			long userId,
			Map<String, Object> result,
			Map<String, Object> params,
			Map<String, Object> sessionAuth) {
		Map<String, Object> authInfo = buildAuthInfo(sessionAuth, userId, companyId);
		LinkedHashMap<String, Object> orderData = new LinkedHashMap<>();
		orderData.put("order_id", result.get("order_id"));
		orderData.put("company_id", companyId);
		orderData.put("user_id", userId);
		orderData.put("wxa_appid", stringVal(authInfo.get("wxapp_appid")));
		orderData.put("open_id", stringVal(authInfo.get("open_id")));
		orderData.put("total_fee", result.get("total_fee"));
		orderData.put("title", result.get("title"));
		orderData.put("fee_symbol", result.get("fee_symbol"));
		orderData.put("fee_rate", result.get("fee_rate"));
		if (result.get("create_time") != null) {
			orderData.put("create_time", result.get("create_time"));
		}
		if (params.get("auto_cancel_time") != null) {
			orderData.put("auto_cancel_time", params.get("auto_cancel_time"));
		} else if (result.get("auto_cancel_time") != null) {
			orderData.put("auto_cancel_time", result.get("auto_cancel_time"));
		}
		sendPayOrdersRemindJobDispatchPublisher.publish(orderData);
	}

	private Map<String, Object> buildAuthInfo(Map<String, Object> sessionAuth, long userId, long companyId) {
		LinkedHashMap<String, Object> authInfo = new LinkedHashMap<>();
		if (sessionAuth != null) {
			authInfo.putAll(sessionAuth);
		}
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		for (Map.Entry<String, Object> e : member.entrySet()) {
			authInfo.putIfAbsent(e.getKey(), e.getValue());
		}
		return authInfo;
	}

	private Map<String, Object> buildPaymentAuthInfoForOrderNew(
			Map<String, Object> sessionAuth, long orderUserId, long companyId) {
		if (sessionAuth == null || longVal(sessionAuth.get("user_id")) == orderUserId) {
			return buildAuthInfo(sessionAuth, orderUserId, companyId);
		}
		LinkedHashMap<String, Object> authInfo = new LinkedHashMap<>();
		authInfo.putAll(sessionAuth);
		Map<String, Object> member = memberAccountService.getMemberInfo(orderUserId, companyId);
		for (Map.Entry<String, Object> e : member.entrySet()) {
			authInfo.put(e.getKey(), e.getValue());
		}
		return authInfo;
	}

	private void hydrateCurrency(long companyId, Map<String, Object> result) {
		if (result.containsKey("fee_type") && StringUtils.hasText(stringVal(result.get("fee_type")))) {
			return;
		}
		CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
		result.put("fee_type", cur.getCurrency() != null ? cur.getCurrency() : "CNY");
		result.put("fee_rate", cur.getRate() != null ? cur.getRate() : 1.0);
		result.put("fee_symbol", cur.getSymbol() != null ? cur.getSymbol() : "￥");
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseFeeToInt(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			return (int) Math.min(Math.max(x, 0L), Integer.MAX_VALUE);
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			long x = Long.parseLong(s);
			return (int) Math.min(Math.max(x, 0L), Integer.MAX_VALUE);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int intInput(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return def;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
