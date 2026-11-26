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
 * 仅 OpenAPI 文档；固定失败信封（HTTP 仍为 200）。
 *
 * <p>{@code JurisdictionController#role} 为占位实现：无任何入参、无业务逻辑，
 * 恒返回 {@code OpenapiEnvelope.fail("E4007", "找不到方法", null)}（方法未实现，对齐 PHP 旧契约）。
 * 公网网关签名失败时先行返回 E4001/E4002/E4003/E4004（data 为回显请求参数 Map）。
 */
@Schema(name = "EcxJurisdictionRoleFailEnvelope")
public class EcxJurisdictionRoleFailEnvelopeDoc {

	@Schema(description = "业务状态（恒 fail）", example = "fail")
	public String status;

	@Schema(description = "业务码：恒 E4007 找不到方法；网关签名失败时为 E4001/E4002/E4003/E4004", example = "E4007")
	public String code;

	@Schema(description = "错误说明", example = "找不到方法")
	public String message;

	@Schema(description = "附加数据：本接口恒为 null；网关签名失败时为回显的请求参数 Map", nullable = true)
	public Object data;
}