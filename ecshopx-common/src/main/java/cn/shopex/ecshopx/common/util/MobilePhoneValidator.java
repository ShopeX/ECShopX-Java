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

package cn.shopex.ecshopx.common.util;

import java.util.regex.Pattern;

/**
 * 手机号格式校验：中国大陆与埃及本地/国际号。
 */
public final class MobilePhoneValidator {

	private static final Pattern CN = Pattern.compile("^1[3456789]\\d{9}$");

	/** 埃及本地：01[0125] + 8 位 */
	private static final Pattern EG_LOCAL = Pattern.compile("^01[0125]\\d{8}$");

	/** 埃及国际：可选 +/00，再 20，可选国内前导 0，再 1[0125]+8 位 */
	private static final Pattern EG_INTL = Pattern.compile("^(\\+|00)?200?1[0125]\\d{8}$");

	private MobilePhoneValidator() {}

	public static boolean isCnMobile(String mobile) {
		String normalized = normalize(mobile);
		return !normalized.isEmpty() && CN.matcher(normalized).matches();
	}

	public static boolean isCnOrEgMobile(String mobile) {
		String normalized = normalize(mobile);
		if (normalized.isEmpty()) {
			return false;
		}
		return CN.matcher(normalized).matches()
				|| EG_LOCAL.matcher(normalized).matches()
				|| EG_INTL.matcher(normalized).matches();
	}

	static String normalize(String mobile) {
		if (mobile == null) {
			return "";
		}
		return mobile.replace(" ", "").replace("-", "");
	}
}
