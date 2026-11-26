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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberController#tagRelationPush} 与
 * {@code MemberTagRelationInboundPushService}（groupRelationsByUserId / resolveTagIds）读取的全部 key。
 * relations 仅从 body 读取（无 query 通道）。
 */
@Schema(
		name = "EcxMemberTagRelationPushBody",
		description =
				"会员标签关系推送参数。两种模式：relations 非空数组 → 正常模式（按 user_id 分组整体替换标签）；"
						+ "relations 缺失/空/非数组且 action=clear → 清空模式（须 user_id，mobile 兜底定位）。"
						+ " action/user_id/mobile 双通道（body 含键优先，mergeString trim 语义）",
		example =
				"{\"relations\":[{\"user_id\":\"10\",\"mobile\":\"13900000001\",\"tag_id\":\"20\","
						+ "\"wechat_tag_id\":\"wx-1\"}]}")
public class EcxMemberTagRelationPushBodyDoc {

	@Schema(
			description = "动作；\"clear\" 且 relations 缺失/空/非数组时进入清空模式；其余值不影响正常模式判定",
			example = "clear")
	public String action;

	@Schema(
			description = "会员 ID（清空模式定位，PHP-truthy；缺失/空/\"0\" 报 E1001 清空模式需要提供 user_id）",
			example = "10")
	public String user_id;

	@Schema(description = "手机号（清空模式 user_id 查无会员时兜底定位）；可选", example = "13900000001")
	public String mobile;

	@ArraySchema(
			arraySchema =
					@Schema(
							description =
									"标签关系数组（仅 body 通道）；非空数组进入正常模式；非 Map 元素被忽略"))
	public List<RelationRowDoc> relations;

	@Schema(name = "EcxMemberTagRelationPushRow", description = "单条标签关系")
	public static class RelationRowDoc {

		@Schema(
				description = "会员 ID；必填（PHP-empty 的行被跳过）；同 user_id 的行归为一组整体替换",
				requiredMode = Schema.RequiredMode.REQUIRED,
				example = "10")
		public String user_id;

		@Schema(description = "手机号；可选，user_id 查无会员时兜底定位（组内取首个非空值）", example = "13900000001")
		public String mobile;

		@Schema(
				description = "本地标签 ID（members_tags.tag_id）；wechat_tag_id 未提供时按此定位；查无标签跳过该行",
				example = "20")
		public String tag_id;

		@Schema(
				description = "企微标签 ID（members_tags.wechat_tag_id）；提供时优先于 tag_id 定位；查无标签跳过该行",
				example = "wx-1")
		public String wechat_tag_id;
	}
}