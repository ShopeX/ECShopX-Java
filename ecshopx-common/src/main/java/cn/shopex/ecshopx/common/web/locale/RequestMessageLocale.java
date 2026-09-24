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

package cn.shopex.ecshopx.common.web.locale;

import cn.shopex.ecshopx.common.config.LangueProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * C 端文案语言：请求里的 {@code country_code} 优先，未传时用调用方给出的公司默认语种（后管语言设置）。
 * 英文包文件名是 {@code messages_en_US}，因此 {@code en-CN} 的 MessageSource Locale 用 {@code en-US}。
 */
public final class RequestMessageLocale {

	private RequestMessageLocale() {}

	public static String langTag(
			LangueProperties langueProperties, HttpServletRequest request, Map<String, ?> body, String companyDefaultLanguage) {
		return langTag(langueProperties, countryCode(request, body), companyDefaultLanguage);
	}

	public static String langTag(LangueProperties langueProperties, String rawCountryCode, String companyDefaultLanguage) {
		if (StringUtils.hasText(rawCountryCode)) {
			return langueProperties.resolveToSupportedTag(rawCountryCode.trim());
		}
		if (StringUtils.hasText(companyDefaultLanguage)) {
			return langueProperties.resolveToSupportedTag(companyDefaultLanguage.trim());
		}
		return langueProperties.resolveToSupportedTag(null);
	}

	public static Locale messageLocale(
			LangueProperties langueProperties, HttpServletRequest request, Map<String, ?> body, String companyDefaultLanguage) {
		return toMessageLocale(langTag(langueProperties, request, body, companyDefaultLanguage));
	}

	public static Locale toMessageLocale(String langTag) {
		if ("en-CN".equalsIgnoreCase(langTag)) {
			return Locale.forLanguageTag("en-US");
		}
		String tag = langTag == null ? "" : langTag.replace('_', '-');
		return Locale.forLanguageTag(tag);
	}

	public static String countryCode(HttpServletRequest request, Map<String, ?> body) {
		String raw = request == null ? null : request.getParameter("country_code");
		if (!StringUtils.hasText(raw) && body != null) {
			Object fromBody = body.get("country_code");
			if (fromBody != null) {
				raw = fromBody.toString();
			}
		}
		return StringUtils.hasText(raw) ? raw.trim() : null;
	}
}
