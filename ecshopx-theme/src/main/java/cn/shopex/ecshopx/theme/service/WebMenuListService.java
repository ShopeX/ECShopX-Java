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

import cn.shopex.ecshopx.theme.domain.WebMenu;
import cn.shopex.ecshopx.theme.domain.WebMenuItem;
import cn.shopex.ecshopx.theme.mapper.WebMenuItemMapper;
import cn.shopex.ecshopx.theme.mapper.WebMenuMapper;
import cn.shopex.ecshopx.theme.support.WebMenuResponseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WebMenuListService {

	private final WebMenuMapper webMenuMapper;

	private final WebMenuItemMapper webMenuItemMapper;

	private final WebMenuResponseMapper responseMapper;

	public Map<String, Object> listMenus(long companyId, int page, int pageSize, String name) {
		page = Math.max(1, page);
		pageSize = Math.min(100, Math.max(1, pageSize));

		String nameFilter = StringUtils.hasText(name) ? name.trim() : null;

		LambdaQueryWrapper<WebMenu> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenu::getCompanyId, companyId);
		if (nameFilter != null) {
			wrapper.like(WebMenu::getName, "%" + nameFilter + "%");
		}
		wrapper.orderByDesc(WebMenu::getId);

		long total = webMenuMapper.selectCount(wrapper);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		if (total == 0L) {
			result.put("list", List.of());
			return result;
		}

		wrapper.select(
				WebMenu::getId,
				WebMenu::getCompanyId,
				WebMenu::getName,
				WebMenu::getKey,
				WebMenu::getStatus,
				WebMenu::getCreatedAt,
				WebMenu::getUpdatedAt);
		Page<WebMenu> pager = webMenuMapper.selectPage(new Page<>(page, pageSize), wrapper);

		List<Long> menuIds = pager.getRecords().stream()
				.map(WebMenu::getId)
				.toList();
		List<WebMenuItem> items = findItemsByMenuIds(companyId, menuIds);
		Map<Long, Integer> counts = countItemsByMenuIds(menuIds, items);
		Map<Long, String> topLevelNames = topLevelItemNamesByMenuIds(menuIds, items);

		List<Map<String, Object>> list = new ArrayList<>();
		for (WebMenu menu : pager.getRecords()) {
			Map<String, Object> row = responseMapper.toMenuResponse(menu);
			row.put("items_count", counts.getOrDefault(menu.getId(), 0));
			row.put("top_level_item_names", topLevelNames.getOrDefault(menu.getId(), ""));
			list.add(row);
		}
		result.put("list", list);
		return result;
	}

	private List<WebMenuItem> findItemsByMenuIds(long companyId, List<Long> menuIds) {
		if (menuIds == null || menuIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<WebMenuItem> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenuItem::getCompanyId, companyId)
				.in(WebMenuItem::getMenuId, menuIds);
		return webMenuItemMapper.selectList(wrapper);
	}

	private Map<Long, Integer> countItemsByMenuIds(List<Long> menuIds, List<WebMenuItem> items) {
		Map<Long, Integer> counts = new LinkedHashMap<>();
		for (Long menuId : menuIds) {
			counts.put(menuId, 0);
		}
		for (WebMenuItem item : items) {
			Long menuId = item.getMenuId();
			if (menuId != null) {
				counts.put(menuId, counts.getOrDefault(menuId, 0) + 1);
			}
		}
		return counts;
	}

	private Map<Long, String> topLevelItemNamesByMenuIds(List<Long> menuIds, List<WebMenuItem> items) {
		Map<Long, List<String>> grouped = new LinkedHashMap<>();
		for (Long menuId : menuIds) {
			grouped.put(menuId, new ArrayList<>());
		}

		List<WebMenuItem> topLevelItems = items.stream()
				.filter(item -> item.getParentId() != null && item.getParentId() == 0L)
				.sorted(Comparator.comparing(WebMenuItem::getSort, Comparator.nullsFirst(Integer::compareTo))
						.thenComparing(WebMenuItem::getId, Comparator.nullsFirst(Long::compareTo)))
				.toList();
		for (WebMenuItem item : topLevelItems) {
			Long menuId = item.getMenuId();
			if (menuId == null || !grouped.containsKey(menuId)) {
				continue;
			}
			if (StringUtils.hasText(item.getName())) {
				grouped.get(menuId).add(item.getName().trim());
			}
		}

		Map<Long, String> result = new LinkedHashMap<>();
		for (Map.Entry<Long, List<String>> entry : grouped.entrySet()) {
			result.put(entry.getKey(), String.join(",", entry.getValue()));
		}
		return result;
	}
}
