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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，5 键与 query 同名双通道
 * （{@code OpenapiRequestParams#mergeString}：body 含键优先、值 null 视为未传、trim）。
 * 校验与落库见 {@code OpenapiThirdApiV2MemberTagCreateService}。
 */
@Schema(
		name = "EcxMemberTagAddBody",
		description =
				"新增会员标签参数（5 键，body 含键优先否则 query 同名键）。"
						+ " 标签名同租户唯一（重复 E5562）；颜色键 PHP elvis 语义（空/\"0\" 取默认色）。")
public class EcxMemberTagAddBodyDoc {

	@Schema(
			description =
					"标签名；必填非空白（E5001「分类名称必填」——源码文案如此，实为标签名）；trim 后落库；"
							+ "同租户同名已存在报 E5562「该标签名已存在」；zh-CN 多语言分表同步",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "聚合测试类目")
	public String tag_name;

	@Schema(
			description = "标签分类 ID；可选，宽松 parseInt（null/空白/非数字按 0）；不校验分类存在",
			example = "0")
	public String category_id;

	@Schema(
			description = "标签描述；可选（null/空白落库 null，响应回显 null）",
			example = "")
	public String description;

	@Schema(
			description =
					"标签背景色；可选，PHP elvis：null/空白/\"0\" 取默认 rgba(255, 25, 57, 1)；"
							+ "其他值 trim 原样落库（不校验颜色格式）",
			example = "rgba(255, 25, 57, 1)")
	public String tag_color;

	@Schema(
			description =
					"标签字体色；可选，elvis 语义同 tag_color，默认 rgba(255, 255, 255, 1)",
			example = "rgba(255, 255, 255, 1)")
	public String font_color;
}