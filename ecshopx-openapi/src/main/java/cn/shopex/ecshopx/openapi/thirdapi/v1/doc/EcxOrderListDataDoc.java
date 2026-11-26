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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiThirdApiV1OrderListService#executeOpenapiOrderList}：
 * count 为该会员全部订单数（非本页）；list 为**订单×商品行展平**（每商品行重复订单字段，12 键；
 * 行内 total_fee 被商品行金额覆盖，订单金额仅存在于展平前的中间结构）；
 * 金额字段均为元两位小数字符串（空结果时为数值 0）。
 */
@Schema(
		name = "EcxOrderListData",
		description = "会员订单统计 + 订单商品行展平列表（create_time 倒序；无商品行的订单被跳过）")
public class EcxOrderListDataDoc {

	@Schema(description = "该会员订单总数（全部，非本页条数）", example = "1")
	public Long count;

	@Schema(description = "累计订单金额（元，两位小数字符串；无订单时为数值 0）", example = "99.00")
	public Object total_fee;

	@Schema(description = "平均订单金额（元，两位小数字符串，total_fee/count HALF_UP；无订单时为数值 0）", example = "99.00")
	public Object order_avg;

	@ArraySchema(arraySchema = @Schema(description = "订单商品行展平列表；无订单时为空数组"))
	public List<FlatRowDoc> list;

	@Schema(name = "EcxOrderListFlatRow", description = "单条订单商品行（订单字段 + 商品字段展平，12 键）")
	public static class FlatRowDoc {

		@Schema(description = "订单 ID（字符串化）", example = "10001")
		public String order_id;

		@Schema(description = "支付交易号（成功支付单 trade_no；无/为 0 时为 \"-\"）", example = "-")
		public String trade_no;

		@Schema(
				description = "订单状态；NOTPAY 且已过 auto_cancel_time 时展示为 CANCEL",
				example = "WAIT_BUYER_CONFIRM")
		public String order_status;

		@Schema(
				description = "商品行金额（元，两位小数字符串；展平时覆盖订单级 total_fee，线上行值为准）",
				example = "99.00")
		public String total_fee;

		@Schema(description = "下单时间（Unix 秒）", nullable = true)
		public Integer create_time;

		@Schema(description = "商品名称", nullable = true)
		public String item_name;

		@Schema(description = "商品/SKU ID", nullable = true)
		public Long item_id;

		@Schema(description = "货品编码；null→空串", example = "A1")
		public String item_bn;

		@Schema(description = "商品单价（元，两位小数字符串）", example = "99.00")
		public String price;

		@Schema(description = "购买数量", nullable = true)
		public Long num;

		@Schema(description = "规格描述；null→空串", example = "")
		public String item_spec_desc;

		@Schema(description = "商品图；null→空串", example = "")
		public String pic;
	}
}