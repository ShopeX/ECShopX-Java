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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code CouponController#userConsumeCard} 经
 * {@code OpenapiRequestParams#mergeRequiredString} 读取的全部 key。
 */
@Schema(
		name = "EcxCouponVerifyBody",
		description =
				"优惠券状态更新参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxCouponVerifyBodyDoc {

	@Schema(
			description = "已发放优惠券编码（user_discount.code，租户内定位）；必填",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "CODE-1")
	public String coupon_code;

	@Schema(
			description =
					"目标状态：\"1\" 核销（user_discount.status 置 2 已使用）；\"2\" 反核销/恢复（置 6）；"
							+ "其他值报「不支持的核销状态」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			allowableValues = {"1", "2"},
			example = "1")
	public String coupon_status;
}