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

package cn.shopex.ecshopx.goods.web;

import jakarta.servlet.http.HttpServletRequest;

public final class DatapassBlockResolver {

	private DatapassBlockResolver() {
	}

	public static boolean isBlocked(HttpServletRequest request) {
		if (truthyDatapassToken(request.getHeader("X-Datapass-Block"))) {
			return true;
		}
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Boolean b && b) {
			return true;
		}
		if (attr != null && truthyDatapassToken(attr.toString())) {
			return true;
		}
		return truthyDatapassToken(request.getParameter("x-datapass-block"));
	}

	/**
	 * Reads {@code x-datapass-block} from query string only for admin list APIs; headers are not consulted.
	 */
	public static boolean isBlockedFromQueryParameter(HttpServletRequest request) {
		return truthyDatapassToken(request.getParameter("x-datapass-block"));
	}

	private static boolean truthyDatapassToken(String v) {
		if (v == null) {
			return false;
		}
		String t = v.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return !"false".equalsIgnoreCase(t);
	}
}
