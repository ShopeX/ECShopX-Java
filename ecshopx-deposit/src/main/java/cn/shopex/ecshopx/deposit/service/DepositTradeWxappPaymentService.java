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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DepositTradeWxappPaymentService {

	private final DepositTradeMapper depositTradeMapper;
	private final DepositWxpayRechargeUnifyService depositWxpayRechargeUnifyService;

	public DepositTradeWxappPaymentService(
			DepositTradeMapper depositTradeMapper,
			DepositWxpayRechargeUnifyService depositWxpayRechargeUnifyService) {
		this.depositTradeMapper = depositTradeMapper;
		this.depositWxpayRechargeUnifyService = depositWxpayRechargeUnifyService;
	}

	public Map<String, Object> depositPayment(Map<String, Object> authInfo, Map<String, Object> mergedInput) {
		String depositTradeId = String.valueOf(mergedInput.get("order_id")).trim();

		Object payTypeRaw = mergedInput.get("pay_type");
		if (isBlankOrZeroLike(payTypeRaw)) {
			throw new BadRequestException("支付方式必填");
		}
		String payType = String.valueOf(payTypeRaw).trim().toLowerCase(Locale.ROOT);

		DepositTrade row = depositTradeMapper.selectById(depositTradeId);
		if (row == null) {
			throw new ResourceException("添加失败");
		}
		String rowCompany = row.getCompanyId() == null ? "" : row.getCompanyId().trim();
		Object authCompany = authInfo.get("company_id");
		String authCompanyStr = authCompany == null ? "" : String.valueOf(authCompany).trim();
		if (!rowCompany.equals(authCompanyStr)) {
			throw new BadRequestException("当前订单不存在");
		}

		String tradeStatus = row.getTradeStatus() == null ? "" : row.getTradeStatus().trim();
		if (!"NOTPAY".equals(tradeStatus)) {
			throw new BadRequestException("当前订单不需要支付");
		}

		if ("alipaymini".equals(payType)) {
			Object aliUid = authInfo.get("alipay_user_id");
			if (!StringUtils.hasText(aliUid == null ? null : String.valueOf(aliUid).trim())) {
				throw new BadRequestException("请在支付宝小程序授权登录");
			}
		}

		long moneyFen;
		try {
			moneyFen = Long.parseLong(row.getMoney() == null ? "0" : row.getMoney().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("当前订单不存在");
		}
		if (moneyFen <= 0L) {
			throw new BadRequestException("当前订单不存在");
		}

		if (isWxPayChannel(payType)) {
			long companyId = Long.parseLong(rowCompany);
			String openId = str(authInfo.get("open_id"));
			String wxaAppId = str(authInfo.get("wxapp_appid"));
			String woaAppId = str(authInfo.get("woa_appid"));
			String shopName = row.getShopName() == null ? "" : row.getShopName();
			String detail = row.getDetail();
			if (!StringUtils.hasText(detail)) {
				Object d = mergedInput.get("detail");
				detail = d == null ? "" : String.valueOf(d).trim();
			}
			String clientIp = clientIpFrom(mergedInput);

			Map<String, Object> unifyOut =
					depositWxpayRechargeUnifyService.unifyAndBuildClientPayParams(
							companyId,
							0L,
							depositTradeId,
							moneyFen,
							openId,
							wxaAppId,
							woaAppId,
							shopName,
							detail,
							clientIp,
							payType);
			Map<String, Object> payResult = new LinkedHashMap<>(unifyOut);
			payResult.put("order_type", "recharge");
			return payResult;
		}

		throw new BadRequestException("无此类型支付！");
	}

	private static boolean isWxPayChannel(String payType) {
		if (payType == null || payType.isEmpty()) {
			return false;
		}
		return "wxpay".equals(payType)
				|| "wxpayh5".equals(payType)
				|| payType.startsWith("wxpay");
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

	public Map<String, Object> getDepositTradeRowMapOrThrow(String depositTradeId) {
		if (depositTradeId == null || depositTradeId.isBlank()) {
			throw new ResourceException("添加失败");
		}
		DepositTrade row = depositTradeMapper.selectById(depositTradeId.trim());
		if (row == null) {
			throw new ResourceException("添加失败");
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("deposit_trade_id", row.getDepositTradeId());
		m.put("company_id", row.getCompanyId());
		m.put("member_card_code", row.getMemberCardCode());
		m.put("shop_id", row.getShopId());
		m.put("shop_name", row.getShopName());
		m.put("user_id", row.getUserId());
		m.put("mobile", row.getMobile());
		m.put("open_id", row.getOpenId());
		m.put("money", row.getMoney());
		m.put("trade_type", row.getTradeType());
		m.put("trade_status", row.getTradeStatus());
		m.put("transaction_id", row.getTransactionId());
		m.put("recharge_rule_id", row.getRechargeRuleId());
		m.put("bank_type", row.getBankType());
		m.put("authorizer_appid", row.getAuthorizerAppid());
		m.put("pay_type", row.getPayType());
		m.put("wxa_appid", row.getWxaAppid());
		m.put("detail", row.getDetail());
		m.put("time_start", row.getTimeStart());
		m.put("time_expire", row.getTimeExpire());
		m.put("fee_type", row.getFeeType());
		m.put("cur_fee_type", row.getCurFeeType());
		m.put("cur_fee_rate", row.getCurFeeRate());
		m.put("cur_fee_symbol", row.getCurFeeSymbol());
		m.put("cur_pay_fee", row.getCurPayFee());
		return m;
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
}
