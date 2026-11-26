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
 * 仅 OpenAPI 文档；SKU 规格键值（spec_items[].item_spec 元素）。
 * {@code OpenapiThirdApiV2NormalItemsEntityDetailService#formatSpec}（6 键）。
 */
@Schema(name = "EcxItemEntityGetSpecKV")
public class EcxItemEntityGetSpecKVDoc {

	@Schema(description = "规格属性 ID（items_attributes.attribute_id）", example = "1")
	public Long spec_id;

	@Schema(description = "规格值 ID（items_attribute_values.attribute_value_id）", example = "1")
	public Long spec_value_id;

	@Schema(description = "规格名（属性名）", example = "500mg")
	public String spec_name;

	@Schema(description = "自定义规格值名（无自定义为 null）", nullable = true)
	public String spec_custom_value_name;

	@Schema(description = "规格值名（展示值）", example = "红")
	public String spec_value_name;

	@Schema(description = "规格图 URL（可为 null）", nullable = true, example = "https://example.test/key.jpg")
	public String spec_image_url;
}