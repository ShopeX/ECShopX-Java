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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；list 行（9 键，SPU 维度）。
 * {@code OpenapiDistributorItemV2ListEnrichmentService#apply} 整行替换：
 * 店铺独立库存/价格（is_total_store=0 时取 distribution_distributor_items SPU 行覆盖）。
 */
@Schema(name = "EcxDistributorItemListItem")
public class EcxDistributorItemListItemDoc {

	@Schema(description = "店铺 ID（整型）", example = "1001")
	public Integer distributor_id;

	@Schema(description = "SPU 商品 ID（items.item_id，主行 is_default=1；整型）", example = "1001")
	public Integer item_id;

	@Schema(description = "商品货号（items.item_bn；null 回退空串）", example = "A1")
	public String item_code;

	@Schema(description = "商品名称（默认语言分表覆盖后；null 回退空串）", example = "示例商品")
	public String item_name;

	@Schema(
			description = "库存：is_total_store=0 且店铺 SPU 行有值时取店铺独立库存，否则总部库存",
			example = "10")
	public Integer store;

	@Schema(
			description = "价格（元，两位小数字符串，分÷100 HALF_UP）；is_total_store=0 时取店铺独立价格",
			example = "99.00")
	public String price;

	@Schema(
			description = "店铺可售：该 SPU 任一 SKU 店铺行 goods_can_sale=true 即 1，否则 0（无店铺行恒 0）",
			allowableValues = {"0", "1"},
			example = "1")
	public Integer goods_can_sale;

	@Schema(
			description = "是否使用总部库存：店铺 SPU 行 is_total_store=true→1；false→0；无店铺行默认 1",
			allowableValues = {"0", "1"},
			example = "1")
	public Integer is_total_store;

	@Schema(description = "上下架状态（items.approve_status：onsale/instock/offline_sale/only_show）", example = "onsale")
	public String status;
}