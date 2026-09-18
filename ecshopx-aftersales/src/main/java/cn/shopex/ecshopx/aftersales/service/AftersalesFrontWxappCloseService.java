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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesFrontWxappCloseService {

	private static final char FW_COMMA = '\uFF0C';

	private final AftersalesService aftersalesService;

	public AftersalesFrontWxappCloseService(AftersalesService aftersalesService) {
		this.aftersalesService = aftersalesService;
	}

	public Map<String, Object> closeConfirm(HttpServletRequest request, LinkedHashMap<String, Object> merged) {
		validateCloseMatrix(merged);

		long companyId = parseLongStrict(merged.get("company_id"), "企业id必填");
		long aftersalesBn = parseLongStrict(merged.get("aftersales_bn"), "售后单号必填");
		long effectiveUserId = longVal(merged.get("user_id"));

		merged.put("memo", "消费者主动关闭售后");

		return aftersalesService.closeAftersalesForConsumer(
				companyId,
				aftersalesBn,
				effectiveUserId,
				resolveOperatorType(request),
				effectiveUserId,
				merged,
				"售后单号：" + aftersalesBn + "，消费者主动关闭售后",
				this);
	}

	private String resolveOperatorType(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			return "user";
		}
		Object ot = authRaw.get("operator_type");
		if (ot == null) {
			return "user";
		}
		String s = String.valueOf(ot).trim();
		return s.isEmpty() ? "user" : s;
	}

	private void validateCloseMatrix(LinkedHashMap<String, Object> merged) {
		List<String> segments = new ArrayList<>();
		if (isBlank(merged.get("aftersales_bn"))) {
			segments.add("售后单号必填");
		}
		if (isBlank(merged.get("company_id"))) {
			segments.add("企业id必填");
		}
		if (isBlank(merged.get("user_id"))) {
			segments.add("会员id必填");
		}
		if (!segments.isEmpty()) {
			throw new ResourceException(trimEdgeFullWidthComma(String.join(String.valueOf(FW_COMMA), segments)));
		}
		parseLongStrict(merged.get("company_id"), "企业id必填");
		parseLongStrict(merged.get("aftersales_bn"), "售后单号必填");
		parseLongStrict(merged.get("user_id"), "会员id必填");
	}

	private static long parseLongStrict(Object o, String absentMessage) {
		if (o == null) {
			throw new ResourceException(absentMessage);
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(absentMessage);
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException(absentMessage);
		}
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

	private static boolean isBlank(Object o) {
		return o == null || !StringUtils.hasText(String.valueOf(o).trim());
	}

	private static String trimEdgeFullWidthComma(String s) {
		if (s == null) {
			return "";
		}
		String r = s;
		while (r.startsWith(String.valueOf(FW_COMMA))) {
			r = r.substring(1);
		}
		while (r.endsWith(String.valueOf(FW_COMMA))) {
			r = r.substring(0, r.length() - 1);
		}
		return r;
	}
}
