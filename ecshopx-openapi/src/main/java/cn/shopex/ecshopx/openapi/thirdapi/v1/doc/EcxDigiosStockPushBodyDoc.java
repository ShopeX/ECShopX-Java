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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code DigiOsStockController#stockPush} /
 * {@code resolveSkuList}：body 含 sku_list 键时取原始值（可为数组）；否则读 query 字符串（须为 JSON 数组文本）。
 * 行校验见 {@code DigiOsStockPushMapper#parseAndValidate}。
 */
@Schema(
		name = "EcxDigiosStockPushBody",
		description = "DigiOS 门店库存推送参数（推荐 JSON body 直接传数组；query 同名参数须为 JSON 数组字符串）",
		example = "{\"sku_list\":[{\"sku_id\":\"A1\",\"stock\":10,\"shop_code\":\"S01\"}]}")
public class EcxDigiosStockPushBodyDoc {

	@ArraySchema(
			arraySchema =
					@Schema(
							description =
									"库存行列表；必填、非空，单次最多 200 条（ecshopx.digios.stock-push-max-rows 可配）",
							requiredMode = Schema.RequiredMode.REQUIRED))
	public List<SkuRowDoc> sku_list;

	@Schema(name = "EcxDigiosStockPushSkuRow", description = "单条库存行")
	public static class SkuRowDoc {

		@Schema(
				description = "货品编码（items.item_bn）；必填非空白",
				requiredMode = Schema.RequiredMode.REQUIRED,
				example = "A1")
		public String sku_id;

		@Schema(
				description = "库存数；必填，0–999999999 的整数（数字或纯数字字符串，小数/负数/超界报错）",
				requiredMode = Schema.RequiredMode.REQUIRED,
				minimum = "0",
				maximum = "999999999",
				example = "10")
		public Integer stock;

		@Schema(
				description = "门店店铺号（distributor.shop_code）；必填，空白或 \"0\" 视为未填",
				requiredMode = Schema.RequiredMode.REQUIRED,
				example = "S01")
		public String shop_code;
	}
}