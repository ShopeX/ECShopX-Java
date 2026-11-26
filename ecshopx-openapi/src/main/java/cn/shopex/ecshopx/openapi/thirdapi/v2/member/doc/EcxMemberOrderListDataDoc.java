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
@Schema(name = "EcxMemberOrderListData")
public class EcxMemberOrderListDataDoc {

	@Schema(description = "满足过滤条件的订单总数（整型）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	@Schema(description = "订单行数组（order_id 降序；无结果为空数组）")
	public List<EcxMemberOrderListRowDoc> list;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxMemberOrderListPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}

	/** 订单行（13 固定键），来源 formatOrderRows + appendDistributorNames；枚举取自 NormalOrders 域注释。 */
	@Schema(name = "EcxMemberOrderListRow")
	public static class EcxMemberOrderListRowDoc {

		@Schema(description = "订单号（字符串化的 bigint）", example = "100")
		public String order_id;

		@Schema(
				description = "下单时间（yyyy-MM-dd HH:mm:ss，系统时区；null/≤0 为空串；示例为格式演示值）",
				example = "2026-01-01 00:00:00")
		public String create_time;

		@Schema(
				description = "订单金额（元，字符串 2 位小数；库内分字符串 HALF_UP 除 100，含小数点按元×100 归一；非法按 0）",
				example = "0.00")
		public String total_fee;

		@Schema(
				description =
						"订单种类（normal 普通/groups 拼团/community 社区活动/bargain 助力/seckill 秒杀/"
								+ "shopguide 导购/pointsmall 积分商城/excard 兑换券）",
				example = "normal")
		public String order_class;

		@Schema(description = "订单类型（normal 普通实体订单）", example = "normal")
		public String order_type;

		@Schema(description = "贸易类型（整型：0 普通订单/1 跨境订单；null 按 0）", example = "0")
		public Integer type;

		@Schema(
				description =
						"订单状态（DONE 完成/NOTPAY 未支付/PART_PAYMENT 部分付款/WAIT_GROUPS_SUCCESS 等待拼团/"
								+ "PAYED 已支付/CANCEL 已取消/WAIT_BUYER_CONFIRM 待收货）",
				example = "NOTPAY")
		public String order_status;

		@Schema(
				description = "自提状态（PENDING 等待自提/DONE 自提完成/NOTZITI 非自提/APPROVE 审核通过（药品自提））",
				example = "NOTZITI")
		public String ziti_status;

		@Schema(
				description =
						"取消状态（NO_APPLY_CANCEL 未申请/WAIT_PROCESS 等待审核/REFUND_PROCESS 退款处理/"
								+ "SUCCESS 取消成功/FAILS 取消失败）",
				example = "NO_APPLY_CANCEL")
		public String cancel_status;

		@Schema(
				description = "支付状态（NOTPAY 未支付/PAYED 已支付/ADVANCE_PAY 预付款完成/TAIL_PAY 支付尾款中）",
				example = "NOTPAY")
		public String pay_status;

		@Schema(
				description = "跨境审核状态（approved 成功/processing 审核中/rejected 拒绝；可为 null）",
				nullable = true,
				example = "processing")
		public String audit_status;

		@Schema(description = "店铺（分销商）ID（整型；null 按 0，0=总部）", example = "0")
		public Integer distributor_id;

		@Schema(description = "店铺名称（按 distributor_id 回查；总部/查无为空串）", example = "")
		public String distributor_name;
	}
}