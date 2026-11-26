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
 * 仅 OpenAPI 文档；DELETE 请求可选 JSON body，字段来源 {@code ItemsAttributesController#deleteItemBrand}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key。
 */
@Schema(
		name = "EcxItemBrandDeleteBody",
		description =
				"删除商品品牌参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）")
public class EcxItemBrandDeleteBodyDoc {

	@Schema(
			description =
					"品牌 ID（items_attributes.attribute_id）；必填非空否则 E5001「品牌ID必填」；"
							+ "trim 后非整数/空白视为无操作直接成功（PHP 旧契约）；"
							+ "有关联商品/积分商城商品在用报 E5113 对应文案",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String brand_id;
}