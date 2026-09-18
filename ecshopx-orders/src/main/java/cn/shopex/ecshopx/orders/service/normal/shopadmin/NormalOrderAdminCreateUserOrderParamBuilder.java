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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.admin.MemberLookupByUserIdOnlyService;
import cn.shopex.ecshopx.orders.support.ScalarEmptyCompat;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;

@Service
public class NormalOrderAdminCreateUserOrderParamBuilder {

	private static final Pattern INT_STRING = Pattern.compile("^-?\\d+$");

	private final ShopadminNormalOrderMarkdownValidator markdownValidator;
	private final MemberLookupByUserIdOnlyService memberLookupByUserIdOnlyService;

	public NormalOrderAdminCreateUserOrderParamBuilder(
			ShopadminNormalOrderMarkdownValidator markdownValidator,
			MemberLookupByUserIdOnlyService memberLookupByUserIdOnlyService) {
		this.markdownValidator = markdownValidator;
		this.memberLookupByUserIdOnlyService = memberLookupByUserIdOnlyService;
	}

	public NormalOrderCreateState build(
			long companyId, long operatorId, Map<String, Object> mergedInput, HttpServletRequest request) {
		NormalOrderCreateState state = new NormalOrderCreateState();
		if (mergedInput.containsKey("markdown") && mergedInput.get("markdown") != null) {
			markdownValidator.validate(mergedInput.get("markdown"));
		}
		Object raw = mergedInput.get("user_id");
		long uid;
		if (ScalarEmptyCompat.isEmpty(raw)) {
			uid = 0L;
		} else if (raw instanceof Number n) {
			uid = n.longValue();
		} else {
			String s = raw.toString().trim();
			if (!INT_STRING.matcher(s).matches()) {
				uid = 0L;
			} else {
				try {
					uid = Long.parseLong(s);
				} catch (NumberFormatException e) {
					throw new ResourceException("user_id 超出范围");
				}
			}
		}
		Map<String, Object> userinfo = null;
		if (uid > 0L) {
			userinfo = memberLookupByUserIdOnlyService.requireMemberRowForFirstHop(uid);
		}
		state.getParams().put("promotion", "normal");
		state.getParams().put("order_source", "shop_offline");
		state.getParams().put("is_online_order", Boolean.FALSE);
		state.getParams().put("receipt_type", "ziti");
		state.getParams().put("order_type", "normal_shopadmin");
		state.getParams().put("source_from", "dianwu");
		state.getParams().put("pay_type", firstString(mergedInput.get("pay_type"), "pos"));
		state.getParams().put("not_use_coupon", firstInt(mergedInput.get("not_use_coupon"), 0));
		state.getParams().put("coupon_discount", firstString(mergedInput.get("coupon_discount"), "0"));
		state.getParams().put("point_use", firstString(mergedInput.get("point_use"), "0"));
		state.getParams().put("remark", firstString(mergedInput.get("remark"), ""));
		state.getParams().put("distributor_id", parseNonNegativeLongLoose(mergedInput.get("distributor_id")));
		state.getParams().put("user_id", uid);
		if (userinfo != null) {
			state.getParams().put("mobile", stringOrEmpty(userinfo.get("mobile")));
			state.getParams().put("authorizer_appid", stringOrEmpty(userinfo.get("woa_appid")));
			state.getParams().put("wxa_appid", stringOrEmpty(userinfo.get("wxapp_appid")));
		} else {
			state.getParams().put("mobile", "");
			state.getParams().put("authorizer_appid", "");
			state.getParams().put("wxa_appid", "");
		}
		if (mergedInput.containsKey("markdown") && mergedInput.get("markdown") != null) {
			state.getParams().put("markdown", mergedInput.get("markdown"));
		}
		state.getParams().put("operator_id", operatorId);
		state.getParams().put("company_id", companyId);
		state.getParams().put("country_code", firstString(mergedInput.get("country_code"), "zh-CN"));
		return state;
	}

	private static String firstString(Object v, String def) {
		if (v == null) {
			return def;
		}
		String s = v.toString().trim();
		return StringUtils.hasText(s) ? s : def;
	}

	private static int firstInt(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static long parseNonNegativeLongLoose(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			return Math.max(0L, x);
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			long x = Long.parseLong(s);
			return Math.max(0L, x);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringOrEmpty(Object v) {
		return v == null ? "" : v.toString();
	}
}
