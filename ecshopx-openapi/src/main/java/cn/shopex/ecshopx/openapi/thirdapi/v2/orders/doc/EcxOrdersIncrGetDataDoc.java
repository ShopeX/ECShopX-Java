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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；data 分页结构（{@code OpenapiThirdApiV2OrderListFormatSupport#formatOrderListStruct}，
 * 4 固定键，键序 total_count/is_last_page/pager/list）。is_last_page 整型 0/1。
 * 注意：mobile 查无会员时 data 为空数组 []（非本结构）。
 * 行 22 固定键（mapOrder），时间键 yyyy-MM-dd HH:mm:ss（系统时区），epoch ≤0/null 转 ""。
 */
@Schema(name = "EcxOrdersIncrGetData")
public class EcxOrdersIncrGetDataDoc {

	@Schema(description = "满足过滤条件的订单总数（整型）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔；ceil(total_count/page_size)≤page 为 1）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	@Schema(description = "订单行数组（update_time 降序；无结果为空数组）")
	public List<RowDoc> list;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxOrdersIncrGetPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}

	/** 订单行（22 固定键）。 */
	@Schema(name = "EcxOrdersIncrGetRow")
	public static class RowDoc {

		@Schema(description = "订单号（字符串）", example = "100")
		public String order_id;

		@Schema(description = "成功交易单号（trade_state=SUCCESS；无/空/\"0\" 为 \"-\"）", example = "-")
		public String trade_no;

		@Schema(description = "订单标题（null 转空串）", example = "")
		public String title;

		@Schema(description = "订单总金额（**分字符串原样透传**，订单表 total_fee 为字符串列；可 null）", nullable = true, example = "8000")
		public String total_fee;

		@Schema(description = "店铺编码（平台单/查不到为空串）", example = "")
		public String shop_code;

		@Schema(description = "店铺名称（平台单/查不到为空串）", example = "")
		public String shop_name;

		@Schema(description = "下单手机号（订单表存储原值透传，null 转空串）", example = "")
		public String mobile;

		@Schema(
				description = "订单状态：DONE 完成 / PAYED 已支付 / NOTPAY 未支付 / CANCEL 已取消 / WAIT_BUYER_CONFIRM 待收货",
				example = "PAYED")
		public String order_status;

		@Schema(
				description = "支付状态：NOTPAY 未支付 / PAYED 已支付 / ADVANCE_PAY 预付款完成 / TAIL_PAY 支付尾款中",
				example = "PAYED")
		public String pay_status;

		@Schema(
				description = "订单创建时间（yyyy-MM-dd HH:mm:ss，系统时区；epoch ≤0/null 转空串；示例为格式演示值）",
				example = "2026-01-01 00:00:00")
		public String create_time;

		@Schema(
				description = "订单更新时间（yyyy-MM-dd HH:mm:ss，系统时区；epoch ≤0/null 转空串；示例为格式演示值）",
				example = "2026-01-01 00:00:00")
		public String update_time;

		@Schema(description = "快递公司编码（null 转空串）", example = "SF")
		public String delivery_corp;

		@Schema(description = "快递单号（null 转空串）", example = "SFTRACK001")
		public String delivery_code;

		@Schema(
				description = "发货时间（yyyy-MM-dd HH:mm:ss，系统时区；epoch ≤0/null 转空串；示例为格式演示值）",
				example = "2026-01-01 00:00:00")
		public String delivery_time;

		@Schema(description = "发货状态：PENDING 待发货 / PARTAIL 部分发货（代码拼写）/ DONE 已发货", example = "DONE")
		public String delivery_status;

		@Schema(description = "会员折扣金额，单位分（整型，null 转 0）", example = "0")
		public Integer member_discount;

		@Schema(description = "优惠券抵扣金额，单位分（整型，null 转 0）", example = "0")
		public Integer coupon_discount;

		@Schema(description = "优惠总额，单位分（整型，null 转 0——本行不做会员+券回退求和）", example = "0")
		public Integer discount_fee;

		@Schema(description = "订单优惠明细数组（解析失败/无优惠为空数组；行 5 固定键，结构同订单详情接口）")
		public List<EcxOrderGetDataDoc.DiscountInfoDoc> discount_info;

		@Schema(
				description =
						"取消状态：NO_APPLY_CANCEL 未申请 / WAIT_PROCESS 等待审核 / REFUND_PROCESS 退款处理 /"
								+ " SUCCESS 取消成功 / FAILS 取消失败",
				example = "NO_APPLY_CANCEL")
		public String cancel_status;

		@Schema(
				description = "订单完成时间（yyyy-MM-dd HH:mm:ss，系统时区；epoch ≤0/null 转空串）",
				example = "")
		public String end_time;

		@Schema(description = "是否平台自营单（布尔，distributor_id=0 为 true）", example = "true")
		public Boolean is_self;
	}
}