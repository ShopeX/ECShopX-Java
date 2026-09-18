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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsActivityAdminRowAssembler;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsTeamItemsListService;
import cn.shopex.ecshopx.promotions.service.SeckillActivityCreateService;
import cn.shopex.ecshopx.promotions.service.wxapp.WxappSeckillActivityItemStoreReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsItemsDetailPromotionActivityService {

	private static final Logger log = LoggerFactory.getLogger(WxappGoodsItemsDetailPromotionActivityService.class);

	private final ItemsRepository itemsRepository;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final SeckillActivityMapper seckillActivityMapper;
	private final WxappSeckillActivityItemStoreReadService wxappSeckillActivityItemStoreReadService;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;
	private final PromotionGroupsTeamItemsListService promotionGroupsTeamItemsListService;
	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final LimitPromotionsMapper limitPromotionsMapper;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final SeckillActivityCreateService seckillActivityCreateService;
	private final PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler;
	private final ObjectMapper objectMapper;

	public WxappGoodsItemsDetailPromotionActivityService(ItemsRepository itemsRepository, SeckillRelGoodsMapper seckillRelGoodsMapper,
			SeckillActivityMapper seckillActivityMapper, WxappSeckillActivityItemStoreReadService wxappSeckillActivityItemStoreReadService,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper,
			PromotionGroupsTeamItemsListService promotionGroupsTeamItemsListService,
			LimitItemPromotionsMapper limitItemPromotionsMapper,
			LimitPromotionsMapper limitPromotionsMapper, ItemsRelTagsRepository itemsRelTagsRepository,
			SeckillActivityCreateService seckillActivityCreateService,
			PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.seckillActivityMapper = seckillActivityMapper;
		this.wxappSeckillActivityItemStoreReadService = wxappSeckillActivityItemStoreReadService;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionGroupsRelGoodsMapper = promotionGroupsRelGoodsMapper;
		this.promotionGroupsTeamItemsListService = promotionGroupsTeamItemsListService;
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.seckillActivityCreateService = seckillActivityCreateService;
		this.promotionGroupsActivityAdminRowAssembler = promotionGroupsActivityAdminRowAssembler;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getCurrentActivityByItemId(long companyId, long itemId, long distributorId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		Items p1 = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (p1 == null) {
			return null;
		}
		List<Long> candidateItemIds = resolveCandidateItemIds(p1, companyId, itemId);

		Map<String, Object> seckillData = trySeckillBranch(companyId, p1, candidateItemIds, distributorId, now);
		if (seckillData != null) {
			return seckillData;
		}
		Map<String, Object> groupData = tryGroupBranch(companyId, p1, candidateItemIds, now);
		if (groupData != null) {
			return groupData;
		}
		Map<String, Object> limitData = tryLimitedBuyBranch(companyId, p1, candidateItemIds, distributorId, now);
		if (limitData != null) {
			return limitData;
		}
		return null;
	}

	private List<Long> resolveCandidateItemIds(Items p1, long companyId, long p1ItemId) {
		Object nospec = p1.getNospec();
		boolean multi = isMultiSpec(nospec);
		Long def = p1.getDefaultItemId();
		if (multi && def != null && def > 0L) {
			List<Items> list = itemsRepository.listByDefaultItemIdAndCompany(def, companyId);
			List<Long> ids = new ArrayList<>();
			for (Items it : list) {
				if (it.getItemId() != null) {
					ids.add(it.getItemId());
				}
			}
			if (!ids.isEmpty()) {
				return ids;
			}
		}
		return List.of(p1ItemId);
	}

	private static boolean isMultiSpec(Object nospec) {
		if (nospec == null) {
			return false;
		}
		if (nospec instanceof Boolean b) {
			return !b;
		}
		String s = nospec.toString().trim();
		return !"true".equalsIgnoreCase(s) && !"1".equals(s);
	}

	private Map<String, Object> trySeckillBranch(long companyId, Items p1, List<Long> candidateItemIds, long distributorId, int now) {
		LambdaQueryWrapper<SeckillRelGoods> w = new LambdaQueryWrapper<>();
		w.eq(SeckillRelGoods::getCompanyId, companyId)
				.in(SeckillRelGoods::getItemId, candidateItemIds)
				.and(x -> x.eq(SeckillRelGoods::getDisabled, false).or().isNull(SeckillRelGoods::getDisabled))
				.le(SeckillRelGoods::getActivityReleaseTime, now)
				.ge(SeckillRelGoods::getActivityEndTime, now)
				.orderByAsc(SeckillRelGoods::getId);
		List<SeckillRelGoods> seckillList = seckillRelGoodsMapper.selectList(w);
		if (seckillList == null || seckillList.isEmpty()) {
			return null;
		}
		Map<String, Map<String, Object>> listMap = new LinkedHashMap<>();
		Long totalStore = null;
		long lastSeckillId = 0L;
		Integer basePrice = p1.getPrice();
		for (SeckillRelGoods row : seckillList) {
			long sid = row.getSeckillId() != null ? row.getSeckillId() : 0L;
			lastSeckillId = sid;
			long iid = row.getItemId() != null ? row.getItemId() : 0L;
			String key = String.valueOf(iid);
			Map<String, Object> cell = new LinkedHashMap<>();
			cell.put("seckill_id", sid);
			cell.put("item_id", iid);
			cell.put("limit_num", row.getLimitNum() != null ? row.getLimitNum() : 0);
			cell.put("sales_store", row.getSalesStore() != null ? row.getSalesStore() : 0);
			cell.put("price", basePrice != null ? basePrice : 0);
			cell.put("activity_price", row.getActivityPrice() != null ? row.getActivityPrice() : 0);
			if ("normal".equals(row.getSeckillType())) {
				long st = wxappSeckillActivityItemStoreReadService.resolveTotalStoreForSeckillNormal(companyId, iid, sid, now);
				cell.put("store", st);
				if (log.isDebugEnabled()) {
					log.debug("seckillList itemId={} seckillId={} store={}", iid, sid, st);
				}
				totalStore = (totalStore == null ? 0L : totalStore) + st;
			}
			listMap.put(key, cell);
		}
		if (log.isDebugEnabled()) {
			log.debug("totalStore:{}", totalStore);
		}
		SeckillActivity act = lastSeckillId > 0 ? seckillActivityMapper.selectById(lastSeckillId) : null;
		if (act == null) {
			return null;
		}
		Map<String, Object> info = seckillActivityCreateService.buildGoodsDetailSeckillActivityInfo(act);
		long itemDist = p1.getDistributorId() != null ? p1.getDistributorId().longValue() : 0L;
		long infoSource = act.getSourceId() != null ? act.getSourceId() : 0L;
		if (itemDist != infoSource) {
			return null;
		}
		String distCsv = act.getDistributorId();
		if (StringUtils.hasText(distCsv) && distributorId > 0) {
			boolean allowed = false;
			for (String p : distCsv.split(",")) {
				String t = p.trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					if (Long.parseLong(t) == distributorId) {
						allowed = true;
						break;
					}
				} catch (NumberFormatException ignored) {
				}
			}
			if (!allowed) {
				return null;
			}
		}
		Map<String, Object> activityData = new LinkedHashMap<>();
		activityData.put("list", listMap);
		activityData.put("info", info);
		if ("normal".equals(act.getSeckillType())) {
			activityData.put("activity_type", "seckill");
		} else {
			activityData.put("activity_type", "limited_time_sale");
		}
		if (totalStore != null) {
			info.put("item_total_store", totalStore);
		}
		return activityData;
	}

	private Map<String, Object> tryGroupBranch(long companyId, Items p1, List<Long> candidateItemIds, int now) {
		Map<String, Object> viaRel = tryGroupBranchFromRelGoods(companyId, p1, candidateItemIds, now);
		if (viaRel != null) {
			return viaRel;
		}
		return tryGroupBranchFromActivityGoodsId(companyId, p1, candidateItemIds, now);
	}

	private Map<String, Object> tryGroupBranchFromRelGoods(long companyId, Items p1, List<Long> candidateItemIds, int now) {
		long nowSec = now;
		LambdaQueryWrapper<PromotionGroupsRelGoods> relW = new LambdaQueryWrapper<>();
		relW.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
				.in(PromotionGroupsRelGoods::getItemId, candidateItemIds);
		List<PromotionGroupsRelGoods> hitRels = promotionGroupsRelGoodsMapper.selectList(relW);
		if (hitRels == null || hitRels.isEmpty()) {
			return null;
		}
		Set<Long> actIds = hitRels.stream()
				.map(PromotionGroupsRelGoods::getGroupsActivityId)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (actIds.isEmpty()) {
			return null;
		}
		LambdaQueryWrapper<PromotionGroupsActivity> actW = new LambdaQueryWrapper<>();
		actW.eq(PromotionGroupsActivity::getCompanyId, companyId)
				.eq(PromotionGroupsActivity::getDisabled, false)
				.in(PromotionGroupsActivity::getGroupsActivityId, actIds)
				.le(PromotionGroupsActivity::getBeginTime, nowSec)
				.ge(PromotionGroupsActivity::getEndTime, nowSec)
				.orderByDesc(PromotionGroupsActivity::getCreated);
		List<PromotionGroupsActivity> validActs = promotionGroupsActivityMapper.selectList(actW);
		if (validActs == null || validActs.isEmpty()) {
			return null;
		}
		PromotionGroupsActivity activity = validActs.get(0);
		Long actId = activity.getGroupsActivityId();
		if (actId == null) {
			return null;
		}
		LambdaQueryWrapper<PromotionGroupsRelGoods> allRelW = new LambdaQueryWrapper<>();
		allRelW.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
				.eq(PromotionGroupsRelGoods::getGroupsActivityId, actId)
				.orderByAsc(PromotionGroupsRelGoods::getItemId);
		List<PromotionGroupsRelGoods> allRels = promotionGroupsRelGoodsMapper.selectList(allRelW);
		if (allRels == null || allRels.isEmpty()) {
			return null;
		}
		Set<Long> candidateSet = new LinkedHashSet<>(candidateItemIds);
		List<PromotionGroupsRelGoods> relsForList = allRels.stream()
				.filter(rel -> rel.getItemId() != null && candidateSet.contains(rel.getItemId()))
				.collect(Collectors.toList());
		if (relsForList.isEmpty()) {
			return null;
		}
		Map<Long, Integer> priceByItemId = buildGroupItemPriceMap(companyId, relsForList);
		Integer fallbackPrice = p1.getPrice();
		Map<String, Map<String, Object>> listMap = new LinkedHashMap<>();
		long totalStore = 0L;
		long limitNum = activity.getLimitBuyNum() != null ? activity.getLimitBuyNum() : 0L;
		for (PromotionGroupsRelGoods rel : relsForList) {
			long iid = rel.getItemId();
			String key = String.valueOf(iid);
			Map<String, Object> cell = new LinkedHashMap<>();
			cell.put("item_id", iid);
			cell.put("limit_num", limitNum);
			cell.put("price", priceByItemId.getOrDefault(iid, fallbackPrice != null ? fallbackPrice : 0));
			cell.put("activity_price", rel.getActivityPrice() != null ? rel.getActivityPrice() : 0L);
			long st = rel.getActivityStore() != null ? rel.getActivityStore() : 0L;
			cell.put("store", st);
			totalStore += st;
			listMap.put(key, cell);
		}
		return buildGroupActivityResponse(companyId, activity, listMap, totalStore, now);
	}

	private Map<String, Object> tryGroupBranchFromActivityGoodsId(long companyId, Items p1, List<Long> candidateItemIds, int now) {
		long nowSec = now;
		LambdaQueryWrapper<PromotionGroupsActivity> w = new LambdaQueryWrapper<>();
		w.eq(PromotionGroupsActivity::getCompanyId, companyId)
				.eq(PromotionGroupsActivity::getDisabled, false)
				.in(PromotionGroupsActivity::getGoodsId, candidateItemIds)
				.le(PromotionGroupsActivity::getBeginTime, nowSec)
				.ge(PromotionGroupsActivity::getEndTime, nowSec)
				.orderByDesc(PromotionGroupsActivity::getCreated);
		List<PromotionGroupsActivity> groupList = promotionGroupsActivityMapper.selectList(w);
		if (groupList == null || groupList.isEmpty()) {
			return null;
		}
		Map<String, Map<String, Object>> listMap = new LinkedHashMap<>();
		long totalStore = 0L;
		Integer basePrice = p1.getPrice();
		for (PromotionGroupsActivity row : groupList) {
			long gid = row.getGoodsId() != null ? row.getGoodsId() : 0L;
			String key = String.valueOf(gid);
			Map<String, Object> cell = new LinkedHashMap<>();
			cell.put("item_id", gid);
			cell.put("limit_num", row.getLimitBuyNum() != null ? row.getLimitBuyNum() : 0L);
			cell.put("price", basePrice != null ? basePrice : 0);
			cell.put("activity_price", row.getActPrice() != null ? row.getActPrice() : 0L);
			long st = row.getStore() != null ? row.getStore() : 0L;
			cell.put("store", st);
			totalStore += st;
			listMap.put(key, cell);
		}
		return buildGroupActivityResponse(companyId, groupList.get(0), listMap, totalStore, now);
	}

	private Map<Long, Integer> buildGroupItemPriceMap(long companyId, List<PromotionGroupsRelGoods> rels) {
		List<Long> itemIds = rels.stream()
				.map(PromotionGroupsRelGoods::getItemId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());
		if (itemIds.isEmpty()) {
			return Map.of();
		}
		List<Items> items = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, itemIds);
		Map<Long, Integer> priceByItemId = new LinkedHashMap<>();
		if (items == null) {
			return priceByItemId;
		}
		for (Items it : items) {
			if (it.getItemId() != null && it.getPrice() != null) {
				priceByItemId.put(it.getItemId(), it.getPrice());
			}
		}
		return priceByItemId;
	}

	private Map<String, Object> buildGroupActivityResponse(long companyId, PromotionGroupsActivity activity,
			Map<String, Map<String, Object>> listMap, long totalStore, int now) {
		Map<String, Object> info = promotionGroupsActivityAdminRowAssembler.toRow(activity, now);
		info.put("item_total_store", totalStore);
		Map<String, Object> activityData = new LinkedHashMap<>();
		activityData.put("activity_type", "group");
		activityData.put("list", listMap);
		activityData.put("info", info);
		if (Boolean.TRUE.equals(activity.getRigUp())) {
			Long actId = activity.getGroupsActivityId();
			List<Map<String, Object>> teams = loadGroupsTeams(companyId, actId);
			activityData.put("groups_list", teams);
		}
		return activityData;
	}

	private List<Map<String, Object>> loadGroupsTeams(long companyId, Long actId) {
		if (actId == null) {
			return List.of();
		}
		return promotionGroupsTeamItemsListService.getGroupsTeamByItems(companyId, actId, 1, 4);
	}

	private Map<String, Object> tryLimitedBuyBranch(long companyId, Items p1, List<Long> candidateItemIds, long distributorId, int now) {
		List<LimitItemPromotions> limitItems = new ArrayList<>();
		List<LimitItemPromotions> normalRows =
				limitItemPromotionsMapper.selectWxappActiveLimitItemsForDetail(companyId, candidateItemIds, distributorId, now);
		if (normalRows != null) {
			limitItems.addAll(normalRows);
		}
		List<Items> itemRows = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, candidateItemIds);
		if (itemRows == null) {
			itemRows = List.of();
		}
		Set<Long> categoryIds = new LinkedHashSet<>();
		Set<Long> brandIds = new LinkedHashSet<>();
		Set<Long> defaultItemIdsForTags = new LinkedHashSet<>();
		for (Items it : itemRows) {
			Long catId = parsePositiveLongOrNull(it.getItemCategory());
			if (catId != null) {
				categoryIds.add(catId);
			}
			Integer bid = it.getBrandId();
			if (bid != null && bid > 0) {
				brandIds.add(bid.longValue());
			}
			Long def = it.getDefaultItemId();
			if (def != null && def > 0L) {
				defaultItemIdsForTags.add(def);
			}
		}
		if (!categoryIds.isEmpty()) {
			List<LimitItemPromotions> catRows = limitItemPromotionsMapper.selectWxappActiveLimitItemsForDetailByType(companyId,
					new ArrayList<>(categoryIds), "category", distributorId, now);
			if (catRows != null) {
				limitItems.addAll(catRows);
			}
		}
		Set<Long> tagIds = new LinkedHashSet<>();
		if (!defaultItemIdsForTags.isEmpty()) {
			for (ItemsRelTags rel : itemsRelTagsRepository.listRowsByCompanyIdAndItemIdIn(companyId, defaultItemIdsForTags)) {
				if (rel.getTagId() != null) {
					tagIds.add(rel.getTagId());
				}
			}
		}
		if (!tagIds.isEmpty()) {
			List<LimitItemPromotions> tagRows = limitItemPromotionsMapper.selectWxappActiveLimitItemsForDetailByType(companyId,
					new ArrayList<>(tagIds), "tag", distributorId, now);
			if (tagRows != null) {
				limitItems.addAll(tagRows);
			}
		}
		if (!brandIds.isEmpty()) {
			List<LimitItemPromotions> brandRows = limitItemPromotionsMapper.selectWxappActiveLimitItemsForDetailByType(companyId,
					new ArrayList<>(brandIds), "brand", distributorId, now);
			if (brandRows != null) {
				limitItems.addAll(brandRows);
			}
		}
		if (limitItems.isEmpty()) {
			return null;
		}
		Map<String, LimitItemPromotions> limitItemInfo = new LinkedHashMap<>();
		for (LimitItemPromotions v : limitItems) {
			String key = "normal".equals(v.getItemType()) ? String.valueOf(v.getItemId()) : "special";
			LimitItemPromotions prev = limitItemInfo.get(key);
			long prevN = prev != null && prev.getLimitNum() != null ? prev.getLimitNum() : Long.MAX_VALUE;
			long curN = v.getLimitNum() != null ? v.getLimitNum() : Long.MAX_VALUE;
			if (prev == null || curN < prevN) {
				limitItemInfo.put(key, v);
			}
		}
		long limitId = 0L;
		for (LimitItemPromotions v : limitItemInfo.values()) {
			limitId = v.getLimitId() != null ? v.getLimitId() : 0L;
		}
		if (limitItemInfo.isEmpty() || limitId <= 0L) {
			return null;
		}
		LimitPromotions limitRow = limitPromotionsMapper.selectById(limitId);
		if (limitRow == null) {
			return null;
		}
		long itemDist = p1.getDistributorId() != null ? p1.getDistributorId().longValue() : 0L;
		long limitSource = limitRow.getSourceId() != null ? limitRow.getSourceId() : 0L;
		if (itemDist != limitSource) {
			return null;
		}
		LimitItemPromotions globalLimitRow = limitItemInfo.remove("special");
		Map<String, Object> limitInfo = limitPromotionToDetailMap(limitRow);
		@SuppressWarnings("unchecked")
		Map<String, Object> rule =
				limitInfo.get("rule") instanceof Map<?, ?> rm ? new LinkedHashMap<>((Map<String, Object>) rm) : new LinkedHashMap<>();
		limitInfo.put("rule", rule);
		Map<String, Object> activityData = new LinkedHashMap<>();
		activityData.put("activity_type", "limited_buy");
		Map<String, Map<String, Object>> listOut = new LinkedHashMap<>();
		if (!limitItemInfo.isEmpty()) {
			for (Map.Entry<String, LimitItemPromotions> e : limitItemInfo.entrySet()) {
				LimitItemPromotions v = e.getValue();
				Map<String, Object> cell = new LinkedHashMap<>();
				long iid = v.getItemId() != null ? v.getItemId() : 0L;
				cell.put("item_id", iid);
				long ln = v.getLimitNum() != null ? v.getLimitNum() : 0L;
				cell.put("limit_num", ln);
				cell.put("price", 9999);
				cell.put("sales_store", 0);
				listOut.put(e.getKey(), cell);
				rule.put("limit", ln);
				long vDist = v.getDistributorId() != null ? v.getDistributorId() : 0L;
				if (vDist > 0) {
					rule.put("day", 0);
				}
				Object dayObj = rule.get("day");
				int day = dayObj instanceof Number ? ((Number) dayObj).intValue() : 0;
				if (day == 0) {
					limitInfo.put("describe", "该商品活动期间只能购买" + ln + "件");
				} else {
					limitInfo.put("describe", "该商品活动期间内" + day + "天只能购买" + ln + "件");
				}
			}
		} else if (globalLimitRow != null) {
			long gLimit = globalLimitRow.getLimitNum() != null ? globalLimitRow.getLimitNum() : 0L;
			rule.put("limit", gLimit);
			Object dayObj = rule.get("day");
			int day = dayObj instanceof Number ? ((Number) dayObj).intValue() : 0;
			if (day == 0) {
				limitInfo.put("describe", "该商品活动期间只能购买" + gLimit + "件");
			} else {
				limitInfo.put("describe", "该商品活动期间内" + day + "天只能购买" + gLimit + "件");
			}
			for (long iid : candidateItemIds) {
				Map<String, Object> cell = new LinkedHashMap<>();
				cell.put("item_id", iid);
				cell.put("limit_num", gLimit);
				cell.put("price", 9999);
				cell.put("sales_store", 0);
				listOut.put(String.valueOf(iid), cell);
			}
		} else {
			return null;
		}
		activityData.put("list", listOut);
		activityData.put("info", limitInfo);
		return activityData;
	}

	private static Long parsePositiveLongOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private Map<String, Object> limitPromotionToDetailMap(LimitPromotions lp) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("limit_id", lp.getLimitId());
		m.put("company_id", lp.getCompanyId());
		m.put("limit_name", lp.getLimitName());
		m.put("limit_type", lp.getLimitType());
		m.put("valid_grade", parseValidGradeList(lp.getValidGrade()));
		m.put("source_id", lp.getSourceId());
		m.put("source_type", lp.getSourceType());
		String ruleRaw = lp.getRule();
		if (StringUtils.hasText(ruleRaw)) {
			try {
				m.put("rule", objectMapper.readValue(ruleRaw, new TypeReference<Map<String, Object>>() {}));
			} catch (Exception e) {
				m.put("rule", new LinkedHashMap<String, Object>());
			}
		} else {
			m.put("rule", new LinkedHashMap<String, Object>());
		}
		m.put("describe", "");
		return m;
	}

	private static List<String> parseValidGradeList(String validGrade) {
		if (validGrade == null || !StringUtils.hasText(validGrade.trim())) {
			return List.of();
		}
		return Arrays.stream(validGrade.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
	}
}
