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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code CouponController#updateUserCard} 经
 * {@code OpenapiRequestParams#mergeString}/{@code mergeRequiredString} 读取的全部 key。
 */
@Schema(
		name = "EcxCouponUpdateBody",
		description =
				"优惠券有效期更新参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxCouponUpdateBodyDoc {

	@Schema(description = "优惠券模板ID（第三方编码，对应 discount_cards.card_code）；必填", requiredMode = Schema.RequiredMode.REQUIRED)
	public String template;

	@Schema(description = "优惠券规则ID（第三方编码，对应 discount_cards.card_rule_code）；可选，缺省按空串匹配", defaultValue = "")
	public String rule;

	@Schema(description = "已发放优惠券编码（user_discount.code，配合券模板定位记录）；必填", requiredMode = Schema.RequiredMode.REQUIRED, example = "CODE-1")
	public String code;

	@Schema(
			description =
					"新的生效时间，Unix 秒级时间戳；必填（空白报「优惠券生效时间必填」）；"
							+ "非整数不报错，按 0 写入（DiscountCardParamNormalize.parseIntFlexible）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1764115200")
	public String start_time;

	@Schema(
			description =
					"新的失效时间，Unix 秒级时间戳；必填（空白报「优惠券失效时间必填」）；"
							+ "非整数不报错，按 0 写入；本接口不校验是否已过期",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1766707200")
	public String end_time;
}