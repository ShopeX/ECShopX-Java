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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；成功信封（{@code OpenapiResponseBodyAdvice} 自动包装，message 固定「成功」）。
 * data 为主类目树数组（is_main_category=1、distributor_id=0，全量不分页，根 pid=0）。
 * 与 ecx.item.category.get 共用 {@code OpenapiThirdApiV2ItemCategoryListService#executeOpenapiGetCategoryList}
 *（仅 isMainCategory 过滤值不同），节点结构复用 {@link EcxItemCategoryGetNodeDoc}。
 */
@Schema(name = "EcxItemMaincategoryGetSuccessEnvelope")
public class EcxItemMaincategoryGetSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Advice 包装固定「成功」）", example = "成功")
	public String message;

	@ArraySchema(
			arraySchema = @Schema(description = "主类目树（一级节点数组，children 递归）；无主类目为 []"),
			schema = @Schema(implementation = EcxItemCategoryGetNodeDoc.class))
	public List<EcxItemCategoryGetNodeDoc> data;
}