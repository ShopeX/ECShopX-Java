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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebMenuDeleteService {

	private final WebMenuMapper webMenuMapper;

	private final WebMenuItemMapper webMenuItemMapper;

	@Transactional(rollbackFor = Exception.class)
	public void delete(long menuId, long companyId) {
		WebMenu menu = findMenu(menuId, companyId);
		if (menu == null) {
			throw new ResourceException("菜单不存在");
		}

		LambdaQueryWrapper<WebMenuItem> itemWrapper = new LambdaQueryWrapper<>();
		itemWrapper.eq(WebMenuItem::getMenuId, menuId)
				.eq(WebMenuItem::getCompanyId, companyId);
		webMenuItemMapper.delete(itemWrapper);
		webMenuMapper.deleteById(menu.getId());
	}

	private WebMenu findMenu(long menuId, long companyId) {
		LambdaQueryWrapper<WebMenu> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenu::getId, menuId)
				.eq(WebMenu::getCompanyId, companyId);
		return webMenuMapper.selectOne(wrapper);
	}
}
