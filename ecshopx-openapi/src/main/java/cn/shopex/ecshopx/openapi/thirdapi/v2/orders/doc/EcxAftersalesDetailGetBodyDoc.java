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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源
 * {@code AftersalesController#getAftersalesDetail} 经 {@code OpenapiRequestParams#originalString}
 * 读取的全部 key（原始值不 trim，查询时 trim）。
 */
@Schema(
		name = "EcxAftersalesDetailGetBody",
		description =
				"售后详情参数（body 与 query 同名 key 双通道：body 含键时取 body 原始值——不 trim，"
						+ "body 无该键时用 query）")
public class EcxAftersalesDetailGetBodyDoc {

	@Schema(
			description =
					"售后单号；必填非空白（trim 后），否则 E5001「请填写售后单号」；"
							+ "按租户 + aftersales_bn 精确匹配，查无/重复报 E5320「没有售后信息」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "202605101200001")
	public String aftersales_bn;
}