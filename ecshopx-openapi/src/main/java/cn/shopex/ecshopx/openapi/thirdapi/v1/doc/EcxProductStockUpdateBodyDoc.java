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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ItemController#updateItemStore} 经
 * {@code OpenapiRequestParams#mergeRequiredString} 读取的 key；行内 key 来源
 * {@code OpenapiProductStockUpdatePortImpl#updateItemStore}。
 */
@Schema(
		name = "EcxProductStockUpdateBody",
		description =
				"库存同步参数（sku_list 双通道：body 含键时以 body 为准，null/空白不回退 query）。"
						+ "sku_list 须为 JSON 数组（字符串或 body 原生数组均可），非法报「数据有误」",
		example = "{\"sku_list\":[{\"sku_id\":\"A1\",\"stock\":10,\"shop_code\":\"S01\"}]}")
public class EcxProductStockUpdateBodyDoc {

	@ArraySchema(
			arraySchema =
					@Schema(
							description = "库存行数组；必填非空白（「sku_list必填」）；行缺 sku_id 或 stock 键时静默跳过",
							requiredMode = Schema.RequiredMode.REQUIRED))
	public List<SkuRowDoc> sku_list;

	@Schema(name = "EcxProductStockUpdateSkuRow", description = "单条库存行")
	public static class SkuRowDoc {

		@Schema(
				description = "货品编码（items.item_bn）；空白行跳过；查无商品记入 not_find_sku",
				requiredMode = Schema.RequiredMode.REQUIRED,
				example = "A1")
		public String sku_id;

		@Schema(
				description =
						"库存数（覆盖式）；键必须存在（缺键跳过）；总部分支宽松解析：非整数/负数按原值 intValue，"
								+ "非法字符串按 0（无 0-999999999 范围校验）",
				requiredMode = Schema.RequiredMode.REQUIRED,
				example = "10")
		public Integer stock;

		@Schema(
				description =
						"门店店铺号；PHP-empty（空/\"0\"/0）走总部库存分支；非空走门店分支——"
								+ "查无门店记「门店不存在」，门店存在时当前实现固定返回「请填写店铺ID」错误（v1 门店分支未完成）",
				example = "S01")
		public String shop_code;
	}
}