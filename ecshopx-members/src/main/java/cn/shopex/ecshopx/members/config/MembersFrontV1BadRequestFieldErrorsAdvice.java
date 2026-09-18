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

package cn.shopex.ecshopx.members.config;

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
 * Members front v1: {@link BadRequestException} mapped to Dingo-style JSON when the controller
 * declares {@code badRequest != NONE}, including optional {@code data.errors} for field-level
 * validation codes and {@code data.status_code} (honouring an embedded code when set).
 */
@RestControllerAdvice(basePackages = "cn.shopex.ecshopx.members.api.front.v1")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MembersFrontV1BadRequestFieldErrorsAdvice {

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
		DingoResponse ann = resolveDingoAnnotation(request);
		if (ann == null || ann.badRequest() == DingoResponse.BadRequestStyle.NONE) {
			return null;
		}
		boolean hasFieldErrors = ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty();
		Integer embedded = ex.getEmbeddedStatusCode();
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
		int plainStatusCode =
				switch (ann.badRequest()) {
					case DINGO_400 -> resolveCode(embedded, 400);
					case DINGO_422 -> resolveCode(embedded, 422);
					case DINGO_FIXED -> 422;
					case NONE -> throw new IllegalStateException();
				};
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		data.put("status_code", plainStatusCode);
		return ResponseEntity.ok(Map.of("data", data));
	}
}
