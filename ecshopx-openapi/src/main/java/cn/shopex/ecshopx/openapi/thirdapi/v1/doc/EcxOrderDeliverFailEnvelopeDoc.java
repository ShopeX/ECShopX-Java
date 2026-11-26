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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。
 *
 * <p>业务失败均为 {@code ResourceException} → {@code OpenapiExceptionHandler#handleResource} → E0001、data=null：
 * 订单号必填 / 发货商品信息必填 / 快递公司编码必填 / 快递单编码必填（mergeRequiredString）；
 * 此订单不存在（order_id 非整数或查无）；订单已发货，请勿重复发货（delivery_status=DONE）；
 * 获取订单信息失败；操作失败（item_info 非法 JSON/无匹配 PENDING 行/无订单关联/发货核心异常统一吞并）。
 * 鉴权失败：E4004「未授权」（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxOrderDeliverFailEnvelope")
public class EcxOrderDeliverFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 业务失败；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明", example = "订单已发货，请勿重复发货")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}