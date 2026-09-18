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

package cn.shopex.ecshopx.distribution.service.pickup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public final class DistributorPickupStoreHoursCompat {

	private final ObjectMapper objectMapper;

	public DistributorPickupStoreHoursCompat(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
	}

	public List<List<String>> formatDistributorHour(String hour) {
		if (hour == null || !StringUtils.hasText(hour.trim())) {
			return Collections.emptyList();
		}
		String t = hour.trim();
		List<List<String>> jsonPairs = new ArrayList<>();
		try {
			List<?> root = objectMapper.readValue(t, new TypeReference<List<?>>() {});
			if (root != null) {
				for (Object elem : root) {
					if (elem instanceof List<?> q && q.size() == 4) {
						jsonPairs.add(
								List.of(String.valueOf(q.get(2)).trim(), String.valueOf(q.get(3)).trim()));
					} else if (elem instanceof List<?> p && p.size() == 2) {
						jsonPairs.add(
								List.of(String.valueOf(p.get(0)).trim(), String.valueOf(p.get(1)).trim()));
					}
				}
			}
		} catch (JsonProcessingException e) {
			jsonPairs.clear();
		}
		if (!jsonPairs.isEmpty()) {
			return jsonPairs;
		}
		int i = t.indexOf('-');
		if (i < 0) {
			return Collections.emptyList();
		}
		String left = t.substring(0, i).trim();
		String right = t.substring(i + 1).trim();
		if (StringUtils.hasText(left) && StringUtils.hasText(right)) {
			return List.of(List.of(left, right));
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unused")
	public List<String> extractWorkdaysFromHours(List<List<String>> hours) {
		return List.of("1", "2", "3", "4", "5", "6", "7");
	}
}
