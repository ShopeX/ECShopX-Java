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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceLevelListByMaterialService {

	private final ResourceLevelRelServiceMapper resourceLevelRelServiceMapper;
	private final ResourceLevelMapper resourceLevelMapper;

	public ResourceLevelListByMaterialService(
			ResourceLevelRelServiceMapper resourceLevelRelServiceMapper, ResourceLevelMapper resourceLevelMapper) {
		this.resourceLevelRelServiceMapper = resourceLevelRelServiceMapper;
		this.resourceLevelMapper = resourceLevelMapper;
	}

	/**
	 * Lists active resource levels for a shop linked to the given material (service) id, with {@code materialId}
	 * on each row (last rel wins per resource level when duplicates exist).
	 */
	@Transactional(readOnly = true)
	public List<Map<String, Object>> listByMaterial(long companyId, String labelIdRaw, long shopId) {
		Long materialId = parseMaterialId(labelIdRaw);
		if (materialId == null) {
			return List.of();
		}
		LambdaQueryWrapper<ResourceLevelRelService> relW =
				Wrappers.<ResourceLevelRelService>lambdaQuery()
						.eq(ResourceLevelRelService::getCompanyId, companyId)
						.eq(ResourceLevelRelService::getMaterialId, materialId);
		List<ResourceLevelRelService> rels = resourceLevelRelServiceMapper.selectList(relW);
		if (rels == null || rels.isEmpty()) {
			return List.of();
		}
		LinkedHashMap<Long, Long> materialByLevel = new LinkedHashMap<>();
		LinkedHashSet<Long> orderedIds = new LinkedHashSet<>();
		for (ResourceLevelRelService rel : rels) {
			Long rid = rel.getResourceLevelId();
			if (rid == null) {
				continue;
			}
			materialByLevel.put(rid, rel.getMaterialId());
			orderedIds.add(rid);
		}
		if (orderedIds.isEmpty()) {
			return List.of();
		}
		String companyIdStr = String.valueOf(companyId);
		String shopIdStr = String.valueOf(shopId);
		LambdaQueryWrapper<ResourceLevel> lvW =
				Wrappers.<ResourceLevel>lambdaQuery()
						.eq(ResourceLevel::getCompanyId, companyIdStr)
						.eq(ResourceLevel::getShopId, shopIdStr)
						.eq(ResourceLevel::getStatus, "active")
						.in(ResourceLevel::getResourceLevelId, orderedIds);
		List<ResourceLevel> rows = resourceLevelMapper.selectList(lvW);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		Map<Long, ResourceLevel> byId = new LinkedHashMap<>();
		for (ResourceLevel rl : rows) {
			byId.put(rl.getResourceLevelId(), rl);
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Long rid : orderedIds) {
			ResourceLevel rl = byId.get(rid);
			if (rl == null) {
				continue;
			}
			LinkedHashMap<String, Object> map = ResourceLevelGetService.toBaseMap(rl);
			map.put("materialId", Objects.requireNonNullElse(materialByLevel.get(rid), materialId));
			out.add(map);
		}
		return out;
	}

	private static Long parseMaterialId(String labelIdRaw) {
		if (labelIdRaw == null) {
			return null;
		}
		String s = labelIdRaw.trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
