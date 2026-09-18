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

package cn.shopex.ecshopx.chinaumspay.api.admin.v1;

import cn.shopex.ecshopx.common.exception.ResourceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = DivisionController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DivisionControllerResourceExceptionAdvice {

	private static final Logger log = LoggerFactory.getLogger(DivisionControllerResourceExceptionAdvice.class);

	@ExceptionHandler(ResourceException.class)
	public ResponseEntity<Map<String, Object>> handleResourceException(ResourceException ex, HttpServletRequest request) {
		log.warn("{} {}", request.getRequestURI(), ex.getMessage());
		return ResponseEntity.ok(Map.of("data", Map.of("message", ex.getMessage(), "status_code", 422)));
	}
}
