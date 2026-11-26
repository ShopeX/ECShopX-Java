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
 * 仅 OpenAPI 文档；成功信封 data（8 固定键 + 条件键 goods_params/goods_spec）。
 * {@code OpenapiThirdApiV2ItemMainCategoryDetailService#formatMainCategoryDetail}：
 * 三级类目行 + goods_params（attribute_type=item_params）/goods_spec（其余类型，attribute_id 倒序）。
 * 路径复核失败/详情为空时 data 为空数组 []（PHP 旧契约，非对象）。
 */
@Schema(name = "EcxItemMaincategoryDetailGetData")
public class EcxItemMaincategoryDetailGetDataDoc {

	@Schema(description = "三级类目 ID（字符串形态）", example = "1")
	public String category_id;

	@Schema(description = "三级类目名称（null 回退空串）", example = "HP")
	public String category_name;

	@Schema(description = "排序值（字符串形态）", example = "1")
	public String sort;

	@Schema(description = "层级（字符串形态；三级为 3）", example = "3")
	public String category_level;

	@Schema(description = "路径（祖先 ID 逗号串含自身；null 回退空串）", example = "1")
	public String path;

	@Schema(description = "类目图片 URL（null 回退空串）", example = "/fixtures/logo.png")
	public String image_url;

	@Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss（库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String created;

	@Schema(description = "更新时间 yyyy-MM-dd HH:mm:ss（库值 null 时为 null）", nullable = true,
			example = "2023-11-15 06:13:21")
	public String updated;

	@ArraySchema(
			arraySchema = @Schema(description = "商品参数属性数组（条件键：goods_params 配置非空才出现）"),
			schema = @Schema(implementation = EcxItemMaincategoryAttrDoc.class))
	public List<EcxItemMaincategoryAttrDoc> goods_params;

	@ArraySchema(
			arraySchema = @Schema(description = "商品规格属性数组（条件键：goods_spec 配置非空才出现；attribute_id 倒序）"),
			schema = @Schema(implementation = EcxItemMaincategoryAttrDoc.class))
	public List<EcxItemMaincategoryAttrDoc> goods_spec;
}