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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷（4 键），来源
 * {@code OpenapiThirdApiV1MemberMemberInfoService}：mobile/unionid 分支或 defaultNotMemberReturn。
 * 各键类型随分支变化（PHP 旧契约）：非会员时 mobile/unionid/uid 均为空串；
 * mobile 分支无 unionid 时 unionid 为布尔 false；unionid 分支手机号取 region_mobile 优先（未解密）否则解密 mobile，均无则 null。
 */
@Schema(
		name = "EcxMemberQueryData",
		description = "会员身份查询结果（查无会员非失败，is_member=N）",
		example = "{\"is_member\":\"Y\",\"mobile\":\"13800000000\",\"unionid\":\"union-test\",\"uid\":5}")
public class EcxMemberQueryDataDoc {

	@Schema(description = "是否会员：Y 是；N 否", allowableValues = {"Y", "N"}, example = "Y")
	public String is_member;

	@Schema(
			description =
					"手机号：mobile 分支为解密明文（空/解密失败为空串）；unionid 分支 region_mobile 优先（原样）"
							+ "否则解密 mobile，均无则 null；非会员为空串",
			nullable = true,
			example = "13800000000")
	public String mobile;

	@Schema(
			description =
					"微信 unionid：mobile 分支查无关联时为布尔 false（PHP 旧契约）；unionid 分支回显；非会员为空串",
			example = "union-test")
	public Object unionid;

	@Schema(description = "会员 ID：命中为数值；非会员为空串（PHP 旧契约）", example = "5")
	public Object uid;
}