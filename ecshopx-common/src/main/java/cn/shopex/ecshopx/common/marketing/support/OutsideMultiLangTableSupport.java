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
 * Multi-lang physical shard table suffix, aligned with PHP {@code MultiLangItem::normalizeTableLanguage}:
 * legacy three locales keep camelCase; all other tags drop {@code -} and lower-case (e.g. {@code zh-TW} → {@code zhtw}).
 */
public final class OutsideMultiLangTableSupport {

	private static final Map<String, String> LEGACY_TABLE_LANGUAGE_SUFFIXES = Map.of(
			"zh-CN", "zhCN",
			"en-CN", "enCN",
			"ar-SA", "arSA");

	private OutsideMultiLangTableSupport() {}

	public static String normalizeLangTableSuffix(String langTag) {
		if (!StringUtils.hasText(langTag)) {
			return "zhCN";
		}
		String trimmed = langTag.trim();
		if ("undefined".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
			return "zhCN";
		}
		String legacy = LEGACY_TABLE_LANGUAGE_SUFFIXES.get(trimmed);
		if (legacy != null) {
			return legacy;
		}
		// case-insensitive legacy match
		for (Map.Entry<String, String> e : LEGACY_TABLE_LANGUAGE_SUFFIXES.entrySet()) {
			if (e.getKey().equalsIgnoreCase(trimmed)) {
				return e.getValue();
			}
		}
		String s = trimmed.replace("-", "");
		if (!s.matches("[A-Za-z0-9]+")) {
			return "zhCN";
		}
		return s.toLowerCase(Locale.ROOT);
	}
}
