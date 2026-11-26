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
 * <p>分页参数非整数：{@code OpenapiLegacyZeroCodeFailException} → {@code handleLegacyZeroCodeFail} →
 * code=E0000 但 status=fail（PHP 旧契约）、data=null。
 * 查询异常：{@code ResourceException} → E0001、data=null。
 * 鉴权失败：E4004「未授权」、data=null（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxCouponListFailEnvelope")
public class EcxCouponListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description =
					"业务码：E0000 且 status=fail 为分页参数非整数（PHP 旧契约）；E0001 查询异常；E4004 未授权",
			example = "E0000")
	public String code;

	@Schema(description = "错误说明", example = "A non-numeric value encountered")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}