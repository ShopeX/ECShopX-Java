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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * E0001（ResourceException → BUSINESS_ERROR）：「请填写订单编号」（order_id 与 ziti_code 均 PHP falsy）/
 * 「订单相应的明细不存在」（ziti_code 无 Redis 映射或过期）。
 * E0000 + status=fail（OpenapiLegacyZeroCodeFailException，PHP 零码契约）：
 * 「订单号为{orderId}的订单不存在」（buildOrderBundle BadRequest 且文案以「订单号为」开头时转换）。
 * 非信封通道：order_id 非整数 → BadRequest「参数错误」经 GlobalExceptionHandler → HTTP 400
 * ApiResult{code:400,msg:参数错误}；其他未处理异常 → HTTP 500 ApiResult。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxOrderGetFailEnvelope")
public class EcxOrderGetFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description = "业务码：E0001 参数/核销码错误；E0000 订单不存在（PHP 零码失败契约，status=fail）；E4004 未授权",
			example = "E0001")
	public String code;

	@Schema(description = "错误说明（各分支真实文案见类注释）", example = "请填写订单编号")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}