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

package cn.shopex.ecshopx.espier.api.admin.v1;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public final class EspierAdminJwtControllerSupport {

	private EspierAdminJwtControllerSupport() {}

	@SuppressWarnings("unchecked")
	public static long extractCompanyId(HttpServletRequest request) {
		Map<String, Object> user =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (user == null) {
			throw new ResourceException("未登录");
		}
		Object v = user.get("company_id");
		if (v == null) {
			throw new ResourceException("无法获取公司ID");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(v.toString());
	}

	public static Long parseLongOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static String optionalTrimmedString(Object v) {
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		return s.isEmpty() ? null : s;
	}

	public static long parseDistributorId(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		if (t.charAt(0) == '+') {
			t = t.substring(1).trim();
		}
		if (t.contains(".") || t.toLowerCase().contains("e")) {
			throw new BadRequestException("distributor_id 格式错误");
		}
		try {
			long v = Long.parseLong(t);
			if (v < 0) {
				throw new BadRequestException("distributor_id 格式错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 格式错误");
		}
	}
}
