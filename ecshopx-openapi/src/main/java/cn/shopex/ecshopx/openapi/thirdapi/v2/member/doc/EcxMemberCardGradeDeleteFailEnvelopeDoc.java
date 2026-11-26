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
 * E5001「请求参数错误」（grade_id null/空串）；
 * E5533「会员等级无法删除，该会员等级下扔存在关联的会员」（源码原文含错字「扔」，PHP 对齐保留）；
 * E0000+status=fail「$entity must be an object, NULL given.」（等级查无，PHP 旧契约零码失败，
 * OpenapiLegacyZeroCodeFailException 通道）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberCardGradeDeleteFailEnvelope")
public class EcxMemberCardGradeDeleteFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5533 存在关联会员；E0000（配合 status=fail）等级查无；E4004 未授权",
			example = "E5533")
	public String code;

	@Schema(description = "错误说明", example = "会员等级无法删除，该会员等级下扔存在关联的会员")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}