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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200，application/json；成功时本接口返回 image/png 二进制）。
 *
 * <p>三类业务失败：
 * <ul>
 * <li>参数校验失败（{@code OpenapiWxappQrcodeParams}：path_type/scene/id 非法）→
 *     {@code ResourceException}「参数错误」→ E0001、data=null；</li>
 * <li>租户未绑定 yykweishop 授权小程序（{@code OpenapiWxappQrcodePortImpl}）→
 *     {@code OpenapiLegacyZeroCodeFailException}「参数错误」→ PHP 旧契约：status=fail 但 code=E0000、data=null；</li>
 * <li>微信生成失败（{@code WxaUnlimitedQrcodeClient}）→ E0001：41030「小程序页面尚未发布或未通过审核」、
 *     空响应「获取小程序码失败：响应为空」、JSON 错误体透传微信 errmsg（解析失败「获取小程序码失败」）、
 *     WxErrorException 透传 errorMsg（兜底「微信接口调用失败」）。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxWxappQrcodeFailEnvelope")
public class EcxWxappQrcodeFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description = "业务码：E0001 参数/微信生成失败；E0000 未绑定小程序（PHP 旧契约 fail）；E4004 未授权",
			example = "E0001")
	public String code;

	@Schema(description = "错误说明（参数错误 / 小程序页面尚未发布或未通过审核 / 微信 errmsg 等）", example = "参数错误")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}