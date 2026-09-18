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
 */
public class CountryCodeLocaleFilter extends OncePerRequestFilter {

	private final LangueProperties langueProperties;

	public CountryCodeLocaleFilter(LangueProperties langueProperties) {
		this.langueProperties = langueProperties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String raw = request.getParameter("country_code");
		if (!StringUtils.hasText(raw)) {
			raw = firstAcceptLanguageTag(request.getHeader("Accept-Language"));
		}
		String tag = langueProperties.resolveToSupportedTag(raw);
		Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
		LocaleContextHolder.setLocale(locale, true);
		try {
			filterChain.doFilter(request, response);
		} finally {
			LocaleContextHolder.resetLocaleContext();
		}
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
