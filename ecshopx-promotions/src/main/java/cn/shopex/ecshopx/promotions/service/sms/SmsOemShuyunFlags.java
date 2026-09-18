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

package cn.shopex.ecshopx.promotions.service.sms;

import java.util.Locale;
import org.springframework.core.env.Environment;

public final class SmsOemShuyunFlags {

	private SmsOemShuyunFlags() {}

	public static boolean effectiveTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b.booleanValue();
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		if (v instanceof CharSequence s) {
			String t = s.toString().trim();
			if (t.isEmpty()) {
				return false;
			}
			String lower = t.toLowerCase(Locale.ROOT);
			if ("0".equals(lower)
					|| "false".equals(lower)
					|| "no".equals(lower)
					|| "off".equals(lower)) {
				return false;
			}
			return true;
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return false;
		}
		String lower = t.toLowerCase(Locale.ROOT);
		if ("0".equals(lower)
				|| "false".equals(lower)
				|| "no".equals(lower)
				|| "off".equals(lower)) {
			return false;
		}
		return true;
	}

	public static boolean isOemShuyun(Environment environment) {
		return effectiveTruthy(environment.getProperty("ecshopx.request-field.oem-shuyun"));
	}
}
