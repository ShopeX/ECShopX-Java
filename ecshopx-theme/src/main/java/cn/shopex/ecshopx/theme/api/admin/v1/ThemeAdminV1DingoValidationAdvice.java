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

package cn.shopex.ecshopx.theme.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

@RestControllerAdvice(basePackages = "cn.shopex.ecshopx.theme.api.admin.v1")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ThemeAdminV1DingoValidationAdvice {

	private final MessageSource messageSource;

	public ThemeAdminV1DingoValidationAdvice(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HandlerMethod handlerMethod) {
		if (handlerMethod == null) {
			return null;
		}
		DingoResponse dingo =
				handlerMethod.getMethod().getDeclaringClass().getAnnotation(DingoResponse.class);
		if (dingo == null) {
			return null;
		}
		DingoResponse.BadRequestStyle badRequest = dingo.badRequest();
		if (badRequest == DingoResponse.BadRequestStyle.NONE) {
			return null;
		}
		int statusCode;
		if (badRequest == DingoResponse.BadRequestStyle.DINGO_400) {
			statusCode = 400;
		} else if (badRequest == DingoResponse.BadRequestStyle.DINGO_422
				|| badRequest == DingoResponse.BadRequestStyle.DINGO_FIXED) {
			statusCode = 422;
		} else {
			return null;
		}
		StringBuilder message = new StringBuilder();
		for (ObjectError oe : ex.getBindingResult().getAllErrors()) {
			if (message.length() > 0) {
				message.append("; ");
			}
			message.append(resolveDisplayMessage(oe.getDefaultMessage()));
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message.toString());
		data.put("status_code", statusCode);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.status(200).body(body);
	}

	private String resolveDisplayMessage(String defaultMessage) {
		if (defaultMessage == null) {
			return "";
		}
		String trimmed = defaultMessage.trim();
		if (trimmed.length() >= 2 && trimmed.startsWith("{") && trimmed.endsWith("}")) {
			String code = trimmed.substring(1, trimmed.length() - 1);
			return messageSource.getMessage(
					code, null, defaultMessage, LocaleContextHolder.getLocale());
		}
		return defaultMessage;
	}
}
