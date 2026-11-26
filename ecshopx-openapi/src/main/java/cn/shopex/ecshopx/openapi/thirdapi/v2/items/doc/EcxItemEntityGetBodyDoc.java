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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源 {@code NormalItemsController#getItemSpuDetail}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key。
 */
@Schema(
		name = "EcxItemEntityGetBody",
		description =
				"平台商品详情参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）")
public class EcxItemEntityGetBodyDoc {

	@Schema(
			description =
					"商品货号（items.item_bn，取 is_default=true 默认 SKU）；必填非空白否则 E5001「货号必填」；"
							+ "查无/详情为空 E5101「商品找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "SKU200")
	public String item_bn;
}