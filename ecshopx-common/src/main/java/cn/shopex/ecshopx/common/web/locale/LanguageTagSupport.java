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

/**
 * 已知 {@code company_id} 时，在部署 list 基础上再做公司 enabled clamp（ECX-10021 §4.6）。
 */
public final class LanguageTagSupport {

	private LanguageTagSupport() {}

	@SafeVarargs
	public static String effectiveTag(
			LangueProperties langueProperties,
			CompanyLanguageResolver resolver,
			long companyId,
			Map<String, ?>... sources) {
		return effectiveTag(langueProperties, resolver, companyId, null, sources);
	}

	@SafeVarargs
	public static String effectiveTag(
			LangueProperties langueProperties,
			CompanyLanguageResolver resolver,
			long companyId,
			String queryParam,
			Map<String, ?>... sources) {
		String deploymentTag = RequestCountryCode.resolve(langueProperties, queryParam, sources);
		if (resolver == null || companyId <= 0L) {
			return deploymentTag;
		}
		return resolver.resolveEffectiveTag(companyId, deploymentTag);
	}
}
