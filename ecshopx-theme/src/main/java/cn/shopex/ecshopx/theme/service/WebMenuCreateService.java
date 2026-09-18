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
import cn.shopex.ecshopx.theme.api.admin.v1.dto.CreateWebMenuRequest;
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
public class WebMenuCreateService {

	private final WebMenuMapper webMenuMapper;

	private final WebMenuResponseMapper responseMapper;

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(long companyId, CreateWebMenuRequest request) {
		String name = request.getName() == null ? "" : request.getName().trim();
		String key = request.getKey() == null ? "" : request.getKey().trim();
		if (name.isEmpty() || key.isEmpty()) {
			throw new ResourceException("name 与 key 不能为空");
		}
		if (existsDuplicateKey(companyId, key)) {
			throw new ResourceException("同一店铺下 key 已存在");
		}

		LocalDateTime now = LocalDateTime.now();
		WebMenu menu = new WebMenu();
		menu.setCompanyId(companyId);
		menu.setName(name);
		menu.setKey(key);
		menu.setStatus(request.getStatus() == null ? 1 : request.getStatus());
		menu.setCreatedAt(now);
		menu.setUpdatedAt(now);
		webMenuMapper.insert(menu);
		return responseMapper.toMenuResponse(menu);
	}

	private boolean existsDuplicateKey(long companyId, String key) {
		LambdaQueryWrapper<WebMenu> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(WebMenu::getCompanyId, companyId)
				.eq(WebMenu::getKey, key);
		return webMenuMapper.selectCount(wrapper) > 0L;
	}
}
