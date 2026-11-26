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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ItemsCategoryController#updateItemCategory}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key，
 * 校验见 {@code OpenapiThirdApiV2ItemCategoryUpdateService}。
 */
@Schema(
		name = "EcxItemCategoryUpdateBody",
		description =
				"更新商品分类参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）。"
						+ " category_name 每次全量覆盖（trim 落库）；sort/image_url 键不存在不改动；"
						+ " 服务端固定附带 customize_page_id=0 重置。")
public class EcxItemCategoryUpdateBodyDoc {

	@Schema(
			description =
					"分类 ID；必填非空否则 E5001「分类ID必填」；trim 后非整数/租户内查无 E5120「更新的分类不存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String category_id;

	@Schema(
			description =
					"分类名称；必填非空否则 E5001「分类名称必填」；改名时同父级/同店铺范围重名（排除自身）"
							+ " E5120「分类名称不能重复」（与原名相同跳过查重）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "聚合测试类目")
	public String category_name;

	@Schema(
			description = "排序值；可选，键存在时须整数字符串（非整数 E5120「排序必须大于等于0」）",
			example = "1")
	public String sort;

	@Schema(description = "分类图片 URL；可选（键不存在不改动；无格式校验）", example = "/fixtures/logo.png")
	public String image_url;
}