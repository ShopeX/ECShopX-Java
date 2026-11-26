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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * E5001「分类ID必填」（null/空白）；
 * E5571「该标签分类不存在」（非整数/负数/company_id+category_id 查无/超 int 范围）；
 * E0000+status=fail「删除失败,该分类下已有标签」（PHP 旧契约零码失败，
 * OpenapiLegacyZeroCodeFailException 通道；先删后查触发事务回滚，分类保留）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberTagCategoryDeleteFailEnvelope")
public class EcxMemberTagCategoryDeleteFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 分类ID必填；E5571 该标签分类不存在；E0000（配合 status=fail）分类下已有标签；E4004 未授权",
			example = "E5571")
	public String code;

	@Schema(description = "错误说明", example = "该标签分类不存在")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}