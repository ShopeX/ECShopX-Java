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
 * 仅 OpenAPI 文档；data 分页结构，来源
 * {@code OpenapiThirdApiV2MemberTagListService#formatListStructPhpHandlerOrder}
 * （4 固定键，PHP handler 键序：total_count/list/is_last_page/pager）。
 * 行经 {@code reshapeToMobileUsernameOnly} 仅保留 mobile/username 两键（user_id 不透出）。
 * is_last_page 为整型 0/1（ceil(total_count/page_size) ≤ page → 1）。
 */
@Schema(name = "EcxMemberCardGradeRelListData")
public class EcxMemberCardGradeRelListDataDoc {

	@Schema(
			description = "总数（整型）：grade 分支为 DISTINCT 会员数；vip 分支为关联行数（非去重会员数）",
			example = "1")
	public Long total_count;

	@Schema(description = "会员行数组（user_id 降序；每行仅 mobile/username；无结果为空数组）")
	public List<EcxMemberCardGradeRelListRowDoc> list;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxMemberCardGradeRelListPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}

	/** 会员行（2 固定键；敏感字段解密后明文，查无/空值均空串）。 */
	@Schema(name = "EcxMemberCardGradeRelListRow")
	public static class EcxMemberCardGradeRelListRowDoc {

		@Schema(
				description = "会员手机号（明文；members.mobile 密文解密，null/空为空串）",
				example = "13800138000")
		public String mobile;

		@Schema(
				description = "会员用户名（members_info.username 密文解密；查无 info 行或空值为空串）",
				example = "张三")
		public String username;
	}
}