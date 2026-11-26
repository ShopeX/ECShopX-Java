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
 * 仅 OpenAPI 文档；成功信封 data（3 固定键 + standard 店铺分支 2 条件键）。
 * {@code OpenapiThirdApiV2ItemStoreGetService#executeGetStoreDetail}。
 */
@Schema(name = "EcxItemStoreGetData")
public class EcxItemStoreGetDataDoc {

	@Schema(description = "商品货号（items.item_bn 字符串化）", example = "SKU200")
	public String item_code;

	@Schema(description = "商品名称（字符串化）", example = "Wire test goods")
	public String item_name;

	@Schema(
			description = "库存（整型，null 按 0）；standard 店铺分支命中店铺商品行时被其 store 覆盖",
			example = "10")
	public Integer store;

	@Schema(description = "【standard 店铺分支】店铺号（null 回退空串）", example = "S001")
	public String distributor_code;

	@Schema(description = "【standard 店铺分支】店铺名称（null 回退空串）", example = "新门店名")
	public String distributor_name;
}