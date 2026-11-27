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
 * 仅 OpenAPI 文档；AdaPay 异步回调请求体（{@code @FlexibleBody Map}，不改运行时签名）。
 * 顶层固定三键由 {@code AdapayCallbackOrchestratorService.handle} 读取；
 * data 为 JSON 字符串，验签后解析，键集合随 type 而定（见各 PayloadDoc）。
 */
@Schema(name = "AdapayCallbackBody", description = "AdaPay 异步回调请求体（JSON 或 form 双通道）")
public class AdapayCallbackBodyDoc {

	@Schema(
			description =
					"事件类型；固定枚举：payment.succeeded / payment.failed / corp_member.succeeded /"
							+ " corp_member.failed / corp_member_update.succeeded / corp_member_update.failed /"
							+ " payment_reverse.succeeded / payment_reverse.failed /"
							+ " payment.close.succeeded / payment.close.failed；"
							+ "其余值报 400「unknown type」；缺失按空串处理（同样 unknown type）",
			example = "payment.succeeded")
	public String type;

	@Schema(
			description =
					"事件负载 JSON 字符串（先按原文验签再解析；form 通道为字符串，JSON 通道通常为字符串，"
							+ "对象形态按 toString 处理会验签失败）。空/非法 JSON 报 400「回调 data 不是合法 JSON」。"
							+ "键集合随 type：payment.* 见 AdapayCallbackPaymentPayload；corp_member.* 见"
							+ " AdapayCallbackCorpMemberPayload；corp_member_update.* 见"
							+ " AdapayCallbackCorpMemberUpdatePayload；payment_reverse.* 见"
							+ " AdapayCallbackPaymentReversePayload；payment.close.* 解析后忽略",
			example =
					"{\"order_no\":\"3651023000015\",\"status\":\"succeeded\",\"pay_channel\":\"wx_lite\","
							+ "\"description\":\"depositRecharge\",\"expend\":{\"bank_type\":\"ALIPAY\"},"
							+ "\"id\":\"tx1\"}")
	public String data;

	@Schema(
			description =
					"AdaPay 对 data 原文的 SHA1withRSA 签名（Base64）；用配置的 adapay.callback.rsa-public-key 验签；"
							+ "公钥未配置/缺失/验签不过均报 400「签名验证失败」。示例为占位 Base64，非真实签名",
			example = "K0x1eS5wq8Zm3nQ1rT7vYb2cD4eF6gH8iJ0kL2mN4oP6qR8sT0uV2wX4yZ6aB8cD0eF==")
	public String sign;
}