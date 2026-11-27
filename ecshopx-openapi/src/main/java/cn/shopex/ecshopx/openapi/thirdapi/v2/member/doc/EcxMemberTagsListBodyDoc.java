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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，4 键与 query 同名双通道：
 * page/page_size/tag_name 走 {@code originalString}（不 trim），country_code 走 {@code mergeString}（trim）；
 * body 含键优先、值 null 视为未传。分页解析见
 * {@code OpenapiThirdApiV2MemberTagsListParams#resolve}（ResourceException → E0001），
 * 过滤与多语言见 {@code OpenapiThirdApiV2MemberTagListService}。
 */
@Schema(
		name = "EcxMemberTagsListBody",
		description =
				"会员标签列表参数（4 键，body 含键优先否则 query 同名键）。"
						+ " 按租户查 members_tags（含系统标签，不过滤 tag_kind），created 降序。")
public class EcxMemberTagsListBodyDoc {

	@Schema(
			description = "页码；默认 1。PHP 式解析后 max(…,1)；「当前页面最小值为1」分支不可达",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。原始解析值 ≥500 报 E0001「每页的大小最大为500！」（500 本身也报错；"
							+ "分页失败走 ResourceException 通道），再取 max(…,20)，实际生效范围 [20,499]",
			example = "20")
	public String page_size;

	@Schema(
			description = "标签名模糊过滤（LIKE %值%）；PHP isset 非空语义（null/空串/\"0\" 忽略；不 trim 原样进 LIKE）",
			example = "聚合测试类目")
	public String tag_name;

	@Schema(
			description =
					"多语言代码（如 zh-CN/en-US）；mergeString trim，空白默认 zh-CN；"
							+ "按语言分表覆盖 tag_name（description 覆盖后被库内原值还原——仅 tag_name 实际生效）",
			example = "zh-CN")
	public String country_code;
}