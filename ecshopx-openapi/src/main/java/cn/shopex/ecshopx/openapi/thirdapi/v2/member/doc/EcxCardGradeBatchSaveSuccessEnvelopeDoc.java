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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；成功信封（{@code OpenapiResponseBodyAdvice} 自动包装，message 固定「成功」）。
 * data 为保存后租户全量等级的数云 5 字段数组；回读为空时 Handler 返回 null → data=null。
 */
@Schema(name = "EcxCardGradeBatchSaveSuccessEnvelope")
public class EcxCardGradeBatchSaveSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Advice 包装固定「成功」）", example = "成功")
	public String message;

	@ArraySchema(
			arraySchema = @Schema(description = "租户全量等级（数云 5 字段/行）；回读为空时为 null", nullable = true),
			schema = @Schema(implementation = EcxCardGradeBatchSaveRowDoc.class))
	public List<EcxCardGradeBatchSaveRowDoc> data;
}