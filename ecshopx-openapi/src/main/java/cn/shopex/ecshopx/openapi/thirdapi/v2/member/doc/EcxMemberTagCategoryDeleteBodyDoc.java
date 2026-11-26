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
 * 校验与删除见 {@code OpenapiThirdApiV2MemberTagCategoryDeleteService}。
 */
@Schema(
		name = "EcxMemberTagCategoryDeleteBody",
		description =
				"删除会员标签分类参数（1 键，body 含键优先否则 query 同名键）。"
						+ " 注意执行顺序：先 DELETE 分类行，再检查分类下标签——有标签时抛零码失败异常"
						+ " 触发 @Transactional 回滚（分类恢复），对外表现为删除被拒。")
public class EcxMemberTagCategoryDeleteBodyDoc {

	@Schema(
			description =
					"分类 ID；必填非空白（E5001「分类ID必填」）；trim 后须可 parseLong"
							+ "（非整数/查无/超 int 范围均 E5571「该标签分类不存在」）；"
							+ "分类下已有标签报 E0000+fail「删除失败,该分类下已有标签」（事务回滚，分类保留）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String category_id;
}