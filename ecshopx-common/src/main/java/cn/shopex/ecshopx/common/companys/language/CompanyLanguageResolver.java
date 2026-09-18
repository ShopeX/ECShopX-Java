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

import java.util.List;

/**
 * 公司级语言配置解析（ECX-10021）。实现位于 {@code ecshopx-companys}，读 Redis
 * {@code languageSetting:{companyId}}。
 */
public interface CompanyLanguageResolver {

	/** 已启用且部署支持的 canonical code，按固定展示顺序。 */
	List<String> listEnabledCodes(long companyId);

	/** 公司有效默认语言（已启用且部署支持）。 */
	String getDefaultLanguage(long companyId);

	/**
	 * normalize → 部署 list → 公司 enabled → clamp 到公司默认。
	 *
	 * @param rawTag 原始 {@code country_code} / Accept-Language 等，可为空
	 */
	String resolveEffectiveTag(long companyId, String rawTag);
}
