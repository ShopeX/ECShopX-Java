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
 * 仅 OpenAPI 文档；data 结构（4 固定键，键序 count/total_fee/order_avg/list），来源
 * {@code OpenapiThirdApiV2OrderListService#executeOpenapiOrderList}。
 * count/total_fee/order_avg 统计该会员**全部订单**（不随分页变化，含未支付/已取消）；
 * list 为分页内订单按商品行展平（订单 create_time 降序，行内 SQL 无显式排序），
 * 无商品行的订单被跳过；无订单时 count/total_fee/order_avg 为整型 0（非字符串 "0.00"）。
 * 金额键为元字符串（分 ÷100，2 位小数，HALF_UP）。
 */
@Schema(name = "EcxOrderListData")
public class EcxOrderListDataDoc {

	@Schema(description = "该会员订单总数（整型，全部订单不分页）", example = "1")
	public Long count;

	@Schema(
			description = "该会员下单总金额（元字符串，2 位小数，全部订单 total_fee 分求和 ÷100；无订单时为整型 0）",
			example = "80.00")
	public Object total_fee;

	@Schema(
			description = "客单价（元字符串，2 位小数，total_fee÷count HALF_UP；无订单时为整型 0）",
			example = "80.00")
	public Object order_avg;

	@Schema(description = "订单商品行展平数组（每行=订单 5 键+商品 8 键，同订单多商品重复订单键；无数据为空数组）")
	public List<RowDoc> list;

	/** 展平行（12 键：订单 5 + 商品 8；行级 total_fee 覆盖订单级同名键）。 */
	@Schema(name = "EcxOrderListRow")
	public static class RowDoc {

		@Schema(description = "订单号（字符串）", example = "100")
		public String order_id;

		@Schema(description = "成功交易单号（trade_state=SUCCESS；无/空/\"0\" 为 \"-\"）", example = "-")
		public String trade_no;

		@Schema(
				description =
						"订单状态：DONE 完成 / PAYED 已支付 / NOTPAY 未支付 / CANCEL 已取消 / WAIT_BUYER_CONFIRM 待收货",
				example = "PAYED")
		public String order_status;

		@Schema(
				description =
						"**行商品金额**（元字符串，2 位小数）——代码先放订单总额后被行金额覆盖，实际恒为商品行 total_fee",
				example = "80.00")
		public String total_fee;

		@Schema(description = "订单创建时间（Unix 秒，整型透传，可 null）", nullable = true, example = "1767225600")
		public Integer create_time;

		@Schema(description = "商品名称（SQL 原值透传，可 null）", nullable = true, example = "Item-A")
		public String item_name;

		@Schema(description = "货品 ID（整型透传）", example = "1")
		public Long item_id;

		@Schema(description = "商品编码/货号（null 转空串）", example = "")
		public String item_bn;

		@Schema(description = "商品单价（元字符串，2 位小数）", example = "80.00")
		public String price;

		@Schema(description = "购买数量（整型透传）", example = "1")
		public Integer num;

		@Schema(description = "商品规格描述（null 转空串）", example = "")
		public String item_spec_desc;

		@Schema(description = "商品图片（null 转空串）", example = "")
		public String pic;
	}
}