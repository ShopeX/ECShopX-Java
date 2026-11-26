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
 * 仅 OpenAPI 文档；PUT 请求可选 JSON body，字段来源
 * {@code OpenapiThirdApiV2ItemStoreSyncParams#WHITELIST}（3 键，body 含键优先否则 query），
 * 校验与双分支见 {@code OpenapiThirdApiV2ItemStoreSyncService}。
 */
@Schema(
		name = "EcxItemStoreSyncBody",
		description =
				"商品库存同步参数（3 键白名单，body 含键优先否则 query 同名键）。"
						+ " 总部分支覆盖 items.store（与现值相同跳过）；"
						+ " standard + distributor_code 分支覆盖/新建店铺商品行 store。两分支均同步 Redis 库存。")
public class EcxItemStoreSyncBodyDoc {

	@Schema(
			description = "商品货号（items.item_bn）；必填非空白否则 E5001「商品货号参数错误」；查无 E5101「商品找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "SKU200")
	public String item_code;

	@Schema(
			description =
					"同步库存数；必填，非负整数（字符串须 ^[0-9]+$ 且 ≤Integer.MAX；数值须整值 ≥0），"
							+ "否则 E5001「同步库存数参数错误」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "10")
	public String store;

	@Schema(
			description =
					"店铺号（shop_code）；可选，键存在时须标量（对象/数组报 E5001「店铺ID参数错误」）；"
							+ "仅 productModel=standard 且非空白走店铺分支（查无 E5401「店铺找不到」；"
							+ "店铺行/SPU 行总部发货报 E5410「商品总部发货无法更新库存和价格」）；platform 模式走总部分支",
			example = "S001")
	public String distributor_code;
}