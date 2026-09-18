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

package cn.shopex.ecshopx.theme.support;

import cn.shopex.ecshopx.theme.domain.WebMenuItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WebMenuItemResponseMapper {

	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ObjectMapper objectMapper;

	public Map<String, Object> toFlatResponse(WebMenuItem item) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", item.getId());
		map.put("menu_id", item.getMenuId());
		map.put("parent_id", item.getParentId());
		map.put("name", item.getName());
		map.put("image_url", item.getImageUrl());
		map.put("link_type", item.getLinkType());
		map.put("link_value", item.getLinkValue());
		map.put("link_extra", decodeLinkExtra(item.getLinkExtra()));
		map.put("sort", item.getSort());
		map.put("status", item.getStatus());
		map.put("created_at", item.getCreatedAt() == null ? null : item.getCreatedAt().format(DATETIME_FORMATTER));
		map.put("updated_at", item.getUpdatedAt() == null ? null : item.getUpdatedAt().format(DATETIME_FORMATTER));
		return map;
	}

	public Object decodeLinkExtra(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		try {
			Object decoded = objectMapper.readValue(raw, Object.class);
			return decoded instanceof Map || decoded instanceof List ? decoded : List.of();
		} catch (Exception ex) {
			return List.of();
		}
	}
}
