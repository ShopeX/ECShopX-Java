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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.domain.WebMenu;
import cn.shopex.ecshopx.theme.domain.WebMenuItem;
import cn.shopex.ecshopx.theme.mapper.WebMenuItemMapper;
import cn.shopex.ecshopx.theme.mapper.WebMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebMenuItemBatchSortService {

	private final WebMenuMapper webMenuMapper;

	private final WebMenuItemMapper webMenuItemMapper;

	@Transactional(rollbackFor = Exception.class)
	public void batchSort(long menuId, long companyId, JsonNode body) {
		WebMenu menu = findMenu(menuId, companyId);
		if (menu == null) {
			throw new ResourceException("菜单不存在");
		}

		JsonNode sortsNode = resolveSortsNode(body);
		Map<Long, Integer> pairs = parsePairs(sortsNode);
		if (pairs.isEmpty()) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		for (Map.Entry<Long, Integer> entry : pairs.entrySet()) {
			WebMenuItem item = findItem(entry.getKey(), menuId, companyId);
			if (item == null) {
				continue;
			}
			item.setSort(entry.getValue());
			item.setUpdatedAt(now);
			webMenuItemMapper.updateById(item);
		}
	}

	private WebMenu findMenu(long menuId, long companyId) {
		LambdaQueryWrapper<WebMenu> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenu::getId, menuId)
				.eq(WebMenu::getCompanyId, companyId);
		return webMenuMapper.selectOne(wrapper);
	}

	private WebMenuItem findItem(long itemId, long menuId, long companyId) {
		LambdaQueryWrapper<WebMenuItem> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenuItem::getId, itemId)
				.eq(WebMenuItem::getMenuId, menuId)
				.eq(WebMenuItem::getCompanyId, companyId);
		return webMenuItemMapper.selectOne(wrapper);
	}

	private JsonNode resolveSortsNode(JsonNode body) {
		if (body == null || body.isNull()) {
			return null;
		}
		if (body.has("sorts") && !body.get("sorts").isNull()) {
			return body.get("sorts");
		}
		if (body.has("items") && !body.get("items").isNull()) {
			return body.get("items");
		}
		if (body.isArray() || body.isObject()) {
			return body;
		}
		return null;
	}

	private Map<Long, Integer> parsePairs(JsonNode node) {
		Map<Long, Integer> pairs = new LinkedHashMap<>();
		if (node == null || node.isNull()) {
			return pairs;
		}
		if (node.isArray()) {
			for (JsonNode row : node) {
				if (!row.isObject()) {
					continue;
				}
				JsonNode idNode = row.get("id");
				if (idNode == null || idNode.isNull()) {
					continue;
				}
				long itemId = idNode.asLong();
				int sort = row.has("sort") ? row.get("sort").asInt(0) : 0;
				pairs.put(itemId, sort);
			}
			return pairs;
		}
		if (node.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				String key = field.getKey();
				if (!key.matches("\\d+")) {
					continue;
				}
				long itemId;
				try {
					itemId = Long.parseLong(key);
				} catch (NumberFormatException ex) {
					continue;
				}
				pairs.put(itemId, field.getValue().asInt(0));
			}
		}
		return pairs;
	}
}
