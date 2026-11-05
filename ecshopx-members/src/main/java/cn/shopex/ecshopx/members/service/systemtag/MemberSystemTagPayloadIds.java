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

package cn.shopex.ecshopx.members.service.systemtag;

import java.util.Map;

public final class MemberSystemTagPayloadIds {

	private MemberSystemTagPayloadIds() {}

	public static long asLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	public static long companyId(Map<String, Object> payload) {
		return payload == null ? 0L : asLong(payload.get("company_id"));
	}

	public static long userId(Map<String, Object> payload) {
		return payload == null ? 0L : asLong(payload.get("user_id"));
	}

	public static long orderId(Map<String, Object> payload) {
		return payload == null ? 0L : asLong(payload.get("order_id"));
	}

	public static String text(Map<String, Object> payload, String key) {
		if (payload == null || payload.get(key) == null) {
			return "";
		}
		return String.valueOf(payload.get(key)).trim();
	}
}
