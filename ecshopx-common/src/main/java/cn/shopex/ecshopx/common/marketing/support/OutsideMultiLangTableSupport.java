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

package cn.shopex.ecshopx.common.marketing.support;

import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Multi-lang physical shard table suffix.
 *
 * <p>Canonical tags map to camelCase suffixes ({@code zh-CN}→{@code zhCN}, {@code zh-TW}→{@code zhTW}).
 * Other {@code language-REGION} tags use the same rule: drop {@code -}, lower-case the language part,
 * upper-case the region part(s).
 */
public final class OutsideMultiLangTableSupport {

	private static final Map<String, String> TABLE_LANGUAGE_SUFFIXES = Map.of(
			"zh-CN", "zhCN",
			"en-CN", "enCN",
			"ar-SA", "arSA",
			"zh-TW", "zhTW",
			"ja-JP", "jaJP",
			"ru-RU", "ruRU");

	private OutsideMultiLangTableSupport() {}

	public static String normalizeLangTableSuffix(String langTag) {
		if (!StringUtils.hasText(langTag)) {
			return "zhCN";
		}
		String trimmed = langTag.trim();
		if ("undefined".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
			return "zhCN";
		}
		String known = TABLE_LANGUAGE_SUFFIXES.get(trimmed);
		if (known != null) {
			return known;
		}
		for (Map.Entry<String, String> e : TABLE_LANGUAGE_SUFFIXES.entrySet()) {
			if (e.getKey().equalsIgnoreCase(trimmed)) {
				return e.getValue();
			}
		}
		String camel = toCamelLocaleSuffix(trimmed);
		if (camel == null || !camel.matches("[A-Za-z0-9]+")) {
			return "zhCN";
		}
		return camel;
	}

	/** {@code zh-TW} / {@code zhtw} → {@code zhTW}; invalid input → {@code null}. */
	private static String toCamelLocaleSuffix(String langTag) {
		String[] parts = langTag.split("-");
		if (parts.length >= 2) {
			StringBuilder sb = new StringBuilder();
			sb.append(parts[0].toLowerCase(Locale.ROOT));
			for (int i = 1; i < parts.length; i++) {
				sb.append(parts[i].toUpperCase(Locale.ROOT));
			}
			return sb.toString();
		}
		String s = langTag.replace("-", "");
		if (s.length() < 2 || !s.matches("[A-Za-z0-9]+")) {
			return null;
		}
		return s.substring(0, 2).toLowerCase(Locale.ROOT) + s.substring(2).toUpperCase(Locale.ROOT);
	}
}
