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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.promotions.domain.PromotionActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionActivityMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionActivityCreateService {

	private static final long FOREVER_END_EPOCH = 5000000000L;
	private static final int VALID_NUM = 1;
	private static final Set<String> ALLOWED_ACTIVITY_TYPES =
			Set.of(
					"member_birthday",
					"member_anniversary",
					"member_day",
					"member_upgrade",
					"member_vip_upgrade");

	private final PromotionActivityMapper promotionActivityMapper;
	private final PromotionActivityMultiLangWriteService promotionActivityMultiLangWriteService;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public PromotionActivityCreateService(
			PromotionActivityMapper promotionActivityMapper,
			PromotionActivityMultiLangWriteService promotionActivityMultiLangWriteService,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.promotionActivityMapper = promotionActivityMapper;
		this.promotionActivityMultiLangWriteService = promotionActivityMultiLangWriteService;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	public void checkActiveValidNum(long companyId, String activityTypeRaw) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		if (activityTypeRaw != null && !activityTypeRaw.equals(activityTypeRaw.trim())) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.automation.invalid_activity_type", null, locale));
		}
		String activityType = activityTypeRaw == null ? "" : activityTypeRaw.trim();
		if (!StringUtils.hasText(activityType) || !ALLOWED_ACTIVITY_TYPES.contains(activityType)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.automation.invalid_activity_type", null, locale));
		}
		long count =
				promotionActivityMapper.selectCount(
						new LambdaQueryWrapper<PromotionActivity>()
								.eq(PromotionActivity::getActivityType, activityType)
								.eq(PromotionActivity::getCompanyId, companyId)
								.eq(PromotionActivity::getActivityStatus, "valid"));
		if (count >= VALID_NUM) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.activity_limit_reached", null, locale));
		}
	}

	public Map<String, Object> updateStatusInvalid(long companyId, String activityIdRaw) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		if (!StringUtils.hasText(activityIdRaw)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.no_update_data_found", null, locale));
		}
		long activityId;
		try {
			activityId = Long.parseLong(activityIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.no_update_data_found", null, locale));
		}
		if (activityId <= 0L) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.no_update_data_found", null, locale));
		}
		PromotionActivity row =
				promotionActivityMapper.selectOne(
						new LambdaQueryWrapper<PromotionActivity>()
								.eq(PromotionActivity::getCompanyId, companyId)
								.eq(PromotionActivity::getActivityId, activityId));
		if (row == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.no_update_data_found", null, locale));
		}
		row.setActivityStatus("invalid");
		row.setUpdated((int) Instant.now().getEpochSecond());
		int affected = promotionActivityMapper.updateById(row);
		if (affected == 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.no_update_data_found", null, locale));
		}
		PromotionActivity refreshed = promotionActivityMapper.selectById(activityId);
		if (refreshed == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.automation.activity_persist_read_failed_after_update", null, locale));
		}
		return toActivityListRow(refreshed);
	}

	public Map<String, Object> createActivity(Map<String, Object> data, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);

		Object titleObj = data.get("title");
		if (titleObj == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.automation.activity_name_max_20", null, locale));
		}
		String title = titleObj.toString().trim();
		if (!StringUtils.hasText(title) || title.codePointCount(0, title.length()) > 20) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.automation.activity_name_max_20", null, locale));
		}

		if (data.get("is_forever") != null
				&& "true".equalsIgnoreCase(String.valueOf(data.get("is_forever")).trim())) {
			long beginEpoch = Instant.now().getEpochSecond();
			long endEpoch = FOREVER_END_EPOCH;
			data.put("begin_time", beginEpoch);
			data.put("end_time", endEpoch);
			data.remove("is_forever");
		} else {
			long beginEpoch = requireFlexibleEpochSeconds(data.get("begin_time"), locale);
			long endEpoch = requireFlexibleEpochSeconds(data.get("end_time"), locale);
			long nowSec = Instant.now().getEpochSecond();
			if (endEpoch <= nowSec) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.automation.select_valid_time", null, locale));
			}
			data.put("begin_time", beginEpoch);
			data.put("end_time", endEpoch);
		}

		normalizeSmsIsopen(data);

		Object companyRaw = data.get("company_id");
		if (companyRaw == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long companyId =
				companyRaw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(companyRaw).trim());
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		Object atRaw = data.get("activity_type");
		String activityType = atRaw == null ? "" : String.valueOf(atRaw).trim();
		if (!ALLOWED_ACTIVITY_TYPES.contains(activityType)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.invalid_activity_type", null, locale));
		}

		long count =
				promotionActivityMapper.selectCount(
						new LambdaQueryWrapper<PromotionActivity>()
								.eq(PromotionActivity::getActivityType, activityType)
								.eq(PromotionActivity::getCompanyId, companyId)
								.eq(PromotionActivity::getActivityStatus, "valid"));
		if (count >= VALID_NUM) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.activity_limit_reached", null, locale));
		}

		Object dcRaw = data.get("discount_config");
		if (!(dcRaw instanceof Map<?, ?>)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.discount_required", null, locale));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dc = (Map<String, Object>) dcRaw;

		boolean couponsEmpty =
				!dc.containsKey("coupons") || discountTierCount(dc.get("coupons"), true, locale) == 0;
		boolean goodsEmpty = !dc.containsKey("goods") || discountTierCount(dc.get("goods"), false, locale) == 0;
		if (couponsEmpty && goodsEmpty) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.discount_required", null, locale));
		}

		if (dc.containsKey("coupons")
				&& dc.get("coupons") != null
				&& discountTierCount(dc.get("coupons"), true, locale) > 10) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.max_10_coupons_allowed", null, locale));
		}
		if (dc.containsKey("goods")
				&& dc.get("goods") != null
				&& discountTierCount(dc.get("goods"), false, locale) > 10) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.max_10_products_allowed", null, locale));
		}

		Object tc = data.get("trigger_condition");
		if (!(tc instanceof Map<?, ?>)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.automation.trigger_condition_must_be_map", null, locale));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> triggerCondition = (Map<String, Object>) tc;
		validateTriggerByType(activityType, triggerCondition, locale);

		PromotionActivity entity = new PromotionActivity();
		entity.setCompanyId(companyId);
		entity.setActivityType(activityType);
		entity.setTitle(title);
		entity.setTriggerCondition(writeJson(triggerCondition, locale));
		entity.setDiscountConfig(writeJson(dc, locale));
		Object smsParamsObj = data.get("sms_params");
		if (smsParamsObj instanceof Map<?, ?> spMap) {
			entity.setSmsParams(writeJson(spMap, locale));
		} else {
			entity.setSmsParams(null);
		}
		Object smsOpen = data.get("sms_isopen");
		entity.setSmsIsopen(smsOpen == null ? "false" : String.valueOf(smsOpen));
		entity.setActivityStatus("valid");
		entity.setBeginTime(toLong(data.get("begin_time")));
		entity.setEndTime(toLong(data.get("end_time")));
		int ts = (int) Instant.now().getEpochSecond();
		entity.setCreated(ts);
		entity.setUpdated(ts);

		promotionActivityMapper.insert(entity);
		Long id = entity.getActivityId();
		if (id == null || id <= 0L) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.activity_persist_read_failed", null, locale));
		}

		promotionActivityMultiLangWriteService.addTitleForNewActivity(id, companyId, data, requestLangTag);

		PromotionActivity row = promotionActivityMapper.selectById(id);
		if (row == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.activity_persist_read_failed", null, locale));
		}
		return toActivityListRow(row);
	}

	private void normalizeSmsIsopen(Map<String, Object> data) {
		if (!data.containsKey("sms_isopen")) {
			data.put("sms_isopen", "false");
			return;
		}
		Object v = data.get("sms_isopen");
		if (v instanceof String) {
			return;
		}
		if (v instanceof Boolean b) {
			data.put("sms_isopen", b ? "true" : "false");
			return;
		}
		if (v instanceof Number n) {
			data.put("sms_isopen", n.intValue() != 0 ? "true" : "false");
			return;
		}
		data.put("sms_isopen", "false");
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
			} catch (DateTimeParseException e) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
			}
		}
		throw new BadRequestException(
				messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
	}

	private int discountTierCount(Object o, boolean couponsPath, Locale locale) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Map<?, ?> m) {
			return m.size();
		}
		if (o instanceof java.util.Collection<?> c) {
			return c.size();
		}
		String key =
				couponsPath
						? "promotions.automation.invalid_discount_coupons_shape"
						: "promotions.automation.invalid_discount_goods_shape";
		throw new ResourceException(messageSource.getMessage(key, null, locale));
	}

	private void validateTriggerByType(String activityType, Map<String, Object> triggerCondition, Locale locale) {
		switch (activityType) {
			case "member_birthday" -> {
				String t = stringOrNull(triggerCondition.get("trigger_time"));
				if (t == null
						|| !(t.equals("birthday_month") || t.equals("birthday_week") || t.equals("birthday_day"))) {
					throw new ResourceException(
							messageSource.getMessage("promotions.automation.please_select_gift_method", null, locale));
				}
			}
			case "member_anniversary" -> {
				String t = stringOrNull(triggerCondition.get("trigger_time"));
				if (t == null
						|| !(t.equals("anniversary_month")
								|| t.equals("anniversary_week")
								|| t.equals("anniversary_day"))) {
					throw new ResourceException(
							messageSource.getMessage("promotions.automation.please_select_gift_method", null, locale));
				}
			}
			case "member_day" -> {
				Object tt = triggerCondition.get("trigger_time");
				if (!(tt instanceof Map<?, ?> timeMapRaw)) {
					throw new ResourceException(
							messageSource.getMessage("promotions.automation.please_select_gift_method", null, locale));
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> timeMap = (Map<String, Object>) timeMapRaw;
				Object typeObj = timeMap.get("type");
				String type = typeObj == null ? "" : String.valueOf(typeObj).trim();
				switch (type) {
					case "every_year" -> {
						if (!nonEmptyNumberOrString(timeMap.get("month"))
								|| !nonEmptyNumberOrString(timeMap.get("day"))) {
							throw new ResourceException(
									messageSource.getMessage(
											"promotions.automation.please_select_specific_gift_date", null, locale));
						}
					}
					case "every_month" -> {
						if (!nonEmptyNumberOrString(timeMap.get("day"))) {
							throw new ResourceException(
									messageSource.getMessage(
											"promotions.automation.please_select_specific_gift_date", null, locale));
						}
					}
					case "every_week" -> {
						if (!nonEmptyNumberOrString(timeMap.get("week"))) {
							throw new ResourceException(
									messageSource.getMessage(
											"promotions.automation.please_select_specific_gift_date", null, locale));
						}
					}
					default -> throw new ResourceException(
							messageSource.getMessage("promotions.automation.please_select_gift_method", null, locale));
				}
			}
			case "member_upgrade", "member_vip_upgrade" -> {
				// no-op
			}
			default -> throw new ResourceException(
					messageSource.getMessage("promotions.automation.invalid_activity_type", null, locale));
		}
	}

	private static String stringOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof String s) {
			String t = s.trim();
			return t.isEmpty() ? null : t;
		}
		return null;
	}

	private static boolean nonEmptyNumberOrString(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return false;
		}
		return !"0".equals(s);
	}

	private String writeJson(Object value, Locale locale) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.automation.activity_persist_read_failed", null, locale));
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}

	public Map<String, Object> toActivityListRow(PromotionActivity row) {
		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		ZoneId z = ZoneId.systemDefault();

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("activity_id", row.getActivityId());
		out.put("company_id", row.getCompanyId());
		out.put("activity_type", row.getActivityType());
		out.put("title", row.getTitle());
		out.put("trigger_condition", readJsonTriggerOrDiscount(row.getTriggerCondition()));
		out.put("discount_config", readJsonTriggerOrDiscount(row.getDiscountConfig()));
		out.put("sms_params", readJsonMapOrNull(row.getSmsParams()));
		out.put("sms_isopen", row.getSmsIsopen());
		out.put("activity_status", row.getActivityStatus());
		Long bt = row.getBeginTime();
		Long et = row.getEndTime();
		if (bt != null) {
			out.put("begin_time", dayFmt.format(Instant.ofEpochSecond(bt).atZone(z)));
		} else {
			out.put("begin_time", null);
		}
		if (et != null) {
			out.put("end_time", dayFmt.format(Instant.ofEpochSecond(et).atZone(z)));
		} else {
			out.put("end_time", null);
		}
		Integer cr = row.getCreated();
		Integer up = row.getUpdated();
		out.put(
				"created",
				cr == null
						? null
						: dtFmt.format(Instant.ofEpochSecond(cr.longValue()).atZone(z)));
		out.put(
				"updated",
				up == null
						? null
						: dtFmt.format(Instant.ofEpochSecond(up.longValue()).atZone(z)));

		long nowSec = Instant.now().getEpochSecond();
		if ("valid".equals(row.getActivityStatus())) {
			if (row.getBeginTime() != null && row.getBeginTime() > nowSec) {
				out.put("status", "ready");
			} else {
				out.put("status", "processing");
			}
		} else {
			out.put("status", "invalid");
		}

		out.put("is_forever", Long.valueOf(FOREVER_END_EPOCH).equals(row.getEndTime()));
		return out;
	}

	private Object readJsonMapOrNull(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private Object readJsonTriggerOrDiscount(String json) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(json.trim());
			if (node == null || node.isNull() || node.isMissingNode()) {
				return null;
			}
			if ((node.isObject() || node.isArray()) && node.size() == 0) {
				return Collections.emptyList();
			}
			if (node.isObject()) {
				return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
			}
			if (node.isArray()) {
				return objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
			}
			return objectMapper.convertValue(node, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
