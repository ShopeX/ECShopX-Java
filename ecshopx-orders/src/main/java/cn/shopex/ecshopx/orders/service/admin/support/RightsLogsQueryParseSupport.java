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

package cn.shopex.ecshopx.orders.service.admin.support;

import java.math.BigDecimal;
import org.springframework.util.StringUtils;

public final class RightsLogsQueryParseSupport {

	private RightsLogsQueryParseSupport() {}

	public static boolean hasTimeToken(String s) {
		return StringUtils.hasText(s) && !"0".equals(s.trim());
	}

	public static boolean isNumericUnixSecondsToken(String s) {
		if (s == null) {
			return false;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return false;
		}
		try {
			new BigDecimal(t).stripTrailingZeros();
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	public static int parseIntLooseAsInt(String raw) {
		if (raw == null) {
			return 0;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0;
		}
		int i = 0;
		boolean neg = false;
		if (s.charAt(0) == '-') {
			neg = true;
			i = 1;
		}
		int start = i;
		while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
			i++;
		}
		if (i == start) {
			return 0;
		}
		String digitPart = s.substring(start, i);
		try {
			long v = Long.parseLong(digitPart);
			if (neg) {
				v = -v;
			}
			if (v > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (v < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) v;
		} catch (NumberFormatException e) {
			return neg ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		}
	}
}
