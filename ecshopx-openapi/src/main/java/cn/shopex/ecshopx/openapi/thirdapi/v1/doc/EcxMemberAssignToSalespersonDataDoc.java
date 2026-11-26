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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiThirdApiV1MemberAssignMemberToSalespersonService#successData}（回显入参 + 分配时间）。
 */
@Schema(
		name = "EcxMemberAssignToSalespersonData",
		description = "分配结果（回显）",
		example = "{\"employee_number\":\"E9\",\"unionid\":\"union-test\",\"assign_time\":\"2026-11-26 12:00:00\"}")
public class EcxMemberAssignToSalespersonDataDoc {

	@Schema(description = "导购员工编号（入参回显）", example = "E9")
	public String employee_number;

	@Schema(description = "会员微信 unionid（入参回显）", example = "union-test")
	public String unionid;

	@Schema(description = "分配时间，服务器本地时间 yyyy-MM-dd HH:mm:ss", example = "2026-11-26 12:00:00")
	public String assign_time;
}