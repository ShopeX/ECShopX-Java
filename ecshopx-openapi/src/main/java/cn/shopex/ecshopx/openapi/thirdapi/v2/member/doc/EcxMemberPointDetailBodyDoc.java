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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，1 键与 query 同名双通道
 * （{@code OpenapiRequestParams#originalString}：body 含键优先、值 null 视为未传、不 trim；
 * 服务内再 trim 校验），见 {@code OpenapiThirdApiV2MemberPointDetailService}。
 */
@Schema(
		name = "EcxMemberPointDetailBody",
		description = "按手机号查询会员可用积分参数（1 键，body 含键优先否则 query 同名键）。")
public class EcxMemberPointDetailBodyDoc {

	@Schema(
			description =
					"会员手机号（明文）；必填，trim 后须匹配 ^1[3456789][0-9]{9}$"
							+ "（缺失/空白 E5001「手机号必填」，格式错 E5001「手机号填写错误」）；"
							+ "**查无会员不报错**（point=0）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String mobile;
}