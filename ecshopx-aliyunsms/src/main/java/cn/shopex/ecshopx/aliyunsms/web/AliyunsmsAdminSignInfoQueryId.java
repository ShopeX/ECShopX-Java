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

package cn.shopex.ecshopx.aliyunsms.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Resolves sign info primary key from query parameter {@code id} only (last non-empty trimmed value).
 */
public final class AliyunsmsAdminSignInfoQueryId {

	private AliyunsmsAdminSignInfoQueryId() {}

	public static Optional<Long> resolveSignInfoPrimaryKey(HttpServletRequest request) {
		if (!request.getParameterMap().containsKey("id")) {
			return Optional.empty();
		}
		String raw = lastNonEmptyTrim(request.getParameterValues("id"));
		if (raw == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(raw));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static String lastNonEmptyTrim(String[] vals) {
		if (vals == null || vals.length == 0) {
			return null;
		}
		for (int i = vals.length - 1; i >= 0; i--) {
			if (vals[i] == null) {
				continue;
			}
			String t = vals[i].trim();
			if (!t.isEmpty()) {
				return t;
			}
		}
		return null;
	}
}
