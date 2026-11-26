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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberController#memberInfo} 与
 * {@code OpenapiThirdApiV1MemberMemberInfoService#executeOpenapiMemberInfo} 读取的全部 key。
 */
@Schema(
		name = "EcxMemberQueryBody",
		description =
				"会员身份查询参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白不回退 query）。mobile 与 unionid 二选一必填（PHP-empty 视为未提供）；"
						+ "mobile PHP-truthy 优先按手机号，否则按 unionid")
public class EcxMemberQueryBodyDoc {

	@Schema(
			description = "手机号；提供（键存在）时必须匹配 ^1[3-9]\\d{9}$；查无会员返回 is_member=N（非失败）；与 unionid 二选一",
			example = "13800000000")
	public String mobile;

	@Schema(
			description =
					"微信 unionid（wechat 关联）；必须为字符串类型（非字符串非 null 报「请填写unionid」）；"
							+ "查无关联返回 is_member=N",
			example = "union-test")
	public String unionid;
}