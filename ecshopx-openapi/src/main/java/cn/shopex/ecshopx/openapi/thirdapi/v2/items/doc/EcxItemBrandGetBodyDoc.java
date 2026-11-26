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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源
 * {@code OpenapiThirdApiV2ItemBrandListParams#resolve} 经 {@code OpenapiRequestParams#originalString}
 * 读取的全部 key。
 */
@Schema(
		name = "EcxItemBrandGetBody",
		description =
				"商品品牌列表参数（body 与 query 同名 key 双通道：body 含键取原始值，否则用 query；解析时 trim）")
public class EcxItemBrandGetBodyDoc {

	@Schema(description = "页码；PHP 整型解析（含小数取整、非法按 0），缺省 1，小于 1 归一为 1", example = "1")
	public String page;

	@Schema(
			description =
					"每页条数；缺省 20；解析后 ≥500 报 E5002「每页的大小最大为500！」（500 本身也报错）；"
							+ "小于 20 一律归一为 20（PHP 旧契约）",
			example = "20")
	public String page_size;
}