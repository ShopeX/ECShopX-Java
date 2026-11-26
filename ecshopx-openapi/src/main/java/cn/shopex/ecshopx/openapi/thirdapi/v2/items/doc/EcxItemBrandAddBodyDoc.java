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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ItemsAttributesController#createItemBrand}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key。
 */
@Schema(
		name = "EcxItemBrandAddBody",
		description =
				"新增商品品牌参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）。"
						+ " 落库为 items_attributes（attribute_type=brand），名称落库前 trim。")
public class EcxItemBrandAddBodyDoc {

	@Schema(
			description = "品牌名称；必填非空否则 E5001「品牌名称必填」；租户内重名 E5110「品牌名称不能重复」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "FixtureBrand")
	public String brand_name;

	@Schema(description = "品牌 Logo 图片 URL；可选（无格式校验，键不存在则不写）", example = "/fixtures/logo.png")
	public String image_url;
}