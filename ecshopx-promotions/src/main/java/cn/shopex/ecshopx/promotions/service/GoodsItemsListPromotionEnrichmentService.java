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

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionsItemsTag;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
public class GoodsItemsListPromotionEnrichmentService {

	private static final List<String> SHOP_SCOPED_TAG_TYPES = List.of(
			"full_discount", "full_minus", "full_gift", "self_select", "plus_price_buy", "member_preference");
	private static final List<String> ACTIVITY_PRICE_TAG_TYPES = List.of("single_group", "normal", "limited_time_sale");

	private final PromotionsItemsTagMapper promotionsItemsTagMapper;
	private final MarketingActivityMapper marketingActivityMapper;
	private final MemberAccountService memberAccountService;
	private final ObjectMapper objectMapper;

	public GoodsItemsListPromotionEnrichmentService(PromotionsItemsTagMapper promotionsItemsTagMapper,
			MarketingActivityMapper marketingActivityMapper,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper) {
		this.promotionsItemsTagMapper = promotionsItemsTagMapper;
		this.marketingActivityMapper = marketingActivityMapper;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
	}

	public void enrich(List<Map<String, Object>> rows) {
		enrich(rows, 0L);
	}

	public void enrich(List<Map<String, Object>> rows, long userId) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		Long companyId = null;
		for (Map<String, Object> row : rows) {
			Object c = row.get("company_id");
			if (c instanceof Number n && n.longValue() > 0) {
				companyId = n.longValue();
				break;
			}
		}
		if (companyId == null || companyId <= 0L) {
			return;
		}
		Set<Long> goodsIds = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			Object g = row.get("goods_id");
			if (g instanceof Number n && n.longValue() > 0) {
				goodsIds.add(n.longValue());
			}
		}
		if (goodsIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<PromotionsItemsTag> w = new LambdaQueryWrapper<>();
		w.eq(PromotionsItemsTag::getCompanyId, companyId).le(PromotionsItemsTag::getStartTime, now).ge(PromotionsItemsTag::getEndTime, now)
				.and(q -> q.eq(PromotionsItemsTag::getIsAllItems, 1L).or().in(PromotionsItemsTag::getGoodsId, goodsIds));
		w.orderByDesc(PromotionsItemsTag::getActivityPrice);
		List<PromotionsItemsTag> tagRows = promotionsItemsTagMapper.selectList(w);

		Map<Long, Map<Long, Map<String, Object>>> byGoods = new LinkedHashMap<>();
		Map<Long, Map<String, Object>> allItems = new LinkedHashMap<>();
		for (PromotionsItemsTag t : tagRows) {
			Map<String, Object> data = tagToMap(t);
			Long pid = t.getPromotionId();
			if (pid == null) {
				continue;
			}
			if (t.getIsAllItems() != null && t.getIsAllItems() == 1L) {
				allItems.put(pid, data);
			} else if (t.getGoodsId() != null && t.getGoodsId() > 0L) {
				byGoods.computeIfAbsent(t.getGoodsId(), k -> new LinkedHashMap<>()).put(pid, data);
			}
		}

		Set<Long> marketingIds = new LinkedHashSet<>();
		marketingIds.addAll(allItems.keySet());
		for (Map<Long, Map<String, Object>> m : byGoods.values()) {
			marketingIds.addAll(m.keySet());
		}
		Map<Long, MarketingActivity> activityById = loadActivities(marketingIds);

		for (Map<String, Object> items : rows) {
			Object g = items.get("goods_id");
			if (!(g instanceof Number gn) || gn.longValue() <= 0) {
				continue;
			}
			long goodsId = gn.longValue();
			Map<Long, Map<String, Object>> perGoods = byGoods.getOrDefault(goodsId, Map.of());
			List<Map<String, Object>> mergedActs = new ArrayList<>(perGoods.values());
			mergedActs.addAll(allItems.values());
			long itemDistributorId = longOrZero(items.get("distributor_id"));
			List<Map<String, Object>> promotionActivity = new ArrayList<>();
			items.put("promotion_activity", promotionActivity);
			for (Map<String, Object> data : mergedActs) {
				Object pidObj = data.get("promotion_id");
				long promotionId = pidObj instanceof Number p ? p.longValue() : 0L;
				String tagType = data.get("tag_type") != null ? data.get("tag_type").toString() : "";
				if (SHOP_SCOPED_TAG_TYPES.contains(tagType)) {
					MarketingActivity ma = activityById.get(promotionId);
					if (!passesShopScope(ma, itemDistributorId)) {
						continue;
					}
					if ("member_preference".equals(tagType)
							&& !passesMemberPreferenceGrade(ma, userId, companyId)) {
						continue;
					}
				}
				promotionActivity.add(data);
				if (ACTIVITY_PRICE_TAG_TYPES.contains(tagType)) {
					Object ap = data.get("activity_price");
					if (ap instanceof Number an) {
						items.put("activity_price", an.longValue());
					}
				}
			}
		}
	}

	private Map<Long, MarketingActivity> loadActivities(Set<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Map.of();
		}
		List<MarketingActivity> list = marketingActivityMapper.selectBatchIds(ids);
		return list.stream().filter(Objects::nonNull).collect(Collectors.toMap(MarketingActivity::getMarketingId, a -> a, (a, b) -> a));
	}

	private boolean passesMemberPreferenceGrade(MarketingActivity ma, long userId, long companyId) {
		if (userId <= 0L || ma == null) {
			return true;
		}
		List<?> validGrade = decodeJsonList(ma.getValidGrade());
		if (validGrade.isEmpty()) {
			return true;
		}
		Long userGrade = resolveUserGrade(userId, companyId);
		if (userGrade != null && !gradeListContains(validGrade, userGrade)) {
			return false;
		}
		return true;
	}

	private List<?> decodeJsonList(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json.trim(), new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return List.of();
		}
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

	private Long resolveUserGrade(long userId, long companyId) {
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

	private boolean passesShopScope(MarketingActivity ma, long itemDistributorId) {
		if (ma == null) {
			return false;
		}
		if (!StringUtils.hasText(ma.getShopIds())) {
			return false;
		}
		List<Long> shopIds = parseShopIds(ma.getShopIds());
		List<Long> nonZero = shopIds.stream().filter(id -> id != null && id > 0).collect(Collectors.toList());
		if (itemDistributorId > 0) {
			if (!nonZero.isEmpty() && !nonZero.contains(itemDistributorId)) {
				return false;
			}
			long sourceId = ma.getSourceId() != null ? ma.getSourceId() : 0L;
			return itemDistributorId == sourceId;
		}
		if (!shopIds.isEmpty()) {
			Set<Long> set = new LinkedHashSet<>(shopIds);
			return set.contains(itemDistributorId);
		}
		return true;
	}

	private List<Long> parseShopIds(String raw) {
		String t = raw.trim();
		if (t.startsWith("[") && t.endsWith("]")) {
			try {
				JsonNode arr = objectMapper.readTree(t);
				List<Long> out = new ArrayList<>();
				if (arr.isArray()) {
					for (JsonNode n : arr) {
						if (n.isNumber()) {
							out.add(n.longValue());
						} else if (n.isTextual()) {
							try {
								out.add(Long.parseLong(n.asText()));
							} catch (NumberFormatException ignored) {
								// skip
							}
						}
					}
				}
				return out;
			} catch (Exception e) {
				return List.of();
			}
		}
		if ("all".equalsIgnoreCase(t)) {
			return List.of();
		}
		String[] parts = t.split(",");
		List<Long> out = new ArrayList<>();
		for (String p : parts) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				out.add(Long.parseLong(p.trim()));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}

	private Map<String, Object> tagToMap(PromotionsItemsTag t) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (t.getPromotionId() != null) {
			m.put("promotion_id", t.getPromotionId());
		}
		m.put("tag_type", t.getTagType());
		if (t.getActivityPrice() != null) {
			m.put("activity_price", t.getActivityPrice());
		}
		if (t.getStartTime() != null) {
			m.put("start_time", t.getStartTime());
		}
		if (t.getEndTime() != null) {
			m.put("end_time", t.getEndTime());
		}
		if (t.getItemId() != null) {
			m.put("item_id", t.getItemId());
		}
		return m;
	}

	private static long longOrZero(Object v) {
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
}
