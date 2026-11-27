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
 * 仅 OpenAPI 文档；data JSON（type=payment_reverse.succeeded / payment_reverse.failed）中
 * 代码实际读取的键。证据：{@code AdapayCallbackPaymentReverseHandler.succeeded}。
 * 整个 data 原样序列化存档到 adapay_payment_reverse.response_params。
 */
@Schema(
		name = "AdapayCallbackPaymentReversePayload",
		description = "payment_reverse.* 事件 data JSON 字符串内的已读取键（示例按撤销单号生成规则构造）")
public class AdapayCallbackPaymentReversePayloadDoc {

	@Schema(
			description =
					"支付撤销请求订单号（发起时格式：{refund_bn}_rev_{5位随机}{毫秒时间戳}）；非空才按 order_no"
							+ " 更新 adapay_payment_reverse；空串跳过更新仍返回成功",
			example = "20231127001_rev_543211700000000000")
	public String order_no;

	@Schema(description = "撤销状态（原样写入 adapay_payment_reverse.status）", example = "succeeded")
	public String status;
}