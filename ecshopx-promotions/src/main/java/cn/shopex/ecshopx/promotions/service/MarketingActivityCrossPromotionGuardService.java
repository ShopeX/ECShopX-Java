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
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitCategoryPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.domain.PackageItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackageMainItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.dto.MarketingActivityConflictRow;
import cn.shopex.ecshopx.promotions.dto.MarketingActivityItemBindingRow;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitCategoryPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityConflictQueryMapper;
import cn.shopex.ecshopx.promotions.mapper.PackageItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackageMainItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketingActivityCrossPromotionGuardService {

	private static final List<String> BARGAIN_MARKETING_TYPES_ALL = List.of();

	private final MarketingActivityConflictQueryMapper conflictQueryMapper;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final SeckillActivityMapper seckillActivityMapper;
	private final BargainPromotionsMapper bargainPromotionsMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final PackagePromotionsMapper packagePromotionsMapper;
	private final PackageMainItemPromotionsMapper packageMainItemPromotionsMapper;
	private final PackageItemPromotionsMapper packageItemPromotionsMapper;
	private final LimitPromotionsMapper limitPromotionsMapper;
	private final LimitCategoryPromotionsMapper limitCategoryPromotionsMapper;
	private final ObjectMapper objectMapper;

	public MarketingActivityCrossPromotionGuardService(
			MarketingActivityConflictQueryMapper conflictQueryMapper,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			SeckillActivityMapper seckillActivityMapper,
			BargainPromotionsMapper bargainPromotionsMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			LimitItemPromotionsMapper limitItemPromotionsMapper,
			PackagePromotionsMapper packagePromotionsMapper,
			PackageMainItemPromotionsMapper packageMainItemPromotionsMapper,
			PackageItemPromotionsMapper packageItemPromotionsMapper,
			LimitPromotionsMapper limitPromotionsMapper,
			LimitCategoryPromotionsMapper limitCategoryPromotionsMapper,
			ObjectMapper objectMapper) {
		this.conflictQueryMapper = conflictQueryMapper;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.seckillActivityMapper = seckillActivityMapper;
		this.bargainPromotionsMapper = bargainPromotionsMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.packagePromotionsMapper = packagePromotionsMapper;
		this.packageMainItemPromotionsMapper = packageMainItemPromotionsMapper;
		this.packageItemPromotionsMapper = packageItemPromotionsMapper;
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.limitCategoryPromotionsMapper = limitCategoryPromotionsMapper;
		this.objectMapper = objectMapper;
	}

	public void checkActivityValidByLimit(Map<String, Object> params) {
		params.put("seckill_type", List.of("normal"));
		checkGroup(params);
		checkSecKill(params);
		checkBargain(params);
		checkLimitPromotionConflicts(params);
	}

	public void checkActivityValidByPackage(Map<String, Object> packageData, long packageId) {
		long companyId = readLong(packageData.get("company_id"));
		long startEpoch = epochSeconds(packageData.get("start_time"));
		long endEpoch = epochSeconds(packageData.get("end_time"));
		List<Long> childIds = readOrderedDistinctItemIdsFromRows(packageData.get("items"), "item_id");
		List<Long> mainIds = readOrderedDistinctItemIdsFromRows(packageData.get("main_items"), "item_id");
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("start_time", startEpoch);
		p.put("end_time", endEpoch);
		p.put("use_bound", 1);
		p.put("company_id", companyId);
		p.put("shop_ids", List.of());
		p.put("package_id", packageId);
		p.put("item_ids", childIds);
		p.put("main_item_ids", mainIds);
		p.put("seckill_type", List.of("normal"));
		p.put("source_id", readLong(packageData.get("source_id")));
		checkGroup(p);
		checkSecKill(p);
		checkBargain(p);
		checkPackagePromotionOverlapRules(p);
	}

	private static List<Long> readOrderedDistinctItemIdsFromRows(Object rowsObj, String key) {
		if (!(rowsObj instanceof List<?> l)) {
			return List.of();
		}
		LinkedHashSet<Long> seen = new LinkedHashSet<>();
		List<Long> out = new ArrayList<>();
		for (Object row : l) {
			if (!(row instanceof Map<?, ?> m)) {
				continue;
			}
			Object id = m.get(key);
			if (id == null) {
				continue;
			}
			try {
				long lid = id instanceof Number n ? n.longValue() : Long.parseLong(id.toString().trim());
				if (lid <= 0L) {
					continue;
				}
				if (seen.add(lid)) {
					out.add(lid);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	private void checkPackagePromotionOverlapRules(Map<String, Object> p) {
		long companyId = readLong(p.get("company_id"));
		long end = epochSeconds(p.get("end_time"));
		long startFloor = Math.max(epochSeconds(p.get("start_time")), nowEpoch());
		long excludePackageId = readLong(p.get("package_id"));
		LambdaQueryWrapper<PackagePromotions> qw = new LambdaQueryWrapper<>();
		qw.eq(PackagePromotions::getCompanyId, companyId)
				.le(PackagePromotions::getStartTime, end)
				.ge(PackagePromotions::getEndTime, startFloor);
		if (excludePackageId > 0L) {
			qw.ne(PackagePromotions::getPackageId, excludePackageId);
		}
		List<PackagePromotions> pkgs = packagePromotionsMapper.selectList(qw);
		if (pkgs == null || pkgs.isEmpty()) {
			return;
		}
		List<Long> packageIds =
				pkgs.stream().map(PackagePromotions::getPackageId).filter(Objects::nonNull).distinct().toList();
		if (packageIds.isEmpty()) {
			return;
		}
		List<Long> mainIds = readLongList(p.get("main_item_ids"));
		List<Long> childIds = readLongList(p.get("item_ids"));
		if (mainIds.isEmpty() || childIds.isEmpty()) {
			return;
		}
		List<PackageMainItemPromotions> mainHits1 =
				packageMainItemPromotionsMapper.selectList(
						new LambdaQueryWrapper<PackageMainItemPromotions>()
								.eq(PackageMainItemPromotions::getCompanyId, companyId)
								.in(PackageMainItemPromotions::getMainItemId, mainIds)
								.in(PackageMainItemPromotions::getPackageId, packageIds));
		if (mainHits1 != null && !mainHits1.isEmpty()) {
			List<Long> hitPkgIds =
					mainHits1.stream()
							.map(PackageMainItemPromotions::getPackageId)
							.filter(Objects::nonNull)
							.distinct()
							.toList();
			if (!hitPkgIds.isEmpty()) {
				long c1 =
						packageItemPromotionsMapper.selectCount(
								new LambdaQueryWrapper<PackageItemPromotions>()
										.eq(PackageItemPromotions::getCompanyId, companyId)
										.in(PackageItemPromotions::getPackageId, hitPkgIds)
										.in(PackageItemPromotions::getItemId, childIds));
				if (c1 > 0L) {
					throw new ResourceException("相同时间内已经存在组合商品促销");
				}
			}
		}
		List<PackageMainItemPromotions> mainHits2 =
				packageMainItemPromotionsMapper.selectList(
						new LambdaQueryWrapper<PackageMainItemPromotions>()
								.eq(PackageMainItemPromotions::getCompanyId, companyId)
								.in(PackageMainItemPromotions::getMainItemId, childIds)
								.in(PackageMainItemPromotions::getPackageId, packageIds));
		if (mainHits2 != null && !mainHits2.isEmpty()) {
			List<Long> hitPkgIds2 =
					mainHits2.stream()
							.map(PackageMainItemPromotions::getPackageId)
							.filter(Objects::nonNull)
							.distinct()
							.toList();
			if (!hitPkgIds2.isEmpty()) {
				long c2 =
						packageItemPromotionsMapper.selectCount(
								new LambdaQueryWrapper<PackageItemPromotions>()
										.eq(PackageItemPromotions::getCompanyId, companyId)
										.in(PackageItemPromotions::getPackageId, hitPkgIds2)
										.in(PackageItemPromotions::getItemId, mainIds));
				if (c2 > 0L) {
					throw new ResourceException("相同时间内已经存在组合商品促销");
				}
			}
		}
	}

	private void checkLimitPromotionConflicts(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		long windowEnd = epochSeconds(params.get("end_time"));
		long overlapStart = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		Long excludeLimitId =
				params.get("limit_id") instanceof Number n && n.longValue() > 0 ? n.longValue() : null;
		int useBound = toInt(params.get("use_bound"));
		List<LimitPromotions> overlapping =
				limitPromotionsMapper.selectOverlappingLimits(companyId, overlapStart, windowEnd, excludeLimitId);
		if (overlapping == null || overlapping.isEmpty()) {
			return;
		}
		Set<Long> overlappingIds =
				overlapping.stream().map(LimitPromotions::getLimitId).filter(Objects::nonNull).collect(Collectors.toSet());
		Set<Long> candidateIds = new LinkedHashSet<>();
		if (useBound == 1) {
			collectLimitConflictCandidatesUseBoundOne(params, overlapping, candidateIds);
		} else if (useBound == 2) {
			List<Long> categoryIds = readLongListFromRaw(params.get("item_category"));
			if (!categoryIds.isEmpty()) {
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsCategorySql1(
								companyId, categoryIds, overlapStart, windowEnd));
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsCategorySql2(
								companyId, categoryIds, overlapStart, windowEnd));
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsCategorySql3(
								companyId, categoryIds, overlapStart, windowEnd));
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsCategorySql4(
								companyId, categoryIds, overlapStart, windowEnd));
			}
		} else if (useBound == 3) {
			List<Long> tagIds = readLongList(params.get("tag_ids"));
			if (!tagIds.isEmpty()) {
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsTagsSql1(companyId, tagIds, overlapStart, windowEnd));
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsTagsSql2(companyId, tagIds, overlapStart, windowEnd));
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsTagsSql3(companyId, tagIds, overlapStart, windowEnd));
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsTagsSql4(companyId, tagIds, overlapStart, windowEnd));
			}
		} else if (useBound == 4) {
			List<Long> brandIds = readLongList(params.get("brand_ids"));
			if (!brandIds.isEmpty()) {
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsBrandSql1(companyId, brandIds, overlapStart, windowEnd));
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsBrandSql2(companyId, brandIds, overlapStart, windowEnd));
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsBrandSql3(companyId, brandIds, overlapStart, windowEnd));
				addAllLongs(
						candidateIds,
						limitPromotionsMapper.selectLimitIdsBrandSql4(companyId, brandIds, overlapStart, windowEnd));
			}
		}
		candidateIds.retainAll(overlappingIds);
		if (candidateIds.isEmpty()) {
			return;
		}
		for (LimitPromotions row : overlapping) {
			if (row.getLimitId() != null && candidateIds.contains(row.getLimitId())) {
				String limitName = row.getLimitName() != null ? row.getLimitName() : "";
				throw new ResourceException("相同时间内不能重复商品限购活动:" + limitName);
			}
		}
	}

	private static void addAllLongs(Set<Long> target, List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		for (Long id : ids) {
			if (id != null) {
				target.add(id);
			}
		}
	}

	private void collectLimitConflictCandidatesUseBoundOne(
			Map<String, Object> params, List<LimitPromotions> overlapping, Set<Long> candidateIds) {
		long companyId = readLong(params.get("company_id"));
		List<Long> itemIds = readLongList(params.get("item_ids"));
		if (itemIds.isEmpty()) {
			return;
		}
		List<Long> overlapIds =
				overlapping.stream().map(LimitPromotions::getLimitId).filter(Objects::nonNull).distinct().toList();
		if (overlapIds.isEmpty()) {
			return;
		}
		List<LimitItemPromotions> normalHits =
				limitItemPromotionsMapper.selectList(
						new LambdaQueryWrapper<LimitItemPromotions>()
								.eq(LimitItemPromotions::getCompanyId, companyId)
								.eq(LimitItemPromotions::getItemType, "normal")
								.in(LimitItemPromotions::getLimitId, overlapIds)
								.in(LimitItemPromotions::getItemId, itemIds));
		if (normalHits != null) {
			for (LimitItemPromotions row : normalHits) {
				if (row.getLimitId() != null) {
					candidateIds.add(row.getLimitId());
				}
			}
		}
		Map<String, Object> sku = marketingActivityCatalogAccess.loadSkuItemsList(companyId, itemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) sku.get("list");
		if (skuList == null || skuList.isEmpty()) {
			return;
		}
		List<Long> mainCatIds = new ArrayList<>();
		List<Long> brandIdsFromItems = new ArrayList<>();
		for (Map<String, Object> v : skuList) {
			Object cat = v.get("item_category");
			if (cat != null && StringUtils.hasText(cat.toString())) {
				try {
					mainCatIds.add(Long.parseLong(cat.toString().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
			Object br = v.get("brand_id");
			if (br instanceof Number n && n.longValue() > 0) {
				brandIdsFromItems.add(n.longValue());
			}
		}
		mainCatIds = mainCatIds.stream().distinct().toList();
		brandIdsFromItems = brandIdsFromItems.stream().distinct().toList();
		List<Long> defaultItemIds = new ArrayList<>();
		for (Map<String, Object> v : skuList) {
			Object def = v.get("default_item_id");
			long itemId = readLong(v.get("item_id"));
			long defId = def instanceof Number n && n.longValue() > 0 ? n.longValue() : itemId;
			defaultItemIds.add(defId);
		}
		List<Long> itemTagIds =
				marketingActivityCatalogAccess.listDistinctTagIdsByItemIds(
						companyId, defaultItemIds.stream().distinct().toList());
		if (!mainCatIds.isEmpty()) {
			List<LimitCategoryPromotions> catRows =
					limitCategoryPromotionsMapper.selectList(
							new LambdaQueryWrapper<LimitCategoryPromotions>()
									.eq(LimitCategoryPromotions::getCompanyId, companyId)
									.in(LimitCategoryPromotions::getLimitId, overlapIds)
									.in(LimitCategoryPromotions::getCategoryId, mainCatIds));
			if (catRows != null) {
				Map<Long, Integer> useByLimit =
						overlapping.stream()
								.filter(r -> r.getLimitId() != null && r.getUseBound() != null)
								.collect(Collectors.toMap(LimitPromotions::getLimitId, LimitPromotions::getUseBound, (a, b) -> a));
				for (LimitCategoryPromotions row : catRows) {
					Integer ub = useByLimit.get(row.getLimitId());
					if (ub != null && ub == 2 && row.getLimitId() != null) {
						candidateIds.add(row.getLimitId());
					}
				}
			}
		}
		if (!itemTagIds.isEmpty()) {
			for (LimitPromotions lim : overlapping) {
				if (lim.getLimitId() == null || lim.getUseBound() == null || lim.getUseBound() != 3) {
					continue;
				}
				List<Long> limTags = parseJsonLongList(lim.getTagIds());
				if (intersects(limTags, itemTagIds)) {
					candidateIds.add(lim.getLimitId());
				}
			}
		}
		if (!brandIdsFromItems.isEmpty()) {
			for (LimitPromotions lim : overlapping) {
				if (lim.getLimitId() == null || lim.getUseBound() == null || lim.getUseBound() != 4) {
					continue;
				}
				List<Long> limBrands = parseJsonLongList(lim.getBrandIds());
				if (intersects(limBrands, brandIdsFromItems)) {
					candidateIds.add(lim.getLimitId());
				}
			}
		}
	}

	private List<Long> parseJsonLongList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		try {
			List<?> parsed = objectMapper.readValue(raw.trim(), new TypeReference<List<?>>() {});
			if (parsed == null) {
				return List.of();
			}
			List<Long> out = new ArrayList<>();
			for (Object o : parsed) {
				if (o instanceof Number n) {
					out.add(n.longValue());
				} else if (o != null && StringUtils.hasText(o.toString())) {
					try {
						out.add(Long.parseLong(o.toString().trim()));
					} catch (NumberFormatException ignored) {
					}
				}
			}
			return out;
		} catch (Exception e) {
			return List.of();
		}
	}

	private static List<Long> readLongListFromRaw(Object v) {
		if (v instanceof List<?> l) {
			List<Long> out = new ArrayList<>();
			for (Object o : l) {
				if (o instanceof Number n) {
					out.add(n.longValue());
				} else if (o != null && StringUtils.hasText(o.toString())) {
					try {
						out.add(Long.parseLong(o.toString().trim()));
					} catch (NumberFormatException ignored) {
					}
				}
			}
			return out;
		}
		return List.of();
	}

	public void checkGroupActivityForMemberPreference(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		List<Long> itemIds = readLongList(params.get("item_ids"));
		long begin = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		long end = epochSeconds(params.get("end_time"));
		long c = conflictQueryMapper.countPromotionGroupOverlapForItemIds(companyId, itemIds, begin, end, null);
		if (c > 0) {
			throw new ResourceException("在相同时段内，同一个商品只能参加一个活动");
		}
	}

	public void checkBargainActivityForMemberPreference(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		List<Long> itemIds = readLongList(params.get("item_ids"));
		long begin = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		long end = epochSeconds(params.get("end_time"));
		long c = conflictQueryMapper.countBargainOverlapForItemIds(companyId, itemIds, begin, end, null);
		if (c > 0) {
			throw new ResourceException("在相同时段内，同一个商品只能参加一个活动");
		}
	}

	public void checkActivityValidByBargain(Map<String, Object> params) {
		checkMarketingForBargain(params);
		checkGroup(params);
		checkSecKill(params);
		checkBargain(params);
		checkLimitOverlapForItemIds(params);
		checkPackageOverlapForItemIds(params);
	}

	public void checkActivityValidByGroup(Map<String, Object> params) {
		checkMarketingForGroupCreate(params);
		checkGroup(params);
		checkSecKill(params);
		checkBargain(params);
		checkLimitOverlapForItemIds(params);
		checkPackageOverlapForItemIds(params);
	}

	public void checkActivityValidBySecKill(Map<String, Object> params) {
		String rawType = Objects.toString(params.get("seckill_type"), "normal").trim();
		boolean limited = "limited_time_sale".equals(rawType);
		checkMarketingForGroupCreate(params);
		if (!limited) {
			checkLimitOverlapForItemIds(params);
			checkPackageOverlapForItemIds(params);
		}
		checkGroup(params);
		checkSecKill(params);
		checkBargain(params);
	}

	private void checkMarketingForGroupCreate(Map<String, Object> params) {
		params.put("seckill_type", List.of("normal", "limited_time_sale"));
		checkMarketing(
				params,
				List.of(
						"full_discount",
						"full_minus",
						"full_gift",
						"self_select",
						"plus_price_buy",
						"member_preference"));
	}

	public void checkActivityValidByMarketing(Map<String, Object> params) {
		List<String> marketingTypes = new ArrayList<>();
		marketingTypes.add(String.valueOf(params.get("marketing_type")));
		String mt = marketingTypes.get(0);
		switch (mt) {
			case "full_minus" -> marketingTypes.add("full_discount");
			case "full_discount" -> marketingTypes.add("full_minus");
			default -> {
			}
		}
		List<String> seckillTypes = new ArrayList<>(List.of("normal", "limited_time_sale"));
		if ("member_preference".equals(mt)) {
			seckillTypes.clear();
			seckillTypes.add("normal");
		}
		params.put("seckill_type", seckillTypes);
		checkMarketing(params, marketingTypes);
		checkGroup(params);
		checkSecKill(params);
		checkBargain(params);
	}

	public void checkSeckillSearchItemNonMarketing(Map<String, Object> guardParams) {
		checkActivityValidBySecKill(new LinkedHashMap<>(guardParams));
	}

	public void checkSeckillSearchItemMarketing(Map<String, Object> guardParams) {
		checkActivityValidByMarketing(new LinkedHashMap<>(guardParams));
	}

	private void checkMarketing(Map<String, Object> params, List<String> marketingTypes) {
		long companyId = readLong(params.get("company_id"));
		long windowEnd = epochSeconds(params.get("end_time"));
		long overlapStart = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		List<Long> marketingIds = new ArrayList<>();
		List<Long> ub0 = conflictQueryMapper.selectMarketingIdsUseBound0Overlapping(
				companyId, marketingTypes, windowEnd, overlapStart);
		if (ub0 != null && !ub0.isEmpty()) {
			marketingIds.addAll(ub0);
		}
		int useBound = toInt(params.get("use_bound"));
		if (useBound == 0) {
			List<MarketingActivityItemBindingRow> rows =
					conflictQueryMapper.selectActiveMarketingItemBindings(companyId, nowEpoch());
			if (rows != null && !rows.isEmpty()) {
				List<Long> temp = rows.stream().map(MarketingActivityItemBindingRow::getMarketingId).toList();
				marketingIds.addAll(temp.stream().distinct().toList());
			}
		}
		if (useBound == 1) {
			Map<Long, ?> byItems = resolveMarketingIdsByItems(params);
			if (byItems != null && !byItems.isEmpty()) {
				marketingIds.addAll(byItems.keySet());
			}
		}
		if (useBound == 2) {
			List<?> cats = readRawList(params.get("item_category"));
			long catStart = Math.max(startForCategoryTagBrand(params), nowEpoch());
			List<Long> rs = new ArrayList<>();
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemCategoryCategoryType(cats, windowEnd, catStart)));
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemCategoryNormalItems(cats, windowEnd, catStart)));
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemCategoryBrandType(cats, windowEnd, catStart)));
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemCategoryTagType(cats, windowEnd, catStart)));
			marketingIds.addAll(rs);
		}
		if (useBound == 3) {
			List<Long> tagIds = readLongList(params.get("tag_ids"));
			long tStart = Math.max(startForCategoryTagBrand(params), nowEpoch());
			List<Long> rs = new ArrayList<>();
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemTagsTagToTag(tagIds, tStart)));
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemTagsNormalItems(tagIds, windowEnd, tStart)));
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemTagsBrandType(tagIds, windowEnd, tStart)));
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemTagsCategoryType(tagIds, windowEnd, tStart)));
			marketingIds.addAll(rs);
		}
		if (useBound == 4) {
			List<Long> brandIds = readLongList(params.get("brand_ids"));
			long bStart = Math.max(startForCategoryTagBrand(params), nowEpoch());
			List<Long> rs = new ArrayList<>();
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemBrandBrandType(brandIds)));
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemBrandNormalItems(brandIds, windowEnd, bStart)));
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemBrandTagType(brandIds, windowEnd, bStart)));
			rs.addAll(safeList(conflictQueryMapper.selectMarketingIdsByItemBrandCategoryType(brandIds, windowEnd, bStart)));
			marketingIds.addAll(rs);
		}
		List<Long> marketingIdsForFilter = marketingIds.stream().distinct().toList();
		if (marketingIdsForFilter.isEmpty()) {
			return;
		}
		List<MarketingActivityConflictRow> list = conflictQueryMapper.selectOverlappingMarketingActivities(
				companyId, marketingIdsForFilter, marketingTypes, windowEnd, overlapStart);
		long selfMarketingId = params.get("marketing_id") instanceof Number n ? n.longValue() : 0L;
		long sourceId = readLong(params.get("source_id"));
		List<Long> shopIds = readLongList(params.get("shop_ids"));
		for (MarketingActivityConflictRow v : list) {
			String errorMessage =
					"相同时间内不能重复" + nullToEmpty(v.getPromotionTag()) + "活动:" + nullToEmpty(v.getMarketingName());
			if (selfMarketingId > 0 && v.getMarketingId() != null && v.getMarketingId() == selfMarketingId) {
				continue;
			}
			if (v.getSourceId() != null && v.getSourceId() == sourceId) {
				throw new ResourceException(errorMessage);
			}
			if (shopIntersection(parseShopIds(v.getShopIds()), shopIds)) {
				throw new ResourceException(errorMessage);
			}
		}
	}

	private void checkMarketingForBargain(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		long windowEnd = epochSeconds(params.get("end_time"));
		long overlapStart = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		List<Long> marketingIds = new ArrayList<>();
		List<Long> ub0 = conflictQueryMapper.selectMarketingIdsUseBound0Overlapping(
				companyId, BARGAIN_MARKETING_TYPES_ALL, windowEnd, overlapStart);
		if (ub0 != null && !ub0.isEmpty()) {
			marketingIds.addAll(ub0);
		}
		Map<Long, ?> byItems = resolveMarketingIdsByItems(params);
		if (byItems != null && !byItems.isEmpty()) {
			marketingIds.addAll(byItems.keySet());
		}
		List<Long> marketingIdsForFilter = marketingIds.stream().distinct().toList();
		if (marketingIdsForFilter.isEmpty()) {
			return;
		}
		List<MarketingActivityConflictRow> list = conflictQueryMapper.selectOverlappingMarketingActivities(
				companyId, marketingIdsForFilter, BARGAIN_MARKETING_TYPES_ALL, windowEnd, overlapStart);
		long selfMarketingId = params.get("marketing_id") instanceof Number n ? n.longValue() : 0L;
		long sourceId = readLong(params.get("source_id"));
		List<Long> shopIds = readLongList(params.get("shop_ids"));
		for (MarketingActivityConflictRow v : list) {
			String errorMessage =
					"相同时间内不能重复" + nullToEmpty(v.getPromotionTag()) + "活动:" + nullToEmpty(v.getMarketingName());
			if (selfMarketingId > 0 && v.getMarketingId() != null && v.getMarketingId() == selfMarketingId) {
				continue;
			}
			if (v.getSourceId() != null && v.getSourceId() == sourceId) {
				throw new ResourceException(errorMessage);
			}
			if (shopIntersection(parseShopIds(v.getShopIds()), shopIds)) {
				throw new ResourceException(errorMessage);
			}
		}
	}

	private long startForCategoryTagBrand(Map<String, Object> params) {
		return epochSeconds(params.get("start_time"));
	}

	private static List<Long> safeList(List<Long> l) {
		return l == null ? List.of() : l;
	}

	private Map<Long, ?> resolveMarketingIdsByItems(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		List<Long> itemIds = readLongList(params.get("item_ids"));
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		Map<String, Object> sku = marketingActivityCatalogAccess.loadSkuItemsList(companyId, itemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) sku.get("list");
		if (list == null || list.isEmpty()) {
			return Map.of();
		}
		List<Long> defaultItemIds = new ArrayList<>();
		Set<String> mainCats = new LinkedHashSet<>();
		Set<Long> brandIds = new LinkedHashSet<>();
		Map<String, List<Long>> itemInfo = new LinkedHashMap<>();
		itemInfo.put("normal", new ArrayList<>());
		for (Map<String, Object> v : list) {
			long itemId = readLong(v.get("item_id"));
			itemInfo.get("normal").add(itemId);
			Object def = v.get("default_item_id");
			long defId = def instanceof Number n && n.longValue() > 0 ? n.longValue() : itemId;
			defaultItemIds.add(defId);
			String cat = v.get("item_category") != null ? v.get("item_category").toString() : "";
			if (StringUtils.hasText(cat)) {
				mainCats.add(cat.trim());
			}
			Object br = v.get("brand_id");
			if (br instanceof Number n && n.longValue() > 0) {
				brandIds.add(n.longValue());
			}
		}
		List<Long> tagIds = marketingActivityCatalogAccess.listDistinctTagIdsByItemIds(
				companyId, defaultItemIds.stream().distinct().toList());
		List<MarketingActivityItemBindingRow> merged = new ArrayList<>();
		long nowSec = nowEpoch();
		merged.addAll(conflictQueryMapper.selectPromotionItemBindings(companyId, itemIds, "normal", nowSec));
		if (!tagIds.isEmpty()) {
			merged.addAll(conflictQueryMapper.selectPromotionItemBindings(companyId, tagIds, "tag", nowSec));
		}
		if (!brandIds.isEmpty()) {
			merged.addAll(
					conflictQueryMapper.selectPromotionItemBindings(
							companyId, new ArrayList<>(brandIds), "brand", nowSec));
		}
		if (!mainCats.isEmpty()) {
			List<Long> catIds = new ArrayList<>();
			for (String c : mainCats) {
				try {
					catIds.add(Long.parseLong(c));
				} catch (NumberFormatException ignored) {
				}
			}
			if (!catIds.isEmpty()) {
				merged.addAll(conflictQueryMapper.selectPromotionItemBindings(companyId, catIds, "category", nowSec));
			}
		}
		Map<Long, MarketingActivityItemBindingRow> res = new LinkedHashMap<>();
		for (MarketingActivityItemBindingRow row : merged) {
			if (row.getMarketingId() != null) {
				res.putIfAbsent(row.getMarketingId(), row);
			}
		}
		return res;
	}

	private void checkGroup(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		long end = epochSeconds(params.get("end_time"));
		long startFloor = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		LambdaQueryWrapper<PromotionGroupsActivity> w = new LambdaQueryWrapper<>();
		w.eq(PromotionGroupsActivity::getCompanyId, companyId)
				.eq(PromotionGroupsActivity::getDisabled, false)
				.le(PromotionGroupsActivity::getBeginTime, end)
				.ge(PromotionGroupsActivity::getEndTime, startFloor);
		Object gidObj = params.get("groups_activity_id");
		if (gidObj instanceof Number n && n.longValue() > 0L) {
			w.ne(PromotionGroupsActivity::getGroupsActivityId, n.longValue());
		}
		List<PromotionGroupsActivity> rs = promotionGroupsActivityMapper.selectList(w);
		if (rs == null || rs.isEmpty()) {
			return;
		}
		List<Long> goodsIds = rs.stream().map(PromotionGroupsActivity::getGoodsId).filter(Objects::nonNull).toList();
		String errorMessage = "相同时间内已经存在团购活动";
		int useBound = toInt(params.get("use_bound"));
		if (useBound == 0) {
			List<Long> shopIds = readLongList(params.get("shop_ids"));
			if (shopIds.isEmpty() || (shopIds.size() == 1 && shopIds.get(0) == 0)) {
				throw new ResourceException(errorMessage);
			}
		}
		if (useBound == 1) {
			List<Long> itemIds = readLongList(params.get("item_ids"));
			long c = conflictQueryMapper.countItemsByGoodsIdsAndItemIds(companyId, itemIds, goodsIds);
			if (c > 0) {
				throw new ResourceException(errorMessage);
			}
		}
		if (useBound == 2) {
			List<?> cats = readRawList(params.get("item_category"));
			long c = conflictQueryMapper.countItemsByItemCategoryAndGoodsIds(companyId, cats, goodsIds);
			if (c > 0) {
				throw new ResourceException(errorMessage);
			}
		}
		if (useBound == 3) {
			List<Long> tagIds = readLongList(params.get("tag_ids"));
			List<Long> itemIdsFromGoods = new ArrayList<>();
			for (Long gid : goodsIds) {
				List<Long> iids = marketingActivityCatalogAccess.listItemIdsByGoodsId(companyId, gid);
				itemIdsFromGoods.addAll(iids);
			}
			itemIdsFromGoods = itemIdsFromGoods.stream().distinct().toList();
			if (itemIdsFromGoods.isEmpty()) {
				return;
			}
			List<Long> tagHit = conflictQueryMapper.selectDistinctTagIdsByItemIds(companyId, itemIdsFromGoods);
			if (intersects(tagHit, tagIds)) {
				throw new ResourceException(errorMessage);
			}
		}
		if (useBound == 4) {
			List<Long> brandIds = readLongList(params.get("brand_ids"));
			long c = conflictQueryMapper.countItemsByBrandIdsAndGoodsIds(companyId, brandIds, goodsIds);
			if (c > 0) {
				throw new ResourceException(errorMessage);
			}
		}
	}

	private void checkSecKill(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		long end = epochSeconds(params.get("end_time"));
		long startFloor = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		@SuppressWarnings("unchecked")
		List<String> seckillTypes = (List<String>) params.get("seckill_type");
		LambdaQueryWrapper<SeckillActivity> w = new LambdaQueryWrapper<>();
		w.eq(SeckillActivity::getCompanyId, companyId)
				.eq(SeckillActivity::getDisabled, false)
				.le(SeckillActivity::getActivityStartTime, end)
				.ge(SeckillActivity::getActivityEndTime, startFloor);
		if (seckillTypes != null && !seckillTypes.isEmpty()) {
			w.in(SeckillActivity::getSeckillType, seckillTypes);
		}
		long excludeSeckillId = readLongOrZero(params.get("exclude_seckill_id"));
		if (excludeSeckillId > 0L) {
			w.ne(SeckillActivity::getSeckillId, excludeSeckillId);
		}
		List<SeckillActivity> rs = seckillActivityMapper.selectList(w);
		if (rs == null || rs.isEmpty()) {
			return;
		}
		Map<String, List<Long>> secKillIds = new LinkedHashMap<>();
		Map<String, List<String>> secKillNames = new LinkedHashMap<>();
		for (SeckillActivity v : rs) {
			String st = v.getSeckillType() != null ? v.getSeckillType() : "normal";
			secKillIds.computeIfAbsent(st, k -> new ArrayList<>()).add(v.getSeckillId());
			secKillNames.computeIfAbsent(st, k -> new ArrayList<>()).add(v.getActivityName());
		}
		Map<String, String> seckillTypeConf = Map.of("normal", "限时秒杀", "limited_time_sale", "限时特惠");
		int useBound = toInt(params.get("use_bound"));
		long sourceId = readLong(params.get("source_id"));
		if (useBound == 0) {
			String errorMessage = "相同时间内已经存在";
			for (SeckillActivity v : rs) {
				if (v.getSourceId() != null && v.getSourceId() == sourceId) {
					String msg = errorMessage + seckillTypeConf.getOrDefault(v.getSeckillType(), "") + ":" + v.getActivityName();
					throw new ResourceException(msg);
				}
				if (distributorListContains(v.getDistributorId(), sourceId)) {
					String msg = errorMessage + seckillTypeConf.getOrDefault(v.getSeckillType(), "") + ":" + v.getActivityName();
					throw new ResourceException(msg);
				}
			}
		}
		for (Map.Entry<String, List<Long>> e : secKillIds.entrySet()) {
			String seckillType = e.getKey();
			List<Long> ids = e.getValue().stream().filter(Objects::nonNull).distinct().toList();
			if (ids.isEmpty()) {
				continue;
			}
			String typeName = seckillTypeConf.getOrDefault(seckillType, "");
			String errorMessage = "相同时间内已经存在" + typeName;
			List<String> names = secKillNames.getOrDefault(seckillType, List.of());
			errorMessage += "：" + String.join(",", names);
			List<Long> secKillItemIds = selectSeckillItemIdsBySeckillIds(companyId, ids);
			checkValidByItemIds(secKillItemIds, params, errorMessage);
		}
	}

	private void checkBargain(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		long end = epochSeconds(params.get("end_time"));
		long startFloor = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		LambdaQueryWrapper<BargainPromotions> w = new LambdaQueryWrapper<>();
		w.eq(BargainPromotions::getCompanyId, companyId)
				.le(BargainPromotions::getBeginTime, end)
				.ge(BargainPromotions::getEndTime, startFloor);
		Object bidObj = params.get("bargain_id");
		if (bidObj instanceof Number n && n.longValue() > 0L) {
			w.ne(BargainPromotions::getBargainId, n.longValue());
		}
		List<BargainPromotions> rs = bargainPromotionsMapper.selectList(w);
		if (rs == null || rs.isEmpty()) {
			return;
		}
		String errorMessage = "相同时间内已经存在助力砍价活动";
		int useBound = toInt(params.get("use_bound"));
		if (useBound == 0) {
			List<Long> shopIds = readLongList(params.get("shop_ids"));
			if (shopIds.isEmpty() || (shopIds.size() == 1 && shopIds.get(0) == 0)) {
				throw new ResourceException(errorMessage);
			}
		}
		List<Long> bargainItemIds = new ArrayList<>();
		for (BargainPromotions v : rs) {
			if (v.getItemId() != null && StringUtils.hasText(v.getItemId())) {
				try {
					bargainItemIds.add(Long.parseLong(v.getItemId().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		checkValidByItemIds(bargainItemIds.stream().distinct().toList(), params, errorMessage);
	}

	private void checkLimitOverlapForItemIds(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		long windowEnd = epochSeconds(params.get("end_time"));
		long overlapStart = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		List<Long> itemIds = readLongList(params.get("item_ids"));
		if (itemIds.isEmpty()) {
			return;
		}
		List<LimitItemPromotions> list =
				limitItemPromotionsMapper.selectList(
						new LambdaQueryWrapper<LimitItemPromotions>()
								.eq(LimitItemPromotions::getCompanyId, companyId)
								.eq(LimitItemPromotions::getItemType, "normal")
								.in(LimitItemPromotions::getItemId, itemIds)
								.le(LimitItemPromotions::getStartTime, windowEnd)
								.ge(LimitItemPromotions::getEndTime, overlapStart));
		if (list == null || list.isEmpty()) {
			return;
		}
		LimitItemPromotions row = list.get(0);
		String suffix = row.getItemName() != null ? row.getItemName() : "";
		throw new ResourceException("相同时间内不能重复商品限购活动:" + suffix);
	}

	private void checkPackageOverlapForItemIds(Map<String, Object> params) {
		long companyId = readLong(params.get("company_id"));
		long windowEnd = epochSeconds(params.get("end_time"));
		long overlapStart = Math.max(epochSeconds(params.get("start_time")), nowEpoch());
		List<Long> itemIds = readLongList(params.get("item_ids"));
		if (itemIds.isEmpty()) {
			return;
		}
		List<PackagePromotions> pkgs =
				packagePromotionsMapper.selectList(
						new LambdaQueryWrapper<PackagePromotions>()
								.eq(PackagePromotions::getCompanyId, companyId)
								.le(PackagePromotions::getStartTime, windowEnd)
								.ge(PackagePromotions::getEndTime, overlapStart));
		if (pkgs == null || pkgs.isEmpty()) {
			return;
		}
		List<Long> pkgIds = pkgs.stream().map(PackagePromotions::getPackageId).filter(Objects::nonNull).distinct().toList();
		if (pkgIds.isEmpty()) {
			return;
		}
		long mainHit =
				packageMainItemPromotionsMapper.selectCount(
						new LambdaQueryWrapper<PackageMainItemPromotions>()
								.eq(PackageMainItemPromotions::getCompanyId, companyId)
								.in(PackageMainItemPromotions::getPackageId, pkgIds)
								.in(PackageMainItemPromotions::getMainItemId, itemIds));
		long subHit =
				packageItemPromotionsMapper.selectCount(
						new LambdaQueryWrapper<PackageItemPromotions>()
								.eq(PackageItemPromotions::getCompanyId, companyId)
								.in(PackageItemPromotions::getPackageId, pkgIds)
								.in(PackageItemPromotions::getItemId, itemIds));
		if (mainHit > 0L || subHit > 0L) {
			throw new ResourceException("相同时间内已经存在组合商品促销");
		}
	}

	private void checkValidByItemIds(List<Long> checkItemIds, Map<String, Object> params, String errorMessage) {
		if (checkItemIds == null || checkItemIds.isEmpty()) {
			return;
		}
		long companyId = readLong(params.get("company_id"));
		int useBound = toInt(params.get("use_bound"));
		if (useBound == 1) {
			List<Long> itemIds = readLongList(params.get("item_ids"));
			if (intersectsLong(itemIds, checkItemIds)) {
				throw new ResourceException(errorMessage);
			}
		}
		if (useBound == 2) {
			List<?> cats = readRawList(params.get("item_category"));
			long c = conflictQueryMapper.countItemsByItemCategoryAndItemIds(companyId, cats, checkItemIds);
			if (c > 0) {
				throw new ResourceException(errorMessage);
			}
		}
		if (useBound == 3) {
			List<Long> tagIds = readLongList(params.get("tag_ids"));
			List<Long> rs = conflictQueryMapper.selectDistinctTagIdsByItemIds(companyId, checkItemIds);
			if (intersects(rs, tagIds)) {
				throw new ResourceException(errorMessage);
			}
		}
		if (useBound == 4) {
			List<Long> brandIds = readLongList(params.get("brand_ids"));
			long c = conflictQueryMapper.countItemsByBrandIdsAndItemIds(companyId, brandIds, checkItemIds);
			if (c > 0) {
				throw new ResourceException(errorMessage);
			}
		}
	}

	private static boolean intersectsLong(List<Long> a, List<Long> b) {
		Set<Long> bs = new HashSet<>(b);
		for (Long x : a) {
			if (x != null && bs.contains(x)) {
				return true;
			}
		}
		return false;
	}

	private static boolean intersects(List<Long> a, List<Long> b) {
		Set<Long> bs = new HashSet<>(b);
		for (Long x : a) {
			if (x != null && bs.contains(x)) {
				return true;
			}
		}
		return false;
	}

	private static boolean shopIntersection(List<Long> a, List<Long> b) {
		Set<Long> bs = new HashSet<>(b);
		for (Long x : a) {
			if (x != null && bs.contains(x)) {
				return true;
			}
		}
		return false;
	}

	private static List<Long> parseShopIds(String raw) {
		if (!StringUtils.hasText(raw) || "all".equalsIgnoreCase(raw.trim())) {
			return List.of();
		}
		String s = raw.trim();
		if (s.startsWith(",")) {
			s = s.substring(1);
		}
		if (s.endsWith(",")) {
			s = s.substring(0, s.length() - 1);
		}
		if (!StringUtils.hasText(s)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String p : s.split(",")) {
			if (StringUtils.hasText(p)) {
				try {
					out.add(Long.parseLong(p.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return out;
	}

	private List<Long> selectSeckillItemIdsBySeckillIds(long companyId, List<Long> seckillIds) {
		if (seckillIds == null || seckillIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<SeckillRelGoods> w = new LambdaQueryWrapper<>();
		w.eq(SeckillRelGoods::getCompanyId, companyId)
				.in(SeckillRelGoods::getSeckillId, seckillIds)
				.and(q -> q.eq(SeckillRelGoods::getDisabled, false).or().isNull(SeckillRelGoods::getDisabled));
		List<SeckillRelGoods> rows = seckillRelGoodsMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		return rows.stream()
				.map(SeckillRelGoods::getItemId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
	}

	private static long nowEpoch() {
		return System.currentTimeMillis() / 1000L;
	}

	private static long epochSeconds(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return (long) Double.parseDouble(String.valueOf(v).trim());
	}

	private static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static long readLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			String s = String.valueOf(v).trim();
			if (!StringUtils.hasText(s)) {
				return 0L;
			}
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return (int) Double.parseDouble(String.valueOf(v).trim());
	}

	private static List<Long> readLongList(Object v) {
		if (!(v instanceof List<?> l)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object o : l) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o != null && StringUtils.hasText(o.toString())) {
				try {
					out.add(Long.parseLong(o.toString().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return out;
	}

	private static List<?> readRawList(Object v) {
		if (v instanceof List<?> l) {
			return l;
		}
		return List.of();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static boolean distributorListContains(String distributorField, long sourceId) {
		if (!StringUtils.hasText(distributorField)) {
			return false;
		}
		for (String p : distributorField.split(",")) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				if (Long.parseLong(p.trim()) == sourceId) {
					return true;
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return false;
	}
}
