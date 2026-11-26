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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，4 键与 query 同名双通道
 * （{@code OpenapiRequestParams#mergeString}：body 含键优先、值 null 视为未传、trim）。
 * 校验见 {@code OpenapiThirdApiV2MemberRechargeRuleUpdateService} +
 * {@code RechargeRuleUpdateService}（全量覆盖三字段，非 PATCH）。
 */
@Schema(
		name = "EcxRechargeRuleUpdateBody",
		description =
				"修改储值面额规则参数（4 键均必填，body 含键优先否则 query 同名键）。"
						+ " 全量覆盖 money/rule_type/rule_data 三字段（create_time 不变）；"
						+ " 面额（分）同租户唯一（排除自身），冲突报 E5580。")
public class EcxRechargeRuleUpdateBodyDoc {

	@Schema(
			description =
					"储值规则 ID；必填，trim 后须可 parseLong 且 ≥1"
							+ "（否则 E5001「请填写正确的储值规则ID」）；company_id+id 查无报 E5580「修改的充值规则不存在」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String rechargerule_id;

	@Schema(
			description =
					"需付金额（元）；必填非空白（E5001「需付金额必填」）；小数位 ≤2，"
							+ "换算分后 1≤分值≤10,000,000（0.01~100000 元），否则 E5580「请填写正确的充值金额」；"
							+ "与其他规则面额冲突报 E5580「当前面额数已存在，不能重复添加」（同值改自身放行）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "80")
	public String fixed_money;

	@Schema(
			description = "充值赠送类型；必填，仅 money/point（否则 E5001「请填写正确的充值类型」）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "money")
	public String rule_type;

	@Schema(
			description =
					"赠送数值；必填非空白（E5001「请填写正确的赠送金额」）；须非负整数字符串"
							+ "（小数点/科学计数/负数/非数字拒绝）；money 为赠送金额、point 为赠送积分；整数原样落库，0 合法",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "0")
	public String rule_data;
}