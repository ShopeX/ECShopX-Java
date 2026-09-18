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

import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MarketingActivityItemListActivityQuerySupport {

	private static final Logger log = LoggerFactory.getLogger(MarketingActivityItemListActivityQuerySupport.class);

	private static final ZoneId ACTIVITY_RESPONSE_TIME_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter ACTIVITY_RESPONSE_DATETIME =
			DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ACTIVITY_RESPONSE_TIME_ZONE);

	private final MarketingActivityMapper marketingActivityMapper;
	private final ObjectMapper objectMapper;

	public MarketingActivityItemListActivityQuerySupport(
			MarketingActivityMapper marketingActivityMapper, ObjectMapper objectMapper) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.objectMapper = objectMapper;
	}

	public Optional<MarketingActivity> loadActivityRow(long companyId, Long marketingId) {
		if (marketingId == null) {
			return Optional.empty();
		}
		LambdaQueryWrapper<MarketingActivity> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivity::getCompanyId, companyId).eq(MarketingActivity::getMarketingId, marketingId);
		return Optional.ofNullable(marketingActivityMapper.selectOne(w));
	}

	public List<Long> buildDistributorScopeForFullStore(Optional<MarketingActivity> row, long companyId, Long marketingId) {
		if (row.isEmpty()) {
			log.warn(
					"marketing_activity_missing_for_item_list companyId={} marketingId={}",
					companyId,
					marketingId);
			return List.of(0L);
		}
		return parseShopIdListFromShopIdsColumn(row.get().getShopIds(), companyId, marketingId);
	}

	public Map<String, Object> buildActivityPayloadForItemList(MarketingActivity row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("marketing_id", String.valueOf(row.getMarketingId()));
		m.put("marketing_type", row.getMarketingType());
		m.put("rel_marketing_id", String.valueOf(row.getRelMarketingId() != null ? row.getRelMarketingId() : 0L));
		m.put("marketing_name", row.getMarketingName());
		m.put("marketing_desc", row.getMarketingDesc());
		int st = row.getStartTime() != null ? row.getStartTime() : 0;
		int et = row.getEndTime() != null ? row.getEndTime() : 0;
		m.put("start_time", st);
		m.put("end_time", et);
		m.put("start_date", formatActivityDateTime(st));
		m.put("end_date", formatActivityDateTime(et));
		m.put("release_time", releaseTimeForPayload(row.getReleaseTime()));
		m.put("used_platform", row.getUsedPlatform());
		m.put("use_bound", row.getUseBound());
		m.put("tag_ids", decodeJsonArrayField(row.getTagIds()));
		m.put("brand_ids", decodeJsonArrayField(row.getBrandIds()));
		m.put("use_shop", row.getUseShop());
		m.put("shop_ids", shopIdsSegmentsForItemListPayload(row.getShopIds()));
		m.put("valid_grade", decodeValidGradeList(row.getValidGrade()));
		m.put("condition_type", row.getConditionType());
		m.put("condition_value", normalizeDecodedJsonNumbers(parseConditionValueJson(row.getConditionValue())));
		m.put("in_proportion", row.getInProportion());
		m.put("canjoin_repeat", row.getCanjoinRepeat());
		m.put("join_limit", row.getJoinLimit());
		m.put("free_postage", row.getFreePostage());
		m.put("promotion_tag", row.getPromotionTag());
		m.put("check_status", row.getCheckStatus());
		m.put("activity_background", row.getActivityBackground());
		m.put("navbar_color", row.getNavbarColor());
		m.put("timeBackgroundColor", row.getTimeBackgroundColor());
		m.put("reason", row.getReason());
		m.put("item_type", row.getItemType());
		m.put("is_increase_purchase", row.getIsIncreasePurchase());
		m.put("company_id", String.valueOf(row.getCompanyId()));
		int cr = row.getCreated() != null ? row.getCreated() : 0;
		m.put("created", cr);
		m.put("created_date", formatActivityDateTime(cr));
		m.put("updated", row.getUpdated());
		m.put("source_type", row.getSourceType());
		m.put("source_id", String.valueOf(row.getSourceId() != null ? row.getSourceId() : 0L));
		appendRuntimeStatusFields(m, st, et);
		return m;
	}

	private static String formatActivityDateTime(int epochSeconds) {
		return ACTIVITY_RESPONSE_DATETIME.format(Instant.ofEpochSecond(epochSeconds));
	}

	private static Object releaseTimeForPayload(Integer rt) {
		if (rt == null || rt == 0) {
			return null;
		}
		return rt;
	}

	private static void appendRuntimeStatusFields(Map<String, Object> m, int startTime, int endTime) {
		long nowTime = System.currentTimeMillis() / 1000L;
		if (nowTime >= endTime) {
			m.put("status", "end");
		} else if (nowTime >= startTime && nowTime < endTime) {
			m.put("status", "ongoing");
			m.put("last_seconds", (endTime - nowTime) > 0 ? (int) (endTime - nowTime) : 0);
		} else if (nowTime < startTime) {
			m.put("status", "waiting");
		}
	}

	private List<Object> decodeJsonArrayField(String persistedJson) {
		if (!StringUtils.hasText(persistedJson)) {
			return List.of();
		}
		Object parsed = readJsonOrNull(persistedJson.trim());
		if (parsed instanceof List<?> l) {
			return new ArrayList<>(l);
		}
		return List.of();
	}

	private List<Object> decodeValidGradeList(String persistedJson) {
		return decodeJsonArrayField(persistedJson);
	}

	private Object readJsonOrNull(String raw) {
		try {
			return objectMapper.readValue(raw, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private Object parseConditionValueJson(String cv) {
		if (!StringUtils.hasText(cv)) {
			return List.of();
		}
		try {
			return objectMapper.readValue(cv.trim(), Object.class);
		} catch (JsonProcessingException e) {
			return cv;
		}
	}

	private static Object normalizeDecodedJsonNumbers(Object node) {
		if (node instanceof Map<?, ?> map) {
			Map<String, Object> copy = new LinkedHashMap<>();
			for (Map.Entry<?, ?> en : map.entrySet()) {
				copy.put(String.valueOf(en.getKey()), normalizeDecodedJsonNumbers(en.getValue()));
			}
			return copy;
		}
		if (node instanceof List<?> list) {
			List<Object> copy = new ArrayList<>(list.size());
			for (Object item : list) {
				copy.add(normalizeDecodedJsonNumbers(item));
			}
			return copy;
		}
		if (node instanceof Double d) {
			if (Double.isFinite(d) && d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE && d == Math.rint(d)) {
				return (int) Math.rint(d);
			}
			return node;
		}
		if (node instanceof Float f) {
			if (Float.isFinite(f) && f >= Integer.MIN_VALUE && f <= Integer.MAX_VALUE && f == Math.rint(f)) {
				return (int) Math.rint(f);
			}
			return node;
		}
		if (node instanceof BigDecimal bd) {
			try {
				if (bd.stripTrailingZeros().scale() <= 0) {
					return bd.intValueExact();
				}
			} catch (ArithmeticException ignored) {
			}
			return node;
		}
		return node;
	}

	private List<Object> shopIdsSegmentsForItemListPayload(String shopIds) {
		if (!StringUtils.hasText(shopIds)) {
			return List.of();
		}
		String t = shopIds.trim();
		if ("all".equalsIgnoreCase(t)) {
			return List.of();
		}
		if (t.startsWith("[")) {
			try {
				JsonNode root = objectMapper.readTree(t);
				if (root.isArray()) {
					List<Object> out = new ArrayList<>();
					for (JsonNode n : root) {
						if (n == null || n.isNull()) {
							out.add("");
						} else if (n.isTextual()) {
							out.add(n.asText());
						} else if (n.isNumber()) {
							out.add(n.asText());
						} else {
							out.add(n.toString());
						}
					}
					return out;
				}
			} catch (JsonProcessingException e) {
				log.warn("marketing_activity_shop_ids_json_array_parse_failed");
			}
		}
		String[] parts = shopIds.split(",", -1);
		List<Object> out = new ArrayList<>(parts.length);
		for (String p : parts) {
			out.add(p);
		}
		return out;
	}

	public List<Long> parseShopIdListFromShopIdsColumn(String raw, long companyId, Long marketingId) {
		if (raw == null || raw.trim().isEmpty()) {
			return List.of(0L);
		}
		String trimmed = raw.trim();
		if ("all".equalsIgnoreCase(trimmed)) {
			return List.of(0L);
		}
		try {
			JsonNode root = objectMapper.readTree(trimmed);
			if (!root.isArray()) {
				log.warn(
						"marketing_activity_shop_ids_not_array companyId={} marketingId={}",
						companyId,
						marketingId);
				return List.of(0L);
			}
			List<Long> parsed = new ArrayList<>();
			for (JsonNode n : root) {
				if (n.isNumber()) {
					parsed.add(n.longValue());
				} else if (n.isTextual()) {
					try {
						parsed.add(Long.parseLong(n.asText().trim()));
					} catch (NumberFormatException ignored) {
						// skip invalid element
					}
				}
			}
			List<Long> filtered = new ArrayList<>();
			for (Long id : parsed) {
				if (id != null && id > 0L) {
					filtered.add(id);
				}
			}
			if (filtered.isEmpty()) {
				return List.of(0L);
			}
			return Collections.unmodifiableList(new ArrayList<>(filtered));
		} catch (JsonProcessingException e) {
			log.warn(
					"marketing_activity_shop_ids_json_invalid companyId={} marketingId={}",
					companyId,
					marketingId);
			return List.of(0L);
		}
	}
}
