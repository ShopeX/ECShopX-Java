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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；成功信封 data。{@code OpenapiThirdApiV1WxappShopListService#execute}：
 * distribution_distributor 按租户 +（可选 updated 严格区间）过滤，created 升序分页；
 * total_count 为同过滤条件总数。
 */
@Schema(name = "EcxWxappShopListData")
public class EcxWxappShopListDataDoc {

	@Schema(description = "满足过滤条件的店铺总数", example = "1")
	public Long total_count;

	@ArraySchema(
			arraySchema = @Schema(description = "店铺行（created 升序，LIMIT pageSize OFFSET (page-1)*pageSize）；无数据为 []"),
			schema = @Schema(implementation = EcxWxappShopListItemDoc.class))
	public List<EcxWxappShopListItemDoc> list;
}