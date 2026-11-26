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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，3 键与 query 同名双通道
 * （{@code OpenapiRequestParams#mergeString}：body 含键优先、值 null 视为未传、trim）。
 * 校验见 {@code OpenapiThirdApiV2MemberRechargeRuleCreateService} +
 * {@code RechargeRuleCreateService}（面额去重/上限）。
 */
@Schema(
		name = "EcxRechargeRuleAddBody",
		description =
				"新增储值面额规则参数（3 键均必填，body 含键优先否则 query 同名键）。"
						+ " 同租户面额（分）唯一，重复报 E5580；面额总数上限 14。")
public class EcxRechargeRuleAddBodyDoc {

	@Schema(
			description =
					"需付金额（元）；必填非空白（E5001「需付金额必填」）；须可 BigDecimal 解析且小数位 ≤2"
							+ "（去尾零后仍 ≤2），换算为分后须 1 ≤ 分值 ≤ 10,000,000（0.01~100000 元），"
							+ "否则 E5580「请填写正确的充值金额」；同面额已存在报 E5580「当前面额数已存在，不能重复添加」；"
							+ "租户面额 ≥14 报 E5580「最多添加14个面额」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "80")
	public String fixed_money;

	@Schema(
			description =
					"充值赠送类型；必填，仅 money（赠金额）/point（赠积分）"
							+ "（缺失/空白/其他值报 E5001「请填写正确的充值类型」；底层 gift 类型不可经本接口传入）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "money")
	public String rule_type;

	@Schema(
			description =
					"赠送数值；必填非空白（E5001「请填写正确的赠送金额」）；须非负整数字符串"
							+ "（含小数点/科学计数/负数/非数字均报 E5001「请填写正确的赠送金额」）；"
							+ "rule_type=money 为赠送金额、point 为赠送积分；数值原样整数落库（服务端不做单位换算），0 合法",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "0")
	public String rule_data;
}