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

import cn.shopex.ecshopx.common.companys.language.CompanyLanguageResolver;
import cn.shopex.ecshopx.common.config.LangueProperties;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 写多语言路径的语种解析，对齐 PHP {@code $request->input('country_code')}：
 * <ol>
 *   <li>请求体 / 合并参数中的 {@code country_code}（按传入 map 顺序优先）</li>
 *   <li>可选 query {@code country_code}</li>
 *   <li>{@link RequestLangTag}（Filter 已从 query / Accept-Language 写入 Locale）</li>
 * </ol>
 *
 * <p>不在 Filter 中读取 JSON body（避免消费 InputStream）；由 Controller/Service 在
 * body 解析后再调用本工具。
 */
public final class RequestCountryCode {

	private RequestCountryCode() {}

	/** Prefer {@code country_code} in {@code params}, else {@link RequestLangTag}. */
	public static String resolve(LangueProperties langueProperties, Map<String, ?> params) {
		return resolve(langueProperties, null, params);
	}

	/**
	 * 在 {@link #resolve(LangueProperties, String, Map[])} 基础上，若 {@code companyId} 有效则经
	 * {@link CompanyLanguageResolver#resolveEffectiveTag(long, String)} 做公司 enabled clamp。
	 */
	@SafeVarargs
	public static String resolve(
			LangueProperties langueProperties,
			CompanyLanguageResolver resolver,
			long companyId,
			String queryParam,
			Map<String, ?>... sources) {
		return LanguageTagSupport.effectiveTag(langueProperties, resolver, companyId, queryParam, sources);
	}

	/**
	 * Prefer {@code country_code} from each {@code sources} map in order, then {@code queryParam},
	 * then {@link RequestLangTag}.
	 */
	@SafeVarargs
	public static String resolve(LangueProperties langueProperties, String queryParam, Map<String, ?>... sources) {
		if (sources != null) {
			for (Map<String, ?> source : sources) {
				String fromMap = extractCountryCode(source);
				if (StringUtils.hasText(fromMap)) {
					return langueProperties.resolveToSupportedTag(fromMap.trim());
				}
			}
		}
		if (StringUtils.hasText(queryParam)) {
			return langueProperties.resolveToSupportedTag(queryParam.trim());
		}
		return RequestLangTag.current(langueProperties);
	}

	private static String extractCountryCode(Map<String, ?> source) {
		if (source == null || source.isEmpty()) {
			return null;
		}
		Object cc = source.get("country_code");
		if (cc == null) {
			return null;
		}
		String s = cc.toString();
		return StringUtils.hasText(s) ? s : null;
	}
}
