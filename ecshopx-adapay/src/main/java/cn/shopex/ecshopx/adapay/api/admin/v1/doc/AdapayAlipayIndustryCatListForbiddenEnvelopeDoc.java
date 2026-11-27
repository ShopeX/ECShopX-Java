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
 * 仅 OpenAPI 文档；403 失败信封，对应运行时 {@code ApiResult.fail(403, msg)}
 * （{@code AdminAuthInterceptor.writeForbidden} / {@code OperatorJwtAuthenticationFilter}，
 * NON_NULL 序列化，data 省略）。
 */
@Schema(name = "AdapayAlipayIndustryCatListForbiddenEnvelope")
public class AdapayAlipayIndustryCatListForbiddenEnvelopeDoc {

	@Schema(description = "业务码，固定 403", example = "403")
	public Integer code;

	@Schema(description = "错误文案（租户/账号校验失败原因；缺省「未登录」）", example = "未登录")
	public String msg;
}