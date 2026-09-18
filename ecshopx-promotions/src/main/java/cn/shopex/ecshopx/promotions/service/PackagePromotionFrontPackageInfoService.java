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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.PackageItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackageMainItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.mapper.PackageItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackageMainItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import cn.shopex.ecshopx.promotions.port.LimitPromotionAdminGoodsSupportPort;
import cn.shopex.ecshopx.promotions.port.PackagePromotionFrontGoodsItemsDetailPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PackagePromotionFrontPackageInfoService {

	private final PackagePromotionsMapper packagePromotionsMapper;
	private final PackageMainItemPromotionsMapper packageMainItemPromotionsMapper;
	private final PackageItemPromotionsMapper packageItemPromotionsMapper;
	private final LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort;
	private final PackagePromotionFrontGoodsItemsDetailPort packagePromotionFrontGoodsItemsDetailPort;

	public PackagePromotionFrontPackageInfoService(
			PackagePromotionsMapper packagePromotionsMapper,
			PackageMainItemPromotionsMapper packageMainItemPromotionsMapper,
			PackageItemPromotionsMapper packageItemPromotionsMapper,
			LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort,
			PackagePromotionFrontGoodsItemsDetailPort packagePromotionFrontGoodsItemsDetailPort) {
		this.packagePromotionsMapper = packagePromotionsMapper;
		this.packageMainItemPromotionsMapper = packageMainItemPromotionsMapper;
		this.packageItemPromotionsMapper = packageItemPromotionsMapper;
		this.limitPromotionAdminGoodsSupportPort = limitPromotionAdminGoodsSupportPort;
		this.packagePromotionFrontGoodsItemsDetailPort = packagePromotionFrontGoodsItemsDetailPort;
	}

	public Map<String, Object> info(long companyId, long packageId, String authorizerAppId) {
		LambdaQueryWrapper<PackagePromotions> pw = new LambdaQueryWrapper<>();
		pw.eq(PackagePromotions::getPackageId, packageId).eq(PackagePromotions::getCompanyId, companyId);
		PackagePromotions main = packagePromotionsMapper.selectOne(pw);
		if (main == null) {
			throw new ResourceException("未查到相关组合商品");
		}

		LinkedHashMap<String, Object> info = new LinkedHashMap<>();
		info.put("package_id", main.getPackageId());
		info.put("company_id", main.getCompanyId());
		info.put("goods_id", main.getGoodsId());
		info.put("main_item_id", main.getMainItemId());
		info.put(
				"main_item_price",
				main.getMainItemPrice() == null ? 0 : main.getMainItemPrice().intValue());
		info.put("package_name", main.getPackageName());
		info.put("valid_grade", parseValidGradeList(main.getValidGrade()));
		info.put("used_platform", main.getUsedPlatform());
		info.put("free_postage", main.getFreePostage());
		info.put("package_total_price", main.getPackageTotalPrice());
		info.put("start_time", main.getStartTime());
		info.put("end_time", main.getEndTime());
		info.put("package_status", main.getPackageStatus());
		info.put("reason", main.getReason());
		info.put("created", main.getCreated());
		info.put("updated", main.getUpdated());
		info.put("source_type", main.getSourceType());
		info.put("source_id", main.getSourceId() != null ? main.getSourceId() : 0L);

		LambdaQueryWrapper<PackageMainItemPromotions> mw = new LambdaQueryWrapper<>();
		mw.eq(PackageMainItemPromotions::getPackageId, packageId)
				.eq(PackageMainItemPromotions::getCompanyId, companyId)
				.orderByAsc(PackageMainItemPromotions::getMainItemId)
				.last("LIMIT 1000");
		List<PackageMainItemPromotions> mainItemRows = packageMainItemPromotionsMapper.selectList(mw);

		List<Long> mainItemIds = new ArrayList<>();
		for (PackageMainItemPromotions r : mainItemRows) {
			if (r.getMainItemId() != null) {
				mainItemIds.add(r.getMainItemId());
			}
		}

		Map<String, Object> mainSkuPack =
				mainItemIds.isEmpty()
						? Map.of("list", List.<Map<String, Object>>of())
						: limitPromotionAdminGoodsSupportPort.loadSkuItemsList(companyId, mainItemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mainRows =
				mainSkuPack.get("list") instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();

		Map<Long, Map<String, Object>> mainItemdata = new LinkedHashMap<>();
		for (Map<String, Object> row : mainRows) {
			Long iid = toLong(row.get("item_id"));
			if (iid != null && !mainItemdata.containsKey(iid)) {
				mainItemdata.put(iid, row);
			}
		}

		LinkedHashMap<Long, LinkedHashMap<String, Object>> mainPackagePrice = new LinkedHashMap<>();
		List<Map<String, Object>> mainRelItems = new ArrayList<>();
		for (PackageMainItemPromotions value : mainItemRows) {
			Long mid = value.getMainItemId();
			Map<String, Object> skuRow = mid == null ? null : mainItemdata.get(mid);
			if (skuRow == null || skuRow.isEmpty()) {
				continue;
			}
			mainRelItems.add(new LinkedHashMap<>(skuRow));
			LinkedHashMap<String, Object> priceRow = new LinkedHashMap<>();
			priceRow.put("market_price", intFromObject(skuRow.get("price"), 0));
			priceRow.put("price", value.getMainItemPrice() == null ? 0 : value.getMainItemPrice().intValue());
			if (mid != null) {
				mainPackagePrice.put(mid, priceRow);
			}
		}

		Map<String, Object> mainItemInfo = new LinkedHashMap<>();
		if (!mainRelItems.isEmpty()) {
			List<Map<String, Object>> grouped = formatPackageItemsList(mainRelItems);
			Map<String, Object> mainItem = grouped.get(0);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> specs =
					mainItem.get("spec_items") instanceof List<?> raw
							? (List<Map<String, Object>>) mainItem.get("spec_items")
							: List.of();
			boolean hasSpecs = specs != null && !specs.isEmpty();
			List<Long> limitIds = new ArrayList<>();
			if (hasSpecs) {
				for (Map<String, Object> spec : specs) {
					Long sid = toLong(spec.get("item_id"));
					if (sid != null && sid > 0L) {
						limitIds.add(sid);
					}
				}
			}
			long root = resolveRootItemId(mainItem);
			if (root > 0L) {
				if (hasSpecs) {
					mainItemInfo.putAll(
							packagePromotionFrontGoodsItemsDetailPort.loadFrontItemsDetail(
									companyId, root, authorizerAppId, limitIds));
				} else {
					mainItemInfo.putAll(
							packagePromotionFrontGoodsItemsDetailPort.loadFrontItemsDetail(
									companyId, root, authorizerAppId, null));
				}
			}
			List<Integer> marketPrices =
					mainPackagePrice.values().stream()
							.map(m -> (Integer) m.get("market_price"))
							.filter(Objects::nonNull)
							.toList();
			List<Integer> salePrices =
					mainPackagePrice.values().stream()
							.map(m -> (Integer) m.get("price"))
							.filter(Objects::nonNull)
							.toList();
			if (!marketPrices.isEmpty()) {
				mainItemInfo.put("price", Collections.min(marketPrices));
			}
			if (!salePrices.isEmpty()) {
				mainItemInfo.put("package_price", Collections.min(salePrices));
			}
		}
		info.put("mainItem", mainItemInfo);

		Map<String, Object> mainPackagePriceOut = new LinkedHashMap<>();
		for (Map.Entry<Long, LinkedHashMap<String, Object>> e : mainPackagePrice.entrySet()) {
			mainPackagePriceOut.put(String.valueOf(e.getKey()), new LinkedHashMap<>(e.getValue()));
		}
		info.put("main_package_price", mainPackagePriceOut);

		LambdaQueryWrapper<PackageItemPromotions> iw = new LambdaQueryWrapper<>();
		iw.eq(PackageItemPromotions::getPackageId, packageId)
				.eq(PackageItemPromotions::getCompanyId, companyId)
				.orderByDesc(PackageItemPromotions::getCreated)
				.last("LIMIT 1000");
		List<PackageItemPromotions> childRows = packageItemPromotionsMapper.selectList(iw);

		List<Long> childItemIds = new ArrayList<>();
		for (PackageItemPromotions rel : childRows) {
			if (rel.getItemId() != null) {
				childItemIds.add(rel.getItemId());
			}
		}

		Map<String, Object> childSkuPack =
				childItemIds.isEmpty()
						? Map.of("list", List.<Map<String, Object>>of())
						: limitPromotionAdminGoodsSupportPort.loadSkuItemsList(companyId, childItemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> childSkuRows =
				childSkuPack.get("list") instanceof List<?> l2 ? (List<Map<String, Object>>) l2 : List.of();

		Map<Long, Map<String, Object>> itemdata = new LinkedHashMap<>();
		for (Map<String, Object> row : childSkuRows) {
			Long iid = toLong(row.get("item_id"));
			if (iid != null && !itemdata.containsKey(iid)) {
				itemdata.put(iid, row);
			}
		}

		List<Map<String, Object>> relItems = new ArrayList<>();
		Map<String, LinkedHashMap<String, Object>> packagePriceOut = new LinkedHashMap<>();
		Map<Long, List<Integer>> packageMarketPriceByDefault = new LinkedHashMap<>();
		Map<Long, List<Integer>> packageSalePriceByDefault = new LinkedHashMap<>();

		for (PackageItemPromotions rel : childRows) {
			Long childItemId = rel.getItemId();
			if (childItemId == null) {
				continue;
			}
			String priceKey = String.valueOf(childItemId);
			LinkedHashMap<String, Object> bucket =
					packagePriceOut.computeIfAbsent(priceKey, k -> new LinkedHashMap<>());
			bucket.put("market_price", rel.getPrice() == null ? 0 : rel.getPrice().intValue());

			Map<String, Object> skuRow = itemdata.get(childItemId);
			if (skuRow == null || skuRow.isEmpty()) {
				continue;
			}
			relItems.add(new LinkedHashMap<>(skuRow));
			bucket.put("price", rel.getPackagePrice() == null ? 0 : rel.getPackagePrice().intValue());

			Long defaultItemId = rel.getDefaultItemId();
			if (defaultItemId != null && defaultItemId > 0L) {
				int skuMarket = intFromObject(skuRow.get("price"), 0);
				int combo = rel.getPackagePrice() == null ? 0 : rel.getPackagePrice().intValue();
				packageMarketPriceByDefault
						.computeIfAbsent(defaultItemId, k -> new ArrayList<>())
						.add(skuMarket);
				packageSalePriceByDefault
						.computeIfAbsent(defaultItemId, k -> new ArrayList<>())
						.add(combo);
			}
		}
		info.put("package_price", packagePriceOut);

		List<Map<String, Object>> itemLists = new ArrayList<>();
		if (!relItems.isEmpty()) {
			List<Map<String, Object>> groupedChild = formatPackageItemsList(relItems);
			for (Map<String, Object> v : groupedChild) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> specs =
						v.get("spec_items") instanceof List<?> raw
								? (List<Map<String, Object>>) v.get("spec_items")
								: List.of();
				boolean hasSpecs = specs != null && !specs.isEmpty();
				List<Long> limitIds = new ArrayList<>();
				if (hasSpecs) {
					for (Map<String, Object> spec : specs) {
						Long sid = toLong(spec.get("item_id"));
						if (sid != null && sid > 0L) {
							limitIds.add(sid);
						}
					}
				}
				long root = resolveRootItemId(v);
				LinkedHashMap<String, Object> itemInfo = new LinkedHashMap<>();
				if (root > 0L) {
					if (hasSpecs) {
						itemInfo.putAll(
								packagePromotionFrontGoodsItemsDetailPort.loadFrontItemsDetail(
										companyId, root, authorizerAppId, limitIds));
					} else {
						itemInfo.putAll(
								packagePromotionFrontGoodsItemsDetailPort.loadFrontItemsDetail(
										companyId, root, authorizerAppId, null));
					}
				}
				Long defId = toLong(itemInfo.get("default_item_id"));
				if (defId != null
						&& packageMarketPriceByDefault.containsKey(defId)
						&& !packageMarketPriceByDefault.get(defId).isEmpty()) {
					itemInfo.put("price", Collections.min(packageMarketPriceByDefault.get(defId)));
				}
				if (defId != null
						&& packageSalePriceByDefault.containsKey(defId)
						&& !packageSalePriceByDefault.get(defId).isEmpty()) {
					itemInfo.put("package_price", Collections.min(packageSalePriceByDefault.get(defId)));
				}
				itemLists.add(itemInfo);
			}
		}
		info.put("itemLists", itemLists);

		return info;
	}

	private List<String> parseValidGradeList(String validGrade) {
		String vg = validGrade;
		if (vg == null || !StringUtils.hasText(vg.trim())) {
			return List.of();
		}
		return Arrays.stream(vg.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
	}

	private static List<Map<String, Object>> formatPackageItemsList(List<Map<String, Object>> list) {
		if (list == null || list.isEmpty()) {
			return List.of();
		}
		Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
		for (Map<String, Object> row : list) {
			long def = toLongDef(row.get("default_item_id"));
			long actualItemId = toLongDef(row.get("item_id"));
			long itemIdKey = def > 0L ? def : actualItemId;
			if (!result.containsKey(itemIdKey)) {
				result.put(itemIdKey, new LinkedHashMap<>(row));
			}
			if (isPackageMultiSpecRow(row)) {
				Map<String, Object> main = result.get(itemIdKey);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> specs =
						(List<Map<String, Object>>) main.computeIfAbsent("spec_items", k -> new ArrayList<>());
				specs.add(row);
			}
		}
		return new ArrayList<>(result.values());
	}

	private static boolean isPackageMultiSpecRow(Map<String, Object> row) {
		Object ns = row.get("nospec");
		if (ns instanceof Boolean z) {
			return !z;
		}
		return "false".equalsIgnoreCase(String.valueOf(ns)) || "0".equals(String.valueOf(ns));
	}

	private static long resolveRootItemId(Map<String, Object> mainItem) {
		return firstPositive(toLong(mainItem.get("itemId")), toLong(mainItem.get("item_id")));
	}

	private static long firstPositive(Long a, Long b) {
		if (a != null && a > 0L) {
			return a;
		}
		if (b != null && b > 0L) {
			return b;
		}
		return 0L;
	}

	private static long toLongDef(Object o) {
		Long v = toLong(o);
		return v == null ? 0L : v;
	}

	private static Long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return null;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int intFromObject(Object o, int dflt) {
		if (o == null) {
			return dflt;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}
}
