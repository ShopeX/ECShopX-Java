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

import cn.shopex.ecshopx.distribution.service.DistributorDeliveryCapability;
import cn.shopex.ecshopx.distribution.service.DistributorDeliveryCapabilityService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 供应商共享库存前台展示：填充 {@code logistics_store}；
 * <strong>仅供应商商品</strong>按店铺/总店配送方式合成展示 {@code store}，自营等非供应商商品保持本地库存。
 * 配送能力按<strong>商品归属</strong>{@code distributor_id} 解析（归属 0=总店），不按请求门店参数。
 * 拼团/秒杀仅保留活动库存、不叠加共享库存；限时特惠按配送能力合成（对齐 platform-inventory-flows）。
 */
@Service
public class ItemLogisticsStoreEnricher {

	/** 拼团/秒杀跳过配送合成；限时特惠走供应商/归属库存展示。 */
	private static final Set<String> ACTIVITY_SKIP_DISPLAY_TYPES = Set.of("group", "single_group", "seckill");

	private final ItemsRepository itemsRepository;
	private final SupplierItemsRepository supplierItemsRepository;
	private final DistributorDeliveryCapabilityService distributorDeliveryCapabilityService;

	public ItemLogisticsStoreEnricher(
			ItemsRepository itemsRepository,
			SupplierItemsRepository supplierItemsRepository,
			DistributorDeliveryCapabilityService distributorDeliveryCapabilityService) {
		this.itemsRepository = itemsRepository;
		this.supplierItemsRepository = supplierItemsRepository;
		this.distributorDeliveryCapabilityService = distributorDeliveryCapabilityService;
	}

	public int resolveLogisticsStore(long companyId, Items item) {
		if (item == null || companyId <= 0L) {
			return 0;
		}
		long supplierItemId = item.getSupplierItemId() != null ? item.getSupplierItemId().longValue() : 0L;
		if (supplierItemId <= 0L) {
			return 0;
		}
		return loadSupplierStore(companyId, supplierItemId);
	}

	/**
	 * 加购/校验用有效库存：仅供应商商品按归属店配送方式合成本地与共享库存；非供应商直接返回本地库存。
	 * {@code requestDistributorId} 保留兼容，配送解析以 {@link Items#getDistributorId()} 为准。
	 */
	public int combineEffectiveStore(long companyId, long requestDistributorId, int localStore, Items item) {
		if (!isSupplierGoods(item)) {
			return Math.max(localStore, 0);
		}
		long ownerDistributorId = ownerDistributorId(item);
		return combineEffectiveStore(companyId, ownerDistributorId, localStore, resolveLogisticsStore(companyId, item));
	}

	/**
	 * 按给定归属/门店 id 合成（无 Items 时由调用方传入库存所属店；列表店铺覆盖场景传店铺 id）。
	 */
	public int combineEffectiveStore(long companyId, long ownerDistributorId, int localStore, int logisticsStore) {
		DistributorDeliveryCapability cap =
				distributorDeliveryCapabilityService.resolveForFront(companyId, ownerDistributorId);
		return cap.combineDisplayStore(localStore, logisticsStore);
	}

	public Map<Long, Integer> mapLogisticsStoreByPlatformItemIds(long companyId, Collection<Long> platformItemIds) {
		if (companyId <= 0L || platformItemIds == null || platformItemIds.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = platformItemIds.stream().filter(id -> id != null && id > 0L).distinct().toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		List<Items> rows = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, ids);
		Set<Long> supplierItemIds = new LinkedHashSet<>();
		Map<Long, Long> platformToSupplier = new HashMap<>();
		for (Items it : rows) {
			if (it.getItemId() == null) {
				continue;
			}
			long sid = it.getSupplierItemId() != null ? it.getSupplierItemId().longValue() : 0L;
			if (sid > 0L) {
				supplierItemIds.add(sid);
				platformToSupplier.put(it.getItemId(), sid);
			}
		}
		if (supplierItemIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Integer> supplierStore = loadSupplierStoreMap(companyId, supplierItemIds);
		Map<Long, Integer> out = new HashMap<>();
		for (Map.Entry<Long, Long> e : platformToSupplier.entrySet()) {
			out.put(e.getKey(), supplierStore.getOrDefault(e.getValue(), 0));
		}
		return out;
	}

	public void enrichDetailWithLogisticsStore(long companyId, Map<String, Object> detail) {
		if (detail == null || detail.isEmpty() || companyId <= 0L) {
			return;
		}
		List<Long> itemIds = new ArrayList<>();
		long mainId = toLong(detail.get("item_id"));
		if (mainId > 0L) {
			itemIds.add(mainId);
		}
		List<Map<String, Object>> specItems = extractSpecItems(detail.get("spec_items"));
		if (specItems != null) {
			for (Map<String, Object> row : specItems) {
				long sid = toLong(row.get("item_id"));
				if (sid > 0L) {
					itemIds.add(sid);
				}
			}
		}
		Map<Long, Integer> logisticsByItem = mapLogisticsStoreByPlatformItemIds(companyId, itemIds);
		if (mainId > 0L) {
			detail.put("logistics_store", logisticsByItem.getOrDefault(mainId, 0));
		} else {
			detail.put("logistics_store", 0);
		}
		if (specItems != null) {
			for (Map<String, Object> row : specItems) {
				long sid = toLong(row.get("item_id"));
				row.put("logistics_store", logisticsByItem.getOrDefault(sid, 0));
			}
		}
	}

	public void enrichListRowsWithLogisticsStore(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty() || companyId <= 0L) {
			return;
		}
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			long id = toLong(row.get("item_id"));
			if (id > 0L) {
				itemIds.add(id);
			}
		}
		Map<Long, Integer> logisticsByItem = mapLogisticsStoreByPlatformItemIds(companyId, itemIds);
		for (Map<String, Object> row : rows) {
			long id = toLong(row.get("item_id"));
			row.put("logistics_store", logisticsByItem.getOrDefault(id, 0));
		}
	}

	/**
	 * 仅供应商商品按归属店配送方式合成展示库存；非供应商保持本地库存。活动行跳过（保持活动库存）。
	 * {@code requestDistributorId} 保留调用兼容，不参与配送解析。
	 */
	public void applyFrontDisplayStoreTotal(long companyId, long requestDistributorId, Map<String, Object> detail) {
		if (detail == null || detail.isEmpty()) {
			return;
		}
		if (isActivitySkipDisplay(detail)) {
			return;
		}
		long ownerDistributorId = ownerDistributorIdFromRow(detail);
		DistributorDeliveryCapability cap = null;
		if (isSupplierGoods(detail)) {
			cap = distributorDeliveryCapabilityService.resolveForFront(companyId, ownerDistributorId);
			applyCombineToRow(detail, cap, true);
		}
		List<Map<String, Object>> specItems = extractSpecItems(detail.get("spec_items"));
		if (specItems == null || specItems.isEmpty()) {
			return;
		}
		int newSpecSum = 0;
		boolean anySpec = false;
		for (Map<String, Object> row : specItems) {
			if (isActivitySkipDisplay(row)) {
				newSpecSum += toInt(row.get("store"));
				continue;
			}
			if (!isSupplierGoods(row) && !isSupplierGoods(detail)) {
				newSpecSum += toInt(row.get("store"));
				anySpec = true;
				continue;
			}
			long specOwner = ownerDistributorIdFromRow(row);
			if (specOwner <= 0L) {
				specOwner = ownerDistributorId;
			}
			DistributorDeliveryCapability specCap;
			if (specOwner == ownerDistributorId && cap != null) {
				specCap = cap;
			} else {
				specCap = distributorDeliveryCapabilityService.resolveForFront(companyId, specOwner);
			}
			applyCombineToRow(row, specCap, false);
			newSpecSum += toInt(row.get("store"));
			anySpec = true;
		}
		if (anySpec && detail.containsKey("item_total_store")) {
			detail.put("item_total_store", newSpecSum);
		}
	}

	/**
	 * 列表逐行：仅供应商商品按归属店合成。{@code requestDistributorId} 保留兼容，不参与解析。
	 */
	public void applyFrontDisplayStoreTotalList(long companyId, long requestDistributorId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		for (Map<String, Object> row : rows) {
			if (isActivitySkipDisplay(row) || !isSupplierGoods(row)) {
				continue;
			}
			long ownerDistributorId = ownerDistributorIdFromRow(row);
			DistributorDeliveryCapability cap =
					distributorDeliveryCapabilityService.resolveForFront(companyId, ownerDistributorId);
			applyCombineToRow(row, cap, false);
		}
	}

	public void enrichListRowsAndApplyDisplayTotal(long companyId, long requestDistributorId, List<Map<String, Object>> rows) {
		enrichListRowsWithLogisticsStore(companyId, rows);
		applyFrontDisplayStoreTotalList(companyId, requestDistributorId, rows);
	}

	/**
	 * @param applyDisplayTotal false 时仅填充 logistics_store（活动商品用）
	 * @param requestDistributorId 保留兼容，展示合成按详情内商品归属 {@code distributor_id}
	 */
	public void enrichDetailAndApplyDisplayTotal(
			long companyId, long requestDistributorId, Map<String, Object> detail, boolean applyDisplayTotal) {
		enrichDetailWithLogisticsStore(companyId, detail);
		if (applyDisplayTotal) {
			applyFrontDisplayStoreTotal(companyId, requestDistributorId, detail);
		}
	}

	/**
	 * 购物车列表（非结算）：{@code is_total_store=false} 覆盖为店铺本地库存后，
	 * 再按店铺配送方式把供应商 {@code logistics_store} 合入展示 {@code store}。
	 */
	public void applyCartLinesDisplayStoreAfterOverlay(
			long companyId, List<Map<String, Object>> lines, Map<Long, Map<String, Object>> skuByItem) {
		if (lines == null || lines.isEmpty() || companyId <= 0L) {
			return;
		}
		for (Map<String, Object> line : lines) {
			if (isActivitySkipDisplay(line)) {
				continue;
			}
			// 仅店铺独立库存行需要二次合成；总部发货行已在列表 enrich 时合成
			if (!Boolean.FALSE.equals(line.get("is_total_store"))) {
				continue;
			}
			long itemId = toLong(line.get("item_id"));
			Map<String, Object> sku = skuByItem != null ? skuByItem.get(itemId) : null;
			int logistics = sku != null ? toInt(sku.get("logistics_store")) : 0;
			if (logistics <= 0 && itemId > 0L) {
				Integer mapped = mapLogisticsStoreByPlatformItemIds(companyId, List.of(itemId)).get(itemId);
				logistics = mapped != null ? mapped : 0;
			}
			if (logistics <= 0) {
				continue;
			}
			long shopId = toLong(line.get("shop_id"));
			DistributorDeliveryCapability cap =
					distributorDeliveryCapabilityService.resolveForFront(companyId, shopId);
			int local = toInt(line.get("store"));
			line.put("logistics_store", logistics);
			line.put("store", cap.combineDisplayStore(local, logistics));
		}
	}

	public static boolean isActivitySkipDisplay(Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return false;
		}
		if (isSkipActivityType(stringVal(row.get("activity_type")))) {
			return true;
		}
		Object acts = row.get("promotion_activity");
		if (acts instanceof List<?> list) {
			for (Object o : list) {
				if (!(o instanceof Map<?, ?> m)) {
					continue;
				}
				Object tag = m.get("tag_type");
				if (tag == null) {
					tag = m.get("activity_type");
				}
				if (isSkipActivityType(stringVal(tag))) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isSkipActivityType(String type) {
		if (type == null || type.isBlank()) {
			return false;
		}
		return ACTIVITY_SKIP_DISPLAY_TYPES.contains(type.trim());
	}

	private void applyCombineToRow(Map<String, Object> row, DistributorDeliveryCapability cap, boolean touchTotal) {
		int logistics = toInt(row.get("logistics_store"));
		Object storeRaw = row.get("store");
		if (!(logistics <= 0 && storeRaw == null)) {
			row.put("store", cap.combineDisplayStore(toInt(storeRaw), logistics));
		}
		if (touchTotal && row.containsKey("item_total_store")) {
			Object totalRaw = row.get("item_total_store");
			if (!(logistics <= 0 && totalRaw == null)) {
				row.put("item_total_store", cap.combineDisplayStore(toInt(totalRaw), logistics));
			}
		}
	}

	/** 商品归属店：详情/列表行 {@code distributor_id}；缺省 0（总店）。 */
	static long ownerDistributorIdFromRow(Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return 0L;
		}
		return Math.max(0L, toLong(row.get("distributor_id")));
	}

	static long ownerDistributorId(Items item) {
		if (item == null || item.getDistributorId() == null) {
			return 0L;
		}
		return Math.max(0L, item.getDistributorId().longValue());
	}

	/** 供应商商品判定：{@code supplier_item_id > 0}，否则 {@code supplier_id > 0}。 */
	static boolean isSupplierGoods(Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return false;
		}
		if (toLong(row.get("supplier_item_id")) > 0L) {
			return true;
		}
		return toLong(row.get("supplier_id")) > 0L;
	}

	static boolean isSupplierGoods(Items item) {
		if (item == null) {
			return false;
		}
		if (item.getSupplierItemId() != null && item.getSupplierItemId() > 0) {
			return true;
		}
		return item.getSupplierId() != null && item.getSupplierId() > 0;
	}

	private Map<Long, Integer> loadSupplierStoreMap(long companyId, Collection<Long> supplierItemIds) {
		List<SupplierItems> supplierRows = supplierItemsRepository.listByCompanyAndItemIds(companyId, supplierItemIds);
		Map<Long, Integer> out = new HashMap<>();
		for (SupplierItems si : supplierRows) {
			if (si.getItemId() != null) {
				out.put(si.getItemId(), supplierMarketableStore(si));
			}
		}
		return out;
	}

	private int loadSupplierStore(long companyId, long supplierItemId) {
		SupplierItems row = supplierItemsRepository.getByItemIdAndCompany(supplierItemId, companyId);
		return supplierMarketableStore(row);
	}

	/** 停售（is_market != 1）不对外暴露共享库存。 */
	private static int supplierMarketableStore(SupplierItems row) {
		if (row == null || row.getStore() == null) {
			return 0;
		}
		if (row.getIsMarket() == null || row.getIsMarket() != 1) {
			return 0;
		}
		return row.getStore();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> extractSpecItems(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return null;
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				out.add((Map<String, Object>) m);
			}
		}
		return out;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
