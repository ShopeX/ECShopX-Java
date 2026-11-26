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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberController#basicInfo} 与
 * {@code OpenapiThirdApiV1MemberBasicInfoService#executeOpenapiBasicInfo} 读取的全部 key。
 */
@Schema(
		name = "EcxMemberBasicInfoBody",
		description =
				"会员基础信息查询参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白不回退 query）。三选一必填（PHP-empty 语义：空/\"0\"/0/false 视为未提供）；"
						+ "mobile 或 external_member_id 有效时优先按其查询，否则按 unionid")
public class EcxMemberBasicInfoBodyDoc {

	@Schema(
			description =
					"手机号；提供（键存在）时必须匹配 ^1[3-9]\\d{9}$，否则报「请填写正确的手机号」；"
							+ "与 unionid/external_member_id 三选一",
			example = "13800000000")
	public String mobile;

	@Schema(
			description =
					"微信 unionid（members_associations，user_type=wechat）；必须为字符串类型"
							+ "（非字符串非 null 报「请填写unionid」）；查无关联时 data 为空数组 []",
			example = "union-test")
	public String unionid;

	@Schema(
			description =
					"会员 ID（user_id）；必须为字符串类型（非字符串非 null 报「会员id」）；"
							+ "与 mobile 同时提供时联合定位",
			example = "5")
	public String external_member_id;
}