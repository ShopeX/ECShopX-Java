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
 * 校验与删除见 {@code OpenapiThirdApiV2MemberCardGradeDeleteService}。
 */
@Schema(
		name = "EcxMemberCardGradeDeleteBody",
		description =
				"删除会员卡等级参数（1 键，body 含键优先否则 query 同名键）。"
						+ " 该等级下仍有关联会员时拒绝删除；不校验是否默认等级；"
						+ " 仅删 membercard_grade 行，不清理多语言分表与关联卡券包。")
public class EcxMemberCardGradeDeleteBodyDoc {

	@Schema(
			description =
					"等级 ID；必填非空（null/空串报 E5001「请求参数错误」）。原样字符串（不 trim）等值匹配"
							+ " membercard_grade.grade_id，查无报 E0000+fail「$entity must be an object, NULL given.」"
							+ "（PHP 旧契约零码失败）；关联会员检查用 trim+parseLong（非法按 0），"
							+ "该等级下会员数 >0 报 E5533",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String grade_id;
}