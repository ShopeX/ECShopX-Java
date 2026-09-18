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

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Mirrors PHP {@code data_masking($strType, $str)} in {@code app/helpers.php}.
 */
public final class DataMasking {

	private static final Pattern CHINA_MOBILE = Pattern.compile("^1[3456789]\\d{9}$");
	private static final String PLACEHOLDER_IMAGE =
			"https://b-img-cdn.yuanyuanke.cn/image/21/2021/10/21/6522d21e446741584632bc04601feb6fBy5oX5syerwAs6FP1cyfOWJd90z5Mb3g";

	private DataMasking() {
	}

	public enum Type {
		UNAME,
		TRUENAME,
		BIRTHDAY,
		BANKCARD,
		IDCARD,
		MOBILE,
		EMAIL,
		ADDRESS,
		DETAILED_ADDRESS,
		IMAGE,
		SEX
	}

	public static String mask(Type type, String str) {
		if (type == null) {
			return str;
		}
		return switch (type) {
			case UNAME -> maskUname(str);
			case TRUENAME -> maskTruename(str);
			case BIRTHDAY -> maskBirthday(str);
			case BANKCARD -> maskBankcard(str);
			case IDCARD -> maskIdcard(str);
			case MOBILE -> maskMobile(str);
			case EMAIL -> maskEmail(str);
			case ADDRESS -> maskAddress(str);
			case DETAILED_ADDRESS -> maskDetailedAddress(str);
			case IMAGE -> maskImage(str);
			case SEX -> maskSex(str);
		};
	}

	public static String mask(String type, String str) {
		if (type == null || type.isBlank()) {
			return str;
		}
		return switch (type.trim().toLowerCase(Locale.ROOT)) {
			case "uname" -> maskUname(str);
			case "truename" -> maskTruename(str);
			case "birthday" -> maskBirthday(str);
			case "bankcard" -> maskBankcard(str);
			case "idcard" -> maskIdcard(str);
			case "mobile" -> maskMobile(str);
			case "email" -> maskEmail(str);
			case "address" -> maskAddress(str);
			case "detailedaddress" -> maskDetailedAddress(str);
			case "image" -> maskImage(str);
			case "sex" -> maskSex(str);
			default -> str;
		};
	}

	public static String maskUname(String str) {
		if (isBlank(str)) {
			return str;
		}
		String t = str.trim();
		if (CHINA_MOBILE.matcher(t).matches()) {
			return t.substring(0, 3) + "****" + t.substring(7);
		}
		return t;
	}

	public static String maskTruename(String str) {
		if (str == null) {
			return null;
		}
		if (str.trim().isEmpty()) {
			return str;
		}
		String t = str.trim();
		int n = t.codePointCount(0, t.length());
		if (n <= 0) {
			return str;
		}
		if (n == 1) {
			int cp = t.codePointAt(0);
			return new String(Character.toChars(cp));
		}
		int lastCpOffset = t.offsetByCodePoints(0, n - 1);
		int lastCp = t.codePointAt(lastCpOffset);
		String last = new String(Character.toChars(lastCp));
		return "*".repeat(n - 1) + last;
	}

	public static String maskBirthday(String str) {
		if (isBlank(str)) {
			return str;
		}
		String t = str.trim();
		return "****-**-*" + t.substring(t.length() - 1);
	}

	public static String maskBankcard(String str) {
		if (str == null) {
			return null;
		}
		if (str.trim().isEmpty()) {
			return str;
		}
		String t = str.trim();
		int len = t.length();
		if (len < 5) {
			return t;
		}
		return t.substring(0, 4) + "************" + t.substring(len - 4, len - 1);
	}

	public static String maskIdcard(String str) {
		if (str == null) {
			return null;
		}
		String trimmed = str.trim();
		if (trimmed.isEmpty()) {
			return str;
		}
		int n = trimmed.length();
		if (n <= 1) {
			return String.valueOf(trimmed.charAt(n - 1));
		}
		return "*".repeat(n - 1) + trimmed.charAt(n - 1);
	}

	public static String maskMobile(String str) {
		if (str == null) {
			return null;
		}
		if (str.trim().isEmpty()) {
			return str;
		}
		String s = str.trim();
		int n = s.length();
		if (n < 3) {
			return s + "******";
		}
		if (n <= 9) {
			return s.substring(0, 3) + "******";
		}
		return s.substring(0, 3) + "******" + s.substring(9);
	}

	public static String maskEmail(String str) {
		if (isBlank(str)) {
			return str;
		}
		String t = str.trim();
		int pos = t.indexOf('@');
		if (pos <= 0) {
			return t;
		}
		String local = t.substring(0, pos);
		String domain = t.substring(pos);
		if (local.length() <= 1) {
			return local + domain;
		}
		if (local.length() == 2) {
			return local.charAt(0) + "*" + domain;
		}
		return local.charAt(0)
				+ "*".repeat(local.length() - 2)
				+ local.charAt(local.length() - 1)
				+ domain;
	}

	public static String maskAddress(String str) {
		if (isBlank(str)) {
			return str;
		}
		return "******";
	}

	public static String maskDetailedAddress(String str) {
		if (isBlank(str)) {
			return str;
		}
		String t = str.trim();
		return t.codePoints()
						.limit(6)
						.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				+ "******";
	}

	public static String maskImage(String str) {
		if (isBlank(str)) {
			return str;
		}
		return PLACEHOLDER_IMAGE;
	}

	public static String maskSex(String str) {
		if (isBlank(str)) {
			return str;
		}
		return "*";
	}

	public static String maskMobileIfBlocked(String raw, int datapassBlock) {
		if (datapassBlock == 0 || raw == null) {
			return raw;
		}
		return maskMobile(raw);
	}

	public static String maskTruenameIfBlocked(String raw, int datapassBlock) {
		if (datapassBlock == 0 || raw == null) {
			return raw;
		}
		return maskTruename(raw);
	}

	public static String maskMobileIfBlocked(Object raw, int datapassBlock) {
		if (datapassBlock == 0 || raw == null) {
			return raw == null ? null : String.valueOf(raw);
		}
		return maskMobile(String.valueOf(raw));
	}

	public static String maskTruenameIfBlocked(Object raw, int datapassBlock) {
		if (datapassBlock == 0 || raw == null) {
			return raw == null ? null : String.valueOf(raw);
		}
		return maskTruename(String.valueOf(raw));
	}

	public static String placeholderImage() {
		return PLACEHOLDER_IMAGE;
	}

	private static boolean isBlank(String str) {
		return str == null || str.trim().isEmpty();
	}
}
