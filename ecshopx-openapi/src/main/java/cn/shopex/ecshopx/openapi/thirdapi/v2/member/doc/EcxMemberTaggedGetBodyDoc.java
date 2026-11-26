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
 * （{@code OpenapiRequestParams#mergeString}：body 含键优先、值 null 视为未传、trim），
 * 见 {@code OpenapiThirdApiV2MemberTaggedGetService}。
 */
@Schema(
		name = "EcxMemberTaggedGetBody",
		description =
				"查询会员已打标签参数（1 键，body 含键优先否则 query 同名键）。"
						+ " mobile trim 明文与 members.mobile（加密存储列）直接等值（无加密转换，"
						+ "加密启用部署下通常查无 E5501，同 tagged.delete，如实记录）。")
public class EcxMemberTaggedGetBodyDoc {

	@Schema(
			description =
					"会员手机号（明文）；必填非空白（E0001「会员手机号必填」——ResourceException 通道，"
							+ "区别于 tagged.delete 的 E5001）；trim 后直接等值 members.mobile；查无 E5501「会员找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String mobile;
}