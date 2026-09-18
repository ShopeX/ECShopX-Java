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

import cn.shopex.ecshopx.common.auth.H5JwtBlacklistPort;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.DingoResponse.BadRequestStyle;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// ──────────────────────────────────────────────────────────────────────────────
	// 注解解析
	// ──────────────────────────────────────────────────────────────────────────────

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

	// ──────────────────────────────────────────────────────────────────────────────
	// Response helpers
	// ──────────────────────────────────────────────────────────────────────────────

	private static ResponseEntity<?> dingoStyleError(String message, int statusCode) {
		return dingoStyleError(message, statusCode, null);
	}

	/** 兼容信封且 HTTP 状态与 {@code status_code} 一致（PHP {@code HttpException}）。 */
	private static ResponseEntity<?> dingoStyleErrorHttp(String message, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		return ResponseEntity.status(statusCode)
				.contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
				.body(Map.of("data", data));
	}

	private static ResponseEntity<?> dingoStyleError(String message, int statusCode, Integer businessCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		if (businessCode != null) {
			data.put("code", businessCode);
		}
		data.put("status_code", statusCode);
		return ResponseEntity.ok()
				.contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
				.body(Map.of("data", data));
	}

	private static ResponseEntity<?> dingoStyleUnauthorized401() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", "Unable to authenticate user.");
		data.put("code", 401001);
		data.put("status_code", 401);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
				.body(Map.of("data", data));
	}

	private static ResponseEntity<?> dingoStyleUnauthorized401NoBusinessCode(String message) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", 401);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
				.body(Map.of("data", data));
	}

	private static ResponseEntity<?> dingoStyleDisabled401() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", "该账号已被禁用.");
		data.put("code", 401002);
		data.put("status_code", 401);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
				.body(Map.of("data", data));
	}

	private static int resolveCode(Integer embedded, int fallback) {
		return embedded != null ? embedded : fallback;
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// 注解驱动的异常分派
	// ──────────────────────────────────────────────────────────────────────────────

	private static ResponseEntity<?> handleResourceByAnnotation(ResourceException ex, DingoResponse ann) {
		return switch (ann.resource()) {
			case DINGO ->
					dingoStyleError(ex.getMessage(),
							resolveCode(ex.getEmbeddedStatusCode(), 422),
							ex.getEmbeddedBusinessCode());
			case DINGO_FIXED ->
					dingoStyleError(ex.getMessage(), 422);
			case PLAIN ->
					ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
							.body(ApiResult.fail(422, ex.getMessage()));
		};
	}

	private static ResponseEntity<?> handleBadRequestByAnnotation(BadRequestException ex, DingoResponse ann) {
		return switch (ann.badRequest()) {
			case DINGO_400 ->
					dingoStyleError(ex.getMessage(), resolveCode(ex.getEmbeddedStatusCode(), 400));
			case DINGO_422 ->
					dingoStyleError(ex.getMessage(), resolveCode(ex.getEmbeddedStatusCode(), 422));
			case DINGO_FIXED ->
					dingoStyleError(ex.getMessage(), 422);
			case NONE ->
					ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail(400, ex.getMessage()));
		};
	}

	private static ResponseEntity<?> handleUnauthorizedByAnnotation(UnauthorizedException ex, DingoResponse ann) {
		if (ann.unauthorizedHttpOk()) {
			return dingoStyleError(ex.getMessage(), 403);
		}
		if ("该账号已被禁用.".equals(ex.getMessage())) {
			return dingoStyleDisabled401();
		}
		if (H5JwtBlacklistPort.TOKEN_BLACKLISTED_MESSAGE.equals(ex.getMessage())) {
			return dingoStyleUnauthorized401NoBusinessCode(ex.getMessage());
		}
		return dingoStyleUnauthorized401();
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// Exception handlers
	// ──────────────────────────────────────────────────────────────────────────────

	@ExceptionHandler(ResourceException.class)
	public ResponseEntity<?> handleResourceException(ResourceException ex, HttpServletRequest request) {
		log.warn("业务异常 [{}] {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

		DingoResponse ann = resolveDingoAnnotation(request);
		if (ann != null) {
			return handleResourceByAnnotation(ex, ann);
		}

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
			.body(ApiResult.fail(422, ex.getMessage()));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		log.warn("参数异常 [{}] {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

		DingoResponse ann = resolveDingoAnnotation(request);
		if (ann != null && ann.badRequest() != BadRequestStyle.NONE) {
			return handleBadRequestByAnnotation(ex, ann);
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResult.fail(400, ex.getMessage()));
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
		log.warn("认证失败 [{}] {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

		DingoResponse ann = resolveDingoAnnotation(request);
		if (ann != null && ann.unauthorized()) {
			return handleUnauthorizedByAnnotation(ex, ann);
		}

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(ApiResult.fail(401, ex.getMessage()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<?> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
		log.warn("权限拒绝 [{}] {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

		// 与 PHP AccessDeniedHttpException / Dingo 错误信封对齐：data.message + data.status_code
		DingoResponse ann = resolveDingoAnnotation(request);
		if (ann != null) {
			return dingoStyleError(ex.getMessage(), 403);
		}

		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(ApiResult.fail(403, ex.getMessage()));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<?> handleConflict(ConflictException ex, HttpServletRequest request) {
		log.warn("资源冲突 [{}] {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

		DingoResponse ann = resolveDingoAnnotation(request);
		if (ann != null) {
			return dingoStyleErrorHttp(ex.getMessage(), 409);
		}

		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ApiResult.fail(409, ex.getMessage()));
	}

	@ExceptionHandler(TooManyRequestsException.class)
	public ResponseEntity<?> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
		log.warn("请求过于频繁 [{}] {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

		DingoResponse ann = resolveDingoAnnotation(request);
		if (ann != null) {
			return dingoStyleErrorHttp(ex.getMessage(), 429);
		}

		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.body(ApiResult.fail(429, ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
		String msg = ex.getBindingResult().getFieldErrors().stream()
			.map(e -> e.getField() + ": " + e.getDefaultMessage())
			.collect(Collectors.joining("; "));
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
			.body(ApiResult.fail(422, msg));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResult<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResult.fail(400, "缺少参数: " + ex.getParameterName()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResult.fail(400, "参数类型错误: " + ex.getName()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResult<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResult.fail(400, "请求体格式错误"));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResult<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
			.body(ApiResult.fail(405, "不支持 " + ex.getMethod() + " 请求"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<?> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		DingoResponse ann = resolveDingoAnnotation(request);
		if (ann != null && ann.notFound()) {
			return dingoStyleError("404 Not Found", 404);
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiResult.fail(404, "资源不存在"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResult<Void>> handleException(Exception ex, HttpServletRequest request) {
		log.error("未处理异常 [{}] {}", request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiResult.fail(500, "服务器内部错误"));
	}
}
