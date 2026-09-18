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

package cn.shopex.ecshopx.common.exception;

import java.util.List;
import java.util.Map;

/**
 * 通用业务异常。默认 HTTP 422 Unprocessable Entity。
 * <p>
 * 可选携带 {@link #getEmbeddedStatusCode()}，供兼容风格异常响应在 {@code data.status_code} 中写入非 HTTP 语义码。
 * <p>
 * 可选携带 {@link #getEmbeddedBusinessCode()}，供部分路由在 {@code data.code} 中写入业务错误码。
 * <p>
 * 可选携带 {@link #getFieldErrors()}，与兼容风格的字段校验失败 {@code errors} 对齐；由抛出方所在 Controller 的
 * {@code @ExceptionHandler} 与类级 {@code @DingoResponse} 消费（{@code GlobalExceptionHandler} 不展开该字段）。
 */
public class ResourceException extends RuntimeException {

	private final Integer embeddedStatusCode;

	private final Integer embeddedBusinessCode;

	private final Map<String, List<String>> fieldErrors;

	public ResourceException(String message) {
		super(message);
		this.embeddedStatusCode = null;
		this.embeddedBusinessCode = null;
		this.fieldErrors = null;
	}

	public ResourceException(String message, int embeddedStatusCode) {
		super(message);
		this.embeddedStatusCode = embeddedStatusCode;
		this.embeddedBusinessCode = null;
		this.fieldErrors = null;
	}

	public ResourceException(String message, int embeddedStatusCode, int embeddedBusinessCode) {
		super(message);
		this.embeddedStatusCode = embeddedStatusCode;
		this.embeddedBusinessCode = embeddedBusinessCode;
		this.fieldErrors = null;
	}

	/**
	 * @param fieldErrors 字段名 → 错误码列表（如 {@code validation.required}），可为 {@code null}
	 */
	public ResourceException(String message, Map<String, List<String>> fieldErrors) {
		super(message);
		this.embeddedStatusCode = null;
		this.embeddedBusinessCode = null;
		this.fieldErrors = fieldErrors == null ? null : Map.copyOf(fieldErrors);
	}

	public Integer getEmbeddedStatusCode() {
		return embeddedStatusCode;
	}

	public Integer getEmbeddedBusinessCode() {
		return embeddedBusinessCode;
	}

	public Map<String, List<String>> getFieldErrors() {
		return fieldErrors;
	}
}
