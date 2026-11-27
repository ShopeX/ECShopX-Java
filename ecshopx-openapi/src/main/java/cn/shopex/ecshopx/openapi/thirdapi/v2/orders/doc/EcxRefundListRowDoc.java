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

/**
 * 仅 OpenAPI 文档；list 行（15 键）。
 * {@code OpenapiThirdApiV2RefundListFormatSupport#formatOpenApiRefundRow}：
 * 与退款详情（formatOpenApiRefundDetail）差异——无 return_freight、无条件键 cancel_reason；
 * refund_success_time 空/≤0 按 epoch 0 格式化（详情则返回当前时间）。
 * 时间键 yyyy-MM-dd HH:mm:ss（系统时区）；create/update_time null/≤0 按 epoch 0（1970-01-01 08:00:00）。
 */
@Schema(name = "EcxRefundListRow")
public class EcxRefundListRowDoc {

	@Schema(description = "退款单号（字符串形态）", example = "2202608258337278667")
	public String refund_bn;

	@Schema(description = "售后单号（字符串形态；售前取消退款为 null）", nullable = true, example = "202605101200001")
	public String aftersales_bn;

	@Schema(description = "订单号（字符串形态）", example = "88001")
	public String order_id;

	@Schema(description = "支付单号（原值透传，可为空串/null）", nullable = true, example = "")
	public String trade_id;

	@Schema(
			description = "退款渠道：offline 线下退款 / original 原路返回",
			allowableValues = {"offline", "original"},
			example = "original")
	public String refund_channel;

	@Schema(
			description =
					"退款状态：READY 未审核 / AUDIT_SUCCESS 审核成功待退款 / SUCCESS 退款成功 / REFUSE 退款驳回 /"
							+ " CANCEL 撤销退款 / REFUNDCLOSE 退款关闭 / PROCESSING 退款处理中或已发起退款等待到账 /"
							+ " CHANGE 退款异常 / PAY_AFTER_CANCEL 逾期支付待人工审批退款 / DUP_PAY 重复支付待人工审批退款",
			example = "SUCCESS")
	public String refund_status;

	@Schema(description = "应退金额，单位分（整型，非积分支付部分）", example = "100")
	public Integer refund_fee;

	@Schema(description = "实退金额，单位分（整型，默认 0）", example = "100")
	public Integer refunded_fee;

	@Schema(description = "支付方式（同对应支付单，如 wxpay；可空串/null）", nullable = true, example = "")
	public String pay_type;

	@Schema(description = "商户返回退款单号（可 null）", nullable = true)
	public String refund_id;

	@Schema(description = "应退积分（整型）", example = "0")
	public Integer refund_point;

	@Schema(description = "实退积分（整型，默认 0）", example = "0")
	public Integer refunded_point;

	@Schema(
			description =
					"退款成功时间（yyyy-MM-dd HH:mm:ss，系统时区；null/≤0 按 epoch 0 → 1970-01-01 08:00:00——"
							+ "与详情的当前时间不同；示例为格式演示值）",
			example = "2023-11-15 08:00:01")
	public String refund_success_time;

	@Schema(
			description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；null/≤0 按 epoch 0 → 1970-01-01 08:00:00；示例为格式演示值）",
			example = "2023-11-15 08:00:01")
	public String create_time;

	@Schema(
			description = "更新时间（yyyy-MM-dd HH:mm:ss，系统时区；null/≤0 按 epoch 0；示例为格式演示值）",
			example = "2023-11-15 08:00:01")
	public String update_time;
}