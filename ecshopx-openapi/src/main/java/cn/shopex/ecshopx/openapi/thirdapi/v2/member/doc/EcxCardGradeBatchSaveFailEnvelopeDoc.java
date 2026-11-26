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
 * <ul>
 * <li>E5001「请求参数错误」：grade_info 缺失/非 JSON 数组/空数组/无对象元素
 *     （{@code OpenapiMemberCardGradeV2BatchSaveParams} → OpenapiMemberV2FailException）；</li>
 * <li>E5530「{external_id}-{grade_name}[;…]，已有会员在使用，不能变更。」：待删除等级仍有会员
 *     （{@code OpenapiThirdApiV2MemberCardGradeBatchSaveService}）；</li>
 * <li>E0001：落库阶段 ResourceException——要删除的等级不存在或已变更 / 要更新的等级不存在 / 等级保存失败
 *     （{@code MemberCardGradeUpdateService}）。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxCardGradeBatchSaveFailEnvelope")
public class EcxCardGradeBatchSaveFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5530 等级在用不可变更；E0001 落库失败；E4004 未授权", example = "E5530")
	public String code;

	@Schema(description = "错误说明", example = "1-普通会员，已有会员在使用，不能变更。")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}