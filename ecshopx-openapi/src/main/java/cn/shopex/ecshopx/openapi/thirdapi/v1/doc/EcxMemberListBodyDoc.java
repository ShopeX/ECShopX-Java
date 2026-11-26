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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberController#memberInfoList} 与
 * {@code OpenapiMemberInfoListParams#resolveAssociationsPagination} 读取的全部 key。
 */
@Schema(
		name = "EcxMemberListBody",
		description =
				"会员基础信息列表参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白不回退 query）。unionid 键必须提供（值可为空，空时 data=[]）")
public class EcxMemberListBodyDoc {

	@Schema(
			description =
					"微信 unionid，逗号分隔多个（wechat 关联批量定位）；键必填（缺失报「unionid必填」）；"
							+ "值须为字符串或数字（其他类型报「请填写unionid」）；值为 null/空时返回 data=[]",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "union-test")
	public String unionid;

	@Schema(
			description =
					"页码（作用于 unionid 关联分页），从 1 起；PHP-falsy（空/\"0\"）时整体回退 page=1、page_size=10",
			defaultValue = "1",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页条数（关联分页）；仅 page 有效且本键存在时生效；非整数/≤0 时关联不分页（取全部匹配关联）",
			example = "10")
	public String page_size;
}