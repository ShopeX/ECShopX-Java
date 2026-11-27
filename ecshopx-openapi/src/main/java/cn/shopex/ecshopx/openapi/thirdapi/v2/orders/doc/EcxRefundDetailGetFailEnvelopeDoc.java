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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null，经
 * {@code OpenapiExceptionHandler}（OpenapiAftersalesV2FailException）：
 * E5001「请填写退款单号」（refund_bn 缺失/空白）。
 * E5340（ORDER_REFUND_HANDLE_ERROR）：「Undefined array key \"refund_channel\"」（查无此退款单——
 * PHP 错误文案对齐契约）；同租户多行命中或其他异常的 message 原样透传（TooManyResults/DB 异常等）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxRefundDetailGetFailEnvelope")
public class EcxRefundDetailGetFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 退款单号缺失；E5340 查无/处理失败；E4004 未授权", example = "E5340")
	public String code;

	@Schema(description = "错误说明（各分支真实文案见类注释）", example = "Undefined array key \"refund_channel\"")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}