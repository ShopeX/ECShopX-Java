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
 * 仅 OpenAPI 文档；spec_items 数组元素（多规格 SKU 行）。
 * 行内键经 {@code PlatformItemsAddService#mergeSku} 覆盖 SPU 公共字段后走同一落库映射
 *（{@code mapToItems}），故可含任意 SKU 级字段；下列为测试与落库映射证实的常用键。
 */
@Schema(
		name = "EcxItemEntityAddSpecItem",
		description =
				"多规格 SKU 行（nospec=false/0 时必填非空数组，否则 E5100「请填写正确的规格数据」）。"
						+ " 行内键覆盖 SPU 公共字段（audit_status/audit_reason/audit_date 除外）；"
						+ " 未提供 default_item_id 时取最低价 SKU 为默认。")
public class EcxItemEntityAddSpecItemDoc {

	@Schema(description = "SKU 货品编码", example = "SKU200")
	public String item_bn;

	@Schema(description = "SKU 售价（分，整数解析）", example = "10")
	public String price;

	@Schema(description = "SKU 库存（整数解析）", example = "10")
	public Integer store;

	@Schema(description = "上下架状态：onsale/instock/offline_sale/only_show", example = "onsale")
	public String approve_status;

	@Schema(description = "商品编码（goods_bn）", example = "G001")
	public String goods_bn;

	@Schema(description = "条形码", example = "B200")
	public String barcode;

	@Schema(description = "重量（数值解析，非法按 0）", example = "1")
	public String weight;

	@Schema(description = "体积（数值解析，非法按 0；键语义同 SPU volume）")
	public String volume;

	@Schema(description = "成本价（分；键语义同 SPU cost_price）")
	public String cost_price;

	@Schema(description = "市场价（分；键语义同 SPU market_price）")
	public String market_price;
}