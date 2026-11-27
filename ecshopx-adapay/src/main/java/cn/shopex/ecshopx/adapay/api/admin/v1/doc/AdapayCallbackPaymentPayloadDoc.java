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

package cn.shopex.ecshopx.adapay.api.admin.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；data JSON（type=payment.succeeded / payment.failed）中代码实际读取的键。
 * 证据：{@code AdapayCallbackPaymentHandler.succeeded}、
 * {@code AdapayOrdersTradePaymentCallbackService.applyPaymentSucceeded}。
 * 其余 AdaPay 回传键仅原样存档（trade.inital_response），不在本表展开。
 */
@Schema(
		name = "AdapayCallbackPaymentPayload",
		description = "payment.* 事件 data JSON 字符串内的已读取键（示例值取自代码/测试出现过的值或按生成规则构造）")
public class AdapayCallbackPaymentPayloadDoc {

	@Schema(
			description =
					"商户交易单号（=trade.trade_id，创建支付时作为 order_no 传给 AdaPay）；必填非空白，"
							+ "缺失报 400「缺少 order_no」；查无交易报 422「未查询到更新数据」/「更新订单不存在」",
			example = "3651023000015")
	public String order_no;

	@Schema(
			description =
					"支付状态；succeeded → SUCCESS，其余值（含缺失）→ PAYERROR；trade 非 NOTPAY 时报 400"
							+ "「更新已处理，不需要更新」",
			example = "succeeded")
	public String status;

	@Schema(
			description =
					"支付渠道；非空才写入 trade.pay_channel。代码出现的取值：wx_lite / wx_pub / alipay /"
							+ " alipay_wap / alipay_qr",
			example = "wx_lite")
	public String pay_channel;

	@Schema(
			description =
					"交易来源描述（创建支付时传 trade_source_type）。depositRecharge → 储值充值分支"
							+ "（该分支以 null 储值单号调用，恒报 422「储值单不存在或主键无效，无法完成回调」）；"
							+ "membercard → 额外置 trade.adapay_div_status=DIVED；其余走订单支付更新",
			example = "membercard")
	public String description;

	@Schema(
			description = "AdaPay 支付对象 id；非 null 时写入 trade.transaction_id / 储值单 transaction_id",
			example = "tx1")
	public String id;

	@Schema(description = "渠道扩展对象；代码仅读取其中 bank_type")
	public AdapayCallbackPaymentExpendDoc expend;

	@Schema(name = "AdapayCallbackPaymentExpend", description = "payment.* data.expend 内已读取键")
	public static class AdapayCallbackPaymentExpendDoc {

		@Schema(
				description = "付款银行类型（渠道透传，写入 trade.bank_type / 储值单 bank_type；缺失按空串）",
				example = "ALIPAY")
		public String bank_type;
	}
}