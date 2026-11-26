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
 * 校验与删除见 {@code OpenapiThirdApiV2MemberTagDeleteService}。
 */
@Schema(
		name = "EcxMemberTagDeleteBody",
		description =
				"删除会员标签参数（1 键，body 含键优先否则 query 同名键）。"
						+ " 系统标签禁删；删除标签行与全部语言分表行，不清理会员关联（members_rel_tags）。")
public class EcxMemberTagDeleteBodyDoc {

	@Schema(
			description =
					"标签 ID；必填非空白（E5001「标签ID必填」）；trim 后须可 parseLong"
							+ "（非整数/查无均报 E5561「该标签不存在」）；系统标签报 E0001「系统标签不可编辑」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String tag_id;
}