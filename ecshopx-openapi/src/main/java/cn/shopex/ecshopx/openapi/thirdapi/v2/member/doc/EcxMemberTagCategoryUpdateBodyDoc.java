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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，3 键与 query 同名双通道：
 * category_id/category_name 走 mergeString（trim），sort 走 originalString（不 trim）+ 键存在性判定。
 * 校验与更新见 {@code OpenapiThirdApiV2MemberTagCategoryUpdateService}。
 */
@Schema(
		name = "EcxMemberTagCategoryUpdateBody",
		description =
				"修改会员标签分类参数（3 键，body 含键优先否则 query 同名键）。"
						+ " category_id/category_name 必填；category_name 恒写（同名排除自身冲突拒绝）；"
						+ " sort 键存在且 PHP truthy（非空非\"0\"）才更新，否则保留原值。")
public class EcxMemberTagCategoryUpdateBodyDoc {

	@Schema(
			description =
					"分类 ID；必填非空白（E5001「分类ID必填」）；trim 后须可 parseLong"
							+ "（非整数/查无/更新后回读为空均 E5571「该标签分类不存在」）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String category_id;

	@Schema(
			description =
					"分类名称；必填非空白（E5001「分类名称必填」）；trim 恒写；"
							+ "同租户同名（排除自身）已存在 E5572「该分类名已存在」（同名改自身放行）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "聚合测试类目")
	public String category_name;

	@Schema(
			description =
					"排序值；键存在且 PHP truthy（非空非\"0\"）才更新：PHP intval 前导数字截断"
							+ "（\"12abc\"→12，非数字→0，允许负数）；键缺席/空/\"0\" 保留原排序（区别于新增的默认 1）",
			example = "1")
	public String sort;
}