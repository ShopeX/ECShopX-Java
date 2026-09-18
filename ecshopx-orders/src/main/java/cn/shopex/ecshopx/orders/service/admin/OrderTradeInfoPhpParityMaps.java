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

package cn.shopex.ecshopx.orders.service.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OrderTradeInfoPhpParityMaps {

	private static final DateTimeFormatter TRADE_PAY_DATE_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private OrderTradeInfoPhpParityMaps() {}

	public static Map<String, Object> tradeSnakeToPhpCamel(Map<String, Object> s, ObjectMapper objectMapper) {
		LinkedHashMap<String, Object> o = new LinkedHashMap<>();
		o.put("tradeId", s.get("trade_id"));
		o.put("orderId", s.get("order_id"));
		o.put("shopId", s.get("shop_id"));
		o.put("userId", s.get("user_id"));
		o.put("mobile", s.get("mobile"));
		o.put("openId", s.get("open_id"));
		o.put("discountInfo", decodeJsonField(s.get("discount_info"), objectMapper));
		o.put("mchId", s.get("mch_id"));
		o.put("totalFee", s.get("total_fee"));
		o.put("discountFee", s.get("discount_fee"));
		o.put("feeType", s.get("fee_type"));
		o.put("payFee", s.get("pay_fee"));
		o.put("tradeNo", s.get("trade_no"));
		o.put("tradeState", s.get("trade_state"));
		o.put("payType", s.get("pay_type"));
		o.put("transactionId", s.get("transaction_id"));
		o.put("wxaAppid", s.get("wxa_appid"));
		o.put("bankType", s.get("bank_type"));
		o.put("body", s.get("body"));
		o.put("detail", s.get("detail"));
		o.put("timeStart", s.get("time_start"));
		o.put("timeExpire", s.get("time_expire"));
		o.put("companyId", s.get("company_id"));
		o.put("authorizerAppid", s.get("authorizer_appid"));
		o.put("curFeeType", s.get("cur_fee_type"));
		Object curRate = s.get("cur_fee_rate");
		if (curRate instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
			o.put("curFeeRate", n.intValue());
		} else {
			o.put("curFeeRate", curRate);
		}
		o.put("curFeeSymbol", s.get("cur_fee_symbol"));
		o.put("curPayFee", s.get("cur_pay_fee"));
		o.put("distributorId", s.get("distributor_id"));
		o.put("tradeSourceType", s.get("trade_source_type"));
		o.put("couponFee", s.get("coupon_fee"));
		o.put("couponInfo", decodeJsonField(s.get("coupon_info"), objectMapper));
		o.put("initalRequest", s.get("inital_request"));
		o.put("initalResponse", s.get("inital_response"));
		o.put("payChannel", s.get("pay_channel"));
		o.put("divMembers", s.get("div_members"));
		o.put("refundedFee", s.get("refunded_fee"));
		o.put("adapayFeeMode", s.get("adapay_fee_mode"));
		o.put("adapayDivStatus", s.get("adapay_div_status"));
		o.put("adapayFee", s.get("adapay_fee"));
		o.put("dealerId", s.get("dealer_id"));
		o.put("merchantId", stringifyTradeId(s.get("merchant_id")));
		o.put("isSettled", s.get("is_settled"));
		o.put("paymentParams", s.get("payment_params"));
		o.put("supplierId", s.get("supplier_id"));
		o.put("bspayDivStatus", s.get("bspay_div_status"));
		o.put("bspayReqDate", s.get("bspay_req_date"));
		o.put("bspayDivMembers", s.get("bspay_div_members"));
		o.put("bspayFeeMode", s.get("bspay_fee_mode"));
		o.put("bspayFee", s.get("bspay_fee"));
		o.put("payDate", formatTradePayDate(s.get("time_expire")));
		return o;
	}

	public static String formatTradePayDate(Object timeExpire) {
		if (timeExpire == null) {
			return "";
		}
		long sec = longVal(timeExpire);
		if (sec <= 0L) {
			return "";
		}
		if (sec > 9_999_999_999L) {
			sec = sec / 1000L;
		}
		try {
			return TRADE_PAY_DATE_FMT.format(Instant.ofEpochSecond(sec));
		} catch (Exception e) {
			return "";
		}
	}

	private static Object decodeJsonField(Object raw, ObjectMapper objectMapper) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
			return raw;
		}
		if (!(raw instanceof String str) || !StringUtils.hasText(str.trim())) {
			return null;
		}
		try {
			return objectMapper.readValue(str.trim(), new TypeReference<Object>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private static String stringifyTradeId(Object o) {
		if (o == null) {
			return "0";
		}
		if (o instanceof String str) {
			return str;
		}
		return String.valueOf(o);
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
}
