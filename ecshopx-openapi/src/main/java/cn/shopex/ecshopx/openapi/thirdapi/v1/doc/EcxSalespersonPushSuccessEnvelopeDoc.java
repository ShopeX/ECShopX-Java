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
 * 仅 OpenAPI 文档；成功信封。{@code ShopSalespersonController#pushSalesperson} 返回
 * {@code new OpenapiEnvelope("success", "E0000", "操作成功", data)}，data 为回读的导购员行。
 */
@Schema(name = "EcxSalespersonPushSuccessEnvelope")
public class EcxSalespersonPushSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（本接口成功固定「操作成功」）", example = "操作成功")
	public String message;

	@Schema(description = "落库后回读的导购员完整行（23 键）")
	public EcxSalespersonPushDataDoc data;
}