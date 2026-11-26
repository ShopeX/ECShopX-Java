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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ItemsAttributesController#updateItemBrand}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key，
 * 校验见 {@code OpenapiThirdApiV2ItemBrandUpdateService}。
 */
@Schema(
		name = "EcxItemBrandUpdateBody",
		description =
				"更新商品品牌参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）。"
						+ " brand_name 每次全量覆盖（trim 后落库）；image_url 键不存在则不改动。")
public class EcxItemBrandUpdateBodyDoc {

	@Schema(
			description =
					"品牌 ID（attribute_id）；必填非空否则 E5001「品牌ID必填」；"
							+ "trim 后非整数/查无记录 E5110「更新的数据不存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String brand_id;

	@Schema(
			description =
					"品牌名称；必填非空否则 E5001「品牌名称必填」；与其他品牌重名（排除自身）E5110「品牌名称不能重复」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "FixtureBrand")
	public String brand_name;

	@Schema(description = "品牌 Logo URL；可选（键不存在不改动；无格式校验）", example = "/fixtures/logo.png")
	public String image_url;
}