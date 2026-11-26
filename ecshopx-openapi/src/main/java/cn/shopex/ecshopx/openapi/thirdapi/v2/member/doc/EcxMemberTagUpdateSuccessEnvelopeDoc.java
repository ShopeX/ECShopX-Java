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
 * 仅 OpenAPI 文档；成功信封。**Handler 显式构造
 * {@code new OpenapiEnvelope("success","E0000","操作成功",data)}**，
 * Advice 对已是信封的返回原样放行——message 为「操作成功」（非 Advice 默认的「成功」）。
 * data 为更新后回读 8 键行（同新增标签格式）。
 */
@Schema(name = "EcxMemberTagUpdateSuccessEnvelope")
public class EcxMemberTagUpdateSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Handler 显式信封固定「操作成功」）", example = "操作成功")
	public String message;

	@Schema(description = "更新后标签行（8 键）")
	public EcxMemberTagAddDataDoc data;
}