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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code JurisdictionController#sysuser}：{@code Map.of("status", true, "message", "保存成功")}。
 */
@Schema(name = "EcxJurisdictionSysuserData", description = "同步结果", example = "{\"status\":true,\"message\":\"保存成功\"}")
public class EcxJurisdictionSysuserDataDoc {

	@Schema(description = "成功恒为 true", example = "true")
	public Boolean status;

	@Schema(description = "成功提示（固定「保存成功」）", example = "保存成功")
	public String message;
}