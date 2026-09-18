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

package cn.shopex.ecshopx.kaquan.service.vipgrade.pay;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.excard.NormalOrderNumericIdService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VipGradeMembercardTradeSupportService {

	private final TradeMapper tradeMapper;
	private final NormalOrderNumericIdService normalOrderNumericIdService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorMapper distributorMapper;

	public VipGradeMembercardTradeSupportService(
			TradeMapper tradeMapper,
			NormalOrderNumericIdService normalOrderNumericIdService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorMapper distributorMapper) {
		this.tradeMapper = tradeMapper;
		this.normalOrderNumericIdService = normalOrderNumericIdService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorMapper = distributorMapper;
	}

	public Trade findExistingWxpay(long companyId, String orderId) {
		return tradeMapper.selectOne(Wrappers.<Trade>lambdaQuery()
				.eq(Trade::getCompanyId, String.valueOf(companyId))
				.eq(Trade::getOrderId, orderId)
				.eq(Trade::getPayType, "wxpay")
				.eq(Trade::getTradeSourceType, "membercard")
				.eq(Trade::getTradeState, "NOTPAY")
				.orderByDesc(Trade::getTimeStart)
				.last("LIMIT 1"));
	}

	public Trade findExistingWithPaymentParams(long companyId, String orderId, String payType, String payChannel) {
		return tradeMapper.selectOne(Wrappers.<Trade>lambdaQuery()
				.eq(Trade::getCompanyId, String.valueOf(companyId))
				.eq(Trade::getOrderId, orderId)
				.eq(Trade::getPayType, payType)
				.eq(Trade::getPayChannel, payChannel == null ? "" : payChannel)
				.eq(Trade::getTradeSourceType, "membercard")
				.eq(Trade::getTradeState, "NOTPAY")
				.isNotNull(Trade::getPaymentParams)
				.ne(Trade::getPaymentParams, "")
				.orderByDesc(Trade::getTimeStart)
				.last("LIMIT 1"));
	}

	public Trade findOpenMembercardTrade(long companyId, String orderId, String payType, String payChannel) {
		return tradeMapper.selectOne(Wrappers.<Trade>lambdaQuery()
				.eq(Trade::getCompanyId, String.valueOf(companyId))
				.eq(Trade::getOrderId, orderId)
				.eq(Trade::getPayType, payType)
				.eq(Trade::getPayChannel, payChannel == null ? "" : payChannel)
				.eq(Trade::getTradeSourceType, "membercard")
				.eq(Trade::getTradeState, "NOTPAY")
				.orderByDesc(Trade::getTimeStart)
				.last("LIMIT 1"));
	}

	public Trade createMembercardTrade(
			Map<String, Object> data,
			String payType,
			String payChannel,
			String mchIdForWxOrNull) {
		long companyId = longFrom(data.get("company_id"));
		long userId = longFrom(data.get("user_id"));
		String orderId = stringVal(data.get("order_id"));
		int totalFeeOrig = intFrom(data.get("total_fee"), 0);
		int payFeeOrig = intFrom(data.get("pay_fee"), 0);
		long distributorId = longFrom(data.get("distributor_id"));
		long shopId = longFrom(data.get("shop_id"));
		String openId = stringVal(data.get("open_id"));
		String mobilePlain = stringVal(data.get("mobile"));
		String body = stringVal(data.get("body"));
		String detail = stringVal(data.get("detail"));
		String authorizerAppid = stringVal(data.get("authorizer_appid"));
		String wxaAppid = stringVal(data.get("wxa_appid"));

		int payFeeAfter = payFeeOrig;
		int totalFeeStored = totalFeeOrig;
		Float curFeeRate = null;
		String curFeeType = null;
		String curFeeSymbol = null;
		Integer curPayFee = null;
		String payTypeLc = payType == null ? "" : payType.toLowerCase();
		if (payTypeLc.startsWith("wxpay") || payTypeLc.startsWith("alipay")) {
			Object feeRateObj = data.get("fee_rate");
			if (feeRateObj != null) {
				double rate = doubleVal(feeRateObj);
				if (Double.compare(rate, 0.0) != 0) {
					double r = BigDecimal.valueOf(rate).setScale(4, RoundingMode.HALF_UP).doubleValue();
					payFeeAfter = (int) Math.round(payFeeOrig * r);
					totalFeeStored = (int) Math.round(totalFeeOrig * r);
					curFeeRate = (float) r;
					curFeeSymbol = stringVal(data.get("fee_symbol"));
					curFeeType = stringVal(data.get("fee_type"));
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
			Distributor dist = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.eq(Distributor::getDistributorId, distributorId));
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
		row.setTradeSourceType("membercard");
		row.setUserId(String.valueOf(userId));
		row.setMobile(mobileEnc);
		row.setOpenId(openId.isEmpty() ? null : openId);
		row.setTotalFee(totalFeeStored);
		row.setDiscountFee(0);
		row.setPayFee(payFeeAfter);
		row.setFeeType(stringVal(data.get("fee_type")).isEmpty() ? "CNY" : stringVal(data.get("fee_type")));
		row.setTradeState("NOTPAY");
		row.setPayType(payType);
		row.setPayChannel(payChannel == null ? "" : payChannel);
		row.setAuthorizerAppid(authorizerAppid.isEmpty() ? null : authorizerAppid);
		row.setWxaAppid(wxaAppid.isEmpty() ? null : wxaAppid);
		row.setBody(body.isEmpty() ? null : body);
		row.setDetail(detail.isEmpty() ? body : detail);
		row.setTimeStart(timeStart);
		row.setMerchantId(merchantIdForTrade);
		if (mchIdForWxOrNull != null && !mchIdForWxOrNull.isEmpty()) {
			row.setMchId(mchIdForWxOrNull);
		}
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

		if (payFeeAfter == 0) {
			row.setTradeState("SUCCESS");
		}

		tradeMapper.insert(row);
		return row;
	}

	private static long longFrom(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intFrom(Object o, int d) {
		if (o == null) {
			return d;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return d;
		}
	}

	private static double doubleVal(Object o) {
		if (o == null) {
			return 0d;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0d;
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
