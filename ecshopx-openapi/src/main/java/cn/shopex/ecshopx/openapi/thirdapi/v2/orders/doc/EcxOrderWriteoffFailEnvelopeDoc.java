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
 * {@code OpenapiExceptionHandler#handleOrderWriteoffV2Fail}（OpenapiOrderWriteoffV2FailException）：
 * E5001 参数缺失（请填写订单编号 / 请填写提货码——提货码开关开启且未传）。
 * E5301：「订单相应的明细不存在」（order_id 非整数/≤0/无此单）/
 * 「自提订单相应的明细不存在」（receipt_type 非 ziti，外层前置校验）。
 * E5310 处理失败：「无此类型订单！」（order_type 空/supplier_order/membercard/service 族）/
 * 「自提订单状态不正确，不能进行操作」（外层：order_status≠PAYED 或 ziti_status≠PENDING 或
 * pay_status≠PAYED 或 cancel_status 不在 NO_APPLY_CANCEL/FAILS）/
 * 「未查询到提货人联系手机！」/「提货码验证错误！」/
 * 内层互斥后二次校验文案透传（此订单不存在！/订单收货方式不允许核销/订单状态不允许核销/
 * 自提状态不允许核销/订单未支付，无法核销/订单取消状态不允许核销）/
 * 「订单状态已变更，无法核销」（并发下 PENDING 条件更新 0 行）及其他下游异常 message。
 * 未捕获异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxOrderWriteoffFailEnvelope")
public class EcxOrderWriteoffFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description = "业务码：E5001 参数缺失；E5301 订单/自提单不存在；E5310 处理失败；E4004 未授权",
			example = "E5310")
	public String code;

	@Schema(description = "错误说明（各分支真实文案见类注释）", example = "自提订单状态不正确，不能进行操作")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}