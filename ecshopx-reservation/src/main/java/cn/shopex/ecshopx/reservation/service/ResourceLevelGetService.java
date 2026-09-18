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
import cn.shopex.ecshopx.reservation.domain.ResourceLevelRelService;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelMapper;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelRelServiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceLevelGetService {

	private final ResourceLevelMapper resourceLevelMapper;
	private final ResourceLevelRelServiceMapper resourceLevelRelServiceMapper;

	public ResourceLevelGetService(
			ResourceLevelMapper resourceLevelMapper, ResourceLevelRelServiceMapper resourceLevelRelServiceMapper) {
		this.resourceLevelMapper = resourceLevelMapper;
		this.resourceLevelRelServiceMapper = resourceLevelRelServiceMapper;
	}

	/**
	 * @return {@link Collections#emptyList()} when no main row, invalid id, or no rel rows; otherwise a map with 11
	 *     base keys plus {@code materialIds}.
	 */
	@Transactional(readOnly = true)
	public Object getResourceLevel(long companyId, String pathLevelId) {
		String trimmed = pathLevelId == null ? "" : pathLevelId.trim();
		if (trimmed.isEmpty()) {
			return Collections.emptyList();
		}
		final long resourceLevelId;
		try {
			resourceLevelId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}

		String companyIdStr = String.valueOf(companyId);
		LambdaQueryWrapper<ResourceLevel> main = new LambdaQueryWrapper<>();
		main.eq(ResourceLevel::getResourceLevelId, resourceLevelId).eq(ResourceLevel::getCompanyId, companyIdStr);
		ResourceLevel row = resourceLevelMapper.selectOne(main);
		if (row == null) {
			return Collections.emptyList();
		}

		LinkedHashMap<String, Object> map = toBaseMap(row);

		LambdaQueryWrapper<ResourceLevelRelService> rel = new LambdaQueryWrapper<>();
		rel.eq(ResourceLevelRelService::getResourceLevelId, resourceLevelId)
				.eq(ResourceLevelRelService::getCompanyId, companyId)
				.orderByAsc(ResourceLevelRelService::getShopId)
				.orderByAsc(ResourceLevelRelService::getMaterialId);
		List<ResourceLevelRelService> relRows = resourceLevelRelServiceMapper.selectList(rel);
		if (relRows == null || relRows.isEmpty()) {
			return Collections.emptyList();
		}

		List<Long> materialIds = new ArrayList<>();
		for (ResourceLevelRelService r : relRows) {
			Long mid = r.getMaterialId();
			if (mid != null) {
				materialIds.add(mid);
			}
		}
		map.put("materialIds", materialIds);
		return map;
	}

	/** Eleven ordered keys aligned with other resource-level detail responses. */
	public static LinkedHashMap<String, Object> toBaseMap(ResourceLevel rl) {
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
