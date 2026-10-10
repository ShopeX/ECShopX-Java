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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.springframework.util.StringUtils;

public record MemberTaskFavoriteRule(Set<Long> itemIds, Set<Long> distributorIds) {

	public boolean matches(long itemId, long distributorId) {
		boolean itemOk = itemIds.isEmpty() || (itemId > 0L && itemIds.contains(itemId));
		boolean shopOk =
				distributorIds.isEmpty()
						|| (distributorId > 0L && distributorIds.contains(distributorId));
		return itemOk && shopOk;
	}

	public static MemberTaskFavoriteRule parse(String ruleConfigJson, ObjectMapper objectMapper) {
		if (!StringUtils.hasText(ruleConfigJson)) {
			return new MemberTaskFavoriteRule(Set.of(), Set.of());
		}
		try {
			JsonNode node = objectMapper.readTree(ruleConfigJson);
			return new MemberTaskFavoriteRule(readIds(node.get("item_ids")), readIds(node.get("distributor_ids")));
		} catch (Exception e) {
			return new MemberTaskFavoriteRule(Set.of(), Set.of());
		}
	}

	private static Set<Long> readIds(JsonNode arr) {
		Set<Long> out = new HashSet<>();
		if (arr == null || !arr.isArray()) {
			return out;
		}
		for (JsonNode n : arr) {
			if (n != null && n.isNumber() && n.asLong() > 0L) {
				out.add(n.asLong());
			}
		}
		return out;
	}
}
