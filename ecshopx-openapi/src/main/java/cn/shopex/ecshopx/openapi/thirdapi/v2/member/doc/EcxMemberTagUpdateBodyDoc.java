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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，6 键与 query 同名双通道：
 * tag_id/tag_name/tag_color/font_color/category_id 走 mergeString（trim），
 * description 走 originalString（不 trim）+ 键存在性判定。
 * 校验与更新见 {@code OpenapiThirdApiV2MemberTagUpdateService}。
 */
@Schema(
		name = "EcxMemberTagUpdateBody",
		description =
				"修改会员标签参数（6 键，body 含键优先否则 query 同名键）。"
						+ " tag_id/tag_name/tag_color/font_color 必填；"
						+ " **description 恒写：键缺席也会清空为 null**（非保留原值语义）；"
						+ " category_id 键存在且 truthy（非空非\"0\"）才更新。系统标签禁改；同名（排除自身）冲突拒绝。")
public class EcxMemberTagUpdateBodyDoc {

	@Schema(
			description =
					"标签 ID；必填非空白（E5001「标签ID必填」）；trim 后须可 parseLong"
							+ "（非整数/查无/更新后回读为空均 E5561「该标签不存在」）；系统标签 E0001「系统标签不可编辑」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String tag_id;

	@Schema(
			description =
					"标签名；必填非空白（E5001「标签名称必填」）；trim 落库；"
							+ "同租户同名（排除自身）已存在 E5562「该标签名已存在」；zh-CN 分表同步",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "聚合测试类目")
	public String tag_name;

	@Schema(
			description =
					"标签描述；可选但**恒写**：键存在时非空白 trim 落库、空白/null 落 null；"
							+ "**键缺席同样写 null（清空原描述，不保留）**",
			example = "")
	public String description;

	@Schema(
			description = "标签背景色；必填非空白（E5001「标签颜色必填」）；trim 原样落库（无默认色，不校验格式）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "rgba(255, 25, 57, 1)")
	public String tag_color;

	@Schema(
			description = "标签字体色；必填非空白（E5001「标签字体颜色必填」）；trim 原样落库",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "rgba(255, 255, 255, 1)")
	public String font_color;

	@Schema(
			description =
					"标签分类 ID；键存在且 PHP truthy（非空非\"0\"）才更新：PHP intval 前导数字截断"
							+ "（如 \"12abc\"→12，非数字→0）；键缺席/空/\"0\" 保留原分类；不校验分类存在",
			example = "1")
	public String category_id;
}