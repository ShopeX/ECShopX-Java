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
 * 仅 OpenAPI 文档；POST 请求 JSON body，字段来源 {@code MemberController#tagLibraryPush} 与
 * {@code MemberTagLibraryPushService}（processTagGroup / processTag）读取的全部 key。
 * tag_library 仅从 body 读取（无 query 通道）。
 */
@Schema(
		name = "EcxMemberTagLibraryPushBody",
		description =
				"标签库推送参数（企微标签库全量同步）。tag_library 须为非空对象数组（否则 E1001）；"
						+ "按 wechat_group_id/wechat_tag_id upsert；推送后删除本次未包含的企微来源组/标签（连带关系与会员打标）",
		example =
				"{\"tag_library\":[{\"group_name\":\"t1\",\"description\":\"\",\"wechat_group_id\":\"wx-1\","
						+ "\"taglist\":[{\"tag_name\":\"t1\",\"wechat_tag_id\":\"wx-1\","
						+ "\"tag_color\":\"#3e7bff\",\"font_color\":\"#ffffff\",\"tag_status\":\"online\","
						+ "\"source\":\"wechat\"}]}]}")
public class EcxMemberTagLibraryPushBodyDoc {

	@ArraySchema(
			arraySchema =
					@Schema(
							description = "标签组数组；必填非空且元素须为对象；group_name PHP-empty 的组计入 fail_count 跳过",
							requiredMode = Schema.RequiredMode.REQUIRED))
	public List<GroupRowDoc> tag_library;

	@Schema(name = "EcxMemberTagLibraryPushGroup", description = "单个标签组")
	public static class GroupRowDoc {

		@Schema(
				description = "标签组名称；必填（PHP-empty 跳过该组并计 fail_count）",
				requiredMode = Schema.RequiredMode.REQUIRED,
				example = "t1")
		public String group_name;

		@Schema(description = "描述；缺省空串", defaultValue = "", example = "")
		public String description;

		@Schema(
				description = "企微标签组 ID；提供时按其 upsert（存在则更新名称/描述）；未提供的组新建且不参与多余组清理",
				example = "wx-1")
		public String wechat_group_id;

		@ArraySchema(
				arraySchema = @Schema(description = "组内标签数组；非数组视为空；处理后清理组内未包含的旧关系"))
		public List<TagRowDoc> taglist;
	}

	@Schema(name = "EcxMemberTagLibraryPushTag", description = "单个标签")
	public static class TagRowDoc {

		@Schema(
				description = "标签名称；必填（PHP-empty 跳过该标签，不计失败）",
				requiredMode = Schema.RequiredMode.REQUIRED,
				example = "t1")
		public String tag_name;

		@Schema(description = "描述；缺省空串", defaultValue = "", example = "")
		public String description;

		@Schema(
				description = "企微标签 ID；提供时按其 upsert（存在则更新全部展示字段）；未提供的标签新建且不参与多余标签清理",
				example = "wx-1")
		public String wechat_tag_id;

		@Schema(description = "标签背景色；缺省 #3e7bff（注意与查询接口默认 #ff1939 不同）", defaultValue = "#3e7bff", example = "#3e7bff")
		public String tag_color;

		@Schema(description = "字体颜色；缺省 #ffffff", defaultValue = "#ffffff", example = "#ffffff")
		public String font_color;

		@Schema(description = "标签图标；缺省空串", defaultValue = "", example = "")
		public String tag_icon;

		@Schema(description = "标签状态；缺省 online", defaultValue = "online", example = "online")
		public String tag_status;

		@Schema(description = "来源；缺省 wechat", defaultValue = "wechat", example = "wechat")
		public String source;
	}
}