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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源
 * {@code OpenapiThirdApiV2DistributorItemDownloadParams#resolve} 经
 * {@code OpenapiRequestParams#mergeString} 读取的全部 key。
 */
@Schema(
		name = "EcxDistributorItemDownloadBody",
		description =
				"店铺商品码参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无键读 query 并 trim）")
public class EcxDistributorItemDownloadBodyDoc {

	@Schema(
			description =
					"店铺号（distribution_distributor.shop_code，租户内精确匹配 LIMIT 1）；"
							+ "必填非空白否则 E5001「店铺号参数有误」；查无店铺 E5401「未查询到该店铺」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "S001")
	public String shop_code;

	@Schema(
			description =
					"商品货号（items.item_bn，租户内精确匹配 item_id 升序 LIMIT 1）；"
							+ "必填非空白否则 E5001「商品货号参数错误」；查无商品 E5101「商品找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "A1")
	public String item_code;
}