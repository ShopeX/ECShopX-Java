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

package cn.shopex.ecshopx.adapay.api.admin.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；成功信封，对应运行时 {@code ApiResult.ok(Map.of("status", true))}。
 */
@Schema(name = "AdapayCorpMemberCreateSuccessEnvelope")
public class AdapayCorpMemberCreateSuccessEnvelopeDoc {

	@Schema(description = "业务码，成功固定 200", example = "200")
	public Integer code;

	@Schema(description = "提示文案，成功固定 success", example = "success")
	public String msg;

	@Schema(description = "创建结果")
	public AdapayCorpMemberCreateDataDoc data;

	@Schema(name = "AdapayCorpMemberCreateData")
	public static class AdapayCorpMemberCreateDataDoc {

		@Schema(description = "创建成功恒为 true（失败走异常信封）", example = "true")
		public Boolean status;
	}
}