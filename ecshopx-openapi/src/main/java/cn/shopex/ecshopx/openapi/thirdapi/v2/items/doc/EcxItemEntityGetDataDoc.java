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
 * 仅 OpenAPI 文档；成功信封 data（23 固定键 + 多规格 3 条件键）。
 * {@code OpenapiThirdApiV2NormalItemsEntityDetailService#formatItemSpuDetail}：
 * 默认 SKU 详情经 zh-CN 分表覆盖（item_name/brief/intro）后裁剪；
 * nospec=false（多规格）时追加 spec_items/item_spec_desc/spec_images，且 approve_status/store 为 SKU 聚合值。
 */
@Schema(name = "EcxItemEntityGetData")
public class EcxItemEntityGetDataDoc {

	@Schema(description = "SPU goods_id（整型）", example = "200")
	public Long goods_id;

	@Schema(description = "默认 SKU 货品编码", example = "SKU200")
	public String item_bn;

	@Schema(description = "商品名称（zh-CN 分表覆盖）", example = "Wire test goods")
	public String item_name;

	@Schema(description = "商品简介（zh-CN 分表覆盖）", example = "")
	public String brief;

	@Schema(description = "计量单位", example = "")
	public String item_unit;

	@Schema(description = "排序值（整型）", example = "1")
	public Integer sort;

	@Schema(description = "品牌 ID（整型，可为 null）", nullable = true, example = "3")
	public Integer brand_id;

	@Schema(description = "运费模板 ID（整型，可为 null）", nullable = true, example = "1")
	public Long templates_id;

	@Schema(description = "是否赠品（布尔）", example = "false")
	public Boolean is_gift;

	@ArraySchema(
			arraySchema = @Schema(description = "商品图片 URL 数组"),
			schema = @Schema(type = "string", example = "https://example.test/key.jpg"))
	public List<String> pics;

	@Schema(description = "是否单规格（布尔：true 单规格 / false 多规格）", example = "false")
	public Boolean nospec;

	@Schema(description = "是否展示规格图（布尔）", example = "false")
	public Boolean is_show_specimg;

	@Schema(description = "重量（十进制字符串）", example = "1")
	public String weight;

	@Schema(description = "体积（数值，可为 null）", nullable = true)
	public Double volume;

	@Schema(description = "售价（分，整型；多规格为默认 SKU 值）", example = "10")
	public Integer price;

	@Schema(description = "市场价（分，可为 null）", nullable = true)
	public Integer market_price;

	@Schema(description = "成本价（分，可为 null）", nullable = true)
	public Integer cost_price;

	@Schema(description = "条形码", example = "B200")
	public String barcode;

	@Schema(
			description = "上下架状态；多规格为 SKU 聚合（任一 onsale→onsale，其余按优先级聚合）",
			example = "onsale")
	public String approve_status;

	@Schema(description = "库存（整型；多规格为全部 SKU 库存合计）", example = "10")
	public Integer store;

	@Schema(
			description = "管理（主）类目 ID（字符串；主类目路径查无时为空串）",
			example = "77")
	public String item_main_cat_id;

	@ArraySchema(
			arraySchema = @Schema(description = "销售分类 ID 数组（items_rel_cats 关联，整型元素）"),
			schema = @Schema(type = "integer", format = "int64", example = "1"))
	public List<Long> item_category;

	@Schema(description = "更新时间（Unix 秒整型，键名 update_time 取自库 updated；可为 null）", nullable = true,
			example = "1700000001")
	public Integer update_time;

	@ArraySchema(
			arraySchema = @Schema(description = "【多规格】SKU 行数组（11 键/行）；单规格无此键"),
			schema = @Schema(implementation = EcxItemEntityGetSpecItemDoc.class))
	public List<EcxItemEntityGetSpecItemDoc> spec_items;

	@ArraySchema(
			arraySchema = @Schema(description = "【多规格】规格描述数组（4 键/行含 spec_values）；单规格无此键"),
			schema = @Schema(implementation = EcxItemEntityGetSpecDescDoc.class))
	public List<EcxItemEntityGetSpecDescDoc> item_spec_desc;

	@ArraySchema(
			arraySchema = @Schema(description = "【多规格】规格图数组（4 键/行）；单规格无此键"),
			schema = @Schema(implementation = EcxItemEntityGetSpecImageDoc.class))
	public List<EcxItemEntityGetSpecImageDoc> spec_images;
}