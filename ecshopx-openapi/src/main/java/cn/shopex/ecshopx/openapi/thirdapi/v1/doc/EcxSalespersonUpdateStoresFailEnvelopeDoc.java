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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。业务失败均为 {@code ResourceException} →
 * {@code OpenapiExceptionHandler#handleResource} → E0001、data=null。
 *
 * <p>文案（{@code OpenapiThirdApiV1SalespersonUpdateStoresService}）：
 * 导购员信息查询错误（work_userid 查无匹配）/ 未查询到店铺信息（有效店铺查无，含 store_bn 缺失按空编号查询）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxSalespersonUpdateStoresFailEnvelope")
public class EcxSalespersonUpdateStoresFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 导购/店铺查询失败；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明：导购员信息查询错误 / 未查询到店铺信息", example = "未查询到店铺信息")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}