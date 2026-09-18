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
import cn.shopex.ecshopx.theme.support.WebMenuItemResponseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebMenuFrontReadService {

	private final WebMenuMapper webMenuMapper;

	private final WebMenuItemMapper webMenuItemMapper;

	private final WebMenuTreeBuilder treeBuilder;

	private final WebMenuItemResponseMapper itemResponseMapper;

	public Map<String, Object> byKey(long companyId, String key) {
		WebMenu menu = findActiveByCompanyAndKey(companyId, key);
		if (menu == null) {
			throw new ResourceException("菜单不存在");
		}
		return renderMenuTree(companyId, menu);
	}

	public Map<String, Object> byId(long companyId, long menuId) {
		WebMenu menu = findActiveByCompanyAndId(companyId, menuId);
		if (menu == null) {
			throw new ResourceException("菜单不存在");
		}
		return renderMenuTree(companyId, menu);
	}

	private WebMenu findActiveByCompanyAndKey(long companyId, String key) {
		LambdaQueryWrapper<WebMenu> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenu::getCompanyId, companyId)
				.eq(WebMenu::getKey, key)
				.eq(WebMenu::getStatus, 1);
		return webMenuMapper.selectOne(wrapper);
	}

	private WebMenu findActiveByCompanyAndId(long companyId, long menuId) {
		LambdaQueryWrapper<WebMenu> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenu::getId, menuId)
				.eq(WebMenu::getCompanyId, companyId)
				.eq(WebMenu::getStatus, 1);
		return webMenuMapper.selectOne(wrapper);
	}

	private Map<String, Object> renderMenuTree(long companyId, WebMenu menu) {
		LambdaQueryWrapper<WebMenuItem> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenuItem::getMenuId, menu.getId())
				.eq(WebMenuItem::getCompanyId, companyId)
				.eq(WebMenuItem::getStatus, 1)
				.orderByAsc(WebMenuItem::getParentId)
				.orderByAsc(WebMenuItem::getSort)
				.orderByAsc(WebMenuItem::getId);
		List<WebMenuItem> items = webMenuItemMapper.selectList(wrapper);
		List<WebMenuTreeNode> tree = treeBuilder.buildTree(items);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", menu.getId());
		result.put("name", menu.getName());
		result.put("key", menu.getKey());
		result.put("items", tree.stream().map(this::toFrontNode).toList());
		return result;
	}

	private Map<String, Object> toFrontNode(WebMenuTreeNode node) {
		WebMenuItem item = node.getItem();
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", item.getId());
		map.put("name", item.getName());
		map.put("image_url", item.getImageUrl());
		map.put("link_type", item.getLinkType());
		map.put("link_value", item.getLinkValue());
		map.put("link_extra", itemResponseMapper.decodeLinkExtra(item.getLinkExtra()));
		map.put("sort", item.getSort());
		map.put("children", node.getChildren().stream().map(this::toFrontNode).toList());
		return map;
	}
}
