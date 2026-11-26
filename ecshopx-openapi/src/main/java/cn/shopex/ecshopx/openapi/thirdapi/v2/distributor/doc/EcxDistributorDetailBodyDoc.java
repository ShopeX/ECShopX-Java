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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源 {@code DistributorController#detail}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key。
 */
@Schema(
		name = "EcxDistributorDetailBody",
		description =
				"店铺详情参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）")
public class EcxDistributorDetailBodyDoc {

	@Schema(
			description =
					"店铺号（shop_code 租户内精确匹配 LIMIT 1，不过滤删除态）；必填非空否则 E5001「店铺号必填」；"
							+ "查无 E5401「店铺找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "S001")
	public String shop_code;
}