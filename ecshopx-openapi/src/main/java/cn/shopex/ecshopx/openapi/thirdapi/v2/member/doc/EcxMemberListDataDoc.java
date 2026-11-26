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
 * {@code OpenapiThirdApiV2MemberTagListService#formatListStruct}（4 固定键）。
 * total_count 为 DISTINCT user_id 计数；is_last_page 整型 0/1。
 * 行经 enrichList 完整格式化（区别于 detail 路径）：created/updated 格式化时间、
 * disabled → status 反演（0→1/1→0/其他→null）、habbit 解析数组、sex/edu/income/industry 归一整数枚举。
 */
@Schema(name = "EcxMemberListData")
public class EcxMemberListDataDoc {

	@Schema(description = "满足过滤条件的会员总数（DISTINCT user_id，整型）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	@Schema(description = "会员行数组（user_id 降序；无结果为空数组）")
	public List<EcxMemberListRowDoc> list;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxMemberListPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}
}