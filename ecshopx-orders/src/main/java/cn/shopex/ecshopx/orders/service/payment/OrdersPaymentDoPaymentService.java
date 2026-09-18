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

package cn.shopex.ecshopx.orders.service.payment;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.order.port.VipGradeMembercardTradePaidPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.payment.PaymentSubjectDistributorIdPort;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.deposit.service.DepositTradeConsumeService;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.port.OrdersMiniProgramHfpayPayPort;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import cn.shopex.ecshopx.payment.service.AlipayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.WxpayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.membercard.MembercardAdapayPaymentSdkService;
import cn.shopex.ecshopx.payment.service.membercard.MembercardBsPayPaymentSdkService;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class OrdersPaymentDoPaymentService {

	private static final ObjectMapper SNAKE_ROW =
			new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

	private final TradeMapper tradeMapper;
	private final NormalOrderNumericIdService normalOrderNumericIdService;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final TransactionTemplate transactionTemplate;
	private final OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher;
	private final ObjectMapper objectMapper;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final DepositTradeConsumeService depositTradeConsumeService;
	private final WxpayPaymentConfigValidationService wxpayPaymentConfigValidationService;
	private final AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;
	private final SupplierSubOrdersForPaymentLoadService supplierSubOrdersForPaymentLoadService;
	private final OrdersExternalPayParamBuildService ordersExternalPayParamBuildService;
	private final MembercardBsPayPaymentSdkService membercardBsPayPaymentSdkService;
	private final MembercardAdapayPaymentSdkService membercardAdapayPaymentSdkService;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectProvider<OrdersMiniProgramHfpayPayPort> hfpayPayPort;
	private final ObjectProvider<VipGradeMembercardTradePaidPort> vipGradeMembercardTradePaidPort;
	private final PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort;

	public OrdersPaymentDoPaymentService(
			TradeMapper tradeMapper,
			NormalOrderNumericIdService normalOrderNumericIdService,
			DistributionDistributorPeekMapper distributionDistributorPeekMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			TransactionTemplate transactionTemplate,
			OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher,
			ObjectMapper objectMapper,
			PointMemberAddPointService pointMemberAddPointService,
			DepositTradeConsumeService depositTradeConsumeService,
			WxpayPaymentConfigValidationService wxpayPaymentConfigValidationService,
			AlipayPaymentConfigValidationService alipayPaymentConfigValidationService,
			SupplierSubOrdersForPaymentLoadService supplierSubOrdersForPaymentLoadService,
			OrdersExternalPayParamBuildService ordersExternalPayParamBuildService,
			MembercardBsPayPaymentSdkService membercardBsPayPaymentSdkService,
			MembercardAdapayPaymentSdkService membercardAdapayPaymentSdkService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectProvider<OrdersMiniProgramHfpayPayPort> hfpayPayPort,
			ObjectProvider<VipGradeMembercardTradePaidPort> vipGradeMembercardTradePaidPort,
			PaymentSubjectDistributorIdPort paymentSubjectDistributorIdPort) {
		this.tradeMapper = tradeMapper;
		this.normalOrderNumericIdService = normalOrderNumericIdService;
		this.distributionDistributorPeekMapper = distributionDistributorPeekMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.transactionTemplate = transactionTemplate;
		this.ordersTradeFinishDispatchPublisher = ordersTradeFinishDispatchPublisher;
		this.objectMapper = objectMapper;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.depositTradeConsumeService = depositTradeConsumeService;
		this.wxpayPaymentConfigValidationService = wxpayPaymentConfigValidationService;
		this.alipayPaymentConfigValidationService = alipayPaymentConfigValidationService;
		this.supplierSubOrdersForPaymentLoadService = supplierSubOrdersForPaymentLoadService;
		this.ordersExternalPayParamBuildService = ordersExternalPayParamBuildService;
		this.membercardBsPayPaymentSdkService = membercardBsPayPaymentSdkService;
		this.membercardAdapayPaymentSdkService = membercardAdapayPaymentSdkService;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.companysRedisTemplate = companysRedisTemplate;
		this.hfpayPayPort = hfpayPayPort;
		this.vipGradeMembercardTradePaidPort = vipGradeMembercardTradePaidPort;
		this.paymentSubjectDistributorIdPort = paymentSubjectDistributorIdPort;
	}

	public Map<String, Object> doPayment(Map<String, Object> authInfo, Map<String, Object> data, boolean unusedFalse) {
		long companyId = longVal(data.get("company_id"));
		String orderId = str(data.get("order_id"));
		String payTypeRaw = str(data.get("pay_type"));
		String payType = payTypeRaw.toLowerCase(Locale.ROOT);
		data.put("pay_type", payType);
		long distributorId = longVal(data.get("distributor_id"));
		long distributorIdForSetting =
				paymentSubjectDistributorIdPort.resolveActualDistributorId(companyId, distributorId);
		int payFee = intVal(data.get("pay_fee"), 0);
		String payChannel = str(data.get("pay_channel"));
		String tradeSourceTypeIn = str(data.get("trade_source_type"));
		final String tradeSourceType =
				StringUtils.hasText(tradeSourceTypeIn) ? tradeSourceTypeIn : "normal";
		long orderIdLong = longVal(data.get("order_id_numeric"));

		assertPayConfigReady(payType, companyId, distributorIdForSetting);

		if (isAdapayFamily(payType) || isBspayFamily(payType)) {
			List<Map<String, Object>> subOrders =
					supplierSubOrdersForPaymentLoadService.listMapsByCompanyAndParentOrderId(companyId, orderIdLong);
			data.put("sub_orders", subOrders);
		}

		if (isAdapayFamily(payType) || isBspayFamily(payType)) {
			Trade cached = findExistingWithPaymentParams(companyId, orderId, payType, payFee, payChannel, tradeSourceType);
			if (cached != null) {
				return decodePaymentParamsToResult(cached, tradeSourceType);
			}
		}

		return transactionTemplate.execute(
				status ->
						executeInTransaction(
								authInfo,
								data,
								payType,
								companyId,
								orderId,
								orderIdLong,
								distributorId,
								distributorIdForSetting,
								payFee,
								payChannel,
								tradeSourceType));
	}

	private Map<String, Object> executeInTransaction(
			Map<String, Object> authInfo,
			Map<String, Object> data,
			String payType,
			long companyId,
			String orderId,
			long orderIdLong,
			long distributorId,
			long distributorIdForSetting,
			int payFee,
			String payChannel,
			String tradeSourceType) {
		Trade trade = findOpenTradeWithoutParams(companyId, orderId, payType, payFee, payChannel, tradeSourceType);
		if (trade == null) {
			int pointAmount = intVal(data.get("point_amount"), 0);
			// 现金应付 0 且存在积分抵扣：按纯积分支付建单，避免落 localPay
			if (payFee == 0 && pointAmount > 0 && !"point".equals(payType) && !"deposit".equals(payType)) {
				payType = "point";
				data.put("pay_type", "point");
				payFee = pointAmount;
				data.put("pay_fee", payFee);
			}
			trade = insertPendingTrade(data, payType, payChannel, tradeSourceType, payFee, companyId, orderId, distributorId);
		}

		boolean prescriptionHeavy = boolVal(data.get("prescription_requires_full_pay"));
		if (trade.getPayFee() != null
				&& trade.getPayFee() == 0
				&& !prescriptionHeavy
				&& !"point".equals(payType)
				&& !"deposit".equals(payType)) {
			finalizeTradeSuccess(trade, "localPay");
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_status", true);
			out.put("trade_info", buildTradeInfoMap(orderId, trade.getTradeId(), tradeSourceType));
			return out;
		}

		if ("point".equals(payType)) {
			long userId = longVal(data.get("user_id"));
			int point = intVal(data.get("point_amount"), 0);
			if (point <= 0) {
				throw new ResourceException("积分不足");
			}
			// 积分商城已在创建时扣 order.point；此处仅完结交易，避免纯积分自动支付双扣。
			if (!"normal_pointsmall".equals(tradeSourceType)) {
				pointMemberAddPointService.addPointForManualAdjustment(
						userId, companyId, point, false, "订单" + orderId + "支付扣减积分");
			}
			finalizeTradeSuccess(trade);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_status", true);
			out.put("trade_info", buildTradeInfoMap(orderId, trade.getTradeId(), tradeSourceType));
			return out;
		}

		if ("prepaid_point".equals(payType)) {
			// 企业购预充点：额度已在创单扣减；此处仅建已支付 trade 并完结订单，禁止微信通道
			finalizeTradeSuccess(trade);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_status", true);
			out.put("trade_info", buildTradeInfoMap(orderId, trade.getTradeId(), tradeSourceType));
			return out;
		}

		if ("deposit".equals(payType)) {
			Map<String, Object> consume = new LinkedHashMap<>();
			consume.put("company_id", companyId);
			consume.put("user_id", longVal(data.get("user_id")));
			consume.put("money", (long) payFee);
			consume.put("member_card_code", authInfo == null ? "" : str(authInfo.get("user_card_code")));
			consume.put("shop_id", str(data.get("shop_id")));
			consume.put("shop_name", str(data.get("shop_name")));
			consume.put("mobile", str(data.get("mobile")));
			consume.put("open_id", str(data.get("open_id")));
			consume.put("detail", str(data.get("body")));
			depositTradeConsumeService.consume(consume);
			finalizeTradeSuccess(trade);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("pay_status", true);
			out.put("trade_info", buildTradeInfoMap(orderId, trade.getTradeId(), tradeSourceType));
			return out;
		}

		if ("pos".equals(payType)) {
			finalizeTradeSuccess(trade);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("money", String.valueOf(payFee));
			out.put("pay_status", true);
			out.put("trade_info", buildTradeInfoMap(orderId, trade.getTradeId(), tradeSourceType));
			return out;
		}

		if ("bspay".equals(payType)) {
			Map<String, Object> merchantCfg = requireBspaySettingMap(companyId);
			String body = str(data.get("body"));
			String openId = str(data.get("open_id"));
			String wxaAppId = authInfo == null ? "" : str(authInfo.get("wxapp_appid"));
			String clientIp = str(data.get("client_ip"));
			String remark = str(data.get("remark"));
			String source = str(data.get("source"));
			MembercardBsPayPaymentSdkService.BsPayMembercardOutcome outcome =
					membercardBsPayPaymentSdkService.createPayment(
							companyId,
							merchantCfg,
							trade.getTradeId(),
							orderId,
							payChannel,
							payFee,
							openId,
							wxaAppId,
							body,
							body,
							clientIp,
							remark,
							source,
							false);
			persistBspayAfterCreate(trade.getTradeId(), outcome);
			Map<String, Object> client = new LinkedHashMap<>(outcome.clientPaymentParams());
			client.put("trade_info", buildTradeInfoMap(orderId, trade.getTradeId(), tradeSourceType));
			savePaymentParams(trade.getTradeId(), client);
			return client;
		}

		if ("adapay".equals(payType)) {
			Map<String, Object> merchantCfg = loadAdapayFullConfig(companyId);
			String openId = str(data.get("open_id"));
			String title = str(data.get("body"));
			String desc = str(data.get("detail"));
			if (!StringUtils.hasText(desc)) {
				desc = title;
			}
			MembercardAdapayPaymentSdkService.AdapayMembercardOutcome outcome =
					membercardAdapayPaymentSdkService.createPayment(
							companyId, merchantCfg, trade.getTradeId(), payChannel, payFee, openId, title, desc, tradeSourceType);
			persistAdapayAfterCreate(trade.getTradeId(), outcome);
			Map<String, Object> client = new LinkedHashMap<>(outcome.clientPaymentParams());
			client.put("trade_info", buildTradeInfoMap(orderId, trade.getTradeId(), tradeSourceType));
			savePaymentParams(trade.getTradeId(), client);
			return client;
		}

		if (payType.startsWith("wxpay")
				|| payType.startsWith("alipay")
				|| "hfpay".equals(payType)
				|| "offline_pay".equals(payType)
				|| "paypal".equals(payType)
				|| "doumen_intl".equals(payType)) {
			Map<String, Object> client =
					ordersExternalPayParamBuildService.buildPayParamsOrInvoke(
							payType, companyId, distributorIdForSetting, authInfo, data, trade);
			if (Boolean.TRUE.equals(client.get("pay_status"))) {
				finalizeTradeSuccess(trade);
			}
			if (!client.containsKey("trade_info")) {
				client.put("trade_info", buildTradeInfoMap(orderId, trade.getTradeId(), tradeSourceType));
			}
			savePaymentParams(trade.getTradeId(), client);
			return client;
		}

		throw new BadRequestException("无此类型支付");
	}

	private void assertPayConfigReady(String payType, long companyId, long distributorId) {
		if ("point".equals(payType)
				|| "prepaid_point".equals(payType)
				|| "deposit".equals(payType)
				|| "pos".equals(payType)
				|| "offline_pay".equals(payType)
				|| "paypal".equals(payType)
				|| "doumen_intl".equals(payType)) {
			return;
		}
		if ("hfpay".equals(payType)) {
			OrdersMiniProgramHfpayPayPort port = hfpayPayPort.getIfAvailable();
			if (port == null) {
				throw new ResourceException("不支持支付服务，请联系商家");
			}
			port.assertConfigReady(companyId, distributorId);
			return;
		}
		if (payType.startsWith("wxpay")) {
			wxpayPaymentConfigValidationService.assertConfigComplete(companyId, distributorId);
			return;
		}
		if (payType.startsWith("alipay")) {
			alipayPaymentConfigValidationService.assertConfigComplete(companyId, distributorId);
			return;
		}
		if ("bspay".equals(payType)) {
			requireBspaySettingMap(companyId);
			return;
		}
		if ("adapay".equals(payType)) {
			String appId = loadAdapayAppIdOnly(companyId);
			if (appId == null || appId.trim().isEmpty()) {
				throw new ResourceException("不支持支付服务，请联系商家");
			}
			return;
		}
		throw new BadRequestException("无此类型支付");
	}

	private static boolean isAdapayFamily(String payType) {
		return "adapay".equals(payType);
	}

	private static boolean isBspayFamily(String payType) {
		return "bspay".equals(payType);
	}

	private Trade findExistingWithPaymentParams(
			long companyId,
			String orderId,
			String payType,
			int payFee,
			String payChannel,
			String tradeSourceType) {
		var w = baseTradeMatch(companyId, orderId, payType, payFee, tradeSourceType);
		if (channelKeyed(payType)) {
			w.eq(Trade::getPayChannel, payChannel == null ? "" : payChannel);
		}
		w.isNotNull(Trade::getPaymentParams).ne(Trade::getPaymentParams, "");
		return tradeMapper.selectOne(w.last("LIMIT 1"));
	}

	private Trade findOpenTradeWithoutParams(
			long companyId,
			String orderId,
			String payType,
			int payFee,
			String payChannel,
			String tradeSourceType) {
		var w = baseTradeMatch(companyId, orderId, payType, payFee, tradeSourceType);
		if (channelKeyed(payType)) {
			w.eq(Trade::getPayChannel, payChannel == null ? "" : payChannel);
		}
		return tradeMapper.selectOne(w.last("LIMIT 1"));
	}

	private static LambdaQueryWrapper<Trade> baseTradeMatch(
			long companyId, String orderId, String payType, int payFee, String tradeSourceType) {
		return Wrappers.<Trade>lambdaQuery()
				.eq(Trade::getCompanyId, String.valueOf(companyId))
				.eq(Trade::getOrderId, orderId)
				.eq(Trade::getPayType, payType)
				.eq(Trade::getPayFee, payFee)
				.eq(Trade::getTradeSourceType, tradeSourceType)
				.eq(Trade::getTradeState, "NOTPAY");
	}

	private static boolean channelKeyed(String payType) {
		return "adapay".equals(payType) || "bspay".equals(payType) || "offline_pay".equals(payType) || "paypal".equals(payType);
	}

	private Map<String, Object> decodePaymentParamsToResult(Trade trade, String tradeSourceType) {
		try {
			Map<String, Object> decoded =
					objectMapper.readValue(trade.getPaymentParams(), new TypeReference<Map<String, Object>>() {});
			Map<String, Object> out = new LinkedHashMap<>(decoded);
			out.putIfAbsent("trade_info", buildTradeInfoMap(trade.getOrderId(), trade.getTradeId(), tradeSourceType));
			return out;
		} catch (Exception e) {
			throw new ResourceException("支付失败");
		}
	}

	private void savePaymentParams(String tradeId, Map<String, Object> client) {
		String json;
		try {
			json = objectMapper.writeValueAsString(client);
		} catch (JsonProcessingException e) {
			throw new ResourceException("支付失败");
		}
		tradeMapper.update(
				null,
				new LambdaUpdateWrapper<Trade>()
						.eq(Trade::getTradeId, tradeId)
						.set(Trade::getPaymentParams, json));
	}

	private void persistBspayAfterCreate(String tradeId, MembercardBsPayPaymentSdkService.BsPayMembercardOutcome outcome) {
		tradeMapper.update(
				null,
				new LambdaUpdateWrapper<Trade>()
						.eq(Trade::getTradeId, tradeId)
						.set(Trade::getBspayReqDate, outcome.bspayReqDate())
						.set(Trade::getInitalRequest, outcome.initialRequestJson())
						.set(Trade::getTransactionId, outcome.partyOrderId()));
	}

	private void persistAdapayAfterCreate(String tradeId, MembercardAdapayPaymentSdkService.AdapayMembercardOutcome outcome) {
		tradeMapper.update(
				null,
				new LambdaUpdateWrapper<Trade>()
						.eq(Trade::getTradeId, tradeId)
						.set(Trade::getInitalRequest, outcome.initialRequestJson())
						.set(Trade::getTransactionId, outcome.transactionId()));
	}

	private Trade insertPendingTrade(
			Map<String, Object> data,
			String payType,
			String payChannel,
			String tradeSourceType,
			int payFee,
			long companyId,
			String orderId,
			long distributorId) {
		long userId = longVal(data.get("user_id"));
		long shopId = longVal(data.get("shop_id"));
		String openId = str(data.get("open_id"));
		String mobilePlain = str(data.get("mobile"));
		String body = str(data.get("body"));
		String detail = str(data.get("detail"));
		if (!StringUtils.hasText(detail)) {
			detail = body;
		}
		String authorizerAppid = authStr(data, "authorizer_appid");
		String wxaAppid = authStr(data, "wxa_appid");
		int totalFeeOrig = intVal(data.get("total_fee"), payFee);
		int payFeeOrig = payFee;
		int payFeeAfter = payFeeOrig;
		int totalFeeStored = totalFeeOrig;
		Float curFeeRate = null;
		String curFeeType = null;
		String curFeeSymbol = null;
		Integer curPayFee = null;
		String payTypeLc = payType.toLowerCase(Locale.ROOT);
		if (payTypeLc.startsWith("wxpay") || payTypeLc.startsWith("alipay") || "hfpay".equals(payTypeLc)) {
			Object feeRateObj = data.get("fee_rate");
			if (feeRateObj != null) {
				double rate = doubleVal(feeRateObj);
				if (Double.compare(rate, 0.0) != 0) {
					double r = BigDecimal.valueOf(rate).setScale(4, RoundingMode.HALF_UP).doubleValue();
					payFeeAfter = (int) Math.round(payFeeOrig * r);
					totalFeeStored = (int) Math.round(totalFeeOrig * r);
					curFeeRate = (float) r;
					curFeeSymbol = str(data.get("fee_symbol"));
					curFeeType = str(data.get("fee_type"));
					curPayFee = payFeeOrig;
				}
			} else {
				CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
				Double cr = cur.getRate();
				if (cr != null && Double.compare(cr, 0.0) != 0) {
					double r = BigDecimal.valueOf(cr).setScale(4, RoundingMode.HALF_UP).doubleValue();
					payFeeAfter = (int) Math.round(payFeeOrig * r);
					totalFeeStored = (int) Math.round(totalFeeOrig * r);
					curFeeRate = (float) r;
					curFeeSymbol = cur.getSymbol() != null ? cur.getSymbol() : "";
					curFeeType = cur.getCurrency() != null ? cur.getCurrency() : "";
					curPayFee = payFeeOrig;
				}
			}
		}

		String dealerId = "0";
		Long merchantIdForTrade = 0L;
		String tradeIdCore = String.valueOf(normalOrderNumericIdService.generate(userId));
		if (distributorId > 0L) {
			DistributionDistributorPeek dist =
					distributionDistributorPeekMapper.selectOne(
							new LambdaQueryWrapper<DistributionDistributorPeek>()
									.eq(DistributionDistributorPeek::getCompanyId, companyId)
									.eq(DistributionDistributorPeek::getDistributorId, distributorId)
									.last("LIMIT 1"));
			if (dist != null) {
				if (dist.getDealerId() != null) {
					dealerId = String.valueOf(dist.getDealerId());
				}
				if (dist.getMerchantId() != null) {
					merchantIdForTrade = dist.getMerchantId();
				}
				if (StringUtils.hasText(dist.getShopCode())) {
					tradeIdCore = dist.getShopCode() + tradeIdCore;
				}
			}
		}

		String mobileEnc = sensitiveFieldEncryptor.encrypt(mobilePlain == null ? "" : mobilePlain.trim());
		String timeStart = String.valueOf(System.currentTimeMillis() / 1000L);

		Trade row = new Trade();
		row.setTradeId(tradeIdCore);
		row.setOrderId(orderId);
		row.setCompanyId(String.valueOf(companyId));
		row.setShopId(String.valueOf(shopId));
		row.setDistributorId(String.valueOf(distributorId));
		row.setDealerId(dealerId);
		row.setTradeSourceType(tradeSourceType);
		row.setUserId(String.valueOf(userId));
		row.setMobile(mobileEnc);
		row.setOpenId(openId.isEmpty() ? null : openId);
		row.setTotalFee(totalFeeStored);
		row.setDiscountFee(0);
		row.setPayFee(payFeeAfter);
		row.setFeeType(StringUtils.hasText(str(data.get("fee_type"))) ? str(data.get("fee_type")) : "CNY");
		row.setTradeState("NOTPAY");
		row.setPayType(payType);
		row.setPayChannel(payChannel == null ? "" : payChannel);
		row.setAuthorizerAppid(authorizerAppid.isEmpty() ? null : authorizerAppid);
		row.setWxaAppid(wxaAppid.isEmpty() ? null : wxaAppid);
		row.setBody(body.isEmpty() ? null : body);
		row.setDetail(detail.isEmpty() ? body : detail);
		row.setTimeStart(timeStart);
		row.setMerchantId(merchantIdForTrade);
		if (curFeeRate != null) {
			row.setCurFeeRate(curFeeRate);
			row.setCurFeeType(curFeeType != null ? curFeeType : "CNY");
			row.setCurFeeSymbol(curFeeSymbol != null ? curFeeSymbol : "");
			row.setCurPayFee(curPayFee != null ? curPayFee : payFeeAfter);
		} else {
			row.setCurPayFee(payFeeAfter);
			row.setCurFeeType("CNY");
			row.setCurFeeRate(1.0f);
			row.setCurFeeSymbol("￥");
		}

		tradeMapper.insert(row);
		return row;
	}

	private static String authStr(Map<String, Object> data, String key) {
		Object v = data.get(key);
		return v == null ? "" : v.toString().trim();
	}

	private void finalizeTradeSuccess(Trade trade) {
		finalizeTradeSuccess(trade, null);
	}

	private void finalizeTradeSuccess(Trade trade, String payTypeOverride) {
		String tradeId = trade.getTradeId();
		Trade loaded = tradeMapper.selectById(tradeId);
		if (loaded == null) {
			throw new ResourceException("交易单不存在");
		}
		if ("SUCCESS".equals(loaded.getTradeState())) {
			maybeFulfillVipGradeMembercardSale(loaded);
			publishTradeFinishEvent(loaded);
			return;
		}
		if (!"NOTPAY".equals(loaded.getTradeState())) {
			return;
		}
		String tradeNo = buildTradeNoSuffix(loaded);
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Trade> uw = new LambdaUpdateWrapper<>();
		uw.eq(Trade::getTradeId, tradeId)
				.eq(Trade::getTradeState, "NOTPAY")
				.set(Trade::getTradeState, "SUCCESS")
				.set(Trade::getTimeExpire, String.valueOf(nowSec))
				.set(Trade::getTradeNo, tradeNo);
		if (StringUtils.hasText(payTypeOverride)) {
			uw.set(Trade::getPayType, payTypeOverride);
		}
		int updated = tradeMapper.update(null, uw);
		if (updated <= 0) {
			throw new ResourceException("交易单状态更新失败");
		}
		Trade reloaded = tradeMapper.selectById(tradeId);
		if (reloaded == null) {
			throw new ResourceException("交易单不存在");
		}
		maybeFulfillVipGradeMembercardSale(reloaded);
		publishTradeFinishEvent(reloaded);
	}

	/**
	 * After channel order query confirms success: CAS NOTPAY to SUCCESS, optionally persist channel transaction id, publish finish event.
	 */
	public void finalizeTradeSuccessAfterChannelQuery(
			Trade trade, String transactionId, Map<String, Object> channelResult) {
		String tradeId = trade.getTradeId();
		Trade loaded = tradeMapper.selectById(tradeId);
		if (loaded == null) {
			throw new BadRequestException("支付单不存在");
		}
		if ("SUCCESS".equals(loaded.getTradeState())) {
			maybeFulfillVipGradeMembercardSale(loaded);
			publishTradeFinishEvent(loaded);
			return;
		}
		if (!"NOTPAY".equals(loaded.getTradeState())) {
			throw new BadRequestException("更新已处理，不需要更新");
		}
		String tradeNo = buildTradeNoSuffix(loaded);
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Trade> uw = new LambdaUpdateWrapper<>();
		uw.eq(Trade::getTradeId, tradeId)
				.eq(Trade::getTradeState, "NOTPAY")
				.set(Trade::getTradeState, "SUCCESS")
				.set(Trade::getTimeExpire, String.valueOf(nowSec))
				.set(Trade::getTradeNo, tradeNo);
		if (StringUtils.hasText(transactionId)) {
			uw.set(Trade::getTransactionId, transactionId);
		}
		int updated = tradeMapper.update(null, uw);
		if (updated <= 0) {
			throw new BadRequestException("交易单状态更新失败");
		}
		Trade reloaded = tradeMapper.selectById(tradeId);
		if (reloaded == null) {
			throw new BadRequestException("支付单不存在");
		}
		maybeFulfillVipGradeMembercardSale(reloaded);
		publishTradeFinishEvent(reloaded);
	}

	private void maybeFulfillVipGradeMembercardSale(Trade trade) {
		if (trade == null || !"membercard".equals(trade.getTradeSourceType())) {
			return;
		}
		VipGradeMembercardTradePaidPort port = vipGradeMembercardTradePaidPort.getIfAvailable();
		if (port == null) {
			return;
		}
		String companyIdStr = trade.getCompanyId();
		String userIdStr = trade.getUserId();
		String orderId = trade.getOrderId();
		if (companyIdStr == null
				|| companyIdStr.isBlank()
				|| userIdStr == null
				|| userIdStr.isBlank()
				|| orderId == null
				|| orderId.isBlank()) {
			return;
		}
		long companyId = Long.parseLong(companyIdStr.trim());
		long userId = Long.parseLong(userIdStr.trim());
		port.onTradeSuccess(companyId, orderId.trim(), userId);
	}

	private void publishTradeFinishEvent(Trade trade) {
		Map<String, Object> tradeRow;
		try {
			tradeRow = SNAKE_ROW.convertValue(trade, new TypeReference<Map<String, Object>>() {});
		} catch (IllegalArgumentException e) {
			throw new ResourceException("支付失败");
		}
		ordersTradeFinishDispatchPublisher.publish(tradeRow);
	}

	private String buildTradeNoSuffix(Trade trade) {
		String companyIdStr = trade.getCompanyId() == null ? "0" : trade.getCompanyId();
		String distributorId = trade.getDistributorId() == null ? "0" : trade.getDistributorId();
		String orderId = trade.getOrderId() == null ? "" : trade.getOrderId();
		long seq = nextTodayTradeSequence(companyIdStr, distributorId, orderId);
		String md = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMdd"));
		return md + "-" + seq;
	}

	private long nextTodayTradeSequence(String companyId, String distributorId, String orderId) {
		String today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMdd"));
		String hKey = "h_trade_no_" + companyId + "_" + distributorId + "_" + today;
		String cKey = "c_trade_no_" + companyId + "_" + distributorId + "_" + today;
		String existing = (String) sharedStringRedisTemplate.opsForHash().get(hKey, orderId);
		if (StringUtils.hasText(existing)) {
			return Long.parseLong(existing);
		}
		Long count = sharedStringRedisTemplate.opsForValue().increment(cKey);
		if (count == null) {
			count = 1L;
		}
		sharedStringRedisTemplate.opsForHash().put(hKey, orderId, String.valueOf(count));
		sharedStringRedisTemplate.expire(hKey, Duration.ofSeconds(86400));
		sharedStringRedisTemplate.expire(cKey, Duration.ofSeconds(86400));
		return count;
	}

	private Map<String, Object> requireBspaySettingMap(long companyId) {
		String key = "bspaySetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		Map<String, Object> cfg = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		if (cfg.isEmpty()) {
			throw new ResourceException("请先配置支付信息");
		}
		String sysId = Objects.toString(cfg.get("sys_id"), "").trim();
		if (!StringUtils.hasText(sysId)) {
			throw new ResourceException("请先配置支付信息");
		}
		return cfg;
	}

	private String loadAdapayAppIdOnly(long companyId) {
		String key = "adaPaySetting:" + sha1Hex(String.valueOf(companyId));
		String raw = sharedStringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		try {
			Map<String, Object> map = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			Object v = map.getOrDefault("app_id", "");
			return v == null ? "" : String.valueOf(v);
		} catch (Exception e) {
			return "";
		}
	}

	private Map<String, Object> loadAdapayFullConfig(long companyId) {
		String key = "adaPaySetting:" + sha1Hex(String.valueOf(companyId));
		String raw = sharedStringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("不支持支付服务，请联系商家");
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			throw new ResourceException("不支持支付服务，请联系商家");
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}

	private static Map<String, Object> buildTradeInfoMap(String orderId, String tradeId, String tradeSourceType) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_id", orderId);
		m.put("trade_id", tradeId);
		m.put("trade_source_type", tradeSourceType);
		return m;
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

	private static double doubleVal(Object o) {
		if (o == null) {
			return 0.0;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static boolean boolVal(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		return "1".equals(String.valueOf(o).trim()) || "true".equalsIgnoreCase(String.valueOf(o).trim());
	}
}
