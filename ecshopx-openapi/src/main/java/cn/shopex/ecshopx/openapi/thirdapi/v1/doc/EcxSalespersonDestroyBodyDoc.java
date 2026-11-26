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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ShopSalespersonController#destroySalesperson}
 * 经 {@code OpenapiRequestParams#mergeString} 读取的全部 key。
 */
@Schema(
		name = "EcxSalespersonDestroyBody",
		description =
				"删除导购参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxSalespersonDestroyBodyDoc {

	@Schema(
			description =
					"导购企微 userid（shop_salesperson.work_userid），单值精确匹配（不支持逗号批量），"
							+ "按租户取第一条；查无匹配报「导购员信息查询错误」（缺失按 null 匹配，通常也查无）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "work-u")
	public String employee_number;
}