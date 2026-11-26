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
 * 仅 OpenAPI 文档；DELETE 请求可选 JSON body，字段来源 {@code ItemsCategoryController#deleteItemCategory}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key。
 */
@Schema(
		name = "EcxItemCategoryDeleteBody",
		description =
				"删除商品分类参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）")
public class EcxItemCategoryDeleteBodyDoc {

	@Schema(
			description =
					"分类 ID（items_category.category_id）；必填非空否则 E5001「分类ID必填」；"
							+ "trim 后非整数/空白视为无操作直接成功（PHP 旧契约）；"
							+ "分类树（含子孙）关联商品在用报 E5123「分类下存在商品」，主类目引用在用报「类目下存在商品」；"
							+ "删除会级联删除子级与孙级分类",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String category_id;
}