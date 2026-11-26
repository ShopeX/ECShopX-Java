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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberController#assignMemberToSalesperson}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key。
 */
@Schema(
		name = "EcxMemberAssignToSalespersonBody",
		description =
				"分配客户回调参数（body 与 query 同名 key 双通道：body 含键时取原始值，"
						+ "null 视为未提供且不回退 query；body 无该键时读 query 原样，不 trim）")
public class EcxMemberAssignToSalespersonBodyDoc {

	@Schema(
			description = "导购员工编号（写入 members.fp_salesperson）；必填非空（空→E4003 请填写导购员工编号）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "E9")
	public String employee_number;

	@Schema(
			description =
					"会员微信 unionid（members_associations，user_type=wechat，租户内定位）；"
							+ "必填非空（空→E4003 请填写会员unionid；查无→E4002 会员不存在）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "union-test")
	public String unionid;
}