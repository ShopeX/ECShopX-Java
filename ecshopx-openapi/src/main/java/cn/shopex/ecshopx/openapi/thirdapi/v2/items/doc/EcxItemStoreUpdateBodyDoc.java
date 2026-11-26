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
 * {@code OpenapiThirdApiV2ItemStoreUpdateParams#WHITELIST}（4 键，body 含键优先否则 query），
 * 校验与执行见 {@code OpenapiThirdApiV2ItemStoreUpdateService}。
 */
@Schema(
		name = "EcxItemStoreUpdateBody",
		description =
				"商品库存增量更新参数（4 键白名单，body 含键优先否则 query 同名键）。"
						+ " increase_store 与 decrease_store 恰一必填，对 items.store 做加/减增量；"
						+ " productModel=standard 且 distributor_code 非空白时直接无操作成功（不更新店铺库存）。")
public class EcxItemStoreUpdateBodyDoc {

	@Schema(
			description = "商品货号（items.item_bn）；必填非空白否则 E5001「商品货号参数错误」；查无 E5101「商品找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "SKU200")
	public String item_code;

	@Schema(
			description =
					"增加库存数；与 decrease_store 恰一必填（都缺 E5001「增加库存或减去库存二选一必填」，"
							+ "同时存在 E5001「增加库存或减去库存不能同时存在！」）。非负整数"
							+ "（字符串须 ^[0-9]+$ 且 ≤Integer.MAX；数值须整值 ≥0），否则 E5001「增加库存数参数错误」；"
							+ "为 0 时不写库直接成功（商品存在性仍先校验）",
			example = "3")
	public String increase_store;

	@Schema(
			description =
					"减去库存数；与 increase_store 恰一必填（同上互斥校验）。非负整数"
							+ "（字符串须 ^[0-9]+$ 且 ≤Integer.MAX；数值须整值 ≥0），否则 E5001「减去库存数参数错误」；"
							+ "现库存不足（store < decrease_store）时不扣减、静默成功；为 0 时不写库直接成功（商品存在性仍先校验）",
			example = "3")
	public String decrease_store;

	@Schema(
			description =
					"店铺号（shop_code）；可选，键存在时须标量（对象/数组报 E5001「店铺ID参数错误」）；"
							+ "productModel=standard 且非空白时本接口直接无操作成功（不校验商品/店铺存在、不更新库存）；"
							+ "platform 模式忽略，走总部增量分支",
			example = "S001")
	public String distributor_code;
}