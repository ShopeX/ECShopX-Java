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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 数组元素（标签组），来源
 * {@code MemberTagLibraryQueryService#tagLibrary}：仅本地标签组/标签（wechat_group_id/wechat_tag_id
 * 为 null 或空），未分组标签归入虚拟组（group_id=0「未分组」）。
 */
@Schema(name = "EcxMemberTagLibraryGroup")
public class EcxMemberTagLibraryGroupDoc {

	@Schema(description = "标签组 ID；虚拟未分组组为 0", example = "1")
	public Long group_id;

	@Schema(description = "标签组名称；虚拟组为「未分组」", example = "消费等级")
	public String group_name;

	@Schema(description = "描述；null→空串；虚拟组为「未分配到任何标签组的标签」", example = "")
	public String description;

	@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss；null/虚拟组为空串", example = "2026-11-26 12:00:00")
	public String created;

	@Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss；null/虚拟组为空串", example = "2026-11-26 12:00:00")
	public String updated;

	@ArraySchema(arraySchema = @Schema(description = "组内标签列表（按组关系 created 倒序）；无标签为空数组"))
	public List<TagRowDoc> taglist;

	@Schema(name = "EcxMemberTagLibraryTag", description = "单个标签（11 键）")
	public static class TagRowDoc {

		@Schema(description = "标签 ID", example = "20")
		public Long tag_id;

		@Schema(description = "标签名称", nullable = true)
		public String tag_name;

		@Schema(description = "描述；null→空串", example = "")
		public String description;

		@Schema(description = "标签背景色；空白默认 #ff1939", example = "#ff1939")
		public String tag_color;

		@Schema(description = "字体颜色；空白默认 #ffffff", example = "#ffffff")
		public String font_color;

		@Schema(description = "标签图标；null→空串", example = "")
		public String tag_icon;

		@Schema(description = "标签状态；空白默认 online", example = "online")
		public String tag_status;

		@Schema(description = "来源；空白默认 self", example = "self")
		public String source;

		@Schema(description = "关联的有效会员数（members_rel_tags 联 members 实时 COUNT，非缓存计数）", example = "3")
		public Integer self_tag_count;

		@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss；null 为空串", example = "2026-11-26 12:00:00")
		public String created;

		@Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss；null 为空串", example = "2026-11-26 12:00:00")
		public String updated;
	}
}