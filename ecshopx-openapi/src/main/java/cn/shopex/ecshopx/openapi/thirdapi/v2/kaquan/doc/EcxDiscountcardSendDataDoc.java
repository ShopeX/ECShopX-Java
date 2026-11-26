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

package cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；成功信封 data（{@code OpenapiDiscountCardV2SendPresentationService#toOpenApiData}）：
 * 发放后按 code 回读 kaquan_user_discount（LIMIT 1，无租户条件），4 键 + 条件键 dm_card_code。
 */
@Schema(name = "EcxDiscountcardSendData")
public class EcxDiscountcardSendDataDoc {

	@Schema(
			description = "领取券码（12 位随机数字字符串，租户内唯一；格式由 allocNumericCode 保证，示例为格式演示）",
			example = "000000000000")
	public String code;

	@Schema(description = "有效期开始（Unix 秒，取券模板 begin_date 归一值）", example = "0")
	public Integer begin_date;

	@Schema(description = "有效期结束（Unix 秒，取券模板 end_date 归一值）", example = "0")
	public Integer end_date;

	@Schema(
			description =
					"券状态：新领取恒 unused（db 1/4/10→unused；end_date>0 且已过→expired；2→redeemed；6→expired；其他→空串）",
			allowableValues = {"unused", "redeemed", "expired", ""},
			example = "unused")
	public String status;

	@Schema(
			description =
					"达摩 CRM 券码（条件键：仅租户开启达摩 CRM、券 source_type=dmcrm 且来源非达摩事件、"
							+ "同步发券返回非空时出现；第三方返回值，无固定格式）",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	public String dm_card_code;
}