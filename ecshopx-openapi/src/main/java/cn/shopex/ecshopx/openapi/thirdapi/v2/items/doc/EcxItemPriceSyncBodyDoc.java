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
 * {@code OpenapiThirdApiV2ItemPriceSyncParams#WHITELIST}（5 键，body 含键优先否则 query），
 * 校验与双分支见 {@code OpenapiThirdApiV2ItemPriceSyncService}。
 */
@Schema(
		name = "EcxItemPriceSyncBody",
		description =
				"商品价格同步参数（5 键白名单，body 含键优先否则 query 同名键）。"
						+ " 金额单位元（×100 截断取分，须 >0 分）。总部分支部分更新三价；"
						+ " standard + distributor_code 分支仅同步 price 到店铺商品行（覆盖/新建）。")
public class EcxItemPriceSyncBodyDoc {

	@Schema(
			description = "商品货号（items.item_bn）；必填非空白否则 E5001「商品货号参数错误」；查无 E5101「商品找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "SKU200")
	public String item_code;

	@Schema(
			description =
					"销售价（元）；键存在时须数值且 ×100 截断后 >0 分（否则 E5001「销售价参数错误」）；"
							+ "总部分支三价至少提供一个（否则 E5001「应至少指定一个要更新的价格」）；"
							+ "standard 店铺分支仅本价生效（缺失/非法直接成功无操作），不得小于进行中拼团活动价"
							+ "（E0000+fail PHP 旧契约），店铺行总部发货时报 E5410「商品总部发货无法更新库存和价格」",
			example = "0.10")
	public String price;

	@Schema(
			description = "市场价（元）；键存在时须数值且 >0 分（否则 E5001「市场价参数错误」）；仅总部分支生效",
			example = "99.00")
	public String market_price;

	@Schema(
			description = "成本价（元）；键存在时须数值且 >0 分（否则 E5001「成本价参数错误」）；仅总部分支生效",
			example = "50.00")
	public String cost_price;

	@Schema(
			description =
					"店铺号（shop_code）；可选，键存在时须标量（对象/数组报 E5001「店铺ID参数错误」）；"
							+ "仅 productModel=standard 且非空白走店铺分支（查无 E5401「店铺找不到」）；platform 模式走总部分支",
			example = "S001")
	public String distributor_code;
}