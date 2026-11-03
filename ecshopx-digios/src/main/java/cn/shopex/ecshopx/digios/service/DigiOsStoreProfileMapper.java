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

package cn.shopex.ecshopx.digios.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/** ECX 门店字段 → DigiOS 门店档案字段映射。 */
public final class DigiOsStoreProfileMapper {

	public static final int STATUS_OPEN = 1;
	public static final int STATUS_CLOSED = 2;

	private static final Pattern OP_HOURS_COMMA = Pattern.compile("^\\d{1,2}:\\d{2},\\d{1,2}:\\d{2}$");
	private static final Pattern OP_HOURS_DASH = Pattern.compile("^(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})$");

	private DigiOsStoreProfileMapper() {}

	public static int mapStatus(Object isValid) {
		String normalized = String.valueOf(isValid == null ? "" : isValid).trim().toLowerCase();
		if ("true".equals(normalized) || "1".equals(normalized) || "closed".equals(normalized)) {
			return STATUS_OPEN;
		}
		return STATUS_CLOSED;
	}

	public static String formatOpHours(Object hour, String defaultOpHours, ObjectMapper objectMapper) {
		String defaultHours = StringUtils.hasText(defaultOpHours) ? defaultOpHours.trim() : "10:00,22:00";
		String raw = hour == null ? "" : String.valueOf(hour).trim();
		if (!StringUtils.hasText(raw)) {
			return defaultHours;
		}
		try {
			List<List<Object>> decoded = objectMapper.readValue(raw, new TypeReference<List<List<Object>>>() {});
			if (decoded != null && !decoded.isEmpty()) {
				List<Object> first = decoded.get(0);
				if (first != null && first.size() >= 4) {
					String start = String.valueOf(first.get(2)).trim();
					String end = String.valueOf(first.get(3)).trim();
					if (StringUtils.hasText(start) && StringUtils.hasText(end)) {
						return start + "," + end;
					}
				}
			}
		} catch (Exception ignored) {
			// not json array
		}
		if (OP_HOURS_COMMA.matcher(raw).matches()) {
			return raw;
		}
		var m = OP_HOURS_DASH.matcher(raw);
		if (m.matches()) {
			return m.group(1) + "," + m.group(2);
		}
		return defaultHours;
	}

	public static String composeAddress(Map<String, Object> distributor) {
		String address = trim(distributor.get("address"));
		String house = trim(distributor.get("house_number"));
		return (address + " " + house).trim();
	}

	private static String trim(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
