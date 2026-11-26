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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，2 键与 query 同名双通道，解析见
 * {@code OrderController#parseOptionalIntDefaultZero}（body 含键优先，
 * {@code OpenapiRequestParams#originalString}；缺键/空白/null/非数字 → 0）。
 */
@Schema(
		name = "EcxLogisticsEnabledGetBody",
		description =
				"开启物流公司列表过滤参数（2 键，body 含键优先否则 query 同名键）。"
						+ " 两键均宽松解析：缺失/空白/null/非整数字符串一律按 0，不报参数错误；"
						+ " 按 company_rel_logistics(company_id, distributor_id, supplier_id) 等值匹配已开启维度。")
public class EcxLogisticsEnabledGetBodyDoc {

	@Schema(
			description =
					"店铺（分销商）ID；整数，默认 0（总部维度）；缺键/空白/null/非数字均按 0，"
							+ "与 supplier_id 一起等值匹配租户已开启的物流公司维度",
			example = "0")
	public Integer distributor_id;

	@Schema(
			description = "供应商 ID；整数，默认 0（非供应商维度）；解析规则同 distributor_id",
			example = "0")
	public Integer supplier_id;
}