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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemSkuInfoForStoreService;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.Cart;
import cn.shopex.ecshopx.orders.service.front.wxapp.OrdersCartColumnNamesMapSupport;
import cn.shopex.ecshopx.promotions.service.PackagePromotionInfoService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxappH5PackageCartSupport {

	private final PackagePromotionInfoService packagePromotionInfoService;
	private final ItemsRepository itemsRepository;
	private final DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService;
	private final MemberAccountService memberAccountService;
	private final ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;

	public WxappH5PackageCartSupport(
			PackagePromotionInfoService packagePromotionInfoService,
			ItemsRepository itemsRepository,
			DistributorItemSkuInfoForStoreService distributorItemSkuInfoForStoreService,
			MemberAccountService memberAccountService,
			ItemLogisticsStoreEnricher itemLogisticsStoreEnricher) {
		this.packagePromotionInfoService = packagePromotionInfoService;
		this.itemsRepository = itemsRepository;
		this.distributorItemSkuInfoForStoreService = distributorItemSkuInfoForStoreService;
		this.memberAccountService = memberAccountService;
		this.itemLogisticsStoreEnricher = itemLogisticsStoreEnricher;
	}

	public void assertPackageAddParams(Map<String, Object> params) {
		if (!"package".equals(stringVal(params.get("activity_type")))) {
			return;
		}
		List<String> selected = OrdersCartColumnNamesMapSupport.normalizeItemsIdList(params.get("items_id"));
		if (selected.isEmpty()) {
			throw new ResourceException("请选择组合商品子商品");
		}
		long companyId = longVal(params.get("company_id"));
		long userId = longVal(params.get("user_id"));
		long activityId = longVal(params.get("activity_id"));
		long mainItemId = longVal(params.get("item_id"));
		int num = intVal(params.get("num"));
		long shopId = longVal(params.get("shop_id"));
		String shopType = stringVal(params.get("shop_type"));

		Map<String, Object> packageInfo = loadPackageInfo(companyId, activityId);
		if (packageInfo.isEmpty()) {
			throw new ResourceException("组合商品不存在");
		}
		assertPackageWindowAndGoods(packageInfo, companyId, mainItemId);
		assertSelectedItemsInPackage(packageInfo, selected);
		assertMemberGradeForPackage(userId, companyId, packageInfo);

		List<Long> stockItemIds = new ArrayList<>();
		stockItemIds.add(mainItemId);
		for (String sid : selected) {
			try {
				stockItemIds.add(Long.parseLong(sid.trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		for (Long itemId : stockItemIds) {
			assertItemStockForPackageAdd(companyId, itemId, shopId, shopType, num);
		}
	}

	public Set<Long> collectExtraItemIdsForSkuLoad(Collection<Cart> rows) {
		Set<Long> extra = new LinkedHashSet<>();
		if (rows == null) {
			return extra;
		}
		for (Cart c : rows) {
			if (c == null || !"package".equals(stringVal(c.getActivityType()))) {
				continue;
			}
			for (String sid : OrdersCartColumnNamesMapSupport.splitItemsId(c.getItemsId())) {
				try {
					long id = Long.parseLong(sid.trim());
					if (id > 0L) {
						extra.add(id);
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return extra;
	}

	public boolean enrichPackageCartLine(
			Map<String, Object> line,
			long companyId,
			long userId,
			Map<Long, Map<String, Object>> skuByItem,
			String userDevice) {
		if (!"package".equals(stringVal(line.get("activity_type")))) {
			return true;
		}
		List<String> selected = OrdersCartColumnNamesMapSupport.normalizeItemsIdList(line.get("items_id"));
		if (selected.isEmpty()) {
			return false;
		}
		long activityId = longVal(line.get("activity_id"));
		long mainItemId = longVal(line.get("item_id"));
		Map<String, Object> packageInfo = loadPackageInfo(companyId, activityId);
		if (packageInfo.isEmpty()) {
			return false;
		}
		if (!packageItemValid(companyId, userId, packageInfo, line, selected, skuByItem)) {
			return false;
		}

		Map<Long, Integer> mainPriceByItem = mainItemPriceByItemId(packageInfo);
		int mainPrice = mainPriceByItem.getOrDefault(mainItemId, intVal(line.get("price")));
		int num = intVal(line.get("num"));
		Map<String, Object> mainSku = skuByItem.get(mainItemId);
		if (mainSku != null) {
			int store = intVal(mainSku.get("store"));
			if (num > store) {
				// 库存不足：仅内存按剩余库存参与计价，不回写购物车
				line.put("num", store);
				num = store;
			}
			if (num <= 0) {
				return false;
			}
			copySkuFieldsOntoLine(line, mainSku);
		}

		line.put("price", mainPrice);
		line.put("discount_fee", 0);
		line.put("total_fee", (long) mainPrice * num);
		line.put("parent_id", 0);
		line.put("is_last_price", Boolean.TRUE);

		@SuppressWarnings("unchecked")
		Map<String, Object> newPrice =
				packageInfo.get("new_price") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
		List<Map<String, Object>> packages = new ArrayList<>();
		for (String childKey : selected) {
			long childId;
			try {
				childId = Long.parseLong(childKey.trim());
			} catch (NumberFormatException e) {
				return false;
			}
			Map<String, Object> childSku = skuByItem.get(childId);
			if (childSku == null || childSku.isEmpty()) {
				return false;
			}
			int childPrice = intFromNewPrice(newPrice, childKey);
			Map<String, Object> child = new LinkedHashMap<>();
			Object cartId = line.get("cart_id");
			child.put("parent_id", cartId == null ? "" : String.valueOf(cartId));
			if (!"pc".equals(userDevice)) {
				child.put("price", childPrice);
			}
			child.put("discount_fee", 0);
			child.put("num", line.get("num"));
			child.put("total_fee", (long) childPrice * num);
			child.put("store", childSku.get("store"));
			child.put("market_price", childSku.get("market_price"));
			child.put("brief", childSku.get("brief"));
			child.put("item_type", childSku.get("item_type"));
			child.put("approve_status", childSku.get("approve_status"));
			child.put("item_name", childSku.get("item_name"));
			child.put("item_id", String.valueOf(childId));
			child.put("pics", firstPicUrl(childSku.get("pics")));
			child.put("item_spec_desc", childSku.get("item_spec_desc"));
			child.put("is_last_price", Boolean.TRUE);
			packages.add(child);
		}
		line.put("packages", packages);
		long lineTotal = (long) mainPrice * num;
		for (Map<String, Object> child : packages) {
			lineTotal += longVal(child.get("total_fee"));
		}
		line.put("total_fee", lineTotal);
		return true;
	}

	public static long packageLinePayFen(Map<String, Object> line) {
		long main = (long) intVal(line.get("price")) * intVal(line.get("num"));
		Object raw = line.get("packages");
		if (!(raw instanceof List<?> pkgs) || pkgs.isEmpty()) {
			long tf = longVal(line.get("total_fee"));
			return tf > 0L ? tf : main;
		}
		long sum = main;
		for (Object o : pkgs) {
			if (!(o instanceof Map<?, ?> pkg)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> child = (Map<String, Object>) pkg;
			long tf = longVal(child.get("total_fee"));
			if (tf > 0L) {
				sum += tf;
			} else {
				sum += (long) intVal(child.get("price")) * intVal(child.get("num"));
			}
		}
		return sum;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> loadPackageInfo(long companyId, long packageId) {
		if (packageId <= 0L) {
			return Map.of();
		}
		Object raw = packagePromotionInfoService.info(companyId, String.valueOf(packageId));
		if (raw instanceof Map<?, ?> m && !m.isEmpty()) {
			return (Map<String, Object>) m;
		}
		return Map.of();
	}

	private void assertPackageWindowAndGoods(Map<String, Object> packageInfo, long companyId, long mainItemId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int start = intVal(packageInfo.get("start_time"));
		int end = intVal(packageInfo.get("end_time"));
		if (start > now || end < now) {
			throw new ResourceException("组合商品不存在");
		}
		Items item = itemsRepository.getByItemIdAndCompany(mainItemId, companyId);
		if (item == null) {
			throw new ResourceException("组合商品不存在");
		}
		long pkgGoodsId = longVal(packageInfo.get("goods_id"));
		long itemGoodsId = item.getGoodsId() == null ? 0L : item.getGoodsId().longValue();
		if (pkgGoodsId > 0L && itemGoodsId != pkgGoodsId) {
			throw new ResourceException("组合商品不存在");
		}
	}

	private static void assertSelectedItemsInPackage(Map<String, Object> packageInfo, List<String> selected) {
		@SuppressWarnings("unchecked")
		List<Long> allowed =
				packageInfo.get("package_items") instanceof List<?> l
						? l.stream()
								.map(
										o -> {
											if (o instanceof Number n) {
												return n.longValue();
											}
											try {
												return Long.parseLong(String.valueOf(o).trim());
											} catch (NumberFormatException e) {
												return 0L;
											}
										})
								.filter(id -> id > 0L)
								.toList()
						: List.of();
		for (String sid : selected) {
			try {
				long id = Long.parseLong(sid.trim());
				if (!allowed.contains(id)) {
					throw new ResourceException("组合商品信息错误");
				}
			} catch (NumberFormatException e) {
				throw new ResourceException("组合商品信息错误");
			}
		}
	}

	private void assertMemberGradeForPackage(long userId, long companyId, Map<String, Object> packageInfo) {
		Object vgRaw = packageInfo.get("valid_grade");
		List<?> grades = vgRaw instanceof List<?> l ? l : List.of();
		if (grades.isEmpty()) {
			return;
		}
		Long userGrade = resolveUserGrade(userId, companyId);
		if (userGrade == null || !gradeListContains(grades, userGrade)) {
			throw new ResourceException("需要规定会员才允许购买");
		}
	}

	private void assertItemStockForPackageAdd(
			long companyId, long itemId, long shopId, String shopType, int num) {
		Items base = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (base == null) {
			throw new ResourceException("商品不存在");
		}
		int store = base.getStore() != null ? base.getStore() : 0;
		if (shopId > 0L && "distributor".equals(shopType)) {
			var row = distributorItemSkuInfoForStoreService.findRow(companyId, itemId, shopId);
			if (row != null && (row.getIsTotalStore() == null || !Boolean.TRUE.equals(row.getIsTotalStore()))) {
				store = row.getStore() == null ? 0 : row.getStore().intValue();
			}
		}
		store = itemLogisticsStoreEnricher.combineEffectiveStore(companyId, shopId, store, base);
		if (store < num) {
			throw new ResourceException("库存不足");
		}
	}

	private boolean packageItemValid(
			long companyId,
			long userId,
			Map<String, Object> packageInfo,
			Map<String, Object> line,
			List<String> selected,
			Map<Long, Map<String, Object>> skuByItem) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int start = intVal(packageInfo.get("start_time"));
		int end = intVal(packageInfo.get("end_time"));
		if (start > now || end < now) {
			return false;
		}
		Object vgRaw = packageInfo.get("valid_grade");
		List<?> grades = vgRaw instanceof List<?> l ? l : List.of();
		if (!grades.isEmpty()) {
			Long userGrade = resolveUserGrade(userId, companyId);
			if (userGrade == null || !gradeListContains(grades, userGrade)) {
				return false;
			}
		}
		long mainItemId = longVal(line.get("item_id"));
		if (!singleItemValid(line, mainItemId, skuByItem)) {
			return false;
		}
		for (String sid : selected) {
			long childId;
			try {
				childId = Long.parseLong(sid.trim());
			} catch (NumberFormatException e) {
				return false;
			}
			if (!singleItemValid(line, childId, skuByItem)) {
				return false;
			}
		}
		return true;
	}

	private static boolean singleItemValid(
			Map<String, Object> line, long itemId, Map<Long, Map<String, Object>> skuByItem) {
		Map<String, Object> sku = skuByItem.get(itemId);
		if (sku == null || sku.isEmpty()) {
			return false;
		}
		String approve = stringVal(sku.get("approve_status"));
		if (!"onsale".equals(approve) && !"offline_sale".equals(approve)) {
			return false;
		}
		String shopType = stringVal(line.get("shop_type"));
		String special = stringVal(sku.get("special_type"));
		if ("drug".equals(shopType) && !"drug".equals(special)) {
			return false;
		}
		if (!"drug".equals(shopType) && "drug".equals(special)) {
			return false;
		}
		return true;
	}

	private static Map<Long, Integer> mainItemPriceByItemId(Map<String, Object> packageInfo) {
		Map<Long, Integer> out = new LinkedHashMap<>();
		Object raw = packageInfo.get("main_items");
		if (!(raw instanceof List<?> list)) {
			return out;
		}
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> m)) {
				continue;
			}
			long iid = longVal(m.get("item_id"));
			if (iid > 0L) {
				out.put(iid, intVal(m.get("item_price")));
			}
		}
		return out;
	}

	private static int intFromNewPrice(Map<String, Object> newPrice, String itemKey) {
		Object v = newPrice.get(itemKey);
		if (v == null) {
			v = newPrice.get(String.valueOf(itemKey));
		}
		return intVal(v);
	}

	private static void copySkuFieldsOntoLine(Map<String, Object> line, Map<String, Object> sku) {
		if (sku.containsKey("store")) {
			line.put("store", sku.get("store"));
		}
		if (sku.containsKey("market_price")) {
			line.put("market_price", sku.get("market_price"));
		}
		if (sku.containsKey("brief")) {
			line.put("brief", sku.get("brief"));
		}
		if (sku.containsKey("item_type")) {
			line.put("item_type", sku.get("item_type"));
		}
		if (sku.containsKey("approve_status")) {
			line.put("approve_status", sku.get("approve_status"));
		}
		if (sku.containsKey("item_name")) {
			line.put("item_name", sku.get("item_name"));
		}
		if (sku.containsKey("pics")) {
			line.put("pics", firstPicUrl(sku.get("pics")));
		}
		if (sku.containsKey("item_spec_desc")) {
			line.put("item_spec_desc", sku.get("item_spec_desc"));
		}
		if (sku.containsKey("type")) {
			line.put("type", sku.get("type"));
		}
		if (sku.containsKey("crossborder_tax_rate")) {
			line.put("crossborder_tax_rate", sku.get("crossborder_tax_rate"));
		}
		if (sku.containsKey("taxstrategy_id")) {
			line.put("taxstrategy_id", sku.get("taxstrategy_id"));
		}
		if (sku.containsKey("taxation_num")) {
			line.put("taxation_num", sku.get("taxation_num"));
		}
		if (sku.containsKey("origincountry_id")) {
			line.put("origincountry_id", sku.get("origincountry_id"));
		}
		if (sku.containsKey("is_medicine")) {
			line.put("is_medicine", sku.get("is_medicine"));
		}
		if (sku.containsKey("start_num")) {
			line.put("start_num", sku.get("start_num"));
		}
	}

	private static String firstPicUrl(Object pics) {
		if (pics instanceof Collection<?> coll && !coll.isEmpty()) {
			Object first =
					pics instanceof List<?> list ? list.get(0) : coll.iterator().next();
			return first != null ? String.valueOf(first) : "";
		}
		if (pics instanceof String s) {
			return s.trim();
		}
		return "";
	}

	private Long resolveUserGrade(long userId, long companyId) {
		if (userId <= 0L) {
			return null;
		}
		Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
		Object g = info.get("grade_id");
		if (g instanceof Number n) {
			return n.longValue();
		}
		if (g != null && StringUtils.hasText(g.toString())) {
			try {
				return Long.parseLong(g.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	private static boolean gradeListContains(List<?> validGrade, long userGrade) {
		for (Object o : validGrade) {
			if (o instanceof Number n && n.longValue() == userGrade) {
				return true;
			}
			if (o != null) {
				String s = o.toString().trim();
				if (s.equals(String.valueOf(userGrade))) {
					return true;
				}
				try {
					if (Long.parseLong(s) == userGrade) {
						return true;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return false;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
