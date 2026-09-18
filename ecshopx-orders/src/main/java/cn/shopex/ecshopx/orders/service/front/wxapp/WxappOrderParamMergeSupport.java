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

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class WxappOrderParamMergeSupport {

	private WxappOrderParamMergeSupport() {}

	public static LinkedHashMap<String, Object> applyDefaultsAndAuth(
			Map<String, Object> merged, Map<String, Object> sessionAuth) {
		LinkedHashMap<String, Object> p = new LinkedHashMap<>(merged);
		long companyId = longVal(sessionAuth.get("company_id"));
		long userId = longVal(sessionAuth.get("user_id"));
		p.put("company_id", companyId);
		p.put("user_id", userId);
		p.put("mobile", stringVal(sessionAuth.get("mobile")));
		p.put("nickname", stringVal(sessionAuth.get("nickname")));
		p.put("authorizer_appid", stringVal(sessionAuth.get("woa_appid")));
		String sessionWxappAppid = stringVal(sessionAuth.get("wxapp_appid"));
		p.put("wxa_appid", sessionWxappAppid);
		p.put("wxapp_appid", sessionWxappAppid);
		p.put("alipay_appid", stringVal(sessionAuth.get("alipay_appid")));
		String orderType = stringVal(p.get("order_type"));
		p.put("order_type", StringUtils.hasText(orderType) ? orderType.trim() : "service");
		String payType = stringVal(p.get("pay_type"));
		p.put("pay_type", StringUtils.hasText(payType) ? payType.trim() : "wxpay");
		p.put("point_use", intInput(p.get("point_use"), 0));
		p.put("iscrossborder", intInput(p.get("iscrossborder"), 0));
		p.put("isShopScreen", intInput(p.get("isShopScreen"), 0));
		p.put("isNostores", intInput(p.get("isNostores"), 0));
		String receipt = stringVal(p.get("receipt_type"));
		p.put("receipt_type", StringUtils.hasText(receipt) ? receipt.trim() : "logistics");
		return p;
	}

	public static LinkedHashMap<String, Object> applyDefaultsAndAuthForOrderNew(
			Map<String, Object> merged, Map<String, Object> sessionAuth) {
		LinkedHashMap<String, Object> p = new LinkedHashMap<>(merged);
		long companyId = longVal(sessionAuth.get("company_id"));
		long userId = longVal(sessionAuth.get("user_id"));
		p.put("company_id", companyId);
		p.put("user_id", userId);
		p.put("mobile", stringVal(sessionAuth.get("mobile")));
		p.put("nickname", stringVal(sessionAuth.get("nickname")));
		p.put("authorizer_appid", stringVal(sessionAuth.get("woa_appid")));
		String sessionWxappAppid = stringVal(sessionAuth.get("wxapp_appid"));
		p.put("wxa_appid", sessionWxappAppid);
		p.put("wxapp_appid", sessionWxappAppid);
		p.put("alipay_appid", stringVal(sessionAuth.get("alipay_appid")));

		if (!merged.containsKey("order_type") || merged.get("order_type") == null) {
			p.put("order_type", "service");
		}

		if (!merged.containsKey("pay_type") || merged.get("pay_type") == null) {
			p.put("pay_type", "");
		} else {
			Object pt = merged.get("pay_type");
			if (pt != null && StringUtils.hasText(pt.toString())) {
				p.put("pay_type", pt.toString().trim());
			}
		}

		p.put("point_use", intInput(p.get("point_use"), 0));
		p.put("iscrossborder", intInput(p.get("iscrossborder"), 0));
		p.put("isShopScreen", intInput(p.get("isShopScreen"), 0));
		p.put("isNostores", intInput(p.get("isNostores"), 0));
		String receipt = stringVal(p.get("receipt_type"));
		p.put("receipt_type", StringUtils.hasText(receipt) ? receipt.trim() : "logistics");
		p.put("isSalesmanPage", intInput(merged.get("isSalesmanPage"), 0));
		if (merged.containsKey("promoter_user_id")) {
			p.put("promoter_user_id", longVal(merged.get("promoter_user_id")));
		}
		return p;
	}

	public static LinkedHashMap<String, Object> applyDefaultsAndAuthForFreightFee(
			Map<String, Object> merged, Map<String, Object> sessionAuth) {
		LinkedHashMap<String, Object> p = new LinkedHashMap<>(merged);
		long companyId = longVal(sessionAuth.get("company_id"));
		long userId = longVal(sessionAuth.get("user_id"));
		p.put("company_id", companyId);
		p.put("user_id", userId);
		p.put("mobile", stringVal(sessionAuth.get("mobile")));
		p.put("nickname", stringVal(sessionAuth.get("nickname")));
		p.put("authorizer_appid", stringVal(sessionAuth.get("woa_appid")));
		String sessionWxappAppid = stringVal(sessionAuth.get("wxapp_appid"));
		p.put("wxa_appid", sessionWxappAppid);
		p.put("wxapp_appid", sessionWxappAppid);
		p.put("alipay_appid", stringVal(sessionAuth.get("alipay_appid")));
		String orderType = stringVal(p.get("order_type"));
		p.put("order_type", StringUtils.hasText(orderType) ? orderType.trim() : "service");
		String payType = stringVal(p.get("pay_type"));
		p.put("pay_type", StringUtils.hasText(payType) ? payType.trim() : "wxpay");
		p.put("not_use_coupon", intInput(p.get("not_use_coupon"), 0));
		p.put("iscrossborder", intInput(p.get("iscrossborder"), 0));
		p.put("isShopScreen", intInput(p.get("isShopScreen"), 0));
		p.put("isNostores", intInput(p.get("isNostores"), 0));
		return p;
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
