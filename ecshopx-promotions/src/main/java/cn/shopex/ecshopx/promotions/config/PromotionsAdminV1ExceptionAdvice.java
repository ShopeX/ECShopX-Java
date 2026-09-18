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

package cn.shopex.ecshopx.promotions.config;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "cn.shopex.ecshopx.promotions.api.admin.v1")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PromotionsAdminV1ExceptionAdvice {

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException ex) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		data.put("status_code", statusCode);
		Map<String, List<String>> errors = ex.getFieldErrors();
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	@ExceptionHandler(ResourceException.class)
	public ResponseEntity<Map<String, Object>> handleResourceException(ResourceException ex) {
		Map<String, List<String>> fe = ex.getFieldErrors();
		if (fe != null && !fe.isEmpty()) {
			int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
			return dingoValidationEnvelope(ex.getMessage(), fe, statusCode);
		}
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		if (ex.getEmbeddedBusinessCode() != null) {
			data.put("code", ex.getEmbeddedBusinessCode());
		}
		data.put("status_code", statusCode);
		return ResponseEntity.ok(Map.of("data", data));
	}

	private static ResponseEntity<Map<String, Object>> dingoValidationEnvelope(
			String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}
}
