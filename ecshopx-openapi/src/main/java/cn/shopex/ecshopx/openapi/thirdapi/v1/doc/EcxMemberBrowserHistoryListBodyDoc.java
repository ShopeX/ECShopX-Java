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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberController#geMembertBrowseList} 与
 * {@code OpenapiThirdApiV1MemberBrowseHistoryListService#resolveMemberFilter} 读取的全部 key。
 */
@Schema(
		name = "EcxMemberBrowserHistoryListBody",
		description =
				"会员浏览足迹列表参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白不回退 query）。定位优先级：external_member_id PHP-truthy 时直接作为 user_id"
						+ "（跳过其余校验与会员存在性检查）；否则 mobile/unionid 二选一（mobile 优先）")
public class EcxMemberBrowserHistoryListBodyDoc {

	@Schema(
			description = "手机号；提供（键存在）时必须匹配 ^1[3-9]\\d{9}$；查无会员报「会员信息获取失败」；与 unionid 二选一",
			example = "13800000000")
	public String mobile;

	@Schema(
			description =
					"微信 unionid（wechat 关联）；必须为字符串类型（非字符串非 null 报「请填写unionid」）；"
							+ "查无关联报「会员信息获取失败」",
			example = "union-test")
	public String unionid;

	@Schema(
			description = "会员 ID（user_id）；PHP-truthy 时最高优先，直接按其查询足迹（不校验会员存在）",
			example = "5")
	public String external_member_id;

	@Schema(description = "页码，从 1 起；非整数/缺省按 1", defaultValue = "1", example = "1")
	public String page;

	@Schema(description = "每页条数；非整数/缺省按 10", defaultValue = "10", example = "10")
	public String page_size;
}