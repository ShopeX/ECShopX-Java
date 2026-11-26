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

package cn.shopex.ecshopx.goods.service.saaserp;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpItemStoreSyncPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.promotions.service.WdtErpSyncInventoryPromotionSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class GoodsSaasErpItemStoreSyncAdapter implements SaasErpItemStoreSyncPort {

	private final ItemsMapper itemsMapper;
	private final DistributorMapper distributorMapper;
	private final DistributorItemsMapper distributorItemsMapper;
	private final ItemStoreService itemStoreService;
	private final WdtErpSyncInventoryPromotionSupport wdtErpSyncInventoryPromotionSupport;

	public GoodsSaasErpItemStoreSyncAdapter(
			ItemsMapper itemsMapper,
			DistributorMapper distributorMapper,
			DistributorItemsMapper distributorItemsMapper,
			ItemStoreService itemStoreService,
			WdtErpSyncInventoryPromotionSupport wdtErpSyncInventoryPromotionSupport) {
		this.itemsMapper = itemsMapper;
		this.distributorMapper = distributorMapper;
		this.distributorItemsMapper = distributorItemsMapper;
		this.itemStoreService = itemStoreService;
		this.wdtErpSyncInventoryPromotionSupport = wdtErpSyncInventoryPromotionSupport;
	}

	@Override
	public void sync(long companyId, List<Map<String, Object>> rows) {
		List<Map<String, Object>> safeRows = rows != null ? rows : List.of();
		List<String> productBns = collectProductBns(safeRows);
		if (productBns.isEmpty()) {
			throw new IllegalStateException("商品不存在");
		}
		LambdaQueryWrapper<Items> itemQuery = new LambdaQueryWrapper<>();
		itemQuery.in(Items::getItemBn, productBns);
		if (companyId > 0L) {
			itemQuery.eq(Items::getCompanyId, companyId);
		}
		itemQuery.orderByDesc(Items::getItemId);
		List<Items> loaded = itemsMapper.selectList(itemQuery);
		if (loaded.isEmpty()) {
			throw new IllegalStateException("商品不存在");
		}
		Map<String, Items> itemByBn = new LinkedHashMap<>();
		for (Items item : loaded) {
			if (item.getItemBn() != null) {
				itemByBn.put(item.getItemBn().trim(), item);
			}
		}
		Set<String> activityBns = wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(companyId);
		for (Map<String, Object> row : safeRows) {
			try {
				processRow(companyId, row, itemByBn, activityBns);
			} catch (Exception ex) {
				log.warn("SaaS ERP update_store row failed: {}", ex.getMessage(), ex);
			}
		}
	}

	private void processRow(
			long companyId, Map<String, Object> row, Map<String, Items> itemByBn, Set<String> activityBns) {
		String productBn = stringValue(row.get("product_bn"));
		if (!StringUtils.hasText(productBn)) {
			return;
		}
		productBn = productBn.trim();
		Integer store = parseStore(row.get("product_store"));
		if (store == null) {
			return;
		}
		if (activityBns.contains(productBn)) {
			return;
		}
		Items item = itemByBn.get(productBn);
		if (item == null || item.getItemId() == null) {
			return;
		}
		long itemId = item.getItemId();
		String storeCode = stringValue(row.get("store_code"));
		if (StringUtils.hasText(storeCode)) {
			syncDistributorStore(companyId, storeCode.trim(), itemId, store);
		} else {
			syncPlatformStore(companyId, itemId, store);
		}
	}

	private void syncPlatformStore(long companyId, long itemId, int store) {
		itemStoreService.overwriteItemStore(itemId, store, 0L);
		LambdaUpdateWrapper<Items> update = new LambdaUpdateWrapper<>();
		update.eq(Items::getItemId, itemId);
		if (companyId > 0L) {
			update.eq(Items::getCompanyId, companyId);
		}
		update.set(Items::getStore, store);
		itemsMapper.update(null, update);
	}

	private void syncDistributorStore(long companyId, String shopCode, long itemId, int store) {
		LambdaQueryWrapper<Distributor> distributorQuery = new LambdaQueryWrapper<>();
		distributorQuery.eq(Distributor::getShopCode, shopCode);
		if (companyId > 0L) {
			distributorQuery.eq(Distributor::getCompanyId, companyId);
		}
		distributorQuery.last("LIMIT 1");
		Distributor distributor = distributorMapper.selectOne(distributorQuery);
		if (distributor == null || distributor.getDistributorId() == null) {
			log.warn("SaaS ERP update_store shop not found: shop_code={}", shopCode);
			return;
		}
		long distributorId = distributor.getDistributorId();
		LambdaQueryWrapper<DistributorItems> distributorItemQuery = new LambdaQueryWrapper<>();
		distributorItemQuery
				.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getItemId, itemId);
		if (companyId > 0L) {
			distributorItemQuery.eq(DistributorItems::getCompanyId, companyId);
		}
		distributorItemQuery.last("LIMIT 1");
		DistributorItems distributorItem = distributorItemsMapper.selectOne(distributorItemQuery);
		if (distributorItem == null) {
			log.warn(
					"SaaS ERP update_store distributor item not found: shop_code={}, item_id={}",
					shopCode,
					itemId);
			return;
		}
		if (Boolean.TRUE.equals(distributorItem.getIsTotalStore())) {
			log.warn(
					"SaaS ERP update_store distributor item uses total store: shop_code={}, item_id={}",
					shopCode,
					itemId);
			return;
		}
		itemStoreService.overwriteItemStore(itemId, store, distributorId);
		LambdaUpdateWrapper<DistributorItems> update = new LambdaUpdateWrapper<>();
		update.eq(DistributorItems::getDistributorId, distributorId).eq(DistributorItems::getItemId, itemId);
		if (companyId > 0L) {
			update.eq(DistributorItems::getCompanyId, companyId);
		}
		update.set(DistributorItems::getStore, (long) store);
		distributorItemsMapper.update(null, update);
	}

	private static List<String> collectProductBns(List<Map<String, Object>> rows) {
		List<String> bns = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			String bn = stringValue(row.get("product_bn"));
			if (StringUtils.hasText(bn)) {
				bns.add(bn.trim());
			}
		}
		return bns;
	}

	private static String stringValue(Object raw) {
		if (raw == null) {
			return null;
		}
		String text = String.valueOf(raw).trim();
		return text.isEmpty() ? null : text;
	}

	private static Integer parseStore(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number number) {
			if (raw instanceof Double || raw instanceof Float) {
				double d = number.doubleValue();
				if (d != Math.floor(d)) {
					return null;
				}
			}
			return number.intValue();
		}
		String text = String.valueOf(raw).trim();
		if (!StringUtils.hasText(text)) {
			return null;
		}
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
