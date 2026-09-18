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

package cn.shopex.ecshopx.orders.config;

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

@RestControllerAdvice(basePackages = "cn.shopex.ecshopx.orders.api.admin.v1")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrdersAdminV1ExceptionAdvice {

	@ExceptionHandler({BadRequestException.class, ResourceException.class})
	public ResponseEntity<Map<String, Object>> handleOrdersAdminBusinessErrors(RuntimeException ex) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		int statusCode = 422;
		if (ex instanceof BadRequestException bre && bre.getEmbeddedStatusCode() != null) {
			statusCode = bre.getEmbeddedStatusCode();
		} else if (ex instanceof ResourceException rex && rex.getEmbeddedStatusCode() != null) {
			statusCode = rex.getEmbeddedStatusCode();
		}
		data.put("status_code", statusCode);
		if (ex instanceof BadRequestException bre) {
			Map<String, List<String>> errors = bre.getFieldErrors();
			if (errors != null && !errors.isEmpty()) {
				data.put("errors", errors);
			}
		} else if (ex instanceof ResourceException rex) {
			Map<String, List<String>> errors = rex.getFieldErrors();
			if (errors != null && !errors.isEmpty()) {
				data.put("errors", errors);
			}
			Integer biz = rex.getEmbeddedBusinessCode();
			if (biz != null) {
				data.put("code", biz);
			}
		}
		return ResponseEntity.ok(Map.of("data", data));
	}
}
