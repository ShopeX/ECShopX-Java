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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class OpenapiWxappShopListParams {

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private OpenapiWxappShopListParams() {}

	public sealed interface ParseResult permits Ok, Fail {}

	public record Ok(int page, int pageSize, Long updatedGt, Long updatedLt) implements ParseResult {}

	public record Fail() implements ParseResult {}

	public static ParseResult parse(
			String pageParam,
			String pageSizeParam,
			String startTimeParam,
			String endTimeParam,
			Map<String, Object> body) {
		String pageRaw = OpenapiRequestParams.originalString(pageParam, body, "page");
		String pageSizeRaw = OpenapiRequestParams.originalString(pageSizeParam, body, "pageSize");
		String startTimeRaw = OpenapiRequestParams.originalString(startTimeParam, body, "start_time");
		String endTimeRaw = OpenapiRequestParams.originalString(endTimeParam, body, "end_time");

		Integer page = parseRequiredPhpInteger(pageRaw, 1, null);
		if (page == null) {
			return new Fail();
		}
		Integer pageSize = parseRequiredPhpInteger(pageSizeRaw, 1, 50);
		if (pageSize == null) {
			return new Fail();
		}

		Long updatedGt = null;
		if (OpenapiMemberQueryParams.isPhpTruthy(startTimeRaw)) {
			updatedGt = phpStrtotime(startTimeRaw);
		}
		Long updatedLt = null;
		if (OpenapiMemberQueryParams.isPhpTruthy(endTimeRaw)) {
			updatedLt = phpStrtotime(endTimeRaw);
		}

		return new Ok(page, pageSize, updatedGt, updatedLt);
	}

	private static Integer parseRequiredPhpInteger(String raw, int min, Integer max) {
		if (raw == null || raw.isEmpty()) {
			return null;
		}
		if (raw.contains(".")) {
			return null;
		}
		try {
			int v = Integer.parseInt(raw);
			if (v < min) {
				return null;
			}
			if (max != null && v > max) {
				return null;
			}
			return v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long phpStrtotime(String raw) {
		if (raw == null) {
			return 0L;
		}
		try {
			return LocalDateTime.parse(raw, DATE_TIME).atZone(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException e1) {
			try {
				return LocalDate.parse(raw, DATE_ONLY).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
			} catch (DateTimeParseException e2) {
				return 0L;
			}
		}
	}
}
