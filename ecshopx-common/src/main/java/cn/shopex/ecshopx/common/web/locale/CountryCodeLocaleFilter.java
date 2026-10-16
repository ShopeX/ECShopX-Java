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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 根据请求参数 {@code country_code}（优先）或 {@code Accept-Language} 设置线程级
 * {@link LocaleContextHolder}，与 {@link RequestLangTag}、
 * {@link org.springframework.context.MessageSource} 共用同一 Locale。
 *
 * <p>管理端 JSON POST 常把 {@code country_code} 放在 body，{@link HttpServletRequest#getParameter}
 * 读不到；此时回落 Accept-Language（前端已设置），避免一律当成默认语种写主表。
 *
 * <p>本 Filter 注册较早（{@code HIGHEST_PRECEDENCE + 10}），Undertow 下有时尚未解析 query，
 * 故先 {@link HttpServletRequest#getParameterMap()} 触发表单/query 解析，再读参数；仍为空时从
 * {@link HttpServletRequest#getQueryString()} 解析 {@code country_code}。
 */
public class CountryCodeLocaleFilter extends OncePerRequestFilter {

	private static final String PARAM_COUNTRY_CODE = "country_code";

	private final LangueProperties langueProperties;

	public CountryCodeLocaleFilter(LangueProperties langueProperties) {
		this.langueProperties = langueProperties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String raw = resolveCountryCodeRaw(request);
		if (!StringUtils.hasText(raw)) {
			raw = firstAcceptLanguageTag(request.getHeader("Accept-Language"));
		}
		String tag = langueProperties.resolveToSupportedTag(raw);
		Locale locale = RequestMessageLocale.toMessageLocale(tag);
		LocaleContextHolder.setLocale(locale, true);
		try {
			filterChain.doFilter(request, response);
		} finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	static String resolveCountryCodeRaw(HttpServletRequest request) {
		request.getParameterMap();
		String raw = request.getParameter(PARAM_COUNTRY_CODE);
		if (StringUtils.hasText(raw)) {
			return raw.trim();
		}
		return countryCodeFromQueryString(request.getQueryString());
	}

	static String countryCodeFromQueryString(String queryString) {
		if (!StringUtils.hasText(queryString)) {
			return null;
		}
		for (String segment : queryString.split("&")) {
			if (!StringUtils.hasText(segment)) {
				continue;
			}
			int eq = segment.indexOf('=');
			String name = eq >= 0 ? segment.substring(0, eq) : segment;
			String value = eq >= 0 && eq + 1 < segment.length() ? segment.substring(eq + 1) : "";
			name = URLDecoder.decode(name, StandardCharsets.UTF_8);
			if (!PARAM_COUNTRY_CODE.equals(name)) {
				continue;
			}
			value = URLDecoder.decode(value, StandardCharsets.UTF_8);
			return StringUtils.hasText(value) ? value.trim() : null;
		}
		return null;
	}

	/** {@code zh-CN,zh;q=0.9} → {@code zh-CN} */
	static String firstAcceptLanguageTag(String acceptLanguage) {
		if (!StringUtils.hasText(acceptLanguage)) {
			return null;
		}
		String first = acceptLanguage.split(",")[0].trim();
		if (first.isEmpty()) {
			return null;
		}
		int sc = first.indexOf(';');
		if (sc >= 0) {
			first = first.substring(0, sc).trim();
		}
		return first.isEmpty() ? null : first;
	}
}
