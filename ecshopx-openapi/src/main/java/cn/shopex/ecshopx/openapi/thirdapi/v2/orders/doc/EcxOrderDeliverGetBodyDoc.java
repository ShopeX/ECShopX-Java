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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，1 键与 query 同名双通道（{@code mergeString}：
 * body 含键优先——null/空白串视为未传；否则 query trim 后非空才生效）。
 * 查询见 {@code OpenapiThirdApiV2OrderDeliverListService#executeGetOrderDeliveryListV2}。
 */
@Schema(
		name = "EcxOrderDeliverGetBody",
		description =
				"订单发货单列表参数（1 键，body 含键优先否则 query 同名键）。"
						+ " 按租户查 orders_delivery（发货单 ID 升序），末尾附一条未发货合成行（如有待发余量）。")
public class EcxOrderDeliverGetBodyDoc {

	@Schema(
			description =
					"订单 ID；必填（非空否则 E5001「请填写订单编号」）。"
							+ "trim 后 Long 解析失败不报错——返回成功空结果（delivery_num=0、list=[]）；无此单同样空结果",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "100")
	public String order_id;
}