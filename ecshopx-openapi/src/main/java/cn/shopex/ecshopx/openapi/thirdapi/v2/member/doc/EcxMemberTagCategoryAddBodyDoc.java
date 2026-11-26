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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，2 键与 query 同名双通道：
 * category_name 走 mergeString（trim），sort 走 originalString（不 trim）+ 键存在性判定。
 * 校验与落库见 {@code OpenapiThirdApiV2MemberTagCategoryCreateService}。
 */
@Schema(
		name = "EcxMemberTagCategoryAddBody",
		description =
				"新增会员标签分类参数（2 键，body 含键优先否则 query 同名键）。"
						+ " 分类名同租户唯一（重复 E5572）。")
public class EcxMemberTagCategoryAddBodyDoc {

	@Schema(
			description =
					"分类名称；必填非空白（E5001「分类名称必填」）；trim 落库；"
							+ "同租户同名已存在报 E5572「该分类名已存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "聚合测试类目")
	public String category_name;

	@Schema(
			description =
					"排序值；键缺席或值 null 默认 1；键存在时 PHP intval（前导数字/负号截断，"
							+ "如 \"12abc\"→12、空串/纯负号/非数字→0，允许负数）",
			example = "1")
	public String sort;
}