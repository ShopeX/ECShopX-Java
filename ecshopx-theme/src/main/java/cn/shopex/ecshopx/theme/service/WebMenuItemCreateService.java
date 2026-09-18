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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.CreateWebMenuItemRequest;
import cn.shopex.ecshopx.theme.domain.WebMenu;
import cn.shopex.ecshopx.theme.domain.WebMenuItem;
import cn.shopex.ecshopx.theme.mapper.WebMenuItemMapper;
import cn.shopex.ecshopx.theme.mapper.WebMenuMapper;
import cn.shopex.ecshopx.theme.support.WebMenuItemResponseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WebMenuItemCreateService {

	private final WebMenuMapper webMenuMapper;

	private final WebMenuItemMapper webMenuItemMapper;

	private final WebMenuItemResponseMapper responseMapper;

	private final ObjectMapper objectMapper;

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(long menuId, long companyId, CreateWebMenuItemRequest request) {
		if (findMenu(menuId, companyId) == null) {
			throw new ResourceException("菜单不存在");
		}

		String name = request.getName() == null ? "" : request.getName().trim();
		if (name.isEmpty()) {
			throw new ResourceException("菜单项名称不能为空");
		}

		long parentId = request.getParentId() == null ? 0L : request.getParentId().longValue();
		if (parentId > 0L && findItem(parentId, menuId, companyId) == null) {
			throw new ResourceException("父级菜单项不存在");
		}

		LocalDateTime now = LocalDateTime.now();
		WebMenuItem item = new WebMenuItem();
		item.setMenuId(menuId);
		item.setCompanyId(companyId);
		item.setParentId(parentId);
		item.setName(name);
		item.setImageUrl(normalizeImageUrl(request.getImageUrl()));
		item.setLinkType(normalizeLinkType(request.getLinkType()));
		item.setLinkValue(normalizeLinkValue(request.getLinkValue()));
		item.setLinkExtra(encodeLinkExtra(request.getLinkExtra()));
		item.setSort(request.getSort() == null ? 0 : request.getSort());
		item.setStatus(request.getStatus() == null ? 1 : request.getStatus());
		item.setCreatedAt(now);
		item.setUpdatedAt(now);
		webMenuItemMapper.insert(item);
		return responseMapper.toFlatResponse(item);
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

	private String normalizeImageUrl(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return null;
		}
		return raw.trim();
	}

	private String normalizeLinkType(String raw) {
		String value = raw == null ? "" : raw.trim();
		return value.isEmpty() ? "url" : value;
	}

	private String normalizeLinkValue(String raw) {
		return raw == null || raw.isEmpty() ? null : raw;
	}

	private String encodeLinkExtra(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (node.isTextual()) {
			String text = node.asText();
			return StringUtils.hasText(text.trim()) ? text : null;
		}
		if (!node.isContainerNode()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(node);
		} catch (JsonProcessingException ex) {
			return null;
		}
	}
}
