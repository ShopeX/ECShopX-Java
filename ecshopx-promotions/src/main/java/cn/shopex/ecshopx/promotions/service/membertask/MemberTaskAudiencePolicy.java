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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** C 端人群可见性（audience_mode / tag 组合）。 */
public final class MemberTaskAudiencePolicy {

	private MemberTaskAudiencePolicy() {}

	public static boolean visibleToUser(MemberTask task, Set<Long> userTagIds, ObjectMapper objectMapper) {
		if (task == null) {
			return false;
		}
		String mode = task.getAudienceMode();
		if (mode == null || mode.isBlank() || "unlimited".equals(mode)) {
			return true;
		}
		List<Long> configured = parseTagIds(task.getAudienceTagIds(), objectMapper);
		if (configured.isEmpty()) {
			return false;
		}
		boolean matches = tagMatch(configured, userTagIds, task.getAudienceTagCombine());
		if ("visible".equals(mode)) {
			return matches;
		}
		if ("hidden".equals(mode)) {
			return !matches;
		}
		return false;
	}

	private static boolean tagMatch(List<Long> configured, Set<Long> userTagIds, String combine) {
		if (userTagIds == null || userTagIds.isEmpty()) {
			return false;
		}
		if ("union".equals(combine)) {
			for (Long id : configured) {
				if (userTagIds.contains(id)) {
					return true;
				}
			}
			return false;
		}
		for (Long id : configured) {
			if (!userTagIds.contains(id)) {
				return false;
			}
		}
		return true;
	}

	private static List<Long> parseTagIds(String json, ObjectMapper objectMapper) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			List<Long> ids = objectMapper.readValue(json, new TypeReference<>() {});
			return ids == null ? List.of() : ids;
		} catch (Exception e) {
			return List.of();
		}
	}

	public static Set<Long> emptyTags() {
		return Collections.emptySet();
	}
}
