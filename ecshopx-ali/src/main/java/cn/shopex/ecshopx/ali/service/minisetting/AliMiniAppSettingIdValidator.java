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

package cn.shopex.ecshopx.ali.service.minisetting;

import org.springframework.util.StringUtils;

/**
 * Determines when a {@code setting_id} value is treated as absent for mini-app setting handling:
 * {@code null}, blank or whitespace-only strings, the string {@code "0"}, and numeric zero ({@code 0}).
 */
public final class AliMiniAppSettingIdValidator {

	private AliMiniAppSettingIdValidator() {}

	/**
	 * Returns {@code true} when {@code raw} should be interpreted as no {@code setting_id}
	 * ({@code null}, blank, {@code "0"}, or zero).
	 */
	public static boolean isEmptySettingId(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return true;
			}
			String t = s.trim();
			if ("0".equals(t)) {
				return true;
			}
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		String t = raw.toString().trim();
		if (!StringUtils.hasText(t)) {
			return true;
		}
		if ("0".equals(t)) {
			return true;
		}
		try {
			return Long.parseLong(t) == 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
