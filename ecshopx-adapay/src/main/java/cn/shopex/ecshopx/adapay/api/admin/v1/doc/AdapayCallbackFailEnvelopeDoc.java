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
 * 仅 OpenAPI 文档；失败信封，对应运行时 {@code ApiResult.fail(code, msg)}
 * （类上 {@code @DingoResponse(resource=PLAIN, badRequest=NONE)}：400/422/500 均为标准
 * ApiResult，HTTP 状态与 code 一致；NON_NULL 序列化，data 省略）。
 */
@Schema(name = "AdapayCallbackFailEnvelope")
public class AdapayCallbackFailEnvelopeDoc {

	@Schema(description = "业务码：400 参数/验签失败；422 业务失败；500 未处理异常", example = "400")
	public Integer code;

	@Schema(description = "错误文案", example = "签名验证失败")
	public String msg;
}