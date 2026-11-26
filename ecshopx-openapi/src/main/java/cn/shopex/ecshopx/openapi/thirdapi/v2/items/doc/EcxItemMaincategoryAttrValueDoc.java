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
 * 仅 OpenAPI 文档；属性值行（attribute_values.list 元素，7 键）。
 * {@code OpenapiThirdApiV2ItemMainCategoryDetailService#formatAttributeValue}。
 */
@Schema(name = "EcxItemMaincategoryAttrValue")
public class EcxItemMaincategoryAttrValueDoc {

	@Schema(description = "属性值 ID（字符串形态）", example = "100")
	public String attribute_value_id;

	@Schema(description = "所属属性 ID（字符串形态）", example = "1")
	public String attribute_id;

	@Schema(description = "属性值文本（null 回退空串）", example = "XXL")
	public String attribute_value;

	@Schema(description = "排序值（字符串形态）", example = "0")
	public String sort;

	@Schema(description = "属性值图片 URL（库原样值，可为 null）", nullable = true,
			example = "https://example.test/key.jpg")
	public String image_url;

	@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss（库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String created;

	@Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss（库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String updated;
}