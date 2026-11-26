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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ItemController#goodsList} 经
 * {@code OpenapiGoodsListParams#resolvePage/resolvePageSize} 读取的全部 key。
 */
@Schema(
		name = "EcxProductGoodsListBody",
		description =
				"导购云店商品列表参数（body 与 query 同名 key 双通道：body 含键时取原始值，"
						+ "null 时回退 query；宽松解析，非整数回退默认）")
public class EcxProductGoodsListBodyDoc {

	@Schema(description = "页码，从 1 起；非整数/缺省按 1", defaultValue = "1", example = "1")
	public String page;

	@Schema(
			description = "每页条数；非整数/缺省按 100；≤0 时返回全部商品（不分页）",
			defaultValue = "100",
			example = "100")
	public String page_size;
}