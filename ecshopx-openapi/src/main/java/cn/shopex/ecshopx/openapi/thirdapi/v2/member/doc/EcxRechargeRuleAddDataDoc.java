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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；data 新建面额规则行（5 固定键），来源
 * {@code OpenapiThirdApiV2MemberRechargeRuleCreateService#formatOpenApiResponse}。
 * 该格式同时用于本域 rechargerule.update 接口。
 */
@Schema(name = "EcxRechargeRuleAddData")
public class EcxRechargeRuleAddDataDoc {

	@Schema(description = "面额规则 ID（deposit_recharge_rule.id，整型自增）", example = "1")
	public Integer rechargerule_id;

	@Schema(description = "需付金额（元，字符串 2 位小数；库内分字符串 HALF_UP 除 100）", example = "80.00")
	public String money;

	@Schema(description = "充值赠送类型（money/point）", example = "money")
	public String rule_type;

	@Schema(description = "赠送数值（整型；库内整数字符串 parseInt）", example = "0")
	public Integer rule_data;

	@Schema(
			description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；库内 Unix 秒字符串格式化；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String create_time;
}