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
import cn.shopex.ecshopx.theme.service.dto.WebMenuTreeNode;
import cn.shopex.ecshopx.theme.support.WebMenuResponseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WebMenuDetailService {

	private final WebMenuMapper webMenuMapper;

	private final WebMenuItemMapper webMenuItemMapper;

	private final WebMenuTreeBuilder treeBuilder;

	private final WebMenuResponseMapper responseMapper;

	private final ObjectMapper objectMapper;

	public Map<String, Object> detail(long menuId, long companyId) {
		WebMenu menu = findMenu(menuId, companyId);
		if (menu == null) {
			throw new ResourceException("菜单不存在");
		}

		LambdaQueryWrapper<WebMenuItem> itemWrapper = new LambdaQueryWrapper<>();
		itemWrapper.eq(WebMenuItem::getMenuId, menuId)
				.eq(WebMenuItem::getCompanyId, companyId)
				.orderByAsc(WebMenuItem::getParentId)
				.orderByAsc(WebMenuItem::getSort)
				.orderByAsc(WebMenuItem::getId);
		List<WebMenuItem> items = webMenuItemMapper.selectList(itemWrapper);
		List<WebMenuTreeNode> tree = treeBuilder.buildTree(items);

		Map<String, Object> result = responseMapper.toMenuResponse(menu);
		result.put("items", tree.stream().map(this::toAdminNode).toList());
		return result;
	}

	private WebMenu findMenu(long menuId, long companyId) {
		LambdaQueryWrapper<WebMenu> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenu::getId, menuId)
				.eq(WebMenu::getCompanyId, companyId);
		return webMenuMapper.selectOne(wrapper);
	}

	private Map<String, Object> toAdminNode(WebMenuTreeNode node) {
		WebMenuItem item = node.getItem();
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", item.getId());
		map.put("parent_id", item.getParentId());
		map.put("name", item.getName());
		map.put("image_url", item.getImageUrl());
		map.put("link_type", item.getLinkType());
		map.put("link_value", item.getLinkValue());
		map.put("link_extra", decodeLinkExtra(item.getLinkExtra()));
		map.put("sort", item.getSort());
		map.put("status", item.getStatus());
		map.put("children", node.getChildren().stream().map(this::toAdminNode).toList());
		return map;
	}

	private Object decodeLinkExtra(String raw) {
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
