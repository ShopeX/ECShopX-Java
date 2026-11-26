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
@Schema(name = "EcxMemberPointOrderListData")
public class EcxMemberPointOrderListDataDoc {

	@Schema(description = "满足过滤条件的订单总数（整型）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	@Schema(description = "订单积分行数组（order_id 降序；无结果为空数组）")
	public List<EcxMemberPointOrderListRowDoc> list;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxMemberPointOrderListPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}

	/** 订单积分行（10 固定键；user_id 用于回查后移除不透出），来源 formatPointOrderRows + appendUsernameToList。 */
	@Schema(name = "EcxMemberPointOrderListRow")
	public static class EcxMemberPointOrderListRowDoc {

		@Schema(description = "订单号（字符串化的 bigint，降序排序键）", example = "100")
		public String order_id;

		@Schema(
				description = "下单手机号（订单快照 normal_orders.mobile 解密明文，解密失败回退原值；null/空为空串）",
				example = "13800138000")
		public String mobile;

		@Schema(
				description = "下单时间（yyyy-MM-dd HH:mm:ss，系统时区；null/≤0 为空串；示例为格式演示值）",
				example = "2026-01-01 00:00:00")
		public String create_time;

		@Schema(description = "订单获取积分（get_points，整型；null 按 0）", example = "0")
		public Integer get_points;

		@Schema(description = "购物赠送积分（bonus_points，整型；null 按 0）", example = "0")
		public Integer bonus_points;

		@Schema(description = "订单获取额外积分（extra_points，整型；null 按 0）", example = "0")
		public Integer extra_points;

		@Schema(description = "积分抵扣使用的积分数（point_use，整型；null 按 0）", example = "0")
		public Integer point_use;

		@Schema(
				description = "积分抵扣金额（元，字符串 2 位小数；point_fee 分 HALF_UP 除 100；**null 为空串**，非 \"0.00\"）",
				example = "0.00")
		public String point_fee;

		@Schema(description = "获取积分合计（整型 = get_points + bonus_points + extra_points）", example = "0")
		public Integer total_points;

		@Schema(
				description = "会员用户名（按 user_id 回查 members_info.username 解密；查无为空串；user_id 本身不透出）",
				example = "张三")
		public String username;
	}
}