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
 * （{@code OpenapiRequestParams#originalString}：body 含键优先、值 null 视为未传、不 trim），
 * 校验与删除见 {@code OpenapiThirdApiV2MemberCardVipGradeDeleteService}。
 */
@Schema(
		name = "EcxVipGradeDeleteBody",
		description =
				"删除付费会员卡等级参数（1 键，body 含键优先否则 query 同名键）。"
						+ " 默认等级不可删；仍有关联会员（vip_grade_rel_user，不过滤过期）不可删；"
						+ " 仅删 kaquan_vip_grade 行，不清理关联表。")
public class EcxVipGradeDeleteBodyDoc {

	@Schema(
			description =
					"付费等级 ID；必填非空（null/空串报 E5001「请求参数错误」）。trim 后 parseLong，"
							+ "失败再按整值 double 截断（如 \"2.0\"→2），仍失败按 -1 查询 → E5541「会员付费等级找不到」；"
							+ "默认等级报 E5543「会员付费等级删除错误」；关联会员数 >0 报 E5543（含错字「扔」文案）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "2")
	public String vip_grade_id;
}