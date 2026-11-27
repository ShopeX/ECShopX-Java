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
import java.util.List;

/**
 * 仅 OpenAPI 文档；data 分页结构（{@code OpenapiThirdApiV2MemberTagListService#formatListStruct}，
 * 4 固定键，键序 total_count/is_last_page/pager/list）。
 * 行来源 {@code OpenapiThirdApiV2MemberTagMembersGetService#executeOpenapiGetTagMembers}：
 * 会员按 company_id + members_rel_tags.tag_id 过滤（SELECT DISTINCT，user_id 倒序）；
 * tag_list 为该会员**全部**关联标签（非仅查询标签），selectUserRelTagListByUserIds 按租户查。
 */
@Schema(name = "EcxMemberTagMembersGetData")
public class EcxMemberTagMembersGetDataDoc {

	@Schema(description = "关联该标签的会员总数（整型，count(*) 于 LEFT JOIN 行——会员多标签关联时可能大于去重会员数）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	@Schema(description = "会员行数组（user_id 倒序；无结果为空数组）")
	public List<RowDoc> list;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxMemberTagMembersGetPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}

	/** 会员行（3 固定键：mobile/username/tag_list）。 */
	@Schema(name = "EcxMemberTagMembersGetRow")
	public static class RowDoc {

		@Schema(
				description = "会员手机号（解密后明文；库内 null/空白为空串；解密失败原样透传）",
				example = "13800000000")
		public String mobile;

		@Schema(
				description = "会员用户名（members_info.username，解密后明文；无 info 行/空白为空串）",
				example = "张三")
		public String username;

		@Schema(description = "该会员全部关联标签（非仅查询标签；无关联为空数组）")
		public List<TagItemDoc> tag_list;
	}

	/** 关联标签项（2 固定键），来源 selectUserRelTagListByUserIds（LEFT JOIN members_tags）。 */
	@Schema(name = "EcxMemberTagMembersGetTagItem")
	public static class TagItemDoc {

		@Schema(description = "标签 ID（整型）", example = "1")
		public Long tag_id;

		@Schema(description = "标签名（库内原值，无语言覆盖；关联行悬空时为 null）", nullable = true, example = "高价值会员")
		public String tag_name;
	}
}