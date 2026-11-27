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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * 仅 OpenAPI 文档；成功响应信封，对应运行时
 * {@code AdapayCallbackDingoArrayResponseBodyFactory.buildSuccessEnvelope}
 * （message=成功、status_code=200、data 为 Dingo 数组包装）。
 */
@Schema(name = "AdapayCallbackSuccessEnvelope")
public class AdapayCallbackSuccessEnvelopeDoc {

	@Schema(description = "提示文案，固定「成功」", example = "成功")
	public String message;

	@Schema(description = "业务状态码，固定 200（HTTP 也是 200）", example = "200")
	public Integer status_code;

	@ArraySchema(
			arraySchema =
					@Schema(
							description =
									"Dingo 数组包装：各事件处理器返回 [\"success\"]；"
											+ "type=payment.close.succeeded / payment.close.failed 时为 {}（空对象）"),
			schema = @Schema(oneOf = {String.class, Map.class}))
	public List<Object> data;
}