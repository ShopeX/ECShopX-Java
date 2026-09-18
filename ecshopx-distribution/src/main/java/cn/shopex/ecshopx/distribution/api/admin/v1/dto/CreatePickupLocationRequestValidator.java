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

package cn.shopex.ecshopx.distribution.api.admin.v1.dto;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Runs Bean Validation on {@link CreatePickupLocationRequest} and maps failures to
 * {@link BadRequestException} so {@code @DingoResponse(badRequest = DINGO_422)} applies (HTTP 200 +
 * {@code data.status_code}), avoiding {@code MethodArgumentNotValidException} which bypasses that style.
 */
public final class CreatePickupLocationRequestValidator {

	/**
	 * Field / property-path roots in form order: first failing constraint message matches legacy validator
	 * "first rule" behavior for multi-field bodies.
	 */
	private static final List<String> FIELD_ORDER =
			List.of(
					"name",
					"province",
					"city",
					"area",
					"address",
					"areaCode",
					"contractPhone",
					"hours",
					"workdays",
					"waitPickupDays",
					"latestPickupTime");

	private CreatePickupLocationRequestValidator() {}

	public static void validate(Validator validator, CreatePickupLocationRequest body) {
		Set<ConstraintViolation<CreatePickupLocationRequest>> violations = validator.validate(body);
		if (violations.isEmpty()) {
			return;
		}
		ConstraintViolation<CreatePickupLocationRequest> picked =
				violations.stream()
						.min(Comparator.comparingInt(CreatePickupLocationRequestValidator::rankPath))
						.orElseThrow();
		throw new BadRequestException(picked.getMessage(), 422);
	}

	private static int rankPath(ConstraintViolation<?> v) {
		String path = v.getPropertyPath() == null ? "" : v.getPropertyPath().toString();
		for (int i = 0; i < FIELD_ORDER.size(); i++) {
			String f = FIELD_ORDER.get(i);
			if (path.equals(f) || path.startsWith(f + "[") || path.startsWith(f + ".")) {
				return i;
			}
		}
		return Integer.MAX_VALUE;
	}
}
