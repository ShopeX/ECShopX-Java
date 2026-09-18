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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsTeamItemsListService {

	private static final Logger log = LoggerFactory.getLogger(PromotionGroupsTeamItemsListService.class);

	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final ObjectMapper objectMapper;

	public PromotionGroupsTeamItemsListService(
			PromotionGroupsTeamMapper promotionGroupsTeamMapper, ObjectMapper objectMapper) {
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> getGroupsTeamByItems(long companyId, long actId, int page, int pageSize) {
		int safePage = page < 1 ? 1 : page;
		int safePageSize = pageSize < 1 ? 4 : Math.min(pageSize, 100);
		int offset = safePageSize * (safePage - 1);
		List<LinkedHashMap<String, Object>> rows =
				promotionGroupsTeamMapper.selectGroupsTeamByItemsPage(companyId, actId, offset, safePageSize);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (LinkedHashMap<String, Object> raw : rows) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>(raw);
			normalizeMemberInfoJson(row);
			long endTime = toLongOrZero(row.get("end_time"));
			row.put("over_time", endTime > now ? endTime - now : 0L);
			out.add(row);
		}
		return out;
	}

	private void normalizeMemberInfoJson(LinkedHashMap<String, Object> row) {
		Object raw = row.get("member_info");
		if (raw == null) {
			row.put("member_info", new LinkedHashMap<String, Object>());
			return;
		}
		if (raw instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				if (e.getKey() != null) {
					copy.put(e.getKey().toString(), e.getValue());
				}
			}
			row.put("member_info", copy);
			return;
		}
		if (raw instanceof String str) {
			if (!StringUtils.hasText(str)) {
				row.put("member_info", new LinkedHashMap<String, Object>());
				return;
			}
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> parsed = objectMapper.readValue(str, Map.class);
				LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
				if (parsed != null) {
					for (Map.Entry<String, Object> e : parsed.entrySet()) {
						if (e.getKey() != null) {
							copy.put(e.getKey(), e.getValue());
						}
					}
				}
				row.put("member_info", copy);
			} catch (JsonProcessingException e) {
				log.warn("member_info JSON parse failed: {}", e.getMessage());
				row.put("member_info", new LinkedHashMap<String, Object>());
			}
			return;
		}
		row.put("member_info", new LinkedHashMap<String, Object>());
	}

	private static long toLongOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
