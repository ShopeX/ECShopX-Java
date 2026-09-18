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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceLevelListService {

	private static final long FIXED_PAGE_CURRENT = 1L;
	private static final long FIXED_PAGE_SIZE = 100L;

	private final ResourceLevelMapper resourceLevelMapper;
	private final ResourceLevelRelServiceMapper resourceLevelRelServiceMapper;

	public ResourceLevelListService(
			ResourceLevelMapper resourceLevelMapper, ResourceLevelRelServiceMapper resourceLevelRelServiceMapper) {
		this.resourceLevelMapper = resourceLevelMapper;
		this.resourceLevelRelServiceMapper = resourceLevelRelServiceMapper;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> listResourceLevels(long companyId, String shopIdOrNull) {
		return listResourceLevels(companyId, shopIdOrNull, false);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> listResourceLevels(long companyId, String shopIdOrNull, boolean activeOnly) {
		String companyIdStr = String.valueOf(companyId);
		String trimmedShop = shopIdOrNull == null ? "" : shopIdOrNull.trim();

		LambdaQueryWrapper<ResourceLevel> countW = new LambdaQueryWrapper<>();
		countW.eq(ResourceLevel::getCompanyId, companyIdStr);
		if (!trimmedShop.isEmpty()) {
			countW.eq(ResourceLevel::getShopId, trimmedShop);
		}
		if (activeOnly) {
			countW.eq(ResourceLevel::getStatus, "active");
		}

		long total = resourceLevelMapper.selectCount(countW);
		if (total == 0) {
			return Map.of("total_count", 0L, "list", List.of());
		}

		LambdaQueryWrapper<ResourceLevel> listW = new LambdaQueryWrapper<>();
		listW.eq(ResourceLevel::getCompanyId, companyIdStr);
		if (!trimmedShop.isEmpty()) {
			listW.eq(ResourceLevel::getShopId, trimmedShop);
		}
		if (activeOnly) {
			listW.eq(ResourceLevel::getStatus, "active");
		}
		listW.orderByDesc(ResourceLevel::getResourceLevelId);

		Page<ResourceLevel> page = new Page<>(FIXED_PAGE_CURRENT, FIXED_PAGE_SIZE, false);
		resourceLevelMapper.selectPage(page, listW);

		List<ResourceLevel> records = page.getRecords();
		if (records == null || records.isEmpty()) {
			return Map.of("total_count", total, "list", List.of());
		}

		List<Long> levelIds = new ArrayList<>();
		for (ResourceLevel row : records) {
			Long id = row.getResourceLevelId();
			if (id != null) {
				levelIds.add(id);
			}
		}

		Map<Long, List<Long>> relMap = new HashMap<>();
		if (!levelIds.isEmpty()) {
			LambdaQueryWrapper<ResourceLevelRelService> relW = new LambdaQueryWrapper<>();
			relW.in(ResourceLevelRelService::getResourceLevelId, levelIds)
					.eq(ResourceLevelRelService::getCompanyId, companyId)
					.orderByAsc(ResourceLevelRelService::getShopId)
					.orderByAsc(ResourceLevelRelService::getMaterialId);
			List<ResourceLevelRelService> relRows = resourceLevelRelServiceMapper.selectList(relW);
			if (relRows != null) {
				for (ResourceLevelRelService r : relRows) {
					Long mid = r.getMaterialId();
					if (mid == null) {
						continue;
					}
					Long rid = r.getResourceLevelId();
					relMap.computeIfAbsent(rid, k -> new ArrayList<>()).add(mid);
				}
			}
		}

		List<Map<String, Object>> list = new ArrayList<>(records.size());
		for (ResourceLevel row : records) {
			LinkedHashMap<String, Object> base = ResourceLevelGetService.toBaseMap(row);
			Long rid = row.getResourceLevelId();
			base.put("materialIds", rid == null ? null : relMap.get(rid));
			list.add(base);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}
}
