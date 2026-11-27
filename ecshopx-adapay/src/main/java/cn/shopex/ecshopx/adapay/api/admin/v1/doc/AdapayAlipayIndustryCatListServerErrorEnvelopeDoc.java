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
 * 仅 OpenAPI 文档；500 失败信封，对应运行时 {@code ApiResult.fail(500, "服务器内部错误")}
 * （{@code GlobalExceptionHandler.handleException}，NON_NULL 序列化，data 省略）。
 */
@Schema(name = "AdapayAlipayIndustryCatListServerErrorEnvelope")
public class AdapayAlipayIndustryCatListServerErrorEnvelopeDoc {

	@Schema(description = "业务码，固定 500", example = "500")
	public Integer code;

	@Schema(description = "错误文案，固定「服务器内部错误」", example = "服务器内部错误")
	public String msg;
}