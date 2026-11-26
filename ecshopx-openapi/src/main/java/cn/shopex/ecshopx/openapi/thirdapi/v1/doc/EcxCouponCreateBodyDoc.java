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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code CouponController#userGetCard} 经
 * {@code OpenapiRequestParams#mergeString}/{@code mergeRequiredString} 读取的全部 key。
 */
@Schema(
		name = "EcxCouponCreateBody",
		description =
				"优惠券发放参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxCouponCreateBodyDoc {

	@Schema(description = "优惠券模板ID（第三方编码，对应 discount_cards.card_code）；必填", requiredMode = Schema.RequiredMode.REQUIRED)
	public String template;

	@Schema(description = "优惠券规则ID（第三方编码，对应 discount_cards.card_rule_code）；可选，缺省按空串匹配", defaultValue = "")
	public String rule;

	@Schema(description = "优惠券编码（写入 user_discount.code，同券模板下唯一）；必填", requiredMode = Schema.RequiredMode.REQUIRED, example = "CODE-1")
	public String code;

	@Schema(
			description = "优惠券生效时间，Unix 秒级时间戳字符串；必填，非整数报「优惠券生效时间必填」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1764115200")
	public String start_time;

	@Schema(
			description =
					"优惠券失效时间，Unix 秒级时间戳字符串；必填，非整数报「优惠券失效时间必填」；"
							+ "大于 0 且早于等于当前时间报「领取优惠券失败，优惠券已过期」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1766707200")
	public String end_time;

	@Schema(description = "第三方 CRM 用户标识，按会员 third_data 定位会员；提供但查无会员报「用户不存在」；优先于 user_id")
	public String outer_crm_userid;

	@Schema(description = "会员手机号，按手机号定位会员；提供但查无会员报「用户不存在」；outer_crm_userid 未提供时生效；两者均缺省时券归属 user_id=0")
	public String user_id;
}