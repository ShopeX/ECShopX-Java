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
 * 仅 OpenAPI 文档；spec_images 数组元素（规格图）。
 * {@code PlatformItemsAddService#parseSpecImages}：两键均非 null 才生效，缺键/解析失败静默忽略。
 */
@Schema(name = "EcxItemEntityAddSpecImage")
public class EcxItemEntityAddSpecImageDoc {

	@Schema(description = "规格值 ID（items_attribute_values.attribute_value_id）", example = "1")
	public Long spec_value_id;

	@Schema(description = "规格图片 URL", example = "https://example.test/key.jpg")
	public String item_image_url;
}