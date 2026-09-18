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

package cn.shopex.ecshopx.goods.service.order.normal;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.OrderTypeSlugMapper;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateItemCheckPort;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.cart.wxapp.PointsmallCartSkuLoadService;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListFacadeService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCreateItemCheckPortImpl implements OrderCreateItemCheckPort {

	private static final String MSG_INVALID_ITEMS = "购买商品无效，请重新结算";
	private static final String MSG_SOME_ITEMS_INVALID = "部分商品无效，请重新结算";

	private final GoodsItemsListFacadeService goodsItemsListFacadeService;
	private final DistributorItemsRepository distributorItemsRepository;
	private final PointsmallCartSkuLoadService pointsmallCartSkuLoadService;

	public OrderCreateItemCheckPortImpl(
			GoodsItemsListFacadeService goodsItemsListFacadeService,
			DistributorItemsRepository distributorItemsRepository,
			PointsmallCartSkuLoadService pointsmallCartSkuLoadService) {
		this.goodsItemsListFacadeService = goodsItemsListFacadeService;
		this.distributorItemsRepository = distributorItemsRepository;
		this.pointsmallCartSkuLoadService = pointsmallCartSkuLoadService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void check(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		Object itemsRaw = pr.get("items");
		if (itemsRaw instanceof List<?> rawList && !rawList.isEmpty()) {
			checkCartLineItems(p, pr, rawList);
			return;
		}
		long singleItemId = longVal(pr.get("item_id"), 0L);
		if (singleItemId > 0L) {
			checkSingleItemIdSellable(p, pr, singleItemId);
		}
	}

	private void checkCartLineItems(NormalOrderCreateParams p, Map<String, Object> pr, List<?> rawList) {
		List<Long> itemIds = new ArrayList<>();
		Set<Long> uniq = new LinkedHashSet<>();
		for (Object o : rawList) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			long id = longVal(row.get("item_id"), 0L);
			if (id > 0L && uniq.add(id)) {
				itemIds.add(id);
			}
			collectChildItemIds(row.get("items_id"), uniq, itemIds);
		}
		if (itemIds.isEmpty()) {
			return;
		}
		if (itemIds.size() > 100) {
			throw new ResourceException("单次购买商品种类不能超过100种");
		}
		long companyId = longVal(pr.get("company_id"), 0L);
		if (isPointsmallOrder(pr)) {
			checkPointsmallCartLineItems(itemIds, uniq, companyId);
			return;
		}
		Map<String, Object> pack = querySkuItemsForOrderItemCheck(companyId, itemIds);
		Set<Long> returnedItemIds = skuListItemIdSet(pack.get("list"));
		if (returnedItemIds.isEmpty() && !uniq.isEmpty()) {
			throw new ResourceException(MSG_INVALID_ITEMS);
		}
		if (!returnedItemIds.containsAll(uniq)) {
			throw new ResourceException(MSG_SOME_ITEMS_INVALID);
		}
		assertSupplierDistinctCountWithinTen(pack);
		assertCartLineNumsMatchSkuList(p, pack);
		assertStandardPromotionDistributorSkusIfNeeded(p, pack);
	}

	@SuppressWarnings("unchecked")
	private void checkPointsmallCartLineItems(List<Long> itemIds, Set<Long> uniq, long companyId) {
		Map<String, Object> pack = pointsmallCartSkuLoadService.querySkuPack(companyId, itemIds);
		Set<Long> returnedItemIds = skuListItemIdSet(pack.get("list"));
		if (returnedItemIds.isEmpty() && !uniq.isEmpty()) {
			throw new ResourceException("商品失效，请重新计算");
		}
		if (!returnedItemIds.containsAll(uniq)) {
			throw new ResourceException("部分商品失效，请重新计算");
		}
	}

	private static boolean isPointsmallOrder(Map<String, Object> pr) {
		return "pointsmall".equals(OrderTypeSlugMapper.resolve(stringVal(pr.get("order_type"))).orderClass());
	}

	private void checkSingleItemIdSellable(NormalOrderCreateParams p, Map<String, Object> pr, long itemId) {
		List<Long> itemIds = List.of(itemId);
		long companyId = longVal(pr.get("company_id"), 0L);
		if (isPointsmallOrder(pr)) {
			checkPointsmallCartLineItems(itemIds, Set.of(itemId), companyId);
			return;
		}
		Map<String, Object> pack = querySkuItemsForOrderItemCheck(companyId, itemIds);
		Set<Long> returnedItemIds = skuListItemIdSet(pack.get("list"));
		if (!returnedItemIds.contains(itemId)) {
			throw new ResourceException(MSG_INVALID_ITEMS);
		}
		assertSupplierDistinctCountWithinTen(pack);
		assertStandardPromotionDistributorSkusIfNeeded(p, pack);
		Object listObj = pack.get("list");
		if (!(listObj instanceof List<?> raw) || raw.isEmpty()) {
			throw new ResourceException(MSG_INVALID_ITEMS);
		}
		Map<String, Object> itemInfo = null;
		for (Object o : raw) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			if (longVal(row.get("item_id"), 0L) == itemId) {
				itemInfo = (Map<String, Object>) row;
				break;
			}
		}
		if (itemInfo == null) {
			throw new ResourceException(MSG_INVALID_ITEMS);
		}
		String approve = stringVal(itemInfo.get("approve_status"));
		String orderSource = stringVal(pr.get("order_source"));
		if (!StringUtils.hasText(orderSource)) {
			orderSource = "member";
		}
		if (("shop".equals(orderSource) || "member".equals(orderSource))
				&& !"onsale".equals(approve)
				&& !"offline_sale".equals(approve)) {
			throw new ResourceException("商品{0}已下架");
		}
	}

	private void assertSupplierDistinctCountWithinTen(Map<String, Object> skuPack) {
		Object listObj = skuPack.get("list");
		if (!(listObj instanceof List<?> raw) || raw.isEmpty()) {
			return;
		}
		Set<Long> supplierIds = new HashSet<>();
		for (Object o : raw) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			long sid = longVal(row.get("supplier_id"), 0L);
			if (sid > 0L) {
				supplierIds.add(sid);
			}
		}
		if (supplierIds.size() > 10) {
			throw new ResourceException("单次订单供应商数量不能超过10个");
		}
	}

	private void assertCartLineNumsMatchSkuList(NormalOrderCreateParams p, Map<String, Object> skuPack) {
		Map<String, Object> pr = p.getParams();
		Object itemsRaw = pr.get("items");
		if (!(itemsRaw instanceof List<?> cartLines)) {
			return;
		}
		Object listObj = skuPack.get("list");
		if (!(listObj instanceof List<?> skuLines)) {
			return;
		}
		Set<Long> skuItemIds = new HashSet<>();
		for (Object o : skuLines) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			long iid = longVal(row.get("item_id"), 0L);
			if (iid > 0L) {
				skuItemIds.add(iid);
			}
		}
		for (Object o : cartLines) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			long iid = longVal(row.get("item_id"), 0L);
			if (iid <= 0L) {
				continue;
			}
			long num = longVal(row.get("num"), 0L);
			if (num <= 0L) {
				throw new ResourceException("商品数量需大于0");
			}
			if (!skuItemIds.contains(iid)) {
				throw new ResourceException("商品数量与规格不一致，请重新计算");
			}
		}
	}

	private void assertStandardPromotionDistributorSkusIfNeeded(NormalOrderCreateParams p, Map<String, Object> skuPack) {
		Map<String, Object> pr = p.getParams();
		long distributorId = longVal(pr.get("distributor_id"), 0L);
		if (distributorId <= 0L) {
			return;
		}
		if (!"normal".equals(String.valueOf(pr.getOrDefault("promotion", "")).trim())) {
			return;
		}
		long companyId = longVal(pr.get("company_id"), 0L);
		Object listObj = skuPack.get("list");
		if (!(listObj instanceof List<?> raw)) {
			return;
		}
		for (Object o : raw) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			long itemId = longVal(row.get("item_id"), 0L);
			if (itemId <= 0L) {
				continue;
			}
			if (!isValidDistributorItemSku(distributorId, companyId, itemId, row)) {
				throw new ResourceException("部分店铺商品无效，请重新计算");
			}
		}
	}

	private boolean isValidDistributorItemSku(
			long distributorId, long companyId, long itemId, Map<?, ?> row) {
		if (distributorItemsRepository.existsByDistributorCompanyItem(distributorId, companyId, itemId)) {
			return true;
		}
		long itemDistributorId = longVal(row.get("distributor_id"), 0L);
		return itemDistributorId > 0L && itemDistributorId == distributorId;
	}

	private Map<String, Object> querySkuItemsForOrderItemCheck(long companyId, List<Long> itemIds) {
		LinkedHashMap<String, Object> repo = new LinkedHashMap<>();
		repo.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		repo.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, itemIds);
		return goodsItemsListFacadeService.wxappQuerySkuItemList(companyId, repo, "zh-CN");
	}

	private static Set<Long> skuListItemIdSet(Object listObj) {
		Set<Long> out = new LinkedHashSet<>();
		if (!(listObj instanceof List<?> raw)) {
			return out;
		}
		for (Object o : raw) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			long iid = longVal(row.get("item_id"), 0L);
			if (iid > 0L) {
				out.add(iid);
			}
		}
		return out;
	}

	private static void collectChildItemIds(Object itemsIdRaw, Set<Long> uniq, List<Long> itemIds) {
		if (!(itemsIdRaw instanceof List<?> list)) {
			return;
		}
		for (Object el : list) {
			long childId = longVal(el, 0L);
			if (childId > 0L && uniq.add(childId)) {
				itemIds.add(childId);
			}
		}
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
