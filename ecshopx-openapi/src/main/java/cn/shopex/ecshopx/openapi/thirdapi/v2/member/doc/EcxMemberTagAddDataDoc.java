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
 * 仅 OpenAPI 文档；data 新建标签行（8 固定键），来源
 * {@code OpenapiThirdApiV2MemberTagCreateService#formatOpenApiResponse}。
 */
@Schema(name = "EcxMemberTagAddData")
public class EcxMemberTagAddDataDoc {

	@Schema(description = "标签 ID（members_tags.tag_id，整型自增）", example = "1")
	public Long tag_id;

	@Schema(description = "标签名（trim 后入参）", example = "聚合测试类目")
	public String tag_name;

	@Schema(description = "标签分类 ID（整型；未传/非法为 0）", example = "0")
	public Integer category_id;

	@Schema(description = "标签描述（未传/空白为 null）", nullable = true)
	public String description;

	@Schema(description = "标签背景色（入参或默认 rgba(255, 25, 57, 1)）", example = "rgba(255, 25, 57, 1)")
	public String tag_color;

	@Schema(description = "标签字体色（入参或默认 rgba(255, 255, 255, 1)）", example = "rgba(255, 255, 255, 1)")
	public String font_color;

	@Schema(
			description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String created;

	@Schema(
			description = "更新时间（yyyy-MM-dd HH:mm:ss，系统时区；新建与 created 相同；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String updated;
}