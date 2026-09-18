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

package cn.shopex.ecshopx.kaquan.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Resolves {@code x-datapass-block} for VIP grade order list: same value drives response echo and
 * masking eligibility (no HTTP header; query / path / request attribute only).
 */
public final class VipGradeOrderDatapassSupport {

	private static final String KEY = "x-datapass-block";

	private VipGradeOrderDatapassSupport() {}

	/**
	 * Resolution order (no header): query/form parameter → URI template variable → request attribute → {@code 0}.
	 * URL-encoded form fields are included in {@link HttpServletRequest#getParameter(String)}.
	 */
	public static Object resolveXDatapassBlock(HttpServletRequest request) {
		String queryOrForm = request.getParameter(KEY);
		if (queryOrForm != null) {
			return queryOrForm;
		}
		Object fromPath = resolveFromPathVariables(request);
		if (fromPath != null) {
			return fromPath;
		}
		Object attr = request.getAttribute(KEY);
		if (attr != null) {
			return attr;
		}
		return 0;
	}

	/**
	 * Whether the resolved datapass value enables masking: {@code null}, {@link Boolean#FALSE},
	 * numeric zero, blank after trim, the string {@code "0"}, and {@code "false"} (case-insensitive)
	 * yield {@code false}; {@link Boolean#TRUE}, any non-zero number, or any other non-empty string
	 * yields {@code true}.
	 */
	public static boolean isTruthyForMasking(Object resolved) {
		if (resolved == null) {
			return false;
		}
		if (resolved instanceof Boolean b) {
			return b;
		}
		if (resolved instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = resolved.toString().trim();
		if (s.isEmpty() || "0".equals(s)) {
			return false;
		}
		return !"false".equalsIgnoreCase(s);
	}

	@SuppressWarnings("unchecked")
	private static Object resolveFromPathVariables(HttpServletRequest request) {
		Object raw = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (!(raw instanceof Map<?, ?> map)) {
			return null;
		}
		return map.get(KEY);
	}
}
