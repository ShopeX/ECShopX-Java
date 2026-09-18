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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.UpdateWebMenuRequest;
import cn.shopex.ecshopx.theme.domain.WebMenu;
import cn.shopex.ecshopx.theme.mapper.WebMenuMapper;
import cn.shopex.ecshopx.theme.support.WebMenuResponseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebMenuUpdateService {

	private final WebMenuMapper webMenuMapper;

	private final WebMenuResponseMapper responseMapper;

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> update(long menuId, long companyId, UpdateWebMenuRequest request) {
		WebMenu menu = findMenu(menuId, companyId);
		if (menu == null) {
			throw new ResourceException("菜单不存在");
		}

		if (request.getName() != null) {
			String name = request.getName().trim();
			if (name.isEmpty()) {
				throw new ResourceException("name 不能为空");
			}
			menu.setName(name);
		}
		if (request.getKey() != null) {
			String key = request.getKey().trim();
			if (key.isEmpty()) {
				throw new ResourceException("key 不能为空");
			}
			if (existsDuplicateKey(companyId, key, menuId)) {
				throw new ResourceException("同一店铺下 key 已存在");
			}
			menu.setKey(key);
		}
		if (request.getStatus() != null) {
			menu.setStatus(request.getStatus());
		}
		menu.setUpdatedAt(LocalDateTime.now());
		webMenuMapper.updateById(menu);
		return responseMapper.toMenuResponse(menu);
	}

	private WebMenu findMenu(long menuId, long companyId) {
		LambdaQueryWrapper<WebMenu> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenu::getId, menuId)
				.eq(WebMenu::getCompanyId, companyId);
		return webMenuMapper.selectOne(wrapper);
	}

	private boolean existsDuplicateKey(long companyId, String key, long excludeMenuId) {
		LambdaQueryWrapper<WebMenu> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenu::getCompanyId, companyId)
				.eq(WebMenu::getKey, key)
				.ne(WebMenu::getId, excludeMenuId);
		return webMenuMapper.selectCount(wrapper) > 0L;
	}
}
