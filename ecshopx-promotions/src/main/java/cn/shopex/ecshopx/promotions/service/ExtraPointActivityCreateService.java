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
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import cn.shopex.ecshopx.promotions.domain.ExtraPointActivity;
import cn.shopex.ecshopx.promotions.mapper.ExtraPointActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExtraPointActivityCreateService {

	private static final int MAX_PAGE_SIZE = 500;

	private static final long FOREVER_END_EPOCH = 5000000000L;
	private static final DateTimeFormatter API_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final ExtraPointActivityMapper extraPointActivityMapper;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;
	private final MarketingActivityInfoAssemblySupport marketingActivityInfoAssemblySupport;
	private final DistributorListQueryService distributorListQueryService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;

	public ExtraPointActivityCreateService(
			ExtraPointActivityMapper extraPointActivityMapper,
			ObjectMapper objectMapper,
			MessageSource messageSource,
			MarketingActivityInfoAssemblySupport marketingActivityInfoAssemblySupport,
			DistributorListQueryService distributorListQueryService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService) {
		this.extraPointActivityMapper = extraPointActivityMapper;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
		this.marketingActivityInfoAssemblySupport = marketingActivityInfoAssemblySupport;
		this.distributorListQueryService = distributorListQueryService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
	}

	private String msg(String code, String zhCnFallback, Locale locale) {
		return messageSource.getMessage(code, null, zhCnFallback, locale);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createActivity(
			Map<String, Object> data,
			String requestLangTag,
			boolean requireConditionValueNonEmpty,
			boolean isForeverLiteralTrueEquality) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);

		Object titleObj = data.get("title");
		if (titleObj == null) {
			throw new BadRequestException(
					msg("promotions.extrapoint.activity_name_required_max_20", "活动名称必填且不能超过20个字", locale));
		}
		String title = titleObj.toString().trim();
		if (!StringUtils.hasText(title) || title.codePointCount(0, title.length()) > 20) {
			throw new BadRequestException(
					msg("promotions.extrapoint.activity_name_required_max_20", "活动名称必填且不能超过20个字", locale));
		}

		String cvRawStr = data.get("condition_value") == null ? "" : String.valueOf(data.get("condition_value")).trim();
		boolean cvPresent = StringUtils.hasText(cvRawStr);

		BigDecimal conditionBd;
		if (requireConditionValueNonEmpty && !cvPresent) {
			throw new BadRequestException(
					msg("promotions.extrapoint.set_activity_point_multiplier", "请设置活动积分倍数", locale));
		} else if (!requireConditionValueNonEmpty && !cvPresent) {
			conditionBd = BigDecimal.ZERO;
		} else {
			try {
				conditionBd = new BigDecimal(cvRawStr);
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						msg("promotions.extrapoint.set_correct_point_multiplier", "请设置正确的活动积分倍数", locale));
			}
		}
		if (conditionBd.compareTo(BigDecimal.ONE) < 0) {
			throw new BadRequestException(
					msg("promotions.extrapoint.set_correct_point_multiplier", "请设置正确的活动积分倍数", locale));
		}

		Object rawForever = data.get("is_forever");
		boolean forever =
				isForeverLiteralTrueEquality
						? (rawForever != null && "true".equals(Objects.toString(rawForever)))
						: (rawForever != null && Boolean.parseBoolean(Objects.toString(rawForever)));
		if (forever) {
			data.put("begin_time", Instant.now().getEpochSecond());
			data.put("end_time", FOREVER_END_EPOCH);
			data.remove("is_forever");
		} else {
			Object bt = data.get("begin_time");
			Object et = data.get("end_time");
			if (bt == null
					|| !StringUtils.hasText(String.valueOf(bt).trim())
					|| et == null
					|| !StringUtils.hasText(String.valueOf(et).trim())) {
				throw new BadRequestException(
						msg("promotions.extrapoint.fill_activity_time", "请填写活动时间", locale));
			}
			long beginEpoch = requireFlexibleEpochSeconds(bt, locale);
			long endEpoch = requireFlexibleEpochSeconds(et, locale);
			if (endEpoch <= Instant.now().getEpochSecond()) {
				throw new BadRequestException(
						msg("promotions.extrapoint.select_valid_time", "请选择有效的时间", locale));
			}
			data.put("begin_time", beginEpoch);
			data.put("end_time", endEpoch);
		}

		Object companyRaw = data.get("company_id");
		if (companyRaw == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long companyIdParsed =
				companyRaw instanceof Number n
						? n.longValue()
						: Long.parseLong(String.valueOf(companyRaw).trim());
		if (companyIdParsed <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		Long activityId = parseOptionalLong(data.get("activity_id"), "activity_id", locale);
		checkActiveValid(data, activityId, locale);
		checkActivityParams(data, locale);

		data.put("type", "member_day");
		data.put("activity_status", "valid");
		data.put("created", (int) Instant.now().getEpochSecond());

		Object taRaw = data.get("trigger_amount");
		if (taRaw != null) {
			BigDecimal taParsed;
			try {
				taParsed = new BigDecimal(String.valueOf(taRaw).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						messageSource.getMessage(
								"promotions.extrapoint.invalid_trigger_amount", null, "触发金额格式不正确", locale));
			}
			if (taParsed.compareTo(BigDecimal.ZERO) > 0) {
				Object tcObj = data.get("trigger_condition");
				if (!(tcObj instanceof Map<?, ?>)) {
					throw new BadRequestException(
							messageSource.getMessage(
									"promotions.automation.trigger_condition_must_be_map",
									null,
									"触发条件须为 JSON 对象",
									locale));
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> triggerCondition = (Map<String, Object>) tcObj;
				String triggerAmountPlain = taParsed.stripTrailingZeros().toPlainString();
				triggerCondition.put("trigger_amount", triggerAmountPlain);
				data.remove("trigger_amount");
			}
		}

		int nowEpochSeconds = (int) Instant.now().getEpochSecond();

		if (activityId != null && activityId > 0L) {
			ExtraPointActivity row =
					extraPointActivityMapper.selectOne(
							new LambdaQueryWrapper<ExtraPointActivity>()
									.eq(ExtraPointActivity::getActivityId, activityId)
									.eq(ExtraPointActivity::getCompanyId, companyIdParsed));
			if (row == null) {
				throw new ResourceException(
						msg("promotions.extrapoint.no_update_data_found", "未查询到更新数据", locale));
			}
			applyDataToEntity(row, data, locale, conditionBd, true);
			extraPointActivityMapper.updateById(row);
			ExtraPointActivity reloaded = extraPointActivityMapper.selectById(activityId);
			return entityToResponseMap(reloaded, nowEpochSeconds);
		}

		ExtraPointActivity entity = new ExtraPointActivity();
		applyDataToEntity(entity, data, locale, conditionBd, false);
		extraPointActivityMapper.insert(entity);
		ExtraPointActivity reloaded = extraPointActivityMapper.selectById(entity.getActivityId());
		return entityToResponseMap(reloaded, nowEpochSeconds);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateStatusInvalid(long companyId, Map<String, Object> mergedInput) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Object rawActivityId = mergedInput == null ? null : mergedInput.get("activity_id");
		if (rawActivityId == null || !StringUtils.hasText(String.valueOf(rawActivityId).trim())) {
			throw new ResourceException(
					msg("promotions.extrapoint.no_update_data_found", "未查询到更新数据", locale));
		}
		Long parsed = parseOptionalLong(rawActivityId, "activity_id", locale);
		long activityId = parsed == null ? 0L : parsed.longValue();
		ExtraPointActivity row =
				extraPointActivityMapper.selectOne(
						new LambdaQueryWrapper<ExtraPointActivity>()
								.eq(ExtraPointActivity::getCompanyId, companyId)
								.eq(ExtraPointActivity::getActivityId, activityId));
		if (row == null) {
			throw new ResourceException(
					msg("promotions.extrapoint.no_update_data_found", "未查询到更新数据", locale));
		}
		row.setActivityStatus("invalid");
		row.setUpdated((int) Instant.now().getEpochSecond());
		extraPointActivityMapper.updateById(row);
		ExtraPointActivity reloaded = extraPointActivityMapper.selectById(row.getActivityId());
		if (reloaded == null) {
			throw new ResourceException(
					msg("promotions.extrapoint.no_update_data_found", "未查询到更新数据", locale));
		}
		int nowEpochSeconds = (int) Instant.now().getEpochSecond();
		return entityToResponseMap(reloaded, nowEpochSeconds);
	}

	public Map<String, Object> getActivityList(
			long companyId,
			String beginTimeRaw,
			String endTimeRaw,
			String titleRaw,
			int page,
			int pageSize) {
		int p = Math.max(1, page);
		int ps = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));

		LambdaQueryWrapper<ExtraPointActivity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ExtraPointActivity::getCompanyId, companyId);

		if (!isLooseEmpty(titleRaw)) {
			String titleTrimmed = titleRaw.trim();
			if (StringUtils.hasText(titleTrimmed)) {
				String escaped = escapeSqlLike(titleTrimmed);
				wrapper.apply("title LIKE CONCAT('%',{0},'%') ESCAPE '\\\\'", escaped);
			}
		}

		if (!isLooseEmpty(beginTimeRaw)) {
			long beginBound = coerceFilterScalarToEpochSeconds(beginTimeRaw);
			wrapper.ge(ExtraPointActivity::getBeginTime, beginBound);
			Long endBound =
					(endTimeRaw != null && !isLooseEmpty(endTimeRaw))
							? Long.valueOf(coerceFilterScalarToEpochSeconds(endTimeRaw))
							: null;
			wrapper.apply("end_time <= {0}", endBound);
		}

		wrapper.orderByDesc(ExtraPointActivity::getCreated);

		long total = extraPointActivityMapper.selectCount(wrapper);
		List<Map<String, Object>> rows = new ArrayList<>();
		if (total > 0) {
			int nowEpoch = (int) Instant.now().getEpochSecond();
			Page<ExtraPointActivity> pg = new Page<>(p, ps, false);
			extraPointActivityMapper.selectPage(pg, wrapper);
			for (ExtraPointActivity entity : pg.getRecords()) {
				rows.add(entityToResponseMap(entity, nowEpoch));
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	public Map<String, Object> getActivityInfo(long companyId, long activityId) {
		if (activityId <= 0L) {
			return new LinkedHashMap<>();
		}
		ExtraPointActivity row =
				extraPointActivityMapper.selectOne(
						new LambdaQueryWrapper<ExtraPointActivity>()
								.eq(ExtraPointActivity::getCompanyId, companyId)
								.eq(ExtraPointActivity::getActivityId, activityId));
		if (row == null) {
			return new LinkedHashMap<>();
		}
		int nowEpoch = (int) Instant.now().getEpochSecond();
		Map<String, Object> result = entityToResponseMap(row, nowEpoch);
		Object shopIdsVal = result.get("shop_ids");
		List<Long> filtered = marketingActivityInfoAssemblySupport.filterTruthyShopIds(shopIdsVal);
		if (!filtered.isEmpty()) {
			List<Distributor> dists = distributorListQueryService.listByIdsAndCompany(companyId, filtered);
			List<Map<String, Object>> storeRows = new ArrayList<>();
			for (Distributor d : dists) {
				long did = d.getDistributorId() != null ? d.getDistributorId() : 0L;
				int ds = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
				Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
				storeRows.add(distributorListRowFormatService.formatStoreRow(d, setting, objectMapper));
			}
			result.put("storeLists", storeRows);
		} else if (!isLooseEmpty(shopIdsVal)) {
			result.put("storeLists", List.of());
		}
		return result;
	}

	private static boolean isLooseEmpty(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}

	private static long coerceFilterScalarToEpochSeconds(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		if (t.matches("^-?\\d+$")) {
			try {
				long n = Long.parseLong(t);
				if (Math.abs(n) >= 1_000_000_000_000L) {
					n = n / 1000;
				}
				return n;
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return LeadingNumberParser.parseAsLong(t);
	}

	private static String escapeSqlLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private void applyDataToEntity(
			ExtraPointActivity entity, Map<String, Object> data, Locale locale, BigDecimal conditionBd, boolean isUpdate) {
		entity.setCompanyId(((Number) data.get("company_id")).longValue());
		entity.setType(String.valueOf(data.get("type")));
		entity.setTitle(String.valueOf(data.get("title")).trim());

		Object tc = data.get("trigger_condition");
		try {
			entity.setTriggerCondition(objectMapper.writeValueAsString(tc));
		} catch (JsonProcessingException e) {
			throw new BadRequestException(
					msg("promotions.extrapoint.trigger_condition_json_error", "触发条件格式错误", locale));
		}

		if (data.containsKey("condition_type")) {
			Object ctRaw = data.get("condition_type");
			if (StringUtils.hasText(String.valueOf(ctRaw).trim())) {
				try {
					entity.setConditionType(objectMapper.writeValueAsString(data.get("condition_type")));
				} catch (JsonProcessingException e) {
					throw new BadRequestException(
							msg("promotions.extrapoint.trigger_condition_json_error", "触发条件格式错误", locale));
				}
			} else {
				entity.setConditionType(null);
			}
		} else if (!isUpdate) {
			entity.setConditionType(null);
		}

		entity.setConditionValue(conditionBd.intValue());

		if (!isUpdate || data.containsKey("valid_grade")) {
			Object vg = data.get("valid_grade");
			if (vg == null) {
				entity.setValidGrade(null);
			} else if (vg instanceof Collection<?> col) {
				if (col.isEmpty()) {
					entity.setValidGrade(null);
				} else {
					try {
						entity.setValidGrade(objectMapper.writeValueAsString(col));
					} catch (JsonProcessingException e) {
						throw new BadRequestException(
								msg("promotions.extrapoint.trigger_condition_json_error", "触发条件格式错误", locale));
					}
				}
			} else if (StringUtils.hasText(String.valueOf(vg).trim())) {
				try {
					entity.setValidGrade(objectMapper.writeValueAsString(vg));
				} catch (JsonProcessingException e) {
					throw new BadRequestException(
							msg("promotions.extrapoint.trigger_condition_json_error", "触发条件格式错误", locale));
				}
			} else {
				entity.setValidGrade(null);
			}
		}

		if (data.containsKey("use_shop")) {
			entity.setUseShop(String.valueOf(data.get("use_shop")));
		}

		if (data.containsKey("shop_ids")) {
			Object shops = data.get("shop_ids");
			if (shops == null) {
				entity.setShopIds(null);
			} else if (shops instanceof Collection<?> col) {
				if (col.isEmpty()) {
					entity.setShopIds(null);
				} else {
					List<String> ids = new ArrayList<>();
					for (Object o : col) {
						ids.add(o == null ? "" : o.toString());
					}
					entity.setShopIds("," + String.join(",", ids) + ",");
				}
			} else {
				entity.setShopIds(String.valueOf(shops));
			}
		} else {
			entity.setShopIds(null);
		}

		entity.setBeginTime(((Number) data.get("begin_time")).longValue());
		entity.setEndTime(((Number) data.get("end_time")).longValue());
		entity.setActivityStatus(String.valueOf(data.get("activity_status")));

		if (data.containsKey("created")) {
			Object c = data.get("created");
			entity.setCreated(c instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(c).trim()));
		}
		entity.setUpdated((int) Instant.now().getEpochSecond());
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
			Long parsed = DateExpressionParser.parseToEpochSecond(t, ZoneId.systemDefault());
			if (parsed == null) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
			}
			return parsed;
		}
		throw new BadRequestException(
				messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
	}

	private Long parseOptionalLong(Object raw, String fieldKey, Locale locale) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			String trimmed = s.trim();
			if (!StringUtils.hasText(trimmed)) {
				return null;
			}
			try {
				return Long.parseLong(trimmed);
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						messageSource.getMessage(
								"promotions.extrapoint.invalid_optional_long_field",
								new Object[] {fieldKey},
								"参数「{0}」格式不正确",
								locale));
			}
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		throw new BadRequestException(
				messageSource.getMessage(
						"promotions.extrapoint.invalid_optional_long_field",
						new Object[] {fieldKey},
						"参数「{0}」格式不正确",
						locale));
	}

	private void checkActiveValid(Map<String, Object> data, Long activityId, Locale locale) {
		long companyId = ((Number) data.get("company_id")).longValue();
		long dataBeginTime = ((Number) data.get("begin_time")).longValue();
		long dataEndTime = ((Number) data.get("end_time")).longValue();

		LambdaQueryWrapper<ExtraPointActivity> w = new LambdaQueryWrapper<>();
		w.eq(ExtraPointActivity::getCompanyId, companyId)
				.eq(ExtraPointActivity::getActivityStatus, "valid")
				.le(ExtraPointActivity::getBeginTime, dataEndTime)
				.ge(ExtraPointActivity::getEndTime, dataBeginTime);
		if (activityId != null && activityId > 0L) {
			w.ne(ExtraPointActivity::getActivityId, activityId);
		}
		long cnt = extraPointActivityMapper.selectCount(w);
		if (cnt > 0) {
			throw new ResourceException(
					msg("promotions.extrapoint.same_time_only_one_activity", "同一时间段只允许有一个生效活动", locale));
		}
	}

	private void checkActivityParams(Map<String, Object> data, Locale locale) {
		BigDecimal ta;
		try {
			ta = new BigDecimal(String.valueOf(data.getOrDefault("trigger_amount", "0")).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.extrapoint.invalid_trigger_amount", null, "触发金额格式不正确", locale));
		}
		if (ta.compareTo(BigDecimal.ZERO) < 0) {
			throw new BadRequestException(
					msg("promotions.extrapoint.order_amount_must_positive", "订单金额必须为正值", locale));
		}

		Object tcObj = data.get("trigger_condition");
		if (!(tcObj instanceof Map<?, ?>)) {
			throw new BadRequestException(
					messageSource.getMessage(
							"promotions.automation.trigger_condition_must_be_map",
							null,
							"触发条件须为 JSON 对象",
							locale));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> triggerCondition = (Map<String, Object>) tcObj;
		Object ttObj = triggerCondition.get("trigger_time");
		if (!(ttObj instanceof Map<?, ?>)) {
			throw new BadRequestException(
					msg("promotions.extrapoint.please_select_specific_gift_date", "请选择具体赠送的日期", locale));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> triggerTime = (Map<String, Object>) ttObj;

		String type = String.valueOf(triggerTime.getOrDefault("type", "")).trim();
		if (!Set.of("every_year", "every_month", "every_week", "date").contains(type)) {
			throw new BadRequestException(
					msg("promotions.extrapoint.please_select_gift_method", "请选择赠送方式", locale));
		}

		switch (type) {
			case "every_year" -> {
				String month = Objects.toString(triggerTime.get("month"), "").trim();
				String day = Objects.toString(triggerTime.get("day"), "").trim();
				if (!StringUtils.hasText(month) || !StringUtils.hasText(day)) {
					throw new BadRequestException(
							msg(
									"promotions.extrapoint.please_select_specific_gift_date",
									"请选择具体赠送的日期",
									locale));
				}
			}
			case "every_month" -> {
				String day = Objects.toString(triggerTime.get("day"), "").trim();
				if (!StringUtils.hasText(day)) {
					throw new BadRequestException(
							msg(
									"promotions.extrapoint.please_select_specific_gift_date",
									"请选择具体赠送的日期",
									locale));
				}
			}
			case "every_week" -> {
				String week = Objects.toString(triggerTime.get("week"), "").trim();
				if (!StringUtils.hasText(week)) {
					throw new BadRequestException(
							msg(
									"promotions.extrapoint.please_select_specific_gift_date",
									"请选择具体赠送的日期",
									locale));
				}
			}
			case "date" -> {
				if (!StringUtils.hasText(Objects.toString(triggerTime.get("begin_time"), "").trim())) {
					throw new BadRequestException(
							msg(
									"promotions.extrapoint.please_select_specific_gift_date",
									"请选择具体赠送的日期",
									locale));
				}
			}
			default -> {
			}
		}
	}

	private Map<String, Object> entityToResponseMap(ExtraPointActivity row, int nowEpochSeconds) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("activity_id", row.getActivityId());
		m.put("company_id", row.getCompanyId());
		m.put("type", row.getType());
		m.put("title", row.getTitle());

		Map<String, Object> triggerMap = new LinkedHashMap<>();
		String tcJson = row.getTriggerCondition();
		if (StringUtils.hasText(tcJson)) {
			try {
				triggerMap = objectMapper.readValue(tcJson.trim(), new TypeReference<Map<String, Object>>() {});
			} catch (JsonProcessingException e) {
				triggerMap = new LinkedHashMap<>();
			}
		}
		m.put("trigger_condition", triggerMap);

		m.put("condition_value", row.getConditionValue());

		Object conditionTypeOut = decodeConditionTypeForResponse(row.getConditionType());
		m.put("condition_type", conditionTypeOut);

		String vg = row.getValidGrade();
		if (!StringUtils.hasText(vg)) {
			m.put("valid_grade", List.of());
		} else {
			try {
				m.put("valid_grade", objectMapper.readValue(vg.trim(), new TypeReference<List<Object>>() {}));
			} catch (JsonProcessingException e) {
				m.put("valid_grade", List.of());
			}
		}

		m.put("use_shop", row.getUseShop() == null ? "" : row.getUseShop());

		String shopIds = row.getShopIds();
		if (!StringUtils.hasText(shopIds) || "all".equals(shopIds.trim())) {
			m.put("shop_ids", List.of());
		} else {
			String[] parts = shopIds.split(",", -1);
			m.put("shop_ids", Arrays.asList(parts));
		}

		Long bt = row.getBeginTime();
		Long et = row.getEndTime();
		m.put("begin_time", bt == null ? null : API_TIME.format(Instant.ofEpochSecond(bt)));
		m.put("end_time", et == null ? null : API_TIME.format(Instant.ofEpochSecond(et)));
		Integer cr = row.getCreated();
		Integer up = row.getUpdated();
		m.put("created", cr == null ? null : API_TIME.format(Instant.ofEpochSecond(cr.longValue())));
		m.put("updated", up == null ? null : API_TIME.format(Instant.ofEpochSecond(up.longValue())));

		String dbStatus = row.getActivityStatus();
		String displayStatus;
		if (!"valid".equals(dbStatus)) {
			displayStatus = "end";
		} else {
			long now = nowEpochSeconds;
			long b = bt == null ? 0L : bt;
			long e = et == null ? 0L : et;
			if (b > now) {
				displayStatus = "ready";
			} else if (e > now) {
				displayStatus = "processing";
			} else {
				displayStatus = "end";
			}
		}
		m.put("activity_status", displayStatus);

		boolean isForever = row.getEndTime() != null && row.getEndTime() == FOREVER_END_EPOCH;
		m.put("is_forever", isForever);
		return m;
	}

	private Object decodeConditionTypeForResponse(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			Object decoded = objectMapper.readValue(raw.trim(), Object.class);
			if (decoded instanceof String s && StringUtils.hasText(s)) {
				try {
					return objectMapper.readValue(s.trim(), Object.class);
				} catch (JsonProcessingException e) {
					return decoded;
				}
			}
			return decoded;
		} catch (JsonProcessingException e) {
			return raw.trim();
		}
	}
}
