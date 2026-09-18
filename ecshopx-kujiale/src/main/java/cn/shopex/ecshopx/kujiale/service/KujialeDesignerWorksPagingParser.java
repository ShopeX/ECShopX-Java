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

package cn.shopex.ecshopx.kujiale.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Parses {@code page} / {@code pageSize} query strings without Spring MVC {@code Integer} binding, so values like
 * {@code abc} yield {@code validation.integer} error and HTTP 200 + {@code data.errors} (not MVC 400).
 * Keys follow legacy format: {@code validation.min.numeric}, {@code validation.max.numeric}.
 */
public final class KujialeDesignerWorksPagingParser {

	private KujialeDesignerWorksPagingParser() {}

	public record Result(int page, int pageSize, Map<String, List<String>> fieldErrors) {
		public boolean hasErrors() {
			return fieldErrors != null && !fieldErrors.isEmpty();
		}
	}

	public static Result parse(String pageRaw, String pageSizeRaw) {
		Map<String, List<String>> fe = new LinkedHashMap<>();

		IntOrErr pagePart = parseIntegerQuery(pageRaw);
		IntOrErr sizePart = parseIntegerQuery(pageSizeRaw);

		if (pagePart.parseError()) {
			fe.put("page", List.of("validation.integer"));
		}
		if (sizePart.parseError()) {
			fe.put("pageSize", List.of("validation.integer"));
		}
		if (!fe.isEmpty()) {
			return new Result(0, 0, fe);
		}

		int p = pagePart.supplied() ? pagePart.value() : 1;
		int ps = sizePart.supplied() ? sizePart.value() : 20;

		if (pagePart.supplied() && p < 1) {
			fe.put("page", List.of("validation.min.numeric"));
		}
		if (sizePart.supplied() && ps < 1) {
			fe.put("pageSize", List.of("validation.min.numeric"));
		} else if (sizePart.supplied() && ps > 100) {
			fe.put("pageSize", List.of("validation.max.numeric"));
		}

		return new Result(p, ps, fe);
	}

	private record IntOrErr(int value, boolean supplied, boolean parseError) {
		static IntOrErr absent() {
			return new IntOrErr(0, false, false);
		}
	}

	private static IntOrErr parseIntegerQuery(String raw) {
		if (raw == null) {
			return IntOrErr.absent();
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return IntOrErr.absent();
		}
		try {
			return new IntOrErr(Integer.parseInt(t), true, false);
		} catch (NumberFormatException e) {
			return new IntOrErr(0, true, true);
		}
	}
}
