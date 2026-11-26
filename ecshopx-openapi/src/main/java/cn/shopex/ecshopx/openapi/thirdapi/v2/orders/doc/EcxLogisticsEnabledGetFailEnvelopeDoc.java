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
 * 仅 OpenAPI 文档；HTTP 200 信封内失败：E4004「未授权」（直连 internal 无鉴权上下文），data=null。
 * 本接口无业务失败码：distributor_id/supplier_id 非法值宽松归 0，不抛业务异常。
 * 非 200 错误不走本信封（见 EcxLogisticsEnabledGetErrorBodyDoc）：
 * company_id 超 Integer.MAX → HTTP 400 {"code":400,"msg":"参数错误"}（GlobalExceptionHandler，
 * BadRequestException 无 Openapi 专属映射）；其他未处理异常（如 DB 故障）→ HTTP 500
 * {"code":500,"msg":"服务器内部错误"}。公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxLogisticsEnabledGetFailEnvelope")
public class EcxLogisticsEnabledGetFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：本接口 HTTP 200 失败仅 E4004 未授权", example = "E4004")
	public String code;

	@Schema(description = "错误说明", example = "未授权")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}