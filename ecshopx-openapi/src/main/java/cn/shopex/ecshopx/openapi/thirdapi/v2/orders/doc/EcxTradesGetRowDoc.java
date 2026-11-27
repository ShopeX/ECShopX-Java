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
 * 仅 OpenAPI 文档；交易单行 11 固定键（{@code OpenapiThirdApiV2TradeListService#formatOpenApiTradeRow}，
 * 字段直取 trade 表实体 {@code Trade}，LinkedHashMap 插入序）。
 * 金额列为整型分；time_start/time_expire 经 formatPhpDate 格式化。
 */
@Schema(name = "EcxTradesGetRow")
public class EcxTradesGetRowDoc {

	@Schema(description = "交易单号（trade.trade_id，字符串主键）", example = "5353724000040002")
	public String trade_id;

	@Schema(description = "订单号（trade.order_id，字符串；库内可空，null 透传）", nullable = true, example = "100")
	public String order_id;

	@Schema(description = "商户号（微信支付 mch_id；库内可空，null 透传）", nullable = true)
	public String mch_id;

	@Schema(description = "应付总金额，整型分（trade.total_fee，默认 0）", example = "8000")
	public Integer total_fee;

	@Schema(description = "订单优惠金额，整型分（trade.discount_fee，默认 0）", example = "0")
	public Integer discount_fee;

	@Schema(description = "货币类型（trade.fee_type，如 CNY）", example = "CNY")
	public String fee_type;

	@Schema(description = "支付金额，整型分（trade.pay_fee，默认 0）", example = "8000")
	public Integer pay_fee;

	@Schema(
			description =
					"交易状态（trade.trade_state）：SUCCESS 支付成功 / REFUND 转入退款 / NOTPAY 未支付 /"
							+ " CLOSED 已关闭 / REVOKED 已撤销 / PAYERROR 支付失败",
			example = "SUCCESS")
	public String trade_state;

	@Schema(
			description = "支付方式（trade.pay_type）：wxpay 微信支付 / deposit 预存款支付 / pos 刷卡 / point 积分等",
			example = "wxpay")
	public String pay_type;

	@Schema(
			description =
					"交易起始时间（yyyy-MM-dd HH:mm:ss，系统时区）；库值为 epoch 字符串：≥13 位按毫秒、否则按秒；"
							+ "空/非纯数字/解析失败按 epoch 0 格式化（示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String time_start;

	@Schema(
			description = "交易结束时间（格式化规则同 time_start；库内可空 → epoch 0 格式化；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String time_expire;
}