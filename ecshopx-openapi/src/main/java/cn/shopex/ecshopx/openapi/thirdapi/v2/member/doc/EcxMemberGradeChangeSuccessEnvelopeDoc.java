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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；成功信封。Handler 返回 null，{@code OpenapiResponseBodyAdvice} 包装为
 * {@code OpenapiEnvelope.success(null)}（message 固定「成功」，data 恒 null）。
 * 同级/降级更新亦成功（不触发升级活动）。
 */
@Schema(name = "EcxMemberGradeChangeSuccessEnvelope")
public class EcxMemberGradeChangeSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Advice 包装固定「成功」）", example = "成功")
	public String message;

	@Schema(description = "业务数据，本接口成功恒为 null（无返回体）", nullable = true)
	public Object data;
}