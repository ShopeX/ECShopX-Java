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
 * {@code OpenapiExceptionHandler#handleOrderCancelV2Fail}（OpenapiOrderCancelV2FailException）：
 * E5001 参数缺失（订单编号必填 / 是否同意必填 / 拒绝退款时原因必填）。
 * E5310 处理失败——ResourceException 文案经 RESOURCE_MSG_MAP 映射（OpenapiThirdApiV2OrderConfirmCancelService）：
 * 「没有查到退款单，无法同意取消订单」（未找到可处理的退款单；BadRequest「订单号格式错误」同文案）/
 * 「退款单状态不是待审核状态，无法拒绝」（拒绝时非 READY）/
 * 「退款单状态 已退款，无法继续操作」（已退款成功或状态不允许审核通过）/
 * 「退款单状态 已驳回，无法继续操作」/「退款单状态 已审核通过，无法继续操作」/
 * 「退款单状态 已撤销，无法继续操作」/「退款单状态 已发起退款等待到账，无法继续操作」/
 * 「退款单状态 退款异常，无法继续操作」；
 * 未映射文案原样透传（退款单不存在 / 退款单更新失败 / 订单不存在 / 取消单数据异常及其他下游异常 message）。
 * 未捕获异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxOrderCancelConfirmFailEnvelope")
public class EcxOrderCancelConfirmFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数缺失；E5310 处理失败；E4004 未授权", example = "E5310")
	public String code;

	@Schema(description = "错误说明（各分支真实文案见类注释）", example = "没有查到退款单，无法同意取消订单")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}