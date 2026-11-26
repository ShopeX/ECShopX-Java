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
 * 仅 OpenAPI 文档；DELETE 请求可选 JSON body，1 键与 query 同名双通道
 * （{@code OpenapiRequestParams#mergeString}：body 含键优先、值 null 视为未传、trim），
 * 校验与删除见 {@code OpenapiThirdApiV2MemberRechargeRuleDeleteService}。
 */
@Schema(
		name = "EcxRechargeRuleDeleteBody",
		description =
				"删除储值面额规则参数（1 键，body 含键优先否则 query 同名键）。"
						+ " 按 company_id+id 定位后删除；不校验规则是否被交易引用。")
public class EcxRechargeRuleDeleteBodyDoc {

	@Schema(
			description =
					"储值规则 ID；必填，trim 后须可 parseLong 且 ≥1"
							+ "（null/空白/非整数/小于 1 报 E5001「请填写正确的储值规则ID」）；"
							+ "按 company_id+id 查无报 E5581「未查询到该储值规则」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String rechargerule_id;
}