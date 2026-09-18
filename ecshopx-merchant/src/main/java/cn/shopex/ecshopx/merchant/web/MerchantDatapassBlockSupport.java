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

package cn.shopex.ecshopx.merchant.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class MerchantDatapassBlockSupport {

	private static final String PARAM = "x-datapass-block";
	private static final String HEADER = "X-Datapass-Block";

	private MerchantDatapassBlockSupport() {
	}

	public static Object resolveEchoValue(HttpServletRequest request) {
		String param = request.getParameter(PARAM);
		if (param != null) {
			return param;
		}
		Object attr = request.getAttribute(PARAM);
		if (attr != null) {
			return attr;
		}
		String h = request.getHeader(HEADER);
		if (StringUtils.hasText(h)) {
			return h.trim();
		}
		return 0;
	}

	public static boolean shouldMask(Object datapassBlock) {
		if (datapassBlock == null) {
			return false;
		}
		if (datapassBlock instanceof Boolean b) {
			return b;
		}
		if (datapassBlock instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = datapassBlock.toString().trim();
		if (s.isEmpty() || "0".equals(s)) {
			return false;
		}
		return true;
	}
}
