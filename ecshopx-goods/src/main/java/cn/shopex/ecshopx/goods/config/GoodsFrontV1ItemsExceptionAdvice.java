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

package cn.shopex.ecshopx.goods.config;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.api.front.v1.ItemsController;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {ItemsController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GoodsFrontV1ItemsExceptionAdvice {

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", ex.getMessage());
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 400;
		data.put("status_code", statusCode);
		var errors = ex.getFieldErrors();
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}
}
