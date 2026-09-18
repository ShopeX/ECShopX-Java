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
import cn.shopex.ecshopx.goods.dispatch.ItemDeleteEventDispatchPublisher;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTypeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.promotions.repository.MemberPriceBulkDeleteRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformItemsDeleteService {

	private static final String ERR_DELETE_INFO = "删除商品信息有误";
	private static final String ERR_SHOP_MISMATCH = "店铺商品信息有误，不可删除";

	private final ItemsRepository itemsRepository;
	private final ItemStoreService itemStoreService;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final ItemsRelTypeRepository itemsRelTypeRepository;
	private final DistributorItemsRepository distributorItemsRepository;
	private final MemberPriceBulkDeleteRepository memberPriceBulkDeleteRepository;
	private final ItemsBarcodeRepository itemsBarcodeRepository;
	private final ItemDeleteEventDispatchPublisher itemDeleteEventDispatchPublisher;

	public PlatformItemsDeleteService(ItemsRepository itemsRepository, ItemStoreService itemStoreService,
			ItemRelAttributesRepository itemRelAttributesRepository, ItemsRelCatsRepository itemsRelCatsRepository,
			ItemsRelTypeRepository itemsRelTypeRepository, DistributorItemsRepository distributorItemsRepository,
			MemberPriceBulkDeleteRepository memberPriceBulkDeleteRepository, ItemsBarcodeRepository itemsBarcodeRepository,
			ItemDeleteEventDispatchPublisher itemDeleteEventDispatchPublisher) {
		this.itemsRepository = itemsRepository;
		this.itemStoreService = itemStoreService;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.itemsRelTypeRepository = itemsRelTypeRepository;
		this.distributorItemsRepository = distributorItemsRepository;
		this.memberPriceBulkDeleteRepository = memberPriceBulkDeleteRepository;
		this.itemsBarcodeRepository = itemsBarcodeRepository;
		this.itemDeleteEventDispatchPublisher = itemDeleteEventDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deletePlatformItems(Map<String, Object> merged, long itemId, long distributorIdQuery) {
		deleteCore(merged, itemId, distributorIdQuery);
	}

	public void deletePoolItemsForSupplierRecursion(Map<String, Object> merged, long poolItemId, long distributorIdQuery) {
		deleteCore(merged, poolItemId, distributorIdQuery);
	}

	private void deleteCore(Map<String, Object> merged, long itemId, long distributorIdQuery) {
		long companyId = longProp(merged, "company_id");
		Items row = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (row == null) {
			throw new ResourceException(ERR_DELETE_INFO);
		}
		if (!distributorMatches(distributorIdQuery, row.getDistributorId())) {
			throw new ResourceException(ERR_SHOP_MISMATCH);
		}

		List<Long> itemIds;
		long defaultItemId;
		if (ItemDeleteSpec.isMultiSpec(row.getNospec())) {
			List<Items> skus = itemsRepository.listByDefaultItemIdAndCompany(row.getDefaultItemId(), companyId);
			itemIds = new ArrayList<>();
			for (Items s : skus) {
				itemIds.add(s.getItemId());
			}
			if (itemIds.isEmpty()) {
				itemIds.add(row.getItemId());
			}
			defaultItemId = row.getDefaultItemId() != null ? row.getDefaultItemId() : row.getItemId();
		} else {
			itemIds = List.of(row.getItemId());
			defaultItemId = row.getItemId();
		}

		String itemType = row.getItemType() != null ? row.getItemType() : "services";

		Map<String, Object> itemInfo = ItemDeleteEventItemInfoMapper.toItemInfoMap(row);
		Map<String, Object> eventPayload = new LinkedHashMap<>();
		eventPayload.put("item_id", itemId);
		eventPayload.put("company_id", companyId);
		eventPayload.put("del_ids", new ArrayList<>(itemIds));
		eventPayload.put("item_info", itemInfo);

		for (long skuId : itemIds) {
			itemsRepository.deleteByItemIdsAndCompanyId(companyId, List.of(skuId));
			itemStoreService.deleteItemStore(skuId, 0L);
			itemRelAttributesRepository.deleteByCompanyIdAndItemIds(companyId, List.of(skuId));
		}

		itemsRelCatsRepository.deleteByCompanyIdAndItemIdIn(companyId, List.of(defaultItemId));

		if ("services".equals(itemType)) {
			itemsRelTypeRepository.deleteAllByItemId(defaultItemId);
		}

		distributorItemsRepository.deleteByCompanyIdAndDefaultItemId(companyId, defaultItemId);
		memberPriceBulkDeleteRepository.deleteByCompanyIdAndItemIds(companyId, itemIds);
		itemsBarcodeRepository.deleteByItemIds(companyId, itemIds);

		itemDeleteEventDispatchPublisher.publishAfterCommit(eventPayload);
	}

	private static long longProp(Map<String, Object> merged, String key) {
		Object v = merged.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		return Long.parseLong(v.toString());
	}

	private static boolean distributorMatches(long queryDistributorId, Integer rowDistributorId) {
		long r = rowDistributorId == null ? 0L : rowDistributorId.longValue();
		return Objects.equals(queryDistributorId, r);
	}
}
