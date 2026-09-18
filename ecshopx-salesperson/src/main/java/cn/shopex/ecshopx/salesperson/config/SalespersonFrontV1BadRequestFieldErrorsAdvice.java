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

package cn.shopex.ecshopx.salesperson.config;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Dingo-style {@link BadRequestException} responses for front v1: when field errors are present,
 * adds {@code data.errors}; when only an embedded status code is set (no field errors), returns
 * {@code data.message} + {@code data.status_code} without an {@code errors} key. Otherwise defers
 * to later handlers.
 */
@RestControllerAdvice(basePackages = "cn.shopex.ecshopx.salesperson.api.front.v1")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SalespersonFrontV1BadRequestFieldErrorsAdvice {

	private static DingoResponse resolveDingoAnnotation(HttpServletRequest request) {
		Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
		if (handler instanceof HandlerMethod hm) {
			DingoResponse ann = hm.getMethodAnnotation(DingoResponse.class);
			if (ann == null) {
				ann = hm.getBeanType().getAnnotation(DingoResponse.class);
			}
			return ann;
		}
		return null;
	}

	private static int resolveCode(Integer embedded, int fallback) {
		return embedded != null ? embedded : fallback;
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		boolean hasFieldErrors =
				ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty();
		Integer embedded = ex.getEmbeddedStatusCode();
		if (!hasFieldErrors && embedded == null) {
			return null;
		}
		DingoResponse ann = resolveDingoAnnotation(request);
		if (ann == null || ann.badRequest() == DingoResponse.BadRequestStyle.NONE) {
			return null;
		}
		if (hasFieldErrors) {
			int statusCode =
					switch (ann.badRequest()) {
						case DINGO_400 -> resolveCode(embedded, 400);
						case DINGO_422 -> resolveCode(embedded, 422);
						case DINGO_FIXED -> 422;
						case NONE -> throw new IllegalStateException();
					};
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("message", ex.getMessage());
			data.put("errors", ex.getFieldErrors());
			data.put("status_code", statusCode);
			return ResponseEntity.ok(Map.of("data", data));
		}
		// Embedded status only (no field errors): no `errors` key in this path.
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		data.put("status_code", embedded);
		return ResponseEntity.ok(Map.of("data", data));
	}
}
