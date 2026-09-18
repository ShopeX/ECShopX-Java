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

package cn.shopex.ecshopx.common.companys.language;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

/** 公司语言设置固定四种 canonical code 与展示名（ECX-10021 §2.3）。 */
public final class LanguageCanonicalCodes {

	public static final List<String> FIXED_ORDER =
			List.of("zh-CN", "en-CN", "ar-SA", "zh-TW");

	private static final Map<String, String> ALIASES =
			Map.of(
					"en-us", "en-CN",
					"en", "en-CN",
					"ar", "ar-SA",
					"ar-sa", "ar-SA");

	private static final Map<String, String> DISPLAY_NAMES =
			new LinkedHashMap<>(
					Map.of(
							"zh-CN", "中文",
							"en-CN", "English",
							"ar-SA", "العربية",
							"zh-TW", "中文繁体"));

	private LanguageCanonicalCodes() {}

	public static String displayName(String canonicalCode) {
		return DISPLAY_NAMES.getOrDefault(canonicalCode, canonicalCode);
	}

	public static int sortOf(String canonicalCode) {
		for (int i = 0; i < FIXED_ORDER.size(); i++) {
			if (FIXED_ORDER.get(i).equals(canonicalCode)) {
				return i + 1;
			}
		}
		return 0;
	}

	public static boolean isFixedCanonical(String code) {
		return code != null && FIXED_ORDER.contains(code);
	}

	/** 将请求别名归一化为 canonical；未知固定语言时返回 {@code null}。 */
	public static String normalize(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String trimmed = raw.trim();
		for (String canonical : FIXED_ORDER) {
			if (canonical.equalsIgnoreCase(trimmed)) {
				return canonical;
			}
		}
		String alias = ALIASES.get(trimmed.toLowerCase(Locale.ROOT));
		return alias;
	}
}
