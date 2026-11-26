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
 * 仅 OpenAPI 文档；spec_items 元素（SKU 行，11 键）。
 * {@code OpenapiThirdApiV2NormalItemsEntityDetailService#formatItemSpec}。
 */
@Schema(name = "EcxItemEntityGetSpecItem")
public class EcxItemEntityGetSpecItemDoc {

	@Schema(description = "SKU 货品编码", example = "SKU200")
	public String item_bn;

	@Schema(description = "是否默认 SKU（布尔）", example = "true")
	public Boolean is_default;

	@Schema(description = "上下架状态：onsale/instock/offline_sale/only_show（null 回退空串）", example = "onsale")
	public String approve_status;

	@Schema(description = "重量（十进制字符串，如 \"1\"）", example = "1")
	public String weight;

	@Schema(description = "体积（数值，可为 null）", nullable = true)
	public Double volume;

	@Schema(description = "售价（分，整型）", example = "10")
	public Integer price;

	@Schema(description = "市场价（分，可为 null）", nullable = true)
	public Integer market_price;

	@Schema(description = "成本价（分，可为 null）", nullable = true)
	public Integer cost_price;

	@Schema(description = "条形码", example = "B200")
	public String barcode;

	@ArraySchema(
			arraySchema = @Schema(description = "SKU 规格键值数组"),
			schema = @Schema(implementation = EcxItemEntityGetSpecKVDoc.class))
	public List<EcxItemEntityGetSpecKVDoc> item_spec;

	@Schema(description = "SKU 库存（整型）", example = "10")
	public Integer store;
}