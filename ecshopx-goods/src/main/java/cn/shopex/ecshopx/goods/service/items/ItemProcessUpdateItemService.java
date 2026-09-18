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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelPointAccessRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemProcessUpdateItemService {

	private final ItemsRepository itemsRepository;
	private final ItemsBarcodeRepository itemsBarcodeRepository;
	private final ItemsRelPointAccessRepository itemsRelPointAccessRepository;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final DistributorItemsRepository distributorItemsRepository;
	private final ItemStoreService itemStoreService;
	private final ServicesItemTypeHandler servicesItemTypeHandler;
	private final ObjectMapper objectMapper;

	public ItemProcessUpdateItemService(
			ItemsRepository itemsRepository,
			ItemsBarcodeRepository itemsBarcodeRepository,
			ItemsRelPointAccessRepository itemsRelPointAccessRepository,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsRelCatsRepository itemsRelCatsRepository,
			DistributorItemsRepository distributorItemsRepository,
			ItemStoreService itemStoreService,
			ServicesItemTypeHandler servicesItemTypeHandler,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.itemsBarcodeRepository = itemsBarcodeRepository;
		this.itemsRelPointAccessRepository = itemsRelPointAccessRepository;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.distributorItemsRepository = distributorItemsRepository;
		this.itemStoreService = itemStoreService;
		this.servicesItemTypeHandler = servicesItemTypeHandler;
		this.objectMapper = objectMapper;
	}

	public ProcessUpdateItemResult run(Map<String, Object> params, String itemType) {
		long itemId = toLong(params.get("item_id"));
		long companyId = toLong(params.get("company_id"));
		Items updateItemInfo = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (updateItemInfo == null) {
			throw new ResourceException("更新的商品无效");
		}
		long distributorId = params.get("distributor_id") != null ? toLong(params.get("distributor_id")) : 0L;
		int rowDist = updateItemInfo.getDistributorId() != null ? updateItemInfo.getDistributorId() : 0;
		if (rowDist != (int) distributorId) {
			throw new ResourceException("更新的商品无效");
		}

		boolean multi = isMultiSpec(updateItemInfo.getNospec(), params.get("nospec"));
		List<Long> newItemIds;
		if (multi) {
			List<Map<String, Object>> specItems = parseSpecItems(params.get("spec_items"));
			if (specItems.isEmpty()) {
				throw new ResourceException("请填写正确的规格数据");
			}
			long defaultItemId = updateItemInfo.getDefaultItemId() != null ? updateItemInfo.getDefaultItemId() : itemId;
			List<Items> rsItems = itemsRepository.listByDefaultItemIdAndCompany(defaultItemId, companyId);
			List<Long> newIds = new ArrayList<>();
			for (Map<String, Object> row : specItems) {
				if (row.get("item_id") != null) {
					newIds.add(toLong(row.get("item_id")));
				}
			}
			List<Long> relCleanupIds = newIds.isEmpty() ? List.of(itemId) : newIds;
			List<Long> deleteIds = new ArrayList<>();
			List<Long> distributorDeleteIds = new ArrayList<>();
			for (Items row : rsItems) {
				if (!newIds.contains(row.getItemId())) {
					deleteIds.add(row.getItemId());
					itemStoreService.deleteItemStore(row.getItemId(), 0L);
					int d = row.getDistributorId() != null ? row.getDistributorId() : 0;
					if (d == 0) {
						distributorDeleteIds.add(row.getItemId());
					}
				}
			}
			if (!deleteIds.isEmpty()) {
				itemsRepository.deleteByItemIdsAndCompanyId(companyId, deleteIds);
				itemsRelPointAccessRepository.deleteByItemIds(companyId, deleteIds);
				itemsBarcodeRepository.deleteByItemIds(companyId, deleteIds);
			}
			if (!distributorDeleteIds.isEmpty()) {
				distributorItemsRepository.deleteByItemIdsAndCompany(companyId, distributorDeleteIds);
			}
			if (!deleteIds.isEmpty()) {
				itemsRelCatsRepository.deleteByCompanyIdAndItemIdIn(companyId, deleteIds);
			}
			itemRelAttributesRepository.deleteByCompanyIdAndItemIds(companyId, relCleanupIds);
		} else {
			itemRelAttributesRepository.deleteByCompanyIdAndItemIds(companyId, List.of(itemId));
		}
		if ("services".equals(itemType)) {
			servicesItemTypeHandler.deleteRelTypesForItem(itemId);
		}
		ProcessUpdateItemResult r = new ProcessUpdateItemResult();
		r.setUpdateItemInfo(updateItemInfo);
		return r;
	}

	private List<Map<String, Object>> parseSpecItems(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					out.add(row);
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				return objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
			} catch (Exception e) {
				return List.of();
			}
		}
		return List.of();
	}

	private static boolean isMultiSpec(String dbNospec, Object paramNospec) {
		if (dbNospec != null) {
			String d = dbNospec.toString();
			if ("false".equalsIgnoreCase(d) || "0".equals(d)) {
				return true;
			}
		}
		if (paramNospec == null) {
			return false;
		}
		if (paramNospec instanceof Boolean b) {
			return !b;
		}
		String p = paramNospec.toString();
		return "false".equalsIgnoreCase(p) || "0".equals(p);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
