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
 * 仅 OpenAPI 文档；attribute_values 嵌套对象（total_count + list）。
 * {@code OpenapiThirdApiV2ItemMainCategoryDetailService#formatAttributeValuesNested}；
 * 值列表按 sort 升序 LIMIT 100（{@code ItemsAttributeValuesRepository#listByAttributeId}）。
 */
@Schema(name = "EcxItemMaincategoryAttrValues")
public class EcxItemMaincategoryAttrValuesDoc {

	@Schema(description = "值总数（字符串形态）", example = "1")
	public String total_count;

	@ArraySchema(
			arraySchema = @Schema(description = "属性值行（sort 升序，最多 100 条，7 键/行）"),
			schema = @Schema(implementation = EcxItemMaincategoryAttrValueDoc.class))
	public List<EcxItemMaincategoryAttrValueDoc> list;
}