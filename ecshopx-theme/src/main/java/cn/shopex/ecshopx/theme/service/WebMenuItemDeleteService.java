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
import cn.shopex.ecshopx.theme.domain.WebMenuItem;
import cn.shopex.ecshopx.theme.mapper.WebMenuItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebMenuItemDeleteService {

	private final WebMenuItemMapper webMenuItemMapper;

	@Transactional(rollbackFor = Exception.class)
	public void delete(long itemId, long menuId, long companyId) {
		WebMenuItem item = findItem(itemId, menuId, companyId);
		if (item == null) {
			throw new ResourceException("菜单项不存在");
		}

		List<WebMenuItem> all = findAllByMenu(menuId, companyId);
		Set<Long> ids = collectDescendantIds(itemId, all);
		if (ids.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<WebMenuItem> deleteWrapper = new LambdaQueryWrapper<>();
		deleteWrapper.in(WebMenuItem::getId, ids);
		webMenuItemMapper.delete(deleteWrapper);
	}

	private WebMenuItem findItem(long itemId, long menuId, long companyId) {
		LambdaQueryWrapper<WebMenuItem> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenuItem::getId, itemId)
				.eq(WebMenuItem::getMenuId, menuId)
				.eq(WebMenuItem::getCompanyId, companyId);
		return webMenuItemMapper.selectOne(wrapper);
	}

	private List<WebMenuItem> findAllByMenu(long menuId, long companyId) {
		LambdaQueryWrapper<WebMenuItem> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenuItem::getMenuId, menuId)
				.eq(WebMenuItem::getCompanyId, companyId)
				.orderByAsc(WebMenuItem::getParentId)
				.orderByAsc(WebMenuItem::getSort)
				.orderByAsc(WebMenuItem::getId);
		return webMenuItemMapper.selectList(wrapper);
	}

	private Set<Long> collectDescendantIds(long rootId, List<WebMenuItem> items) {
		Map<Long, List<Long>> byParent = new LinkedHashMap<>();
		for (WebMenuItem item : items) {
			Long parentId = item.getParentId() == null ? 0L : item.getParentId();
			byParent.computeIfAbsent(parentId, key -> new ArrayList<>()).add(item.getId());
		}
		Set<Long> ids = new LinkedHashSet<>();
		Set<Long> stack = new LinkedHashSet<>();
		stack.add(rootId);
		while (!stack.isEmpty()) {
			Long current = stack.iterator().next();
			stack.remove(current);
			if (current == null || !ids.add(current)) {
				continue;
			}
			List<Long> children = byParent.get(current);
			if (children != null) {
				stack.addAll(children);
			}
		}
		return ids;
	}
}
