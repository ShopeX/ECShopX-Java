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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.LimitPromotionMultiLangWriteService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LimitPromotionCreateService {

	private final MessageSource messageSource;
	private final MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;
	private final LimitPromotionsMapper limitPromotionsMapper;
	private final LimitPromotionMultiLangWriteService limitPromotionMultiLangWriteService;
	private final LimitPromotionPersistenceDelegate limitPromotionPersistenceDelegate;
	private final ObjectMapper objectMapper;

	public LimitPromotionCreateService(
			MessageSource messageSource,
			MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService,
			LimitPromotionsMapper limitPromotionsMapper,
			LimitPromotionMultiLangWriteService limitPromotionMultiLangWriteService,
			LimitPromotionPersistenceDelegate limitPromotionPersistenceDelegate,
			ObjectMapper objectMapper) {
		this.messageSource = messageSource;
		this.marketingActivityCrossPromotionGuardService = marketingActivityCrossPromotionGuardService;
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.limitPromotionMultiLangWriteService = limitPromotionMultiLangWriteService;
		this.limitPromotionPersistenceDelegate = limitPromotionPersistenceDelegate;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createLimitPromotion(Map<String, Object> params, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		String limitType = Objects.toString(params.get("limit_type"), "").trim();
		if (!StringUtils.hasText(limitType)) {
			limitType = "global";
			params.put("limit_type", limitType);
		}
		int totalCount = parseIntDefault(params.get("total_count"), 0);
		if ("shop".equalsIgnoreCase(limitType)) {
			params.put("day", 0);
			params.put("limit", 1);
			params.put("use_bound", "goods_import");
			if (totalCount == 0) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.limit.upload_items_required", null, locale));
			}
		}
		String limitName = Objects.toString(params.get("limit_name"), "").trim();
		if (!StringUtils.hasText(limitName)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.limit.limit_name_required", null, locale));
		}
		int dayInt = readNonNegativeInt(params.get("day"), "promotions.limit.day_invalid", locale);
		int limitInt = readMinOneInt(params.get("limit"), "promotions.limit.limit_invalid", locale);
		boolean isLongKeyPresent = params.containsKey("is_long");
		Object isLongVal = params.get("is_long");
		if (!isLongKeyPresent || isLongVal == null) {
			String st = Objects.toString(params.get("start_time"), "").trim();
			String et = Objects.toString(params.get("end_time"), "").trim();
			if (!StringUtils.hasText(st)) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.limit.start_time_required", null, locale));
			}
			if (!StringUtils.hasText(et)) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.limit.end_time_required", null, locale));
			}
		} else {
			if (isLongVal instanceof Map<?, ?> || isLongVal instanceof Iterable<?>) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.limit.is_long_invalid", null, locale));
			}
			if (!(isLongVal instanceof Boolean || isLongVal instanceof Number || isLongVal instanceof String)) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.limit.is_long_invalid", null, locale));
			}
		}
		long startEpoch = requireFlexibleEpochSeconds(params.get("start_time"), locale);
		long endEpoch = requireFlexibleEpochSeconds(params.get("end_time"), locale);
		int start = (int) startEpoch;
		int end = (int) endEpoch;
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (start <= now) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.limit.start_must_after_now", null, locale));
		}
		if (start == 0 || end == 0) {
			throw new ResourceException(messageSource.getMessage("promotions.limit.activity_time_required", null, locale));
		}
		if (start > end) {
			throw new ResourceException(messageSource.getMessage("promotions.limit.start_after_end", null, locale));
		}
		try {
			limitPromotionPersistenceDelegate.normalizeListLikeKeys(params);
			params.put("start_time", start);
			params.put("end_time", end);
			long companyId = readLong(params.get("company_id"));
			List<Long> itemIds = readLongList(params.get("items"));
			String ubStr = Objects.toString(params.get("use_bound"), "").trim();
			limitPromotionPersistenceDelegate.checkActivityForMutation(companyId, ubStr, itemIds, params, locale);
			LimitPromotions entity = new LimitPromotions();
			entity.setCompanyId(companyId);
			entity.setLimitName(limitName);
			entity.setLimitType(limitType);
			List<String> validGradeList = readStringListParam(params.get("valid_grade"));
			entity.setValidGrade(String.join(",", validGradeList));
			try {
				entity.setRule(objectMapper.writeValueAsString(Map.of("day", dayInt, "limit", limitInt)));
			} catch (JsonProcessingException e) {
				throw new ResourceException(e.getMessage() == null ? "error" : e.getMessage());
			}
			entity.setStartTime(start);
			entity.setEndTime(end);
			entity.setSourceId(readLongDefault(params.get("source_id"), 0L));
			entity.setSourceType(
					StringUtils.hasText(Objects.toString(params.get("source_type"), "").trim())
							? Objects.toString(params.get("source_type"), "").trim()
							: "admin");
			int ts = now;
			entity.setCreated(ts);
			entity.setUpdated(ts);
			applyCheckParams(params, entity, ubStr, itemIds, locale);
			LinkedHashMap<String, Object> guardParams = new LinkedHashMap<>();
			guardParams.put("company_id", companyId);
			guardParams.put("start_time", start);
			guardParams.put("end_time", end);
			guardParams.put("use_bound", entity.getUseBound());
			guardParams.put("item_category", params.get("item_category"));
			guardParams.put("tag_ids", readLongList(params.get("tag_ids")));
			guardParams.put("brand_ids", readLongList(params.get("brand_ids")));
			guardParams.put("shop_ids", List.of());
			guardParams.put("item_ids", itemIds);
			guardParams.put("limit_id", 0L);
			guardParams.put("source_id", entity.getSourceId() == null ? 0L : entity.getSourceId());
			marketingActivityCrossPromotionGuardService.checkActivityValidByLimit(guardParams);
			limitPromotionsMapper.insert(entity);
			limitPromotionMultiLangWriteService.addForNewLimit(
					entity.getLimitId(), companyId, params, requestLangTag);
			if (entity.getUseBound() != null && entity.getUseBound() == 2) {
				List<Long> cats = readLongList(params.get("item_category"));
				if (!cats.isEmpty()) {
					limitPromotionPersistenceDelegate.insertCategoryRowsForLimit(
							entity.getLimitId(), companyId, cats);
				}
			}
			List<Map<String, Object>> itemRows = List.of();
			if ("global".equalsIgnoreCase(limitType)) {
				itemRows =
						limitPromotionPersistenceDelegate.insertGlobalItemRelations(
								entity, params, itemIds, limitInt, locale);
			}
			return limitPromotionPersistenceDelegate.buildResponseMap(entity, itemRows, validGradeList);
		} catch (BadRequestException | ResourceException | UnauthorizedException | ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() == null ? "error" : e.getMessage());
		}
	}

	private void applyCheckParams(
			Map<String, Object> params,
			LimitPromotions entity,
			String ubStr,
			List<Long> itemIds,
			Locale locale)
			throws JsonProcessingException {
		if ("goods".equalsIgnoreCase(ubStr)) {
			entity.setUseBound(1);
			entity.setTagIds("[]");
			entity.setBrandIds("[]");
		} else if ("category".equalsIgnoreCase(ubStr)) {
			List<Long> cats = readLongList(params.get("item_category"));
			if (cats.isEmpty()) {
				throw new ResourceException(
						messageSource.getMessage("promotions.limit.item_category_required", null, locale));
			}
			entity.setUseBound(2);
			entity.setTagIds("[]");
			entity.setBrandIds("[]");
		} else if ("tag".equalsIgnoreCase(ubStr)) {
			List<Long> tagIds = readLongList(params.get("tag_ids"));
			if (tagIds.isEmpty()) {
				throw new ResourceException(messageSource.getMessage("promotions.limit.tag_ids_required", null, locale));
			}
			entity.setUseBound(3);
			entity.setTagIds(objectMapper.writeValueAsString(tagIds));
			entity.setBrandIds("[]");
		} else if ("brand".equalsIgnoreCase(ubStr)) {
			List<Long> brandIds = readLongList(params.get("brand_ids"));
			if (brandIds.isEmpty()) {
				throw new ResourceException(
						messageSource.getMessage("promotions.limit.brand_ids_required", null, locale));
			}
			entity.setUseBound(4);
			entity.setBrandIds(objectMapper.writeValueAsString(brandIds));
			entity.setTagIds("[]");
		} else if ("goods_import".equalsIgnoreCase(ubStr)) {
			entity.setUseBound(1);
			entity.setTagIds("[]");
			entity.setBrandIds("[]");
		} else {
			entity.setUseBound(1);
			entity.setTagIds("[]");
			entity.setBrandIds("[]");
		}
	}

	private List<String> readStringListParam(Object v) {
		if (!(v instanceof List<?> l)) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (Object o : l) {
			if (o == null) {
				continue;
			}
			if (o instanceof Number n) {
				out.add(String.valueOf(n.longValue()));
				continue;
			}
			String s = o.toString().trim();
			if (StringUtils.hasText(s)) {
				out.add(s);
			}
		}
		return out;
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

	private int parseIntDefault(Object v, int dft) {
		if (v == null) {
			return dft;
		}
		try {
			if (v instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return dft;
		}
	}

	private int readNonNegativeInt(Object v, String messageKey, Locale locale) {
		try {
			int n;
			if (v instanceof Number num) {
				n = num.intValue();
			} else {
				n = Integer.parseInt(String.valueOf(v).trim());
			}
			if (n < 0) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
			}
			return n;
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
	}

	private int readMinOneInt(Object v, String messageKey, Locale locale) {
		try {
			int n;
			if (v instanceof Number num) {
				n = num.intValue();
			} else {
				n = Integer.parseInt(String.valueOf(v).trim());
			}
			if (n < 1) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
			}
			return n;
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
	}

	private static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static long readLongDefault(Object v, long dft) {
		if (v == null) {
			return dft;
		}
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return dft;
		}
	}

	private long requireFlexibleEpochSeconds(Object v, Locale locale) {
		if (v == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.automation.fill_activity_time", null, locale));
		}
		if (v instanceof Boolean || v instanceof Map<?, ?> || v instanceof Iterable<?>) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
		}
		if (v instanceof Number n) {
			long raw = n.longValue();
			if (raw >= 1_000_000_000_000L) {
				raw = raw / 1000;
			}
			return raw;
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.automation.fill_activity_time", null, locale));
			}
			String t = s.trim();
			if (t.matches("^-?\\d+$")) {
				try {
					long raw = Long.parseLong(t);
					if (raw >= 1_000_000_000_000L) {
						raw = raw / 1000;
					}
					return raw;
				} catch (NumberFormatException e) {
					throw new BadRequestException(
							messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
				}
			}
			try {
				LocalDate localDate = LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
				return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();
			} catch (DateTimeParseException ignored) {
			}
			try {
				LocalDateTime ldt = LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				return ldt.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
			} catch (DateTimeParseException e) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
			}
		}
		throw new BadRequestException(
				messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
	}
}
