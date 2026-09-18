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
import java.util.Locale;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 当前请求语言标签（BCP 47），依赖 {@link CountryCodeLocaleFilter} 对 {@link LocaleContextHolder} 的设置。
 * 与按请求注入解析器 Bean 等价，避免在 Controller/Service 中单独注入语言解析 Bean。
 */
public final class RequestLangTag {

	private RequestLangTag() {}

	/**
	 * 返回规范语言标签；无请求上下文时回落为 {@link LangueProperties#getDefaultLang()}。
	 */
	public static String current(LangueProperties langueProperties) {
		LocaleContext ctx = LocaleContextHolder.getLocaleContext();
		if (ctx != null) {
			Locale loc = ctx.getLocale();
			if (loc != null) {
				return langueProperties.resolveToSupportedTag(loc.toLanguageTag());
			}
		}
		return langueProperties.getDefaultLang();
	}
}
