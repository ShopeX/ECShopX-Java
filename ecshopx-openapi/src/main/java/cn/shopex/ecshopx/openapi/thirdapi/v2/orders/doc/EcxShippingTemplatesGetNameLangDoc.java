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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；运费模板行内 name_lang 条件键（{@code CommonLangModReadService#mergeShippingTemplateListNamesForLocale}）。
 * 本接口 locale 固定 null → zh-CN：仅当 zh-CN 语言分片（outside_item_multi_lang_mod_lang_*）存在非空覆盖名时携带，
 * 单键 "zh-CN" → 覆盖名（同时覆盖行内 name）；无覆盖时整个键不出现。
 */
@Schema(name = "EcxShippingTemplatesGetNameLang")
public class EcxShippingTemplatesGetNameLangDoc {

	@Schema(
			name = "zh-CN",
			description = "zh-CN 覆盖名（条件键：仅语言分片有非空覆盖时出现，值与行内 name 一致）",
			example = "全国包邮模板")
	public String zh_CN;
}