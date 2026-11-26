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
 * 仅 OpenAPI 文档；goods_params/goods_spec 元素（属性行，8 固定键 + 条件键 attribute_values）。
 * {@code OpenapiThirdApiV2ItemMainCategoryDetailService#formatAttribute}。
 */
@Schema(name = "EcxItemMaincategoryAttr")
public class EcxItemMaincategoryAttrDoc {

	@Schema(description = "属性 ID（字符串形态）", example = "1")
	public String attribute_id;

	@Schema(description = "属性名（null 回退空串）", example = "尺码")
	public String attribute_name;

	@Schema(description = "属性备注（null 回退空串）", example = "")
	public String attribute_memo;

	@Schema(description = "排序值（字符串形态）", example = "1")
	public String attribute_sort;

	@Schema(description = "是否展示（库原样字符串，如 \"true\"/\"false\"）", example = "true")
	public String is_show;

	@Schema(description = "是否图片属性（库原样字符串）", example = "false")
	public String is_image;

	@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss（库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String created;

	@Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss（库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String updated;

	@Schema(description = "属性值嵌套对象（total_count + list；条件键，值集合非 Map 形态时不出现）")
	public EcxItemMaincategoryAttrValuesDoc attribute_values;
}