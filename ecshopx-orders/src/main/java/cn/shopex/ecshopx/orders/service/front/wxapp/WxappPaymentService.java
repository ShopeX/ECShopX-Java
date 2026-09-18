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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.deposit.service.DepositTradeWxappPaymentService;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import cn.shopex.ecshopx.orders.service.payment.OrderPrescriptionPaymentGuardService;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentDoPaymentService;
import cn.shopex.ecshopx.orders.service.payment.OrdersPaymentTradeQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappPaymentService {

	private final DepositTradeWxappPaymentService depositTradeWxappPaymentService;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final OrderPrescriptionPaymentGuardService orderPrescriptionPaymentGuardService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	private final OrdersPaymentDoPaymentService ordersPaymentDoPaymentService;
	private final OrdersPaymentTradeQueryService ordersPaymentTradeQueryService;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final WechatUsersMapper wechatUsersMapper;

	public WxappPaymentService(
			DepositTradeWxappPaymentService depositTradeWxappPaymentService,
			OrderAssociationsMapper orderAssociationsMapper,
			OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			OrderPrescriptionPaymentGuardService orderPrescriptionPaymentGuardService,
			NormalOrdersMapper normalOrdersMapper,
			DistributionDistributorPeekMapper distributionDistributorPeekMapper,
			OrdersPaymentDoPaymentService ordersPaymentDoPaymentService,
			OrdersPaymentTradeQueryService ordersPaymentTradeQueryService,
			MembersAssociationsMapper membersAssociationsMapper,
			WechatUsersMapper wechatUsersMapper) {
		this.depositTradeWxappPaymentService = depositTradeWxappPaymentService;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.orderAssociationEffectiveTypeService = orderAssociationEffectiveTypeService;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.orderPrescriptionPaymentGuardService = orderPrescriptionPaymentGuardService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.distributionDistributorPeekMapper = distributionDistributorPeekMapper;
		this.ordersPaymentDoPaymentService = ordersPaymentDoPaymentService;
		this.ordersPaymentTradeQueryService = ordersPaymentTradeQueryService;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.wechatUsersMapper = wechatUsersMapper;
	}

	public Map<String, Object> doPayment(HttpServletRequest request, Map<String, Object> query) {
		LinkedHashMap<String, Object> auth = resolveAuthForPaymentConfig(request);
		long companyId = readCompanyIdStrict(auth);

		Object mcc = query.get("member_card_code");
		Object ucc = query.get("user_card_code");
		String card = null;
		if (mcc != null && StringUtils.hasText(String.valueOf(mcc).trim())) {
			card = String.valueOf(mcc).trim();
		} else if (ucc != null && StringUtils.hasText(String.valueOf(ucc).trim())) {
			card = String.valueOf(ucc).trim();
		}
		if (card != null) {
			auth.put("user_card_code", card);
		}

		if (isMissingOrderId(query.get("order_id"))) {
			String poi = str(query.get("poiid"));
			String sid = str(query.get("shop_id"));
			if (!StringUtils.hasText(poi) || !StringUtils.hasText(sid)) {
				throw new BadRequestException("请选择消费门店");
			}
		}

		if (isMissingOrderId(query.get("order_id")) && parseTotalFeeInt(query.get("total_fee")) <= 0) {
			throw new BadRequestException("请输入正确的消费金额");
		}

		enrichAuthWechatMiniProgramContext(auth, companyId);

		String openId = str(auth.get("open_id"));
		String wxappAppid = str(auth.get("wxapp_appid"));
		if (!StringUtils.hasText(openId) || !StringUtils.hasText(wxappAppid)) {
			throw new BadRequestException("缺少会员open_id或者小程序appid参数");
		}

		long resolvedShopId = longOrZero(query.get("shop_id"));
		if (hasTruthyDistributorId(query.get("distributor_id"))) {
			Object rawDist = query.get("distributor_id");
			long distId = Long.parseLong(String.valueOf(rawDist).trim());
			DistributionDistributorPeek row =
					distributionDistributorPeekMapper.selectOne(
							new LambdaQueryWrapper<DistributionDistributorPeek>()
									.eq(DistributionDistributorPeek::getCompanyId, companyId)
									.eq(DistributionDistributorPeek::getDistributorId, distId)
									.eq(DistributionDistributorPeek::getIsValid, "true")
									.last("LIMIT 1"));
			if (row == null) {
				throw new ResourceException("您所选的店铺已关闭");
			}
			if (row.getKuaizhenStoreId() != null && row.getKuaizhenStoreId() != 0L) {
				resolvedShopId = row.getKuaizhenStoreId();
			}
		}

		String payTypeRaw = str(query.get("pay_type")).toLowerCase(Locale.ROOT);
		if (!"wxpay".equals(payTypeRaw) && !"hfpay".equals(payTypeRaw) && !"deposit".equals(payTypeRaw)) {
			throw new BadRequestException("请选择支付方式");
		}

		boolean isDiscount = true;
		Map<String, Object> data = new LinkedHashMap<>();
		if (!isMissingOrderId(query.get("order_id"))) {
			long orderIdLong = parseOrderIdLong(query.get("order_id"));
			OrderAssociations assoc =
					orderAssociationsMapper.selectOne(
							new LambdaQueryWrapper<OrderAssociations>()
									.eq(OrderAssociations::getCompanyId, companyId)
									.eq(OrderAssociations::getOrderId, orderIdLong)
									.last("LIMIT 1"));
			if (assoc == null) {
				throw new ResourceException("当前订单不存在");
			}
			String st = assoc.getOrderStatus() == null ? "" : assoc.getOrderStatus().trim();
			if (!"NOTPAY".equals(st) && !"PART_PAYMENT".equals(st)) {
				throw new ResourceException("当前订单不需要支付");
			}
			NormalOrders no =
					normalOrdersMapper.selectOne(
							new LambdaQueryWrapper<NormalOrders>()
									.eq(NormalOrders::getCompanyId, companyId)
									.eq(NormalOrders::getOrderId, orderIdLong)
									.last("LIMIT 1"));

			isDiscount = false;
			String orderIdStr = Long.toString(orderIdLong);
			String tradeSourceType =
					resolveTradeSourceType(
							assoc.getOrderType() == null ? "" : assoc.getOrderType().trim(),
							assoc.getOrderClass() == null ? "" : assoc.getOrderClass().trim());

			data.put("company_id", companyId);
			data.put("order_id", orderIdStr);
			data.put("order_id_numeric", orderIdLong);
			data.put("pay_type", payTypeRaw);
			data.put("trade_source_type", tradeSourceType);
			data.put("user_id", assoc.getUserId() == null ? 0L : assoc.getUserId());
			data.put("shop_id", assoc.getShopId() == null ? 0L : assoc.getShopId());
			data.put(
					"distributor_id",
					no != null && no.getDistributorId() != null ? no.getDistributorId() : 0L);
			data.put("mobile", firstNonEmpty(str(assoc.getMobile()), no == null ? "" : str(no.getMobile()), str(auth.get("mobile"))));
			data.put("open_id", openId);
			data.put("wxa_appid", wxappAppid);
			data.put("authorizer_appid", str(auth.get("woa_appid")));
			String title = no != null && StringUtils.hasText(str(no.getTitle())) ? str(no.getTitle()) : str(assoc.getTitle());
			data.put("body", title);
			data.put("detail", title);
			int payFee = no != null ? computeCnyFeeFromNormalOrder(no) : 0;
			if (payFee <= 0 && assoc.getTotalFee() != null) {
				payFee = assoc.getTotalFee().intValue();
			}
			if (payFee <= 0 && no != null) {
				payFee = parseFeeStringToInt(no.getTotalFee());
			}
			data.put("pay_fee", payFee);
			data.put("total_fee", no != null ? parseFeeStringToInt(no.getTotalFee()) : (assoc.getTotalFee() == null ? 0 : assoc.getTotalFee().intValue()));
			if (no != null) {
				data.put("fee_rate", no.getFeeRate());
				data.put("fee_type", str(no.getFeeType()));
				data.put("fee_symbol", str(no.getFeeSymbol()));
				data.put("point_amount", no.getPoint() == null ? 0 : no.getPoint());
				data.put(
						"prescription_requires_full_pay",
						no.getPrescriptionStatus() != null && no.getPrescriptionStatus() != 0);
				data.put("auto_cancel_time", no.getAutoCancelTime());
			} else {
				data.put("fee_rate", assoc.getFeeRate());
				data.put("fee_type", str(assoc.getFeeType()));
				data.put("fee_symbol", str(assoc.getFeeSymbol()));
				data.put("point_amount", 0);
				data.put("prescription_requires_full_pay", false);
			}
			data.put("coupon_code", str(query.get("coupon_code")));
			data.put("poiid", str(query.get("poiid")));
			data.put("return_url", str(query.get("return_url")));
			data.put("client_ip", clientIpFrom(query));
		} else {
			long distributorIdForData =
					hasTruthyDistributorId(query.get("distributor_id"))
							? Long.parseLong(String.valueOf(query.get("distributor_id")).trim())
							: 0L;
			int tf = parseTotalFeeInt(query.get("total_fee"));
			data.put("company_id", companyId);
			data.put("order_id", "");
			data.put("order_id_numeric", 0L);
			data.put("pay_type", payTypeRaw);
			data.put("trade_source_type", "order_pay");
			data.put("user_id", longOrZero(auth.get("user_id")));
			data.put("distributor_id", distributorIdForData);
			data.put("shop_id", resolvedShopId);
			data.put("mobile", str(auth.get("mobile")));
			data.put("open_id", openId);
			data.put("wxa_appid", wxappAppid);
			data.put("authorizer_appid", str(auth.get("woa_appid")));
			String b = str(query.get("body"));
			String d = str(query.get("detail"));
			data.put("body", b);
			data.put("detail", StringUtils.hasText(d) ? d : b);
			data.put("pay_fee", tf);
			data.put("total_fee", tf);
			data.put("fee_rate", null);
			data.put("fee_type", "");
			data.put("fee_symbol", "");
			data.put("point_amount", 0);
			data.put("prescription_requires_full_pay", false);
			data.put("coupon_code", str(query.get("coupon_code")));
			data.put("poiid", str(query.get("poiid")));
			data.put("return_url", str(query.get("return_url")));
			data.put("client_ip", clientIpFrom(query));
		}

		return ordersPaymentDoPaymentService.doPayment(auth, data, isDiscount);
	}

	private static String firstNonEmpty(String a, String b, String c) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		if (StringUtils.hasText(b)) {
			return b;
		}
		return c == null ? "" : c;
	}

	private static LinkedHashMap<String, Object> resolveAuthForPaymentConfig(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> map)) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return auth;
	}

	private void enrichAuthWechatMiniProgramContext(LinkedHashMap<String, Object> auth, long companyId) {
		copyAuthClaimIfAbsent(auth, "open_id", "openid");
		copyAuthClaimIfAbsent(auth, "wxapp_appid", "authorizer_appid");

		if (StringUtils.hasText(str(auth.get("open_id"))) && StringUtils.hasText(str(auth.get("wxapp_appid")))) {
			return;
		}
		long userId = longOrZero(auth.get("user_id"));
		if (userId == 0L) {
			return;
		}
		MembersAssociations ma =
				membersAssociationsMapper.selectOne(
						new LambdaQueryWrapper<MembersAssociations>()
								.eq(MembersAssociations::getCompanyId, companyId)
								.eq(MembersAssociations::getUserId, userId)
								.eq(MembersAssociations::getUserType, "wechat")
								.last("LIMIT 1"));
		if (ma == null || !StringUtils.hasText(ma.getUnionid())) {
			return;
		}
		WechatUsers wu =
				wechatUsersMapper.selectOne(
						new LambdaQueryWrapper<WechatUsers>()
								.eq(WechatUsers::getCompanyId, companyId)
								.eq(WechatUsers::getUnionid, ma.getUnionid().trim())
								.orderByDesc(WechatUsers::getUpdated)
								.last("LIMIT 1"));
		if (wu == null) {
			return;
		}
		if (!StringUtils.hasText(str(auth.get("open_id"))) && StringUtils.hasText(wu.getOpenId())) {
			auth.put("open_id", wu.getOpenId().trim());
		}
		if (!StringUtils.hasText(str(auth.get("wxapp_appid"))) && StringUtils.hasText(wu.getAuthorizerAppid())) {
			auth.put("wxapp_appid", wu.getAuthorizerAppid().trim());
		}
	}

	private static void copyAuthClaimIfAbsent(LinkedHashMap<String, Object> auth, String targetKey, String sourceKey) {
		if (StringUtils.hasText(str(auth.get(targetKey)))) {
			return;
		}
		String v = str(auth.get(sourceKey));
		if (StringUtils.hasText(v)) {
			auth.put(targetKey, v);
		}
	}

	private static boolean isMissingOrderId(Object raw) {
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

	private static boolean hasTruthyDistributorId(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		if (raw instanceof Number n) {
			return n.longValue() != 0L;
		}
		return true;
	}

	private static int parseTotalFeeInt(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if (t.isEmpty()) {
				return 0;
			}
		}
		try {
			return (int) Double.parseDouble(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int computeCnyFeeFromNormalOrder(NormalOrders order) {
		double feeRate = 0.0;
		if (order.getFeeRate() != null) {
			feeRate = order.getFeeRate().doubleValue();
		}
		long totalFee = 0L;
		if (StringUtils.hasText(order.getTotalFee())) {
			try {
				totalFee = Long.parseLong(order.getTotalFee().trim());
			} catch (NumberFormatException ignored) {
				totalFee = 0L;
			}
		}
		BigDecimal fr = BigDecimal.valueOf(feeRate).setScale(4, RoundingMode.HALF_UP);
		return fr.multiply(BigDecimal.valueOf(totalFee)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	public Map<String, Object> query(String tradeId) {
		return ordersPaymentTradeQueryService.resolve(tradeId, new LinkedHashMap<>());
	}

	public Map<String, Object> payment(
			HttpServletRequest request, Map<String, Object> mergedInput, Map<String, Object> authInfo) {
		assertOrderIdPresent(mergedInput.get("order_id"));
		String orderIdRaw = String.valueOf(mergedInput.get("order_id")).trim();
		boolean isCz = firstTwoUnicodeUpper(orderIdRaw).equals("CZ");

		Object payTypeRaw = mergedInput.get("pay_type");
		Object authCodeRaw = mergedInput.get("auth_code");
		boolean authCodePresent = isAuthCodePresent(authCodeRaw);
		if (!authCodePresent && isBlankOrZeroLike(payTypeRaw)) {
			throw new BadRequestException("支付方式必填");
		}

		String normForAdapay = resolveNormalBranchPayType(payTypeRaw, authCodeRaw);
		if ("adapay".equals(normForAdapay) && isBlankOrZeroLike(mergedInput.get("pay_channel"))) {
			throw new BadRequestException("adapay支付方式 pay_channel必传");
		}

		if (isCz) {
			Map<String, Object> czIn = mergedInput;
			if (authCodePresent) {
				czIn = new LinkedHashMap<>(mergedInput);
				czIn.put("pay_type", normForAdapay);
			}
			return depositTradeWxappPaymentService.depositPayment(authInfo, czIn);
		}

		long companyId = readCompanyIdStrict(authInfo);
		long orderIdLong = parseOrderIdLong(mergedInput.get("order_id"));
		String orderIdCanonicalStr = Long.toString(orderIdLong);

		String payType = resolveNormalBranchPayType(payTypeRaw, authCodeRaw);

		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdLong)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new BadRequestException("当前订单不需要支付");
		}
		String assocOrderType = assoc.getOrderType() == null ? "" : assoc.getOrderType().trim();
		if ("supplier_order".equalsIgnoreCase(assocOrderType)) {
			throw new BadRequestException("暂不支持该订单类型的支付");
		}
		String orderStatus = assoc.getOrderStatus() == null ? "" : assoc.getOrderStatus().trim();
		if (!"NOTPAY".equals(orderStatus) && !"PART_PAYMENT".equals(orderStatus)) {
			throw new BadRequestException("当前订单不需要支付");
		}

		String effective = orderAssociationEffectiveTypeService.effectiveOrderType(assoc);
		if (effective == null || effective.isEmpty() || !supportsPaymentBundleForPayment(effective)) {
			throw new BadRequestException("无此类型订单！");
		}

		Map<String, Object> bundle;
		try {
			bundle = adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdCanonicalStr, false);
		} catch (BadRequestException | ResourceException e) {
			throw new BadRequestException("当前订单不存在");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo =
				bundle.get("orderInfo") instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
		if (orderInfo == null) {
			throw new BadRequestException("当前订单不存在");
		}

		// 积分抵扣到 0 元：强制按纯积分支付落交易 pay_type=point
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

		String tradeSourceType =
				resolveTradeSourceType(
						assoc.getOrderType() == null ? "" : assoc.getOrderType().trim(),
						assoc.getOrderClass() == null ? "" : assoc.getOrderClass().trim());
		Map<String, Object> data =
				buildPaymentData(
						mergedInput,
						authInfo,
						companyId,
						orderIdCanonicalStr,
						orderIdLong,
						tradeSourceType,
						payType,
						orderInfo);
		if (authCodePresent) {
			data.put("auth_code", String.valueOf(authCodeRaw).trim());
		}

		Map<String, Object> payResult = ordersPaymentDoPaymentService.doPayment(authInfo, data, false);

		if (orderInfo.containsKey("pay_type")) {
			Object resultPayType = payResult.get("pay_type");
			orderInfo.put("pay_type", resultPayType == null ? "" : String.valueOf(resultPayType));
		}
		payResult.put("order_id", orderIdCanonicalStr);
		payResult.put("team_id", orderInfo.get("team_id"));
		payResult.put("order_info", orderInfo);

		return payResult;
	}

	private static void assertOrderIdPresent(Object raw) {
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
	}

	private static String firstTwoUnicodeUpper(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		int cps = s.codePointCount(0, s.length());
		int n = Math.min(2, cps);
		int end = s.offsetByCodePoints(0, n);
		return s.substring(0, end).toUpperCase(Locale.ROOT);
	}

	private static String resolveNormalBranchPayType(Object payTypeParam, Object authCodeRaw) {
		boolean payTypePresent = !isBlankOrZeroLike(payTypeParam);
		boolean authCodePresent = isAuthCodePresent(authCodeRaw);
		String payType =
				payTypePresent ? String.valueOf(payTypeParam).trim().toLowerCase(Locale.ROOT) : "wxpayh5";
		if (authCodePresent) {
			String ac = String.valueOf(authCodeRaw).trim();
			if (ac.matches("^1[0-5][0-9]{16}$")) {
				payType = "wxpaypos";
			} else if (ac.matches("^(25|26|27|28|29|30)[0-9]{14,22}$")) {
				payType = "alipaypos";
			}
		}
		return payType;
	}

	private static long readCompanyIdStrict(Map<String, Object> authInfo) {
		Object companyIdObj = authInfo.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = Long.parseLong(String.valueOf(companyIdObj).trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
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
			String tradeSourceType,
			String payType,
			Map<String, Object> orderInfo) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("order_id", orderIdCanonicalStr);
		data.put("order_id_numeric", orderIdLong);
		data.put("pay_type", payType);
		data.put("trade_source_type", tradeSourceType);
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
				|| "paypal".equals(p)
				|| "doumen_intl".equals(p);
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
			return new BigDecimal(String.valueOf(raw).trim()).setScale(0, RoundingMode.HALF_UP).intValue();
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

	/**
	 * Aligns with PHP {@code WxappPayment}: {@code order_type + '_' + order_class} when both present.
	 */
	static String resolveTradeSourceType(String orderType, String orderClass) {
		if (StringUtils.hasText(orderType) && StringUtils.hasText(orderClass)) {
			return orderType + "_" + orderClass;
		}
		if (StringUtils.hasText(orderType)) {
			return orderType;
		}
		if (StringUtils.hasText(orderClass)) {
			return orderClass;
		}
		return "order_pay";
	}
}
