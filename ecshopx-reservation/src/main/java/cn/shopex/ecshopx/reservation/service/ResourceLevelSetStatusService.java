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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.reservation.domain.ResourceLevel;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceLevelSetStatusService {

	private final ResourceLevelMapper resourceLevelMapper;

	public ResourceLevelSetStatusService(ResourceLevelMapper resourceLevelMapper) {
		this.resourceLevelMapper = resourceLevelMapper;
	}

	/**
	 * @return {@link Collections#emptyList()} when no row; otherwise a {@link LinkedHashMap} with 11 fixed keys.
	 */
	@Transactional(rollbackFor = Exception.class)
	public Object updateResourceLevelStatus(long companyId, Long resourceLevelId, String newStatus) {
		String companyIdStr = String.valueOf(companyId);
		if (resourceLevelId == null) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<ResourceLevel> exist = new LambdaQueryWrapper<>();
		exist.eq(ResourceLevel::getResourceLevelId, resourceLevelId).eq(ResourceLevel::getCompanyId, companyIdStr);
		ResourceLevel row = resourceLevelMapper.selectOne(exist);
		if (row == null) {
			return Collections.emptyList();
		}

		LambdaUpdateWrapper<ResourceLevel> uw = new LambdaUpdateWrapper<>();
		uw.eq(ResourceLevel::getResourceLevelId, resourceLevelId)
				.eq(ResourceLevel::getCompanyId, companyIdStr)
				.set(ResourceLevel::getStatus, newStatus);
		resourceLevelMapper.update(null, uw);

		ResourceLevel updated = resourceLevelMapper.selectOne(exist);
		if (updated == null) {
			return Collections.emptyList();
		}
		return toResponseMap(updated);
	}

	private static LinkedHashMap<String, Object> toResponseMap(ResourceLevel rl) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("resourceLevelId", rl.getResourceLevelId());
		map.put("companyId", rl.getCompanyId());
		map.put("shopId", rl.getShopId());
		map.put("shopName", rl.getShopName());
		map.put("name", rl.getName());
		map.put("description", rl.getDescription());
		map.put("status", rl.getStatus());
		map.put("imageUrl", rl.getImageUrl());
		map.put("quantity", rl.getQuantity());
		map.put("created", rl.getCreated());
		map.put("updated", rl.getUpdated());
		return map;
	}
}
