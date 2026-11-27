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
 * E5001 参数缺失（请填写订单编号 / 请填写取消原因ID / 请填写取消原因描述——12 无描述或描述 trim 后 >255）；
 * E5301「订单相应的明细不存在」（order_id 非整数 / order_associations 无此单）；
 * E5310 处理失败（请填写正确的取消原因ID / 实体类订单才能取消订单 / 订单号为{orderId}的订单不存在 /
 * 订单已取消 / 订单状态已不能申请取消 / 已发货订单不能取消 / 订单已审核成功，不能取消 / 骑手已取货，订单不能取消 /
 * 「取消原因必选，」——取消执行抛出的 ResourceException 与其他异常的 message 原样透传）。
 * 未捕获异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxOrderCancelFailEnvelope")
public class EcxOrderCancelFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description = "业务码：E5001 参数缺失；E5301 订单不存在；E5310 处理失败；E4004 未授权",
			example = "E5310")
	public String code;

	@Schema(description = "错误说明（各分支真实文案见类注释）", example = "订单状态已不能申请取消")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}