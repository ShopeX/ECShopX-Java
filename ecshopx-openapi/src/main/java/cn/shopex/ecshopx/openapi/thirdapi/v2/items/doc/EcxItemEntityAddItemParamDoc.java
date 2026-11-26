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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；item_params 数组元素（商品参数关联）。
 * {@code PlatformItemsAddService#applyRelData}：逐行 INSERT item_rel_attributes
 *（attribute_type=item_params，挂默认 SKU）。
 */
@Schema(name = "EcxItemEntityAddItemParam")
public class EcxItemEntityAddItemParamDoc {

	@Schema(description = "参数属性 ID（items_attributes.attribute_id）", example = "1")
	public Long attribute_id;

	@Schema(description = "参数值 ID（可选；items_attribute_values.attribute_value_id）", example = "1")
	public Long attribute_value_id;

	@Schema(description = "自定义参数值文本（可选，写入 custom_attribute_value）")
	public String attribute_value_name;
}