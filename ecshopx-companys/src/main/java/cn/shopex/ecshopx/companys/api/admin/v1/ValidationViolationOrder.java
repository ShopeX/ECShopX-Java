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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Stable ordering for Bean Validation {@link Set} results: picks {@code firstMsg} and {@code fieldErrors} by an
 * explicit field order (e.g. DTO field order), not by set iteration order.
 */
final class ValidationViolationOrder {

	private ValidationViolationOrder() {}

	static <T> BadRequestException buildOrderedConstraintViolationException(
			Set<ConstraintViolation<T>> violations, List<String> fieldOrder) {
		return buildOrderedConstraintViolationException(violations, fieldOrder, 422);
	}

	static <T> BadRequestException buildOrderedConstraintViolationException(
			Set<ConstraintViolation<T>> violations, List<String> fieldOrder, int embeddedStatusCode) {
		Map<String, List<String>> byPath = new HashMap<>();
		for (ConstraintViolation<T> v : violations) {
			String path = v.getPropertyPath().toString();
			byPath.computeIfAbsent(path, k -> new ArrayList<>()).add(v.getMessage());
		}
		String firstMsg = null;
		for (String field : fieldOrder) {
			List<String> msgs = byPath.get(field);
			if (msgs != null && !msgs.isEmpty()) {
				firstMsg = msgs.get(0);
				break;
			}
		}
		if (firstMsg == null) {
			firstMsg = violations.iterator().next().getMessage();
		}
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		for (String field : fieldOrder) {
			List<String> msgs = byPath.get(field);
			if (msgs != null && !msgs.isEmpty()) {
				fieldErrors.put(field, msgs);
			}
		}
		if (fieldErrors.size() < byPath.size()) {
			Map<String, List<String>> remainder = new TreeMap<>();
			for (Map.Entry<String, List<String>> e : byPath.entrySet()) {
				if (!fieldErrors.containsKey(e.getKey())) {
					remainder.put(e.getKey(), e.getValue());
				}
			}
			fieldErrors.putAll(remainder);
		}
		return new BadRequestException(firstMsg, fieldErrors, embeddedStatusCode);
	}
}
