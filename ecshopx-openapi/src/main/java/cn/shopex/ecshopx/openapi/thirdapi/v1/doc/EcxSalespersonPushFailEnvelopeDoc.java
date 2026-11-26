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
 * <p>文案（{@code OpenapiThirdApiV1SalespersonPushService}）：请填写导购名称 / 请填写正确的员工类型 /
 * 请填写正确的手机号 / 请填写企业微信userid / 请填写是否启用 / 店铺code未查询到正在开启的店铺信息 /
 * 更新的人员不存在（更新命中 0 行或回读为空）/ 同步导购数据失败（新建后回读为空）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxSalespersonPushFailEnvelope")
public class EcxSalespersonPushFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 参数/店铺/更新失败；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明（见类注释文案清单）", example = "请填写企业微信userid")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}