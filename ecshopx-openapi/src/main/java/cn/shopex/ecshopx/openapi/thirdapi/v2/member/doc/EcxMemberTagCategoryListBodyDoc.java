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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，3 键与 query 同名双通道
 * （{@code OpenapiRequestParams#originalString}：body 含键优先、值 null 视为未传、不 trim），
 * 分页解析复用 {@code OpenapiThirdApiV2ShippingTemplatesListParams#resolve}
 * （失败走 ResourceException → E0001），过滤见
 * {@code OpenapiThirdApiV2MemberTagCategoryListService}。
 */
@Schema(
		name = "EcxMemberTagCategoryListBody",
		description =
				"会员标签分类列表参数（3 键，body 含键优先否则 query 同名键）。"
						+ " 按租户查 tags_category；无显式排序（库默认行序）。")
public class EcxMemberTagCategoryListBodyDoc {

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
			description =
					"分类名模糊过滤（LIKE %值%）；PHP isset 非空语义（null/空串/\"0\" 忽略；不 trim，原样进 LIKE）",
			example = "聚合测试类目")
	public String category_name;
}