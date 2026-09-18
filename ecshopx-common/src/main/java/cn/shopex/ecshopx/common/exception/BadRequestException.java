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
 * 请求参数异常。HTTP 400 Bad Request。
 */
public class BadRequestException extends RuntimeException {

	/** 若设置，模块级 Advice 可将该值写入 {@code data.status_code}，而非默认 HTTP 状态语义。 */
	private final Integer embeddedStatusCode;

	/** 可选：与兼容风格的字段校验失败 {@code errors} 对齐的字段级错误码列表。 */
	private final Map<String, List<String>> fieldErrors;

	public BadRequestException(String message) {
		super(message);
		this.embeddedStatusCode = null;
		this.fieldErrors = null;
	}

	public BadRequestException(String message, int embeddedStatusCode) {
		super(message);
		this.embeddedStatusCode = embeddedStatusCode;
		this.fieldErrors = null;
	}

	/**
	 * @param fieldErrors 字段名 → 错误码列表（如 {@code validation.required}），可为 {@code null}
	 */
	public BadRequestException(String message, Map<String, List<String>> fieldErrors) {
		super(message);
		this.embeddedStatusCode = null;
		this.fieldErrors = fieldErrors == null ? null : Map.copyOf(fieldErrors);
	}

	/**
	 * @param embeddedStatusCode 写入 Dingo 形态 {@code data.status_code}（如 400）
	 */
	public BadRequestException(String message, Map<String, List<String>> fieldErrors, int embeddedStatusCode) {
		super(message);
		this.embeddedStatusCode = embeddedStatusCode;
		this.fieldErrors = fieldErrors == null ? null : Map.copyOf(fieldErrors);
	}

	public Integer getEmbeddedStatusCode() {
		return embeddedStatusCode;
	}

	public Map<String, List<String>> getFieldErrors() {
		return fieldErrors;
	}
}
