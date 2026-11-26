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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。
 *
 * <p>业务失败：type 非 staff 或按 mobile+staff 查无记录时，控制器直接
 * {@code OpenapiEnvelope.fail("E0001", "操作失败", List.of())} —— 注意 data 为空数组 []（非 null）。
 * 鉴权失败：E4004「未授权」、data=null（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxJurisdictionGetuserFailEnvelope")
public class EcxJurisdictionGetuserFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 查无员工；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明", example = "操作失败")
	public String message;

	@ArraySchema(
			arraySchema = @Schema(description = "业务失败时恒为空数组 []；鉴权失败时为 null"),
			schema = @Schema(nullable = true))
	public List<Object> data;
}