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
 * body 含键优先、值 null 视为未传，取值不 trim；服务内 trim 后再解析）。
 * 校验见 {@code OpenapiThirdApiV2OrderCancelService#validateParams/executeOpenapiCancel}，
 * 取消执行见 {@code AdminNormalOrderFullCancelService#execute}（operator_type=openapi，cancel_from=shop）。
 */
@Schema(
		name = "EcxOrderCancelBody",
		description =
				"取消订单参数（3 键，body 含键优先否则 query 同名键）。"
						+ " 仅实体类（normal）订单可取消；未支付直接关单退积分，已支付创建取消单/退款并置 WAIT_PROCESS。")
public class EcxOrderCancelBodyDoc {

	@Schema(
			description =
					"订单 ID；必填（trim 后非空，否则 E5001「请填写订单编号」）。"
							+ "trim 后 Long.parseLong，解析失败或该租户 order_associations 无此单 → E5301「订单相应的明细不存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "100")
	public String order_id;

	@Schema(
			description =
					"取消原因 ID；必填（trim 后非空，否则 E5001「请填写取消原因ID」）。"
							+ "trim 后 Integer.parseInt，解析失败或不在 1..12 → E5310「请填写正确的取消原因ID」。"
							+ "内置文案：1 客户现在不想购买 / 2 客户商品价格较贵 / 3 客户价格波动 / 4 客户商品缺货 /"
							+ " 5 客户重复下单 / 6 客户订单商品选择有误 / 7 客户支付方式选择有误 / 8 客户收货信息填写有误 /"
							+ " 9 客户发票信息填写有误 / 10 客户无法支付订单 / 11 客户长时间未付款 / 12 客户其他原因（configText 置空，"
							+ "须靠 cancel_reason 提供文案）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String cancel_reason_id;

	@Schema(
			description =
					"自定义取消原因描述；cancel_reason_id=12 时必填（trim 后非空，否则 E5001「请填写取消原因描述」）。"
							+ "非 null 且 trim 后长度 >255 也报 E5001「请填写取消原因描述」。"
							+ "trim 后非空则覆盖配置文案作为生效取消原因（同时以 other_reason 落取消记录）；"
							+ "空白回退内置配置文案。已支付订单生效原因与描述均为空 → E5310「取消原因必选，」（文案含尾部逗号）",
			example = "客户电话要求取消")
	public String cancel_reason;
}