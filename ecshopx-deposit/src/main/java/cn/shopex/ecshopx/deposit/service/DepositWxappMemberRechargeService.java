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

package cn.shopex.ecshopx.deposit.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.domain.RechargeRule;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import cn.shopex.ecshopx.deposit.mapper.RechargeRuleMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DepositWxappMemberRechargeService {

	private final RechargeRuleMapper rechargeRuleMapper;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DepositTradeMapper depositTradeMapper;
	private final DepositTradeIdGenerator depositTradeIdGenerator;
	private final DepositWxpayRechargeUnifyService depositWxpayRechargeUnifyService;

	public DepositWxappMemberRechargeService(
			RechargeRuleMapper rechargeRuleMapper,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DepositTradeMapper depositTradeMapper,
			DepositTradeIdGenerator depositTradeIdGenerator,
			DepositWxpayRechargeUnifyService depositWxpayRechargeUnifyService) {
		this.rechargeRuleMapper = rechargeRuleMapper;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.depositTradeMapper = depositTradeMapper;
		this.depositTradeIdGenerator = depositTradeIdGenerator;
		this.depositWxpayRechargeUnifyService = depositWxpayRechargeUnifyService;
	}

	public Map<String, Object> recharge(Map<String, Object> claims, Map<String, Object> merged) {
		long companyId = parseCompanyIdFromClaims(claims);
		long moneyInput = parseTotalFeeFen(merged);
		String payTypeVal = merged.get("pay_type") == null ? "wxpay" : merged.get("pay_type").toString().trim();
		if (payTypeVal.isEmpty()) {
			payTypeVal = "wxpay";
		}
		if ("alipaymini".equals(payTypeVal)) {
			Object ali = claims.get("alipay_user_id");
			if (ali == null || ali.toString().trim().isEmpty()) {
				throw new BadRequestException("请在支付宝小程序授权登录");
			}
		}

		Object shopIdObj = merged.get("shop_id");
		String shopIdStr =
				shopIdObj == null || shopIdObj.toString().trim().isEmpty()
						? "0"
						: shopIdObj.toString().trim();
		Object shopNameObj = merged.get("shop_name");
		String shopName = shopNameObj == null ? "" : shopNameObj.toString();
		Object detailObj = merged.get("detail");
		String detail = detailObj == null ? null : detailObj.toString();

		Object mcc = claims.get("user_card_code");
		String memberCardCode = mcc == null ? "" : mcc.toString();
		String openId = nullToEmpty(claims.get("open_id"));
		String wxaAppId = nullToEmpty(claims.get("wxapp_appid"));
		String woaAppId = nullToEmpty(claims.get("woa_appid"));
		Object mobileObj = claims.get("mobile");
		String mobilePlain = mobileObj == null ? "" : mobileObj.toString();

		long userId = parseUserIdFromClaims(claims);
		String depositTradeId = depositTradeIdGenerator.nextDepositTradeId(userId);

		RechargeRule rule =
				rechargeRuleMapper.selectOneByCompanyIdAndMoney(
						String.valueOf(companyId), String.valueOf(moneyInput));
		String rechargeRuleIdStr = null;
		if (rule != null && rule.getId() != null) {
			rechargeRuleIdStr = String.valueOf(rule.getId());
		}

		CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
		Double rate = cur.getRate();
		boolean multiBranch = rate != null && Double.compare(rate, 0.0) != 0;
		long moneyAfterExchange = moneyInput;
		Double curFeeRateStored = null;
		String curPayFeeStr = null;
		String curFeeSymbol = null;
		String curFeeType = null;
		if (multiBranch) {
			curPayFeeStr = String.valueOf(moneyInput);
			curFeeSymbol = cur.getSymbol() != null ? cur.getSymbol() : "";
			curFeeType = cur.getCurrency() != null ? cur.getCurrency() : "CNY";
			curFeeRateStored =
					BigDecimal.valueOf(rate).setScale(4, RoundingMode.HALF_UP).doubleValue();
			moneyAfterExchange = Math.round(moneyInput * curFeeRateStored);
		}

		String mobileEnc = sensitiveFieldEncryptor.encrypt(mobilePlain);
		DepositTrade row = new DepositTrade();
		row.setDepositTradeId(depositTradeId);
		row.setCompanyId(String.valueOf(companyId));
		row.setMemberCardCode(memberCardCode);
		row.setShopId(shopIdStr);
		row.setShopName(shopName.isEmpty() ? null : shopName);
		row.setUserId(String.valueOf(userId));
		row.setMobile(mobileEnc);
		row.setOpenId(openId.isEmpty() ? null : openId);
		row.setMoney(String.valueOf(moneyAfterExchange));
		row.setTradeType("recharge");
		row.setTradeStatus("NOTPAY");
		row.setAuthorizerAppid(woaAppId.isEmpty() ? null : woaAppId);
		row.setPayType(payTypeVal);
		row.setWxaAppid(wxaAppId.isEmpty() ? null : wxaAppId);
		row.setDetail(detail);
		row.setTimeStart(String.valueOf(System.currentTimeMillis() / 1000L));
		if (rechargeRuleIdStr != null) {
			row.setRechargeRuleId(rechargeRuleIdStr);
		}
		if (multiBranch) {
			row.setCurPayFee(curPayFeeStr);
			row.setCurFeeSymbol(curFeeSymbol);
			row.setCurFeeType(curFeeType);
			row.setCurFeeRate(curFeeRateStored);
		} else {
			row.setFeeType("CNY");
			row.setCurPayFee(String.valueOf(moneyAfterExchange));
			row.setCurFeeType("CNY");
			row.setCurFeeRate(1.0);
			row.setCurFeeSymbol("￥");
		}

		depositTradeMapper.insert(row);
		DepositTrade verify = depositTradeMapper.selectById(depositTradeId);
		if (verify == null) {
			throw new ResourceException("添加失败");
		}

		Object ipObj = merged.get("client_ip");
		String clientIp = ipObj == null ? "" : ipObj.toString();

		if (isWxPayChannel(payTypeVal)) {
			return depositWxpayRechargeUnifyService.unifyAndBuildClientPayParams(
					companyId,
					0L,
					depositTradeId,
					moneyAfterExchange,
					openId,
					wxaAppId,
					woaAppId,
					shopName,
					detail,
					clientIp,
					payTypeVal);
		}
		Map<String, Object> tradeInfo = new LinkedHashMap<>();
		tradeInfo.put("order_id", depositTradeId);
		tradeInfo.put("trade_id", depositTradeId);
		return Map.of("trade_info", tradeInfo);
	}

	public Map<String, Object> rechargeNew(Map<String, Object> claims, Map<String, Object> merged) {
		long companyId = parseCompanyIdFromClaims(claims);
		long moneyInput = parseTotalFeeFenForRechargeNew(merged);

		final String payTypeForDb;
		final Object payTypeForResponse;
		if (!merged.containsKey("pay_type")) {
			payTypeForDb = "wxpayh5";
			payTypeForResponse = "wxpayh5";
		} else {
			Object rawPay = merged.get("pay_type");
			payTypeForResponse = rawPay;
			if (rawPay == null) {
				payTypeForDb = null;
			} else if (rawPay instanceof String) {
				payTypeForDb = (String) rawPay;
			} else {
				payTypeForDb = rawPay.toString();
			}
		}

		final String detail;
		if (!merged.containsKey("detail")) {
			detail = "充值储值";
		} else {
			Object detailObj = merged.get("detail");
			detail = detailObj == null ? null : detailObj.toString();
		}

		Object shopIdObj = merged.get("shop_id");
		String shopIdStr =
				shopIdObj == null || shopIdObj.toString().trim().isEmpty()
						? "0"
						: shopIdObj.toString().trim();
		Object shopNameObj = merged.get("shop_name");
		String shopName = shopNameObj == null ? "" : shopNameObj.toString();

		Object mcc = claims.get("user_card_code");
		String memberCardCode = mcc == null ? "" : mcc.toString();
		String openId = nullToEmpty(claims.get("open_id"));
		String wxaAppId = nullToEmpty(claims.get("wxapp_appid"));
		String woaAppId = nullToEmpty(claims.get("woa_appid"));
		Object mobileObj = claims.get("mobile");
		String mobilePlain = mobileObj == null ? "" : mobileObj.toString();

		long userId = parseUserIdFromClaims(claims);
		String depositTradeId = depositTradeIdGenerator.nextDepositTradeId(userId);

		RechargeRule rule =
				rechargeRuleMapper.selectOneByCompanyIdAndMoney(
						String.valueOf(companyId), String.valueOf(moneyInput));
		String rechargeRuleIdStr = null;
		if (rule != null && rule.getId() != null) {
			rechargeRuleIdStr = String.valueOf(rule.getId());
		}

		CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
		Double rate = cur.getRate();
		boolean multiBranch = rate != null && Double.compare(rate, 0.0) != 0;
		long moneyAfterExchange = moneyInput;
		Double curFeeRateStored = null;
		String curPayFeeStr = null;
		String curFeeSymbol = null;
		String curFeeType = null;
		if (multiBranch) {
			curPayFeeStr = String.valueOf(moneyInput);
			curFeeSymbol = cur.getSymbol() != null ? cur.getSymbol() : "";
			curFeeType = cur.getCurrency() != null ? cur.getCurrency() : "CNY";
			curFeeRateStored =
					BigDecimal.valueOf(rate).setScale(4, RoundingMode.HALF_UP).doubleValue();
			moneyAfterExchange = Math.round(moneyInput * curFeeRateStored);
		}

		String mobileEnc = sensitiveFieldEncryptor.encrypt(mobilePlain);
		DepositTrade row = new DepositTrade();
		row.setDepositTradeId(depositTradeId);
		row.setCompanyId(String.valueOf(companyId));
		row.setMemberCardCode(memberCardCode);
		row.setShopId(shopIdStr);
		row.setShopName(shopName.isEmpty() ? null : shopName);
		row.setUserId(String.valueOf(userId));
		row.setMobile(mobileEnc);
		row.setOpenId(openId.isEmpty() ? null : openId);
		row.setMoney(String.valueOf(moneyAfterExchange));
		row.setTradeType("recharge");
		row.setTradeStatus("NOTPAY");
		row.setAuthorizerAppid(woaAppId.isEmpty() ? null : woaAppId);
		row.setPayType(payTypeForDb);
		row.setWxaAppid(wxaAppId.isEmpty() ? null : wxaAppId);
		row.setDetail(detail);
		row.setTimeStart(String.valueOf(System.currentTimeMillis() / 1000L));
		if (rechargeRuleIdStr != null) {
			row.setRechargeRuleId(rechargeRuleIdStr);
		}
		if (multiBranch) {
			row.setCurPayFee(curPayFeeStr);
			row.setCurFeeSymbol(curFeeSymbol);
			row.setCurFeeType(curFeeType);
			row.setCurFeeRate(curFeeRateStored);
		}

		depositTradeMapper.insert(row);
		DepositTrade verify = depositTradeMapper.selectById(depositTradeId);
		if (verify == null) {
			throw new ResourceException("添加失败");
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("order_id", verify.getDepositTradeId());
		out.put("pay_type", payTypeForResponse);
		out.put("order_type", verify.getTradeType());
		return out;
	}

	private static long parseCompanyIdFromClaims(Map<String, Object> claims) {
		Object o = claims.get("company_id");
		if (o == null) {
			throw new ResourceException("商户ID必填");
		}
		String t = o.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException("商户ID必填");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("商户ID必填");
		}
	}

	private static long parseUserIdFromClaims(Map<String, Object> claims) {
		Object o = claims.get("user_id");
		if (o == null) {
			throw new ResourceException("用户ID必填");
		}
		String t = o.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException("用户ID必填");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("用户ID必填");
		}
	}

	private static long parseTotalFeeFen(Map<String, Object> merged) {
		Object o = merged.get("total_fee");
		if (o == null) {
			throw new ResourceException("请填写正确的充值金额");
		}
		long v;
		try {
			v = new BigDecimal(o.toString().trim()).longValue();
		} catch (NumberFormatException e) {
			throw new ResourceException("请填写正确的充值金额");
		}
		if (v <= 0) {
			throw new ResourceException("请填写正确的充值金额");
		}
		return v;
	}

	private static long parseTotalFeeFenForRechargeNew(Map<String, Object> merged) {
		if (!merged.containsKey("total_fee")) {
			throw new ResourceException("请填写正确的充值金额");
		}
		Object o = merged.get("total_fee");
		if (o == null) {
			throw new ResourceException("请填写正确的充值金额");
		}
		long v;
		try {
			v = new BigDecimal(o.toString().trim()).longValue();
		} catch (NumberFormatException e) {
			throw new ResourceException("请填写正确的充值金额");
		}
		if (v <= 0) {
			throw new ResourceException("请填写正确的充值金额");
		}
		return v;
	}

	private static String nullToEmpty(Object o) {
		return o == null ? "" : o.toString();
	}

	private static boolean isWxPayChannel(String payType) {
		if (payType == null || payType.isEmpty()) {
			return false;
		}
		String lc = payType.trim().toLowerCase(Locale.ROOT);
		return "wxpay".equals(lc)
				|| "wxpayh5".equals(lc)
				|| "wxpayjs".equals(lc)
				|| lc.startsWith("wxpay");
	}
}
