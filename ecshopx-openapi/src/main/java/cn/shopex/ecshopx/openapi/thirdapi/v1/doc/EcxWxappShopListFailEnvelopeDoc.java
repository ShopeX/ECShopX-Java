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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。
 *
 * <p>业务失败：page/pageSize 校验失败（{@code OpenapiWxappShopListParams}）→
 * {@code OpenapiLegacyZeroCodeFailException}「参数不正确.」→ PHP 旧契约：status=fail 但 code=E0000、data=null
 * （{@code OpenapiExceptionHandler#handleLegacyZeroCodeFail}）。时间无法解析不报错（按 0）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxWxappShopListFailEnvelope")
public class EcxWxappShopListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0000 参数不正确（PHP 旧契约 fail）；E4004 未授权", example = "E0000")
	public String code;

	@Schema(description = "错误说明（本接口业务失败固定「参数不正确.」，含句点）", example = "参数不正确.")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}