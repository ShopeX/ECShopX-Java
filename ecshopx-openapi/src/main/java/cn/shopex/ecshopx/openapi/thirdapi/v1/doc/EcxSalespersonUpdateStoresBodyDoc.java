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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ShopSalespersonController#updateSalespersonStores}
 * 经 {@code OpenapiRequestParams#mergeString} 读取的全部 key，
 * 分支语义见 {@code OpenapiThirdApiV1SalespersonUpdateStoresService}。
 */
@Schema(
		name = "EcxSalespersonUpdateStoresBody",
		description =
				"更新导购绑定店铺参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxSalespersonUpdateStoresBodyDoc {

	@Schema(
			description =
					"导购企微 userid（shop_salesperson.work_userid），单值精确匹配（LIMIT 1，不限人员类型）；"
							+ "查无匹配报「导购员信息查询错误」（缺失按 null 匹配，通常也查无）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "work-u")
	public String employee_number;

	@Schema(
			description =
					"店铺编号（distribution_distributor.shop_code），逗号分隔多个（逐段 trim、忽略空段）。"
							+ "数值等于 0（如 \"0\"/\"0.0\"）→ 清空该导购全部店铺关联并成功返回；"
							+ "缺失/空白或全为空段 → 按空编号查询，通常报「未查询到店铺信息」；"
							+ "否则按租户查有效店铺（is_valid=true），查无报「未查询到店铺信息」，"
							+ "命中则先删该导购全部旧关联再绑定全部匹配店铺",
			example = "S01")
	public String store_bn;
}