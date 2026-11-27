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
 * 仅 OpenAPI 文档；POST 请求 JSON body，3 键与 query 同名双通道（均 {@code originalString}：
 * body 含键优先、值 null 视为未传，取值不 trim；服务内 trim 后再校验）。
 * 校验见 {@code OpenapiThirdApiV2OrderConfirmCancelService#validateParams}；
 * 审核执行见 {@code AdminOrderConfirmCancelService#confirmOrderCancel}（order_type 固定 normal、
 * operator_type=openapi、operator_id=0、supplier_id=0，不接受 order_type/refund_bn 入参）。
 */
@Schema(
		name = "EcxOrderCancelConfirmBody",
		description =
				"取消审核参数（3 键，body 含键优先否则 query 同名键）。"
						+ " 对该订单最新一条待审核（READY）的售前退款单（refund_type=1 且未挂售后单）进行审核："
						+ " 1 同意（进入退款流程），0 拒绝（取消申请驳回）。")
public class EcxOrderCancelConfirmBodyDoc {

	@Schema(
			description =
					"订单 ID；必填（trim 后非空，否则 E5001「订单编号必填」）。"
							+ "trim 后 Long.parseLong，解析失败 → E5310「没有查到退款单，无法同意取消订单」"
							+ "（BadRequest「订单号格式错误」的映射文案）；无待审退款单 → 同文案",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "100")
	public String order_id;

	@Schema(
			description =
					"是否同意取消；必填，仅 \"1\"（同意）或 \"0\"（拒绝）——trim 后须匹配 ^[01]$，"
							+ "空白或其他值 → E5001「是否同意必填」。同意=审核通过退款单，拒绝=驳回取消申请",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String cancel_handle;

	@Schema(
			description =
					"拒绝原因；cancel_handle=0 时必填（trim 后非空，否则 E5001「拒绝退款时原因必填」）。"
							+ "trim 后落 cancel_orders.shop_reject_reason 并写订单流程日志；=1 时忽略（仍随参数透传日志）",
			example = "档期已满")
	public String reject_reason;
}