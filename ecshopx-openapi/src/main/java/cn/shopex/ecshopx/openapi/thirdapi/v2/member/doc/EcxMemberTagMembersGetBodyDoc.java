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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源 {@code MemberTagController#getTagMembers}：
 * page/page_size 经 {@code OpenapiThirdApiV2MemberTagsListParams#resolve}（originalString，body 含键取原始值、
 * 值 null 视为未传）；tag_id 经 {@code OpenapiRequestParams#mergeString}（body 含键优先，值经 trim/空白归 null）。
 */
@Schema(
		name = "EcxMemberTagMembersGetBody",
		description =
				"标签关联会员列表参数（body 与 query 同名 key 双通道：body 含键优先；"
						+ "page/page_size 取原始值，tag_id 经 mergeString trim）")
public class EcxMemberTagMembersGetBodyDoc {

	@Schema(description = "页码；PHP 整型解析（含小数取整、非法/空白按 0），缺省 1，小于 1 归一为 1", example = "1")
	public String page;

	@Schema(
			description =
					"每页条数；缺省 20；解析后 ≥500 直接报「每页的大小最大为500！」（E0001，500 本身也报错）；"
							+ "小于 20 一律归一为 20（PHP 旧契约）",
			example = "20")
	public String page_size;

	@Schema(
			description =
					"会员标签 ID；必填（trim 后须为合法整数），缺失/空白/非整数报 E5001「会员标签必填」；"
							+ "租户内查无该标签报 E5561「该标签不存在」",
			example = "1")
	public String tag_id;
}