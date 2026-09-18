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
import cn.shopex.ecshopx.common.util.PlusBuyCartRedisKeys;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingGiftItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PlusPriceBuyItemListService {

	private static final Set<String> PLUS_PRICE_BUY_ROW_BOOLEAN_KEYS = Set.of("nospec", "without_return", "is_checked");

	private final MarketingActivityItemListActivityQuerySupport activityQuerySupport;
	private final MarketingGiftItemsMapper marketingGiftItemsMapper;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public PlusPriceBuyItemListService(
			MarketingActivityItemListActivityQuerySupport activityQuerySupport,
			MarketingGiftItemsMapper marketingGiftItemsMapper,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.activityQuerySupport = activityQuerySupport;
		this.marketingGiftItemsMapper = marketingGiftItemsMapper;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getPlusPriceBuyItem(
			long companyId, long userId, long marketingId, int page, int pageSize) {
		Locale locale = LocaleContextHolder.getLocale();
		Optional<MarketingActivity> opt = activityQuerySupport.loadActivityRow(companyId, marketingId);
		if (opt.isEmpty() || !"plus_price_buy".equals(opt.get().getMarketingType())) {
			throw new ResourceException(
					messageSource.getMessage("promotions.marketing_activity.not_found", null, locale));
		}
		MarketingActivity activity = opt.get();

		LambdaQueryWrapper<MarketingGiftItems> w = new LambdaQueryWrapper<>();
		w.eq(MarketingGiftItems::getCompanyId, companyId).eq(MarketingGiftItems::getMarketingId, marketingId);
		Page<MarketingGiftItems> p = new Page<>(page, pageSize);
		marketingGiftItemsMapper.selectPage(p, w);
		int totalCount = p.getTotal() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) p.getTotal();

		List<Map<String, Object>> list = new ArrayList<>();
		for (MarketingGiftItems g : p.getRecords()) {
			list.add(giftRowToMap(g));
		}

		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Long id = normalizeItemId(row.get("item_id"));
			if (id != null) {
				itemIds.add(id);
			}
		}

		Map<Long, Map<String, Object>> skuByItemId;
		if (itemIds.isEmpty()) {
			skuByItemId = Collections.emptyMap();
		} else {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> skuList =
					(List<Map<String, Object>>)
							marketingActivityCatalogAccess
									.loadSkuItemsListForMarketingGift(companyId, itemIds)
									.getOrDefault("list", List.of());
			skuByItemId = new LinkedHashMap<>();
			for (Map<String, Object> skuRow : skuList) {
				Long id = normalizeItemId(skuRow.get("item_id"));
				if (id != null && !skuByItemId.containsKey(id)) {
					skuByItemId.put(id, skuRow);
				}
			}
		}

		String key = PlusBuyCartRedisKeys.redisKey(companyId, userId, Long.valueOf(marketingId));
		String raw = stringRedisTemplate.opsForValue().get(key);
		long checkedItemId = 0L;
		if (raw != null) {
			String t = raw.trim();
			if (!t.isEmpty()) {
				try {
					checkedItemId = Long.parseLong(t);
				} catch (NumberFormatException ignored) {
					checkedItemId = 0L;
				}
			}
		}

		List<Map<String, Object>> mergedList = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> value = new LinkedHashMap<>(list.get(i));
			Long itemIdLong = normalizeItemId(value.get("item_id"));
			if (checkedItemId != 0L && Objects.equals(itemIdLong, checkedItemId)) {
				value.put("is_checked", Boolean.TRUE);
			}
			Map<String, Object> sku = itemIdLong != null ? skuByItemId.get(itemIdLong) : null;
			if (sku != null && !sku.isEmpty()) {
				Object plusPriceSource = value.get("price");
				Map<String, Object> merged = new LinkedHashMap<>(value);
				merged.put("plus_price", plusPriceSource);
				merged.putAll(sku);
				mergedList.add(plusPriceBuyRowPhpWire(merged));
			} else {
				mergedList.add(plusPriceBuyRowPhpWire(value));
			}
		}

		int end = activity.getEndTime() != null ? activity.getEndTime() : 0;
		long leftTime = (long) end - (System.currentTimeMillis() / 1000L);
		Map<String, Object> promo = buildPlusPriceBuyPromotionActivity(activity, leftTime);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", mergedList);
		out.put("total_count", totalCount);
		out.put("promotion_activity", promo);
		return out;
	}

	private Map<String, Object> buildPlusPriceBuyPromotionActivity(MarketingActivity row, long leftTime) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("marketing_id", String.valueOf(row.getMarketingId()));
		m.put("marketing_type", row.getMarketingType());
		m.put("rel_marketing_id", String.valueOf(row.getRelMarketingId() != null ? row.getRelMarketingId() : 0L));
		m.put("marketing_name", row.getMarketingName());
		m.put("ad_pic", row.getAdPic());
		m.put("marketing_desc", row.getMarketingDesc());
		m.put("start_time", row.getStartTime() != null ? String.valueOf(row.getStartTime()) : "0");
		m.put("end_time", row.getEndTime() != null ? String.valueOf(row.getEndTime()) : "0");
		m.put("release_time", row.getReleaseTime() != null && row.getReleaseTime() != 0 ? String.valueOf(row.getReleaseTime()) : null);
		m.put("used_platform", row.getUsedPlatform() != null ? String.valueOf(row.getUsedPlatform()) : "0");
		m.put("use_bound", row.getUseBound() != null ? String.valueOf(row.getUseBound()) : "0");
		m.put("tag_ids", row.getTagIds() != null ? row.getTagIds() : "[]");
		m.put("brand_ids", row.getBrandIds() != null ? row.getBrandIds() : "[]");
		m.put("use_shop", row.getUseShop() != null ? String.valueOf(row.getUseShop()) : "0");
		m.put("shop_ids", row.getShopIds() != null ? row.getShopIds() : "");
		m.put("valid_grade", row.getValidGrade() != null ? row.getValidGrade() : "");
		m.put("condition_type", row.getConditionType());
		m.put("condition_value", row.getConditionValue() != null ? row.getConditionValue() : "");
		m.put("in_proportion", boolToPhpZeroOne(row.getInProportion()));
		m.put("activity_background", row.getActivityBackground());
		m.put("navbar_color", row.getNavbarColor());
		m.put("timeBackgroundColor", row.getTimeBackgroundColor());
		m.put("canjoin_repeat", boolToPhpZeroOne(row.getCanjoinRepeat()));
		m.put("join_limit", row.getJoinLimit() != null ? String.valueOf(row.getJoinLimit()) : "0");
		m.put("free_postage", boolToPhpZeroOne(row.getFreePostage()));
		m.put("promotion_tag", row.getPromotionTag());
		m.put("check_status", row.getCheckStatus());
		m.put("reason", row.getReason());
		m.put("item_type", row.getItemType());
		m.put("is_increase_purchase", row.getIsIncreasePurchase());
		m.put("company_id", String.valueOf(row.getCompanyId()));
		m.put("created", row.getCreated() != null ? String.valueOf(row.getCreated()) : "0");
		m.put("updated", row.getUpdated() != null ? String.valueOf(row.getUpdated()) : "0");
		m.put("source_type", row.getSourceType());
		m.put("source_id", String.valueOf(row.getSourceId() != null ? row.getSourceId() : 0L));
		m.put("left_time", leftTime);
		return m;
	}

	private static String boolToPhpZeroOne(Boolean value) {
		return Boolean.TRUE.equals(value) ? "1" : "0";
	}

	private static Map<String, Object> plusPriceBuyRowPhpWire(Map<String, Object> source) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> en : source.entrySet()) {
			String key = en.getKey();
			Object value = en.getValue();
			if (PLUS_PRICE_BUY_ROW_BOOLEAN_KEYS.contains(key)) {
				out.put(key, value);
				continue;
			}
			if ("plus_price".equals(key) || "gift_num".equals(key)) {
				out.put(key, value);
				continue;
			}
			out.put(key, plusPriceBuyPhpWireValue(value));
		}
		return out;
	}

	private static Object plusPriceBuyPhpWireValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Map<?, ?> map) {
			Map<String, Object> nested = new LinkedHashMap<>();
			for (Map.Entry<?, ?> en : map.entrySet()) {
				nested.put(String.valueOf(en.getKey()), plusPriceBuyPhpWireValue(en.getValue()));
			}
			return nested;
		}
		if (value instanceof List<?> list) {
			List<Object> out = new ArrayList<>();
			for (Object el : list) {
				out.add(plusPriceBuyPhpWireValue(el));
			}
			return out;
		}
		if (value instanceof Boolean b) {
			return b ? "1" : "0";
		}
		if (value instanceof Number n) {
			return n.toString();
		}
		return value;
	}

	private Map<String, Object> giftRowToMap(MarketingGiftItems g) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", g.getId());
		m.put("marketing_id", g.getMarketingId());
		m.put("item_id", g.getItemId());
		m.put("item_type", g.getItemType());
		m.put("item_name", g.getItemName());
		m.put("price", g.getPrice() != null ? g.getPrice() : 0);
		m.put("store", g.getStore() != null ? g.getStore() : 0);
		m.put("gift_num", g.getGiftNum());
		m.put("pics", decodePicsForGiftRow(g.getPics()));
		m.put("without_return", g.getWithoutReturn());
		m.put("condition_type", g.getConditionType());
		m.put("filter_full", formatFilterFull(g));
		m.put("item_spec_desc", g.getItemSpecDesc());
		m.put("company_id", g.getCompanyId());
		m.put("created", g.getCreated());
		m.put("updated", g.getUpdated());
		return m;
	}

	private Object decodePicsForGiftRow(String picsJson) {
		if (picsJson == null || picsJson.isBlank()) {
			return List.of("null");
		}
		try {
			Object parsed = objectMapper.readValue(picsJson.trim(), List.class);
			if (parsed instanceof List<?> l) {
				return new ArrayList<>(l);
			}
		} catch (JsonProcessingException ignored) {
		}
		return List.of("null");
	}

	private String formatFilterFull(MarketingGiftItems g) {
		Integer fv = g.getFilterFull();
		if ("totalfee".equals(g.getConditionType())) {
			int cents = fv != null ? fv : 0;
			return BigDecimal.valueOf(cents)
					.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
					.toPlainString();
		}
		return String.valueOf(fv != null ? fv : "");
	}

	private static Long normalizeItemId(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
