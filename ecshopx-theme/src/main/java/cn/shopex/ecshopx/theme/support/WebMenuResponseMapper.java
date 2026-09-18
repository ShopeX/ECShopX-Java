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

import cn.shopex.ecshopx.theme.domain.WebMenu;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WebMenuResponseMapper {

	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public Map<String, Object> toMenuResponse(WebMenu menu) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", menu.getId());
		map.put("name", menu.getName());
		map.put("key", menu.getKey());
		map.put("status", menu.getStatus());
		map.put("created_at", formatDateTime(menu.getCreatedAt()));
		map.put("updated_at", formatDateTime(menu.getUpdatedAt()));
		return map;
	}

	public String formatDateTime(LocalDateTime dateTime) {
		return dateTime == null ? null : dateTime.format(DATETIME_FORMATTER);
	}
}
