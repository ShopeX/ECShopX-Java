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
 * 仅 OpenAPI 文档；业务失败 Dingo 信封（HTTP 200），对应
 * {@code GlobalExceptionHandler.dingoStyleError}（类注解 resource=DINGO / badRequest=DINGO_422：
 * BadRequestException 与 ResourceException 均 HTTP 200 + data.status_code=422）。
 */
@Schema(name = "AdapayCorpMemberCreateBizFailEnvelope")
public class AdapayCorpMemberCreateBizFailEnvelopeDoc {

	@Schema(description = "错误详情")
	public AdapayCorpMemberCreateBizFailDataDoc data;

	@Schema(name = "AdapayCorpMemberCreateBizFailData")
	public static class AdapayCorpMemberCreateBizFailDataDoc {

		@Schema(description = "错误文案（校验/业务失败原因）", example = "企业名称必填")
		public String message;

		@Schema(description = "业务状态码（HTTP 仍为 200），本接口失败恒 422", example = "422")
		public Integer status_code;
	}
}