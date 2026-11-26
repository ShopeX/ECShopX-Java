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
 * 仅 OpenAPI 文档；成功信封 data。{@code OperatorController#resetPassword} 返回
 * {@code Map.of("status", Boolean.TRUE)}，经 {@code OpenapiResponseBodyAdvice} 包装为
 * {@code OpenapiEnvelope.success(...)}（message 固定「成功」）。
 */
@Schema(name = "ExcOperatorResetpwdData")
public class ExcOperatorResetpwdDataDoc {

	@Schema(description = "处理结果，成功恒为 true（布尔）", example = "true")
	public Boolean status;
}