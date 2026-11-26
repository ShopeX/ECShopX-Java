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
 * E5001「请求参数错误」（grade_id null/空串；discount 非空且越界/非数字；total_consumption 非空且负数/非数字）；
 * E5531「会员等级找不到」（原样字符串等值查无）；
 * E0001「等级数据序列化失败」（privileges/promotion_condition JSON 序列化异常，ResourceException 通道）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 * PATCH 键传 JSON null 时 presentOptionalString 走 Optional.of(null) 存在 NPE 风险 →
 * 非本信封（GlobalExceptionHandler HTTP 500 {"code":500,"msg":"服务器内部错误"}）。
 */
@Schema(name = "EcxMemberCardGradeUpdateFailEnvelope")
public class EcxMemberCardGradeUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 请求参数错误；E5531 会员等级找不到；E0001 序列化失败；E4004 未授权",
			example = "E5531")
	public String code;

	@Schema(description = "错误说明", example = "会员等级找不到")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}