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

import cn.shopex.ecshopx.common.dispatch.SavePromotionItemTagJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityCategory;
import cn.shopex.ecshopx.promotions.event.MarketingActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityCategoryMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.MarketingActivityMultiLangWriteService;
import cn.shopex.ecshopx.promotions.support.PromotionTagTruncate;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class MarketingActivityCreatePersistenceService {

	private static final ZoneId ACTIVITY_RESPONSE_TIME_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter ACTIVITY_RESPONSE_DATETIME =
			DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ACTIVITY_RESPONSE_TIME_ZONE);

	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityCategoryMapper marketingActivityCategoryMapper;
	private final MarketingActivityMultiLangWriteService marketingActivityMultiLangWriteService;
	private final MarketingActivityCreateItemRelService marketingActivityCreateItemRelService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher;
	private final SavePromotionItemTagJobDispatchPublisher savePromotionItemTagJobDispatchPublisher;
	private final ObjectMapper objectMapper;

	public MarketingActivityCreatePersistenceService(
			MarketingActivityMapper marketingActivityMapper,
			MarketingActivityCategoryMapper marketingActivityCategoryMapper,
			MarketingActivityMultiLangWriteService marketingActivityMultiLangWriteService,
			MarketingActivityCreateItemRelService marketingActivityCreateItemRelService,
			ApplicationEventPublisher applicationEventPublisher,
			SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher,
			SavePromotionItemTagJobDispatchPublisher savePromotionItemTagJobDispatchPublisher,
			ObjectMapper objectMapper) {
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityCategoryMapper = marketingActivityCategoryMapper;
		this.marketingActivityMultiLangWriteService = marketingActivityMultiLangWriteService;
		this.marketingActivityCreateItemRelService = marketingActivityCreateItemRelService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.salespersonItemsShelvesJobDispatchPublisher = salespersonItemsShelvesJobDispatchPublisher;
		this.savePromotionItemTagJobDispatchPublisher = savePromotionItemTagJobDispatchPublisher;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createInTransaction(Map<String, Object> params, String requestLangTag) {
		MarketingActivity entity;
		try {
			entity = buildEntity(params);
		} catch (JsonProcessingException e) {
			throw new cn.shopex.ecshopx.common.exception.BadRequestException("活动规则格式错误");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated(now);
		entity.setUpdated(now);
		int n = marketingActivityMapper.insert(entity);
		if (n <= 0) {
			throw new cn.shopex.ecshopx.common.exception.ResourceException("活动创建失败");
		}
		long marketingId = entity.getMarketingId();
		long companyId = entity.getCompanyId();
		Map<String, Object> row = entityToResponseMap(entity, params);
		marketingActivityMultiLangWriteService.addForNewActivity(marketingId, companyId, params, requestLangTag);
		int ub = entity.getUseBound() != null ? entity.getUseBound() : 0;
		if (ub == 2 && params.get("item_category") instanceof List<?> catList && !catList.isEmpty()) {
			for (Object c : catList) {
				long categoryId = c instanceof Number num ? num.longValue() : Long.parseLong(String.valueOf(c).trim());
				MarketingActivityCategory rel = new MarketingActivityCategory();
				rel.setMarketingId(marketingId);
				rel.setCompanyId(companyId);
				rel.setCategoryId(categoryId);
				rel.setMarketingType(entity.getMarketingType());
				rel.setCategoryLevel(0);
				marketingActivityCategoryMapper.insert(rel);
			}
		}
		marketingActivityCreateItemRelService.createMarketingItemRel(row, params);
		marketingActivityCreateItemRelService.createMarketingGiftItemRel(row, params);
		List<Long> tagJobIds = List.of();
		Object tagJobRaw = params.get("_tag_job_item_ids");
		if (tagJobRaw instanceof List<?> tjl) {
			tagJobIds = toLongList(tjl);
		}
		String marketingTypeRaw = String.valueOf(params.get("marketing_type"));
		String itemTypeForJob = params.get("item_type") != null ? String.valueOf(params.get("item_type")) : "normal";
		int startT = entity.getStartTime() != null ? entity.getStartTime() : 0;
		int endT = entity.getEndTime() != null ? entity.getEndTime() : 0;
		savePromotionItemTagJobDispatchPublisher.publishSavePromotionItemTag(
				companyId,
				marketingId,
				marketingTypeRaw,
				startT,
				endT,
				itemTypeForJob,
				List.copyOf(tagJobIds),
				Map.of());
		MarketingActivityCommittedEvent event = new MarketingActivityCommittedEvent(
				companyId,
				marketingId,
				marketingTypeRaw,
				entity.getStartTime() != null ? entity.getStartTime() : 0,
				entity.getEndTime() != null ? entity.getEndTime() : 0,
				false,
				params.get("item_type") != null ? String.valueOf(params.get("item_type")) : null,
				List.copyOf(tagJobIds),
				Map.of(),
				false);
		params.remove("_tag_job_item_ids");
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				salespersonItemsShelvesJobDispatchPublisher.publish(companyId, marketingId, marketingTypeRaw);
				applicationEventPublisher.publishEvent(event);
			}
		});
		return row;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateInTransaction(
			MarketingActivity existing, Map<String, Object> params, String requestLangTag) {
		long marketingId = existing.getMarketingId();
		long companyId = existing.getCompanyId();
		long paramCompanyId = readLong(params.get("company_id"));
		long paramMarketingId =
				params.get("marketing_id") instanceof Number n ? n.longValue() : readLong(params.get("marketing_id"));
		if (paramCompanyId != companyId || paramMarketingId != marketingId) {
			throw new cn.shopex.ecshopx.common.exception.ResourceException("活动数据不一致");
		}
		marketingActivityCategoryMapper.delete(
				new LambdaQueryWrapper<MarketingActivityCategory>()
						.eq(MarketingActivityCategory::getCompanyId, companyId)
						.eq(MarketingActivityCategory::getMarketingId, marketingId));
		MarketingActivity updated;
		try {
			updated = buildEntity(params);
		} catch (JsonProcessingException e) {
			throw new cn.shopex.ecshopx.common.exception.BadRequestException("活动规则格式错误");
		}
		updated.setMarketingId(existing.getMarketingId());
		updated.setCompanyId(existing.getCompanyId());
		updated.setCreated(existing.getCreated());
		updated.setReleaseTime(existing.getReleaseTime());
		updated.setUpdated((int) (System.currentTimeMillis() / 1000L));
		int rows = marketingActivityMapper.updateById(updated);
		if (rows <= 0) {
			throw new cn.shopex.ecshopx.common.exception.ResourceException("未查询到更新数据");
		}
		Map<String, Object> row = entityToResponseMap(updated, params);
		marketingActivityMultiLangWriteService.addForNewActivity(marketingId, companyId, params, requestLangTag);
		int ub = updated.getUseBound() != null ? updated.getUseBound() : 0;
		if (ub == 2 && params.get("item_category") instanceof List<?> catList && !catList.isEmpty()) {
			for (Object c : catList) {
				long categoryId = c instanceof Number num ? num.longValue() : Long.parseLong(String.valueOf(c).trim());
				MarketingActivityCategory rel = new MarketingActivityCategory();
				rel.setMarketingId(marketingId);
				rel.setCompanyId(companyId);
				rel.setCategoryId(categoryId);
				rel.setMarketingType(updated.getMarketingType());
				rel.setCategoryLevel(0);
				marketingActivityCategoryMapper.insert(rel);
			}
		}
		marketingActivityCreateItemRelService.createMarketingItemRel(row, params);
		marketingActivityCreateItemRelService.createMarketingGiftItemRel(row, params);
		List<Long> tagJobIds = List.of();
		Object tagJobRaw = params.get("_tag_job_item_ids");
		if (tagJobRaw instanceof List<?> tjl) {
			tagJobIds = toLongList(tjl);
		}
		String marketingTypeRaw = String.valueOf(params.get("marketing_type"));
		String itemTypeForJob = params.get("item_type") != null ? String.valueOf(params.get("item_type")) : "normal";
		int startU = updated.getStartTime() != null ? updated.getStartTime() : 0;
		int endU = updated.getEndTime() != null ? updated.getEndTime() : 0;
		savePromotionItemTagJobDispatchPublisher.publishSavePromotionItemTag(
				companyId,
				marketingId,
				marketingTypeRaw,
				startU,
				endU,
				itemTypeForJob,
				List.copyOf(tagJobIds),
				Map.of());
		MarketingActivityCommittedEvent event =
				new MarketingActivityCommittedEvent(
						companyId,
						marketingId,
						marketingTypeRaw,
						updated.getStartTime() != null ? updated.getStartTime() : 0,
						updated.getEndTime() != null ? updated.getEndTime() : 0,
						false,
						params.get("item_type") != null ? String.valueOf(params.get("item_type")) : null,
						List.copyOf(tagJobIds),
						Map.of(),
						false);
		params.remove("_tag_job_item_ids");
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						salespersonItemsShelvesJobDispatchPublisher.publish(companyId, marketingId, marketingTypeRaw);
						applicationEventPublisher.publishEvent(event);
					}
				});
		return row;
	}

	private MarketingActivity buildEntity(Map<String, Object> p) throws JsonProcessingException {
		MarketingActivity e = new MarketingActivity();
		e.setMarketingType(String.valueOf(p.get("marketing_type")));
		if (p.get("rel_marketing_id") instanceof Number n && n.longValue() > 0) {
			e.setRelMarketingId(n.longValue());
		}
		e.setMarketingName(String.valueOf(p.get("marketing_name")));
		if (StringUtils.hasText(stringify(p.get("activity_background")))) {
			e.setActivityBackground(stringify(p.get("activity_background")));
		}
		if (StringUtils.hasText(stringify(p.get("navbar_color")))) {
			e.setNavbarColor(stringify(p.get("navbar_color")));
		}
		if (StringUtils.hasText(stringify(p.get("timeBackgroundColor")))) {
			e.setTimeBackgroundColor(stringify(p.get("timeBackgroundColor")));
		}
		e.setMarketingDesc(String.valueOf(p.get("marketing_desc")));
		e.setStartTime(toInt(p.get("start_time")));
		e.setEndTime(toInt(p.get("end_time")));
		if (p.get("used_platform") != null) {
			e.setUsedPlatform(toInt(p.get("used_platform")));
		} else {
			e.setUsedPlatform(0);
		}
		e.setUseBound((Integer) p.get("use_bound"));
		List<Long> tagIdList = normalizeLongIds(p.get("tag_ids"), "tag_ids");
		if (!tagIdList.isEmpty()) {
			e.setTagIds(objectMapper.writeValueAsString(tagIdList));
		} else {
			e.setTagIds("[]");
		}
		List<Long> brandIdList = normalizeLongIds(p.get("brand_ids"), "brand_ids");
		if (!brandIdList.isEmpty()) {
			e.setBrandIds(objectMapper.writeValueAsString(brandIdList));
		} else {
			e.setBrandIds("[]");
		}
		if (p.get("use_shop") != null) {
			e.setUseShop(toInt(p.get("use_shop")));
		} else {
			e.setUseShop(0);
		}
		List<Long> shopIdList = normalizeLongIds(p.get("shop_ids"), "shop_ids");
		if (!shopIdList.isEmpty()) {
			e.setShopIds("," + shopIdList.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",");
		}
		if (p.get("valid_grade") != null) {
			e.setValidGrade(objectMapper.writeValueAsString(p.get("valid_grade")));
		}
		if (StringUtils.hasText(stringify(p.get("condition_type")))) {
			e.setConditionType(stringify(p.get("condition_type")));
		}
		Object cv = p.get("condition_value");
		if (cv != null) {
			e.setConditionValue(objectMapper.writeValueAsString(cv));
		}
		if (p.get("in_proportion") != null) {
			e.setInProportion(toBool(p.get("in_proportion")));
		}
		if (p.get("canjoin_repeat") != null) {
			e.setCanjoinRepeat(toBool(p.get("canjoin_repeat")));
		}
		if (p.get("join_limit") != null) {
			e.setJoinLimit(toInt(p.get("join_limit")));
		} else {
			e.setJoinLimit(0);
		}
		if (p.get("free_postage") != null) {
			e.setFreePostage(toBool(p.get("free_postage")));
		}
		Object pt = p.get("promotion_tag");
		if (pt != null && StringUtils.hasText(String.valueOf(pt))) {
			e.setPromotionTag(PromotionTagTruncate.toVarchar15(String.valueOf(pt)));
		}
		if (StringUtils.hasText(stringify(p.get("check_status")))) {
			e.setCheckStatus(stringify(p.get("check_status")));
		}
		if (StringUtils.hasText(stringify(p.get("reason")))) {
			e.setReason(stringify(p.get("reason")));
		}
		if (StringUtils.hasText(stringify(p.get("item_type")))) {
			e.setItemType(stringify(p.get("item_type")));
		}
		if (p.get("is_increase_purchase") != null) {
			e.setIsIncreasePurchase(toBool(p.get("is_increase_purchase")));
		}
		e.setCompanyId(readLong(p.get("company_id")));
		if (StringUtils.hasText(stringify(p.get("ad_pic")))) {
			e.setAdPic(stringify(p.get("ad_pic")));
		}
		e.setSourceType(stringify(p.get("source_type")));
		e.setSourceId(p.get("source_id") instanceof Number n ? n.longValue() : 0L);
		return e;
	}

	private Map<String, Object> entityToResponseMap(MarketingActivity e, Map<String, Object> params) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("marketing_id", String.valueOf(e.getMarketingId()));
		m.put("marketing_type", e.getMarketingType());
		m.put("rel_marketing_id", e.getRelMarketingId());
		m.put("marketing_name", e.getMarketingName());
		m.put("marketing_desc", e.getMarketingDesc());
		int st = e.getStartTime() != null ? e.getStartTime() : 0;
		int et = e.getEndTime() != null ? e.getEndTime() : 0;
		m.put("start_time", st);
		m.put("end_time", et);
		m.put("start_date", formatRepositoryDateTime(st));
		m.put("end_date", formatRepositoryDateTime(et));
		m.put("release_time", releaseTimeForResponse(e.getReleaseTime()));
		m.put("used_platform", e.getUsedPlatform());
		m.put("use_bound", e.getUseBound());
		m.put("use_shop", e.getUseShop());
		m.put("shop_ids", shopIdsSegmentsForResponse(e.getShopIds()));
		m.put("company_id", String.valueOf(e.getCompanyId()));
		m.put("condition_type", e.getConditionType());
		Object cvRaw = decodeJsonPreferParams(params.get("condition_value"), e.getConditionValue());
		m.put("condition_value", normalizeDecodedJsonNumbers(cvRaw));
		m.put("check_status", e.getCheckStatus());
		m.put("reason", e.getReason());
		m.put("tag_ids", decodeJsonArrayPreferParams(params.get("tag_ids"), e.getTagIds()));
		m.put("brand_ids", decodeJsonArrayPreferParams(params.get("brand_ids"), e.getBrandIds()));
		m.put("valid_grade", decodeValidGradeForResponse(params.get("valid_grade"), e.getValidGrade()));
		m.put("ad_pic", e.getAdPic());
		m.put("item_type", e.getItemType());
		m.put("in_proportion", e.getInProportion());
		m.put("canjoin_repeat", e.getCanjoinRepeat());
		m.put("free_postage", e.getFreePostage());
		m.put("navbar_color", e.getNavbarColor());
		m.put("activity_background", e.getActivityBackground());
		m.put("timeBackgroundColor", e.getTimeBackgroundColor());
		m.put("is_increase_purchase", e.getIsIncreasePurchase());
		m.put("promotion_tag", e.getPromotionTag());
		m.put("join_limit", e.getJoinLimit());
		int cr = e.getCreated() != null ? e.getCreated() : 0;
		m.put("created", cr);
		m.put("created_date", formatRepositoryDateTime(cr));
		m.put("updated", e.getUpdated());
		m.put("source_id", e.getSourceId());
		m.put("source_type", e.getSourceType());
		appendRuntimeStatusFields(m, st, et);
		return m;
	}

	private static String formatRepositoryDateTime(int epochSeconds) {
		return ACTIVITY_RESPONSE_DATETIME.format(Instant.ofEpochSecond(epochSeconds));
	}

	private static Object releaseTimeForResponse(Integer rt) {
		if (rt == null || rt == 0) {
			return null;
		}
		return rt;
	}

	/**
	 * Splits the persisted {@code shop_ids} comma-separated string into segments for the API payload,
	 * preserving empty leading and trailing segments (same delimiter semantics as {@link String#split(String, int)} with a negative limit).
	 */
	private static List<Object> shopIdsSegmentsForResponse(String shopIds) {
		if (!StringUtils.hasText(shopIds)) {
			return List.of();
		}
		String[] parts = shopIds.split(",", -1);
		List<Object> out = new ArrayList<>(parts.length);
		for (String p : parts) {
			out.add(p);
		}
		return out;
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

	private List<Object> decodeValidGradeForResponse(Object paramValue, String persistedJson) {
		Object raw = paramValue;
		if (raw == null && StringUtils.hasText(persistedJson)) {
			raw = readJsonOrNull(persistedJson);
		} else if (raw instanceof String s && StringUtils.hasText(s)) {
			raw = readJsonOrNull(s);
		}
		if (raw instanceof List<?> l) {
			List<Object> out = new ArrayList<>(l.size());
			for (Object o : l) {
				out.add(o);
			}
			return out;
		}
		return List.of();
	}

	private Object decodeJsonPreferParams(Object paramValue, String persistedJson) {
		if (paramValue != null) {
			return paramValue;
		}
		return readJsonOrString(persistedJson);
	}

	private Object decodeJsonArrayPreferParams(Object paramValue, String persistedJson) {
		if (paramValue instanceof List<?> l && !l.isEmpty()) {
			return paramValue;
		}
		Object parsed = readJsonOrNull(persistedJson);
		return parsed != null ? parsed : (paramValue != null ? paramValue : List.of());
	}

	private Object readJsonOrString(String raw) {
		if (!StringUtils.hasText(raw)) {
			return raw;
		}
		try {
			return objectMapper.readValue(raw, Object.class);
		} catch (JsonProcessingException ex) {
			return raw;
		}
	}

	private Object readJsonOrNull(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return objectMapper.readValue(raw, Object.class);
		} catch (JsonProcessingException ex) {
			return null;
		}
	}

	private List<Long> normalizeLongIds(Object raw, String fieldKey) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Number n) {
			return List.of(n.longValue());
		}
		if (raw instanceof List<?>) {
			List<?> l = (List<?>) raw;
			if (l.isEmpty()) {
				return List.of();
			}
			return toLongListOrBadRequest(l, fieldKey);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return List.of();
			}
			if (t.startsWith("[")) {
				try {
					Object parsed = objectMapper.readValue(t, Object.class);
					if (parsed instanceof List<?>) {
						List<?> l = (List<?>) parsed;
						return l.isEmpty() ? List.of() : toLongListOrBadRequest(l, fieldKey);
					}
					if (parsed instanceof Number num) {
						return List.of(num.longValue());
					}
				} catch (JsonProcessingException ex) {
					throw new cn.shopex.ecshopx.common.exception.BadRequestException(fieldKey + " 格式错误");
				}
				throw new cn.shopex.ecshopx.common.exception.BadRequestException(fieldKey + " 格式错误");
			}
			try {
				return List.of(Long.parseLong(t));
			} catch (NumberFormatException ex) {
				throw new cn.shopex.ecshopx.common.exception.BadRequestException(fieldKey + " 格式错误");
			}
		}
		throw new cn.shopex.ecshopx.common.exception.BadRequestException(fieldKey + " 格式错误");
	}

	private static List<Long> toLongListOrBadRequest(List<?> l, String fieldKey) {
		try {
			return toLongList(l);
		} catch (NumberFormatException ex) {
			throw new cn.shopex.ecshopx.common.exception.BadRequestException(fieldKey + " 格式错误");
		}
	}

	private static List<Long> toLongList(List<?> l) {
		List<Long> out = new ArrayList<>();
		for (Object o : l) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o != null) {
				out.add(Long.parseLong(o.toString().trim()));
			}
		}
		return out;
	}

	private static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return (int) Double.parseDouble(String.valueOf(v).trim());
	}

	private static boolean toBool(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		String s = String.valueOf(v).trim();
		return !"false".equalsIgnoreCase(s) && !"0".equals(s) && StringUtils.hasText(s);
	}

	private static String stringify(Object v) {
		return v == null ? "" : String.valueOf(v);
	}
}
