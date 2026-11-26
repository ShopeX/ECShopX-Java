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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code DiscountCardController#getDiscountCardDetail} 经
 * {@code OpenapiRequestParams#mergeString} 读取的 key；校验见
 * {@code OpenapiThirdApiV1DiscountCardGetDiscountCardDetailService#validateCardIdForOpenapi}。
 */
@Schema(
		name = "EcxDiscountcardInfoBody",
		description =
				"卡券详情查询参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxDiscountcardInfoBodyDoc {

	@Schema(
			description = "卡券模板 ID（kaquan_discount_cards.card_id）；必填，整数字符串（允许负号格式，但负数查无记录）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "3")
	public String card_id;
}