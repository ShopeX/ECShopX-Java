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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。本接口有三种失败码：
 * <ul>
 *   <li>E0002：image_id 超过 100 个，控制器直接 {@code OpenapiEnvelope.fail("E0002","单次最多删除100个图片",null)}。</li>
 *   <li>E0001：image_id 空/\"0\"（{@code ResourceException}「请选择要删除的图片」→ handleResource）。</li>
 *   <li>E0000 且 status=fail：更新影响 0 行（{@code OpenapiLegacyZeroCodeFailException}「未查询到更新数据」→ handleLegacyZeroCodeFail，PHP 旧契约）。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxImageDelFailEnvelope")
public class EcxImageDelFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(
			description = "业务码：E0002 超过100个；E0001 未选图片；E0000 且 status=fail 未查询到更新数据；E4004 未授权",
			example = "E0002")
	public String code;

	@Schema(description = "错误说明", example = "单次最多删除100个图片")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}