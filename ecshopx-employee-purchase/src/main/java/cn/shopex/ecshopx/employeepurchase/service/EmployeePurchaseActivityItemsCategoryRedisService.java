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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseActivityCategoryQueryMapper;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryRowMaps;
import cn.shopex.ecshopx.goods.service.ItemsCategoryTreeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseActivityItemsCategoryRedisService {

	private static final Logger log =
			LoggerFactory.getLogger(EmployeePurchaseActivityItemsCategoryRedisService.class);

	private final EmployeePurchaseActivityCategoryQueryMapper employeePurchaseActivityCategoryQueryMapper;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsCategoryTreeService itemsCategoryTreeService;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate stringRedisTemplate;

	public EmployeePurchaseActivityItemsCategoryRedisService(
			EmployeePurchaseActivityCategoryQueryMapper employeePurchaseActivityCategoryQueryMapper,
			ItemsCategoryRepository itemsCategoryRepository,
			ItemsCategoryTreeService itemsCategoryTreeService,
			ObjectMapper objectMapper,
			StringRedisTemplate stringRedisTemplate) {
		this.employeePurchaseActivityCategoryQueryMapper = employeePurchaseActivityCategoryQueryMapper;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsCategoryTreeService = itemsCategoryTreeService;
		this.objectMapper = objectMapper;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	private static String buildRedisKey(long companyId, long activityId) {
		return "employee_purchase_category:" + companyId + "_" + activityId;
	}

	public List<Map<String, Object>> fetchActivityItemsCategory(long companyId, long activityId) {
		String key = buildRedisKey(companyId, activityId);
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (raw == null || !StringUtils.hasText(raw)) {
			return List.of();
		}
		try {
			List<Map<String, Object>> list =
					objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
			return list == null ? List.of() : list;
		} catch (Exception e) {
			log.warn("Failed to parse employee purchase category cache for key {}", key, e);
			return List.of();
		}
	}

	public void store(long companyId, long activityId, long jwtDistributorId) {
		List<Long> distinctIds = employeePurchaseActivityCategoryQueryMapper.selectDistinctCategoryIds(companyId, activityId);
		if (distinctIds == null || distinctIds.isEmpty()) {
			return;
		}
		LinkedHashSet<Long> allIds = new LinkedHashSet<>(distinctIds);
		List<ItemsCategory> rows = itemsCategoryRepository.listByCompanyAndCategoryIdIn(companyId, distinctIds);
		for (ItemsCategory row : rows) {
			String path = row.getPath();
			if (path == null || path.isEmpty()) {
				continue;
			}
			for (String seg : path.split(",")) {
				if (seg == null || seg.isEmpty()) {
					continue;
				}
				try {
					allIds.add(Long.parseLong(seg.trim()));
				} catch (NumberFormatException ignored) {
					// skip non-numeric path segments
				}
			}
		}
		List<ItemsCategory> fullRows = itemsCategoryRepository.listByCompanyAndCategoryIdIn(companyId, allIds);
		List<Map<String, Object>> flat = fullRows.stream().map(ItemsCategoryRowMaps::fromListEntity).toList();
		List<Map<String, Object>> tree = itemsCategoryTreeService.getTree(flat, 0L, 0, true);
		String key = buildRedisKey(companyId, activityId);
		try {
			stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(tree));
		} catch (JsonProcessingException e) {
			throw new ResourceException("类目数据序列化失败");
		}
	}
}
