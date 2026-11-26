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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code DeliveryController#createDelivery} 经
 * {@code OpenapiRequestParams#mergeRequiredString} 读取的全部 key；item_info 内层 key 来源
 * {@code OpenapiThirdApiV1OrderDeliverService#indexItemInfoBySkuId} / {@code doOrderDelivery}。
 */
@Schema(
		name = "EcxOrderDeliverBody",
		description =
				"订单发货参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）。四字段均业务必填",
		example =
				"{\"order_id\":\"10001\",\"item_info\":\"[{\\\"sku_id\\\":\\\"A1\\\",\\\"qty\\\":1}]\","
						+ "\"lc_code\":\"SF\",\"l_code\":\"SFTRACK001\"}")
public class EcxOrderDeliverBodyDoc {

	@Schema(
			description = "订单号；必填（空→「订单号必填」）；非整数/查无订单→「此订单不存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "10001")
	public String order_id;

	@Schema(
			description =
					"发货商品信息，JSON 数组字符串，元素 {sku_id, qty}：sku_id 匹配订单行 item_bn（货品编码），"
							+ "qty 为本次发货数量；必填（空→「发货商品信息必填」）；"
							+ "仅处理 delivery_status=PENDING 的订单行；非法 JSON/无匹配行→「操作失败」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "[{\"sku_id\":\"A1\",\"qty\":1}]")
	public String item_info;

	@Schema(
			description = "快递公司编码（如 SF/YTO）；必填（空→「快递公司编码必填」）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "SF")
	public String lc_code;

	@Schema(
			description = "快递单号；必填（空→「快递单编码必填」）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "SFTRACK001")
	public String l_code;
}