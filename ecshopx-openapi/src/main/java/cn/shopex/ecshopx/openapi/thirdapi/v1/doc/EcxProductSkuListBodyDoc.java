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

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ItemController#list} 经
 * {@code OpenapiPaginationParams#resolvePage/resolvePageSize/mergeCountryCode} 读取的全部 key。
 */
@Schema(
		name = "EcxProductSkuListBody",
		description =
				"商品 SKU 列表参数（body 与 query 同名 key 双通道：page/page_size body 含键优先，null 回退 query；"
						+ "country_code body 含键时以 body 为准且 null/空白不回退 query）")
public class EcxProductSkuListBodyDoc {

	@Schema(
			description = "页码；必填正整数（缺失/非整数/小于 1 报「page参数必填」）；服务层再兜底 max(page,1)",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String page;

	@Schema(
			description = "每页条数；必填整数（缺失/非整数报「page_size参数必填」）；≤0 按 100，>2000 封顶 2000",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "100")
	public String page_size;

	@Schema(
			description = "语言/国家码，决定 item_name 多语言分表覆盖；缺省 zh-CN",
			defaultValue = "zh-CN",
			example = "zh-CN")
	public String country_code;
}