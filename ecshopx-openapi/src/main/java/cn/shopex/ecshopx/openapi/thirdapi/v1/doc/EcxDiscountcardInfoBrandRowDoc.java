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
 * 仅 OpenAPI 文档；{@code data.brand_list} 单条，来源
 * {@code DiscountCardKaquanDetailFacadeService#itemsAttributeToRow}（仅 attribute_type=brand 的行；
 * attribute_id/company_id/shop_id 非 null 才写键）。
 */
@Schema(name = "EcxDiscountcardInfoBrandRow")
public class EcxDiscountcardInfoBrandRowDoc {

	@Schema(description = "品牌属性 ID", example = "2")
	public Long attribute_id;

	@Schema(description = "租户/公司 ID", example = "100")
	public Long company_id;

	@Schema(description = "属性类型，固定 brand", example = "brand")
	public String attribute_type;

	@Schema(description = "品牌名称", nullable = true)
	public String attribute_name;

	@Schema(description = "品牌备注", nullable = true)
	public String attribute_memo;

	@Schema(description = "排序值（字符串）", nullable = true)
	public String attribute_sort;

	@Schema(description = "所属门店 ID；0 表示平台", example = "0")
	public Long shop_id;
}