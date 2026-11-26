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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/** 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code OpenapiPaginationParams#mergeCountryCode}。 */
@Schema(name = "EcxCompanyInfoBody", description = "云店基本信息查询参数（可选；country_code 与 query 同名参数双通道）")
public class EcxCompanyInfoBodyDoc {

	@Schema(
			description =
					"语言/国家码，决定返回哪种语言的门店展示配置切片。"
							+ " body 含 country_code 键时以 body 为准（null/空白视为未提供，且不回退 query），"
							+ " body 无该键时才读 query 同名参数；两侧均未提供时默认 zh-CN。",
			example = "zh-CN",
			defaultValue = "zh-CN")
	public String country_code;
}