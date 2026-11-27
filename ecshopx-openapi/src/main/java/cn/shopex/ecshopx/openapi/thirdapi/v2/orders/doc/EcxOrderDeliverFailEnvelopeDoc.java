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
 * {@code OpenapiExceptionHandler#handleOrderDeliverV2Fail}（OpenapiOrderDeliverV2FailException）：
 * E5002「发货商品信息格式错误」（item_info 空/非法 JSON/空数组/元素非对象）；
 * E5001 参数缺失（订单号必填 / 发货商品信息的商品名称必填 / 发货商品信息的商品货号必填 /
 * 发货商品信息的发货数量必填 / 快递公司编码必填 / 快递单编码必填）；
 * E5301「订单相应的明细不存在」（order_id 非整数 / 无此单 / 明细为空）；
 * E5312「订单已发货，请勿重复发货」（delivery_status=DONE）；
 * E5310 处理失败（发货商品有误——sku_id 全部不匹配；订单处理错误——无待发行或关联缺失；
 * 及下游 ResourceException/Exception message 透传：订单未支付，不能发货 / 订单已取消，不能发货 /
 * 订单有退款待处理，不能发货 / 订单已发货，不能重复发货 / 自配送订单发货必须选择配送状态 /
 * 自配送订单发货必须选择配送员（merchant 自提单经开放接口必失败——无自配送入参）/
 * 拆单信息不正确 / 订单号为{id}的订单,发货商品数量格式错误：{行} / …数量错误:{值} / …发货商品不正确 /
 * …发货商品已发货 / 供应商商品请由供应商发货 / …数量{num}大于购买数量{remain} /
 * 创建发货单失败 / 订单关联信息不存在 等）。
 * 未捕获异常经 GlobalExceptionHandler → HTTP 500 ApiResult（非本信封）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxOrderDeliverFailEnvelope")
public class EcxOrderDeliverFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description =
					"业务码：E5002 item_info 格式错误；E5001 参数缺失；E5301 订单不存在；E5312 重复发货；E5310 处理失败；E4004 未授权",
			example = "E5310")
	public String code;

	@Schema(description = "错误说明（各分支真实文案见类注释）", example = "订单未支付，不能发货")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}