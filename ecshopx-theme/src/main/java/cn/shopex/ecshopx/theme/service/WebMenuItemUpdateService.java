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
import cn.shopex.ecshopx.theme.support.WebMenuItemResponseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WebMenuItemUpdateService {

	private final WebMenuItemMapper webMenuItemMapper;

	private final WebMenuItemResponseMapper responseMapper;

	private final ObjectMapper objectMapper;

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> update(long itemId, long menuId, long companyId, Map<String, Object> body) {
		WebMenuItem item = findItem(itemId, menuId, companyId);
		if (item == null) {
			throw new ResourceException("菜单项不存在");
		}

		if (body.containsKey("parent_id")) {
			long parentId = toLong(body.get("parent_id"));
			if (parentId == itemId) {
				throw new ResourceException("parent_id 不能指向自身");
			}
			if (parentId > 0L && findItem(parentId, menuId, companyId) == null) {
				throw new ResourceException("父级菜单项不存在");
			}
			item.setParentId(parentId);
		}
		if (body.containsKey("name")) {
			String name = body.get("name") == null ? "" : String.valueOf(body.get("name")).trim();
			if (name.isEmpty()) {
				throw new ResourceException("菜单项名称不能为空");
			}
			item.setName(name);
		}
		if (body.containsKey("image_url")) {
			item.setImageUrl(normalizeImageUrl(body.get("image_url")));
		}
		if (body.containsKey("link_type")) {
			item.setLinkType(normalizeLinkType(body.get("link_type")));
		}
		if (body.containsKey("link_value")) {
			item.setLinkValue(normalizeLinkValue(body.get("link_value")));
		}
		if (body.containsKey("link_extra")) {
			item.setLinkExtra(encodeLinkExtra(body.get("link_extra")));
		}
		if (body.containsKey("sort")) {
			item.setSort(toInt(body.get("sort")));
		}
		if (body.containsKey("status")) {
			item.setStatus(toInt(body.get("status")));
		}
		item.setUpdatedAt(LocalDateTime.now());
		webMenuItemMapper.updateById(item);
		return responseMapper.toFlatResponse(item);
	}

	private WebMenuItem findItem(long itemId, long menuId, long companyId) {
		LambdaQueryWrapper<WebMenuItem> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenuItem::getId, itemId)
				.eq(WebMenuItem::getMenuId, menuId)
				.eq(WebMenuItem::getCompanyId, companyId);
		return webMenuItemMapper.selectOne(wrapper);
	}

	private String normalizeImageUrl(Object raw) {
		if (raw == null) {
			return null;
		}
		String value = String.valueOf(raw).trim();
		return value.isEmpty() ? null : value;
	}

	private String normalizeLinkType(Object raw) {
		String value = raw == null ? "" : String.valueOf(raw).trim();
		return value.isEmpty() ? "url" : value;
	}

	private String normalizeLinkValue(Object raw) {
		if (raw == null) {
			return null;
		}
		String value = String.valueOf(raw);
		return value.isEmpty() ? null : value;
	}

	private String encodeLinkExtra(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return StringUtils.hasText(s.trim()) ? s : null;
		}
		if (!(raw instanceof Map) && !(raw instanceof Iterable)) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException ex) {
			return null;
		}
	}

	private long toLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private int toInt(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException ex) {
			return 0;
		}
	}
}
