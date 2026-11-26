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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ShopSalespersonController#bathUpdateSalespersonStatus}
 * 经 {@code OpenapiRequestParams#mergeString} 读取的全部 key。
 */
@Schema(
		name = "EcxSalespersonBathStatusUpdateBody",
		description =
				"批量更新导购状态参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxSalespersonBathStatusUpdateBodyDoc {

	@Schema(
			description =
					"导购企微 userid（shop_salesperson.work_userid），逗号分隔多个批量匹配；"
							+ "全部查无匹配报「导购员信息查询错误」（缺失时按空串匹配，通常也查无）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "work-u")
	public String employee_number;

	@Schema(
			description = "目标状态：\"1\" 启用（is_valid=true）；其他任何值（含缺失）均置停用（is_valid=false）",
			example = "1")
	public String salesperson_status;
}