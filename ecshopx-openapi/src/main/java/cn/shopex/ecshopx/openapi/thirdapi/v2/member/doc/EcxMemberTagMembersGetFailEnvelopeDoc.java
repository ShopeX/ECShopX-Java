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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），data=null：
 * <ul>
 * <li>分页解析（{@code OpenapiThirdApiV2MemberTagsListParams}）→ {@code ResourceException} → E0001
 *     「每页的大小最大为500！」（解析后 ≥500，含 500）；「当前页面最小值为1」「每页显示数量为1-500」
 *     因先经 max 归一化实际不可达；</li>
 * <li>标签校验（{@code OpenapiThirdApiV2MemberTagMembersGetService}）→
 *     {@code OpenapiMemberTagV2FailException}：E5001「会员标签必填」（缺失/空白/非整数）；
 *     E5561「该标签不存在」（租户内查无）。</li>
 * <li>鉴权失败：{@code UnauthorizedException} → E4004「未授权」（直连 internal 无鉴权上下文）；
 *     公网网关签名校验失败返回网关对应业务码。</li>
 * </ul>
 */
@Schema(name = "EcxMemberTagMembersGetFailEnvelope")
public class EcxMemberTagMembersGetFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 分页超限；E5001 会员标签必填；E5561 该标签不存在；E4004 未授权", example = "E5001")
	public String code;

	@Schema(description = "错误说明", example = "会员标签必填")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}