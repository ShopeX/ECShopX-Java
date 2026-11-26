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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ItemsCategoryController#createItemCategory}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key，
 * 落库见 {@code OpenapiThirdApiV2ItemCategoryCreateService} + {@code ItemsCategoryClassificationService}。
 */
@Schema(
		name = "EcxItemCategoryAddBody",
		description =
				"新增商品分类参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）。"
						+ " 落库 items_category（非主类目，名称 trim；zh-CN 分表同步）。")
public class EcxItemCategoryAddBodyDoc {

	@Schema(
			description = "分类名称；必填非空否则 E5001「分类名称必填」；同父级下重名 E5120「分类名称已存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "聚合测试类目")
	public String category_name;

	@Schema(
			description =
					"上级分类 ID；必填非空否则 E5001「上级分类ID必填」；PHP intval 前导数字解析："
							+ "解析为 0（如 \"0\"/非数字）建一级分类（parent_id=0，level=1）；"
							+ ">0 时父级须存在（查无 E5120「父级分类不存在」），level=父级+1、path=父 path+自身 ID",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "0")
	public String parent_id;

	@Schema(
			description = "排序值；可选，缺省 0；键存在时须整数字符串（非整数 E5120「排序必须大于等于0」）",
			example = "1")
	public String sort;

	@Schema(description = "分类图片 URL；可选（无格式校验，键不存在则不写）", example = "/fixtures/logo.png")
	public String image_url;
}