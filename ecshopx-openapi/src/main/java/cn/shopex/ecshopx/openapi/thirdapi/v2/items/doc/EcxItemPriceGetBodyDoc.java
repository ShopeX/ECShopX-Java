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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源
 * {@code OpenapiThirdApiV2ItemPriceGetParams#WHITELIST}（item_code/distributor_code，
 * body 含键优先否则 query；值为空串时忽略该键）。
 */
@Schema(
		name = "EcxItemPriceGetBody",
		description =
				"商品价格查询参数（2 键白名单，body 含键优先否则 query 同名键；空串值忽略）。"
						+ " item_code 缺省时取租户任意一条商品（LIMIT 1 无排序，PHP 旧契约）。")
public class EcxItemPriceGetBodyDoc {

	@Schema(
			description =
					"商品货号（items.item_bn，item_id 升序 LIMIT 1）；可选，缺省取租户第一条商品；查无 E5101「商品找不到」",
			example = "SKU200")
	public String item_code;

	@Schema(
			description =
					"店铺号（shop_code）；可选，仅租户 productModel=standard 时生效："
							+ "查无店铺 E5401「店铺找不到」；命中后追加 distributor_code/distributor_name，"
							+ "且存在 distributor_id IS NULL 的店铺商品行时用其 price 覆盖返回值；"
							+ "platform 模式忽略本参数",
			example = "S001")
	public String distributor_code;
}