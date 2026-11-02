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

package cn.shopex.ecshopx.members.service.admin;

import org.springframework.util.StringUtils;

/** Normalizes POS / admin member list queries by {@code user_card_code}. */
final class AdminMemberListUserCardCodeQuery {

	private static final String MC_PREFIX = "MC_";

	private AdminMemberListUserCardCodeQuery() {}

	static String normalizeForExactMatch(Object raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = String.valueOf(raw).trim();
		if (!StringUtils.hasText(trimmed)) {
			return null;
		}
		if (trimmed.length() >= MC_PREFIX.length()
				&& trimmed.regionMatches(true, 0, MC_PREFIX, 0, MC_PREFIX.length())) {
			String withoutPrefix = trimmed.substring(MC_PREFIX.length()).trim();
			return StringUtils.hasText(withoutPrefix) ? withoutPrefix : null;
		}
		return trimmed;
	}
}
