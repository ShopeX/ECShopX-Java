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

package cn.shopex.ecshopx.common.openapi;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/** OpenAPI 鉴权上下文（对齐 PHP {@code $request->attributes['auth']['company_id']}）。 */
public final class OpenapiAuthAttributes {

	public static final String REQUEST_ATTRIBUTE =
			"cn.shopex.ecshopx.openapi.OPENAPI_AUTH";

	private OpenapiAuthAttributes() {}

	public static void setAuth(HttpServletRequest request, long companyId) {
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", companyId);
		request.setAttribute(REQUEST_ATTRIBUTE, auth);
	}

	public static long requireCompanyId(HttpServletRequest request) {
		Object attr = request.getAttribute(REQUEST_ATTRIBUTE);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new UnauthorizedException("未授权");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未授权");
		}
		try {
			long companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
			if (companyId <= 0L) {
				throw new UnauthorizedException("未授权");
			}
			return companyId;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未授权");
		}
	}
}
