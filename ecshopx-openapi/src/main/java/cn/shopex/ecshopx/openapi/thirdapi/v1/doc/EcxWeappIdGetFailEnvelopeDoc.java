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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。本接口无业务失败分支（未绑定 appid 时仍成功、weappid=null）。
 *
 * <p>鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文，{@code OpenapiExceptionHandler}）；
 * 公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxWeappIdGetFailEnvelope")
public class EcxWeappIdGetFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E4004 未授权（本接口无业务校验失败分支）", example = "E4004")
	public String code;

	@Schema(description = "错误说明", example = "未授权")
	public String message;

	@Schema(description = "附加数据，失败恒为 null", nullable = true)
	public Object data;
}