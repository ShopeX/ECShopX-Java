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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。两类业务失败均 data=null：
 * <ul>
 * <li>分页解析（{@code OpenapiThirdApiV2AftersalesListParams}）→ {@code ResourceException} → E0001
 *     「每页的大小最大为500！」（解析后 ≥500，含 500）；「当前页面最小值为1」「每页显示数量1-500」
 *     因先经 max 归一化实际不可达；</li>
 * <li>过滤校验（{@code OpenapiThirdApiV2AftersalesListService}）→ {@code OpenapiAftersalesV2FailException}：
 *     E5320 请填写正确的手机号 / 开始时间不能大于结束时间；E5331 会员找不到。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxAftersalesListFailEnvelope")
public class EcxAftersalesListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 分页超限；E5320 手机号/时间校验；E5331 会员找不到；E4004 未授权", example = "E5320")
	public String code;

	@Schema(description = "错误说明", example = "请填写正确的手机号")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}