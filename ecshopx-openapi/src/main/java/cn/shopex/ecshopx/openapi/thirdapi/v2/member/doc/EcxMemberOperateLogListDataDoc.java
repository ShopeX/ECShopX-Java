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
 * is_last_page 为整型 0/1（ceil(total_count/page_size) ≤ page → 1）。
 */
@Schema(name = "EcxMemberOperateLogListData")
public class EcxMemberOperateLogListDataDoc {

	@Schema(description = "满足过滤条件的日志总数（整型）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	@Schema(description = "日志行数组（created 降序；无结果为空数组）")
	public List<EcxMemberOperateLogListRowDoc> list;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxMemberOperateLogListPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}

	/** 日志行（8 固定键），来源 formatOperateLogRows。 */
	@Schema(name = "EcxMemberOperateLogListRow")
	public static class EcxMemberOperateLogListRowDoc {

		@Schema(description = "日志 ID（member_operate_log.id，整型）", example = "1")
		public Long id;

		@Schema(
				description = "操作类型（整型映射：1=修改会员信息(info)/2=修改手机号(mobile)/3=修改会员等级(grade_id)；未知类型 0）",
				example = "2")
		public Integer operate_type;

		@Schema(
				description =
						"旧值（字符串）：mobile 类型若库值为含 mobile 键的 JSON 则取该键值，否则原样"
								+ "（本系统写入明文手机号）；info/grade_id 类型为库内 JSON 差异字符串原样；null 为空串",
				example = "13800138000")
		public String old_data;

		@Schema(description = "新值（字符串，语义同 old_data）", example = "13800000000")
		public String new_data;

		@Schema(description = "操作人（库内原样；OpenAPI 写入的日志为「外部开发者」）", example = "外部开发者")
		public String operater;

		@Schema(
				description = "操作时间（yyyy-MM-dd HH:mm:ss，系统时区；null 为空串；示例为格式演示值）",
				example = "2026-01-01 00:00:00")
		public String created;

		@Schema(
				description =
						"描述（服务端拼接：「{操作人} 于{时间}进行了{类型描述}的操作, 将 {旧值} 改为 {新值}」；"
								+ "类型描述：修改会员信息/修改手机号/修改会员等级，未知类型空串；示例时间为格式演示值）",
				example = "外部开发者 于2026-01-01 00:00:00进行了修改手机号的操作, 将 13800138000 改为 13800000000")
		public String description;

		@Schema(
				description = "会员当前手机号（按 user_id 回查 members.mobile 解密明文；查无/空为空串）",
				example = "13800000000")
		public String mobile;
	}
}