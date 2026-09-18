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
import cn.shopex.ecshopx.promotions.domain.PointUpvaluation;
import cn.shopex.ecshopx.promotions.mapper.PointUpvaluationMapper;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionActivityMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionActivityMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class PointupvaluationActivityCreateService {

	private static final Logger log = LoggerFactory.getLogger(PointupvaluationActivityCreateService.class);

	private static final long FOREVER_END_EPOCH = 5000000000L;
	private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
	private static final Pattern TRIGGER_END_DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

	private final PointUpvaluationMapper pointUpvaluationMapper;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;
	private final PromotionActivityMultiLangWriteService promotionActivityMultiLangWriteService;
	private final PromotionActivityMultiLangReadService promotionActivityMultiLangReadService;

	public PointupvaluationActivityCreateService(
			PointUpvaluationMapper pointUpvaluationMapper,
			ObjectMapper objectMapper,
			MessageSource messageSource,
			PromotionActivityMultiLangWriteService promotionActivityMultiLangWriteService,
			PromotionActivityMultiLangReadService promotionActivityMultiLangReadService) {
		this.pointUpvaluationMapper = pointUpvaluationMapper;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
		this.promotionActivityMultiLangWriteService = promotionActivityMultiLangWriteService;
		this.promotionActivityMultiLangReadService = promotionActivityMultiLangReadService;
	}

	private String msg(String code, String zhCnFallback, Locale locale) {
		return messageSource.getMessage(code, null, zhCnFallback, locale);
	}

	private static boolean isForeverTrue(Object rawIsForever) {
		return rawIsForever != null && "true".equalsIgnoreCase(String.valueOf(rawIsForever).trim());
	}

	private long parseFlexibleEpochSeconds(Object v, Locale locale) {
		String bad = msg("promotions.pointupvaluation.invalid_time_format", "时间格式不正确", locale);
		if (v == null) {
			throw new BadRequestException(bad);
		}
		if (v instanceof Boolean || v instanceof Map<?, ?> || v instanceof Iterable<?>) {
			throw new BadRequestException(bad);
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
				throw new BadRequestException(bad);
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
					throw new BadRequestException(bad);
				}
			}
			try {
				LocalDate localDate = LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
				return localDate.atStartOfDay(SYSTEM_ZONE).toEpochSecond();
			} catch (DateTimeParseException ignored) {
				// fall through
			}
			try {
				LocalDateTime ldt = LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				return ldt.atZone(SYSTEM_ZONE).toEpochSecond();
			} catch (DateTimeParseException e) {
				throw new BadRequestException(bad);
			}
		}
		throw new BadRequestException(bad);
	}

	private List<Object> normalizeToObjectList(Object raw, Locale locale) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				return new ArrayList<>();
			}
			try {
				return objectMapper.readValue(s.trim(), new TypeReference<List<Object>>() {});
			} catch (JsonProcessingException e) {
				throw new BadRequestException(
						msg("promotions.pointupvaluation.invalid_json_array", "数组参数格式不正确", locale));
			}
		}
		if (raw instanceof Collection<?> col) {
			return new ArrayList<>(col);
		}
		return new ArrayList<>(List.of(raw));
	}

	private Map<String, Object> normalizeTriggerCondition(Object raw, Locale locale) {
		if (!(raw instanceof Map<?, ?> rootRaw)) {
			throw new BadRequestException(
					msg(
							"promotions.pointupvaluation.trigger_condition_must_be_object",
							"触发条件须为 JSON 对象",
							locale));
		}
		LinkedHashMap<String, Object> root = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : rootRaw.entrySet()) {
			if (e.getKey() != null) {
				root.put(e.getKey().toString(), e.getValue());
			}
		}
		Object ttObj = root.get("trigger_time");
		if (!(ttObj instanceof Map<?, ?>)) {
			throw new BadRequestException(
					msg(
							"promotions.pointupvaluation.trigger_condition_must_be_object",
							"触发条件须为 JSON 对象",
							locale));
		}
		Map<?, ?> ttMap = (Map<?, ?>) ttObj;
		LinkedHashMap<String, Object> triggerTime = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ttMap.entrySet()) {
			if (e.getKey() != null) {
				triggerTime.put(e.getKey().toString(), e.getValue());
			}
		}
		root.put("trigger_time", triggerTime);
		return root;
	}

	private void validateAndNormalizePointupvaluationPayload(Map<String, Object> data, Locale locale) {
		Object titleObj = data.get("title");
		if (titleObj == null || !StringUtils.hasText(String.valueOf(titleObj).trim())) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.activity_title_required", "活动名称必填", locale));
		}

		Object companyRaw = data.get("company_id");
		if (companyRaw == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		long companyIdParsed;
		try {
			companyIdParsed =
					companyRaw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(companyRaw).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyIdParsed <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		if (!isForeverTrue(data.get("is_forever"))) {
			Object bt = data.get("begin_time");
			Object et = data.get("end_time");
			if (bt == null || !StringUtils.hasText(String.valueOf(bt).trim())) {
				throw new BadRequestException(
						msg(
								"promotions.pointupvaluation.activity_begin_time_required",
								"活动开始时间必填",
								locale));
			}
			if (et == null || !StringUtils.hasText(String.valueOf(et).trim())) {
				throw new BadRequestException(
						msg("promotions.pointupvaluation.activity_end_time_required", "活动结束时间必填", locale));
			}
		}

		Object tcRaw = data.get("trigger_condition");
		if (tcRaw == null || !(tcRaw instanceof Map<?, ?>)) {
			throw new BadRequestException(msg("promotions.pointupvaluation.date_required", "日期必填", locale));
		}

		Object uvRaw = data.get("upvaluation");
		if (uvRaw == null || !StringUtils.hasText(String.valueOf(uvRaw).trim())) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.upvaluation_required", "升值倍数必填", locale));
		}

		Object maxRaw = data.get("max_up_point");
		if (maxRaw == null || !StringUtils.hasText(String.valueOf(maxRaw).trim())) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.max_up_point_required", "升值上限必填", locale));
		}

		if (!data.containsKey("used_scene")) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.used_scene_required", "应用场景必填", locale));
		}
		Object usedSceneVal = data.get("used_scene");
		if (usedSceneVal == null || (usedSceneVal instanceof String us && !StringUtils.hasText(us.trim()))) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.used_scene_required", "应用场景必填", locale));
		}
		List<Object> usedSceneList = normalizeToObjectList(usedSceneVal, locale);
		if (CollectionUtils.isEmpty(usedSceneList)) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.used_scene_required", "应用场景必填", locale));
		}

		List<Object> validGradeList = normalizeToObjectList(data.get("valid_grade"), locale);
		if (CollectionUtils.isEmpty(validGradeList)) {
			throw new BadRequestException(
					msg(
							"promotions.pointupvaluation.please_select_at_least_one_applicable_member",
							"请至少选择一个适用会员",
							locale));
		}

		if (!isForeverTrue(data.get("is_forever"))) {
			String beginStr = Objects.toString(data.get("begin_time"), "").trim();
			String endStr = Objects.toString(data.get("end_time"), "").trim();
			if (beginStr.compareTo(endStr) >= 0) {
				throw new BadRequestException(
						msg(
								"promotions.pointupvaluation.activity_start_time_cannot_greater_than_end_time",
								"活动开始时间不能大于结束时间",
								locale));
			}
		}

		int uv;
		try {
			double d = Double.parseDouble(String.valueOf(uvRaw).trim());
			uv = (int) Math.floor(d);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.upvaluation_invalid", "升值倍数格式不正确", locale));
		}
		if (uv <= 1) {
			throw new BadRequestException(
					msg(
							"promotions.pointupvaluation.upvaluation_must_be_integer_greater_than_one",
							"升值倍数必须是大于1的整数",
							locale));
		}

		data.put("upvaluation", uv);
		if (isForeverTrue(data.get("is_forever"))) {
			data.put("begin_time", Instant.now().getEpochSecond());
			data.put("end_time", FOREVER_END_EPOCH);
			data.remove("is_forever");
		} else {
			data.put("begin_time", parseFlexibleEpochSeconds(data.get("begin_time"), locale));
			data.put("end_time", parseFlexibleEpochSeconds(data.get("end_time"), locale));
		}

		Map<String, Object> triggerRoot = normalizeTriggerCondition(data.get("trigger_condition"), locale);
		Object ttForMutate = triggerRoot.get("trigger_time");
		Map<?, ?> ttRead = (Map<?, ?>) ttForMutate;
		LinkedHashMap<String, Object> triggerTime = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ttRead.entrySet()) {
			if (e.getKey() != null) {
				triggerTime.put(e.getKey().toString(), e.getValue());
			}
		}
		String type = String.valueOf(triggerTime.getOrDefault("type", "")).trim();
		switch (type) {
			case "every_year" -> {
				triggerTime.put("week", "");
				triggerTime.put("begin_time", "");
				triggerTime.put("end_time", "");
				if (!StringUtils.hasText(Objects.toString(triggerTime.get("month"), "").trim())
						|| !StringUtils.hasText(Objects.toString(triggerTime.get("day"), "").trim())) {
					throw new BadRequestException(
							msg("promotions.pointupvaluation.please_select_date", "请选择日期", locale));
				}
			}
			case "every_month" -> {
				triggerTime.put("month", "");
				triggerTime.put("week", "");
				triggerTime.put("begin_time", "");
				triggerTime.put("end_time", "");
				if (!StringUtils.hasText(Objects.toString(triggerTime.get("day"), "").trim())) {
					throw new BadRequestException(
							msg("promotions.pointupvaluation.please_select_date", "请选择日期", locale));
				}
			}
			case "every_week" -> {
				triggerTime.put("month", "");
				triggerTime.put("day", "");
				triggerTime.put("begin_time", "");
				triggerTime.put("end_time", "");
				if (!StringUtils.hasText(Objects.toString(triggerTime.get("week"), "").trim())) {
					throw new BadRequestException(
							msg("promotions.pointupvaluation.please_select_date", "请选择日期", locale));
				}
			}
			case "date" -> {
				triggerTime.put("month", "");
				triggerTime.put("day", "");
				triggerTime.put("week", "");
				if (!StringUtils.hasText(Objects.toString(triggerTime.get("begin_time"), "").trim())
						|| !StringUtils.hasText(Objects.toString(triggerTime.get("end_time"), "").trim())) {
					throw new BadRequestException(
							msg("promotions.pointupvaluation.please_select_date", "请选择日期", locale));
				}
				long bt = parseFlexibleEpochSeconds(triggerTime.get("begin_time"), locale);
				String endRaw = Objects.toString(triggerTime.get("end_time"), "").trim();
				if (!TRIGGER_END_DATE_ONLY.matcher(endRaw).matches()) {
					throw new BadRequestException(
							msg("promotions.pointupvaluation.invalid_time_format", "时间格式不正确", locale));
				}
				long et;
				try {
					LocalDate endDate = LocalDate.parse(endRaw, DateTimeFormatter.ISO_LOCAL_DATE);
					et = endDate.atTime(LocalTime.MAX).atZone(SYSTEM_ZONE).toEpochSecond();
				} catch (DateTimeParseException e) {
					throw new BadRequestException(
							msg("promotions.pointupvaluation.invalid_time_format", "时间格式不正确", locale));
				}
				triggerTime.put("begin_time", bt);
				triggerTime.put("end_time", et);
			}
			default -> throw new BadRequestException(
					msg("promotions.pointupvaluation.please_select_date", "请选择日期", locale));
		}
		triggerRoot.put("trigger_time", triggerTime);
		data.put("trigger_condition", triggerRoot);
		data.put("used_scene", usedSceneList);
		data.put("valid_grade", validGradeList);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createActivity(Map<String, Object> data, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		validateAndNormalizePointupvaluationPayload(data, locale);

		int now = (int) Instant.now().getEpochSecond();
		Object maxRaw = data.get("max_up_point");
		List<Object> usedSceneList = normalizeToObjectList(data.get("used_scene"), locale);
		List<Object> validGradeList = normalizeToObjectList(data.get("valid_grade"), locale);
		PointUpvaluation entity = new PointUpvaluation();
		entity.setCompanyId(((Number) data.get("company_id")).longValue());
		entity.setTitle(String.valueOf(data.get("title")).trim());
		entity.setBeginTime(((Number) data.get("begin_time")).longValue());
		entity.setEndTime(((Number) data.get("end_time")).longValue());
		entity.setUpvaluation((Integer) data.get("upvaluation"));
		int maxUpPoint;
		try {
			maxUpPoint =
					maxRaw instanceof Number n
							? n.intValue()
							: Integer.parseInt(String.valueOf(maxRaw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.max_up_point_invalid", "升值上限须为有效整数", locale));
		}
		entity.setMaxUpPoint(maxUpPoint);
		try {
			entity.setTriggerCondition(objectMapper.writeValueAsString(data.get("trigger_condition")));
			entity.setValidGrade(objectMapper.writeValueAsString(validGradeList));
			entity.setUsedScene(objectMapper.writeValueAsString(usedSceneList));
		} catch (JsonProcessingException e) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.invalid_json_array", "数组参数格式不正确", locale));
		}
		entity.setCreated(now);
		entity.setUpdated(now);
		pointUpvaluationMapper.insert(entity);
		PointUpvaluation reloaded = pointUpvaluationMapper.selectById(entity.getActivityId());
		if (reloaded == null) {
			throw new ResourceException(msg("promotions.automation.activity_persist_read_failed", "活动创建后读取失败", locale));
		}

		promotionActivityMultiLangWriteService.addTitleForNewActivity(
				reloaded.getActivityId(), reloaded.getCompanyId(), data, requestLangTag);
		log.debug(
				"pointupvaluation multilang title persisted activityId={} companyId={}",
				reloaded.getActivityId(),
				reloaded.getCompanyId());

		return toApiRow(reloaded);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateActivity(Map<String, Object> data, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Object activityIdObj = data.get("activity_id");
		if (activityIdObj == null || !StringUtils.hasText(String.valueOf(activityIdObj).trim())) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.activity_id_required", "活动ID必填", locale));
		}
		long activityId;
		try {
			activityId =
					activityIdObj instanceof Number n
							? n.longValue()
							: Long.parseLong(String.valueOf(activityIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.activity_id_invalid", "活动ID无效", locale));
		}
		if (activityId <= 0L) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.activity_id_invalid", "活动ID无效", locale));
		}

		validateAndNormalizePointupvaluationPayload(data, locale);

		long companyId = ((Number) data.get("company_id")).longValue();
		PointUpvaluation existing =
				pointUpvaluationMapper.selectOne(
						new LambdaQueryWrapper<PointUpvaluation>()
								.eq(PointUpvaluation::getCompanyId, companyId)
								.eq(PointUpvaluation::getActivityId, activityId));
		if (existing == null) {
			throw new ResourceException(
					msg("promotions.pointupvaluation.no_update_data_found", "未查询到更新数据", locale));
		}

		Object maxRaw = data.get("max_up_point");
		int maxUpPoint;
		try {
			maxUpPoint =
					maxRaw instanceof Number n
							? n.intValue()
							: Integer.parseInt(String.valueOf(maxRaw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.max_up_point_invalid", "升值上限须为有效整数", locale));
		}

		List<Object> usedSceneList = normalizeToObjectList(data.get("used_scene"), locale);
		List<Object> validGradeList = normalizeToObjectList(data.get("valid_grade"), locale);

		existing.setTitle(String.valueOf(data.get("title")).trim());
		existing.setBeginTime(((Number) data.get("begin_time")).longValue());
		existing.setEndTime(((Number) data.get("end_time")).longValue());
		existing.setUpvaluation((Integer) data.get("upvaluation"));
		existing.setMaxUpPoint(maxUpPoint);
		try {
			existing.setTriggerCondition(objectMapper.writeValueAsString(data.get("trigger_condition")));
			existing.setValidGrade(objectMapper.writeValueAsString(validGradeList));
			existing.setUsedScene(objectMapper.writeValueAsString(usedSceneList));
		} catch (JsonProcessingException e) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.invalid_json_array", "数组参数格式不正确", locale));
		}
		existing.setUpdated((int) Instant.now().getEpochSecond());

		int rows = pointUpvaluationMapper.updateById(existing);
		if (rows == 0) {
			throw new ResourceException(
					msg("promotions.pointupvaluation.no_update_data_found", "未查询到更新数据", locale));
		}

		PointUpvaluation reloaded = pointUpvaluationMapper.selectById(activityId);
		if (reloaded == null) {
			throw new ResourceException(
					msg("promotions.automation.activity_persist_read_failed", "活动创建后读取失败", locale));
		}

		promotionActivityMultiLangWriteService.addTitleForNewActivity(
				reloaded.getActivityId(), reloaded.getCompanyId(), data, requestLangTag);
		log.debug(
				"pointupvaluation multilang title persisted (update) activityId={} companyId={}",
				reloaded.getActivityId(),
				reloaded.getCompanyId());

		return toApiRow(reloaded);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getActivityInfo(long companyId, String activityIdRaw, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		if (activityIdRaw == null || !StringUtils.hasText(activityIdRaw.trim())) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.activity_id_required", "活动ID必填", locale));
		}
		String trimmedId = activityIdRaw.trim();
		long activityId;
		try {
			activityId = Long.parseLong(trimmedId);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.activity_id_invalid", "活动ID无效", locale));
		}
		if (activityId <= 0L) {
			throw new BadRequestException(
					msg("promotions.pointupvaluation.activity_id_invalid", "活动ID无效", locale));
		}

		PointUpvaluation existing =
				pointUpvaluationMapper.selectOne(
						new LambdaQueryWrapper<PointUpvaluation>()
								.eq(PointUpvaluation::getCompanyId, companyId)
								.eq(PointUpvaluation::getActivityId, activityId));
		if (existing == null) {
			throw new ResourceException(
					msg("promotions.pointupvaluation.no_update_data_found", "未查询到更新数据", locale));
		}

		Map<String, Object> row = toApiRow(existing);
		promotionActivityMultiLangReadService.applyTitles(Collections.singletonList(row), requestLangTag);
		enrichPointupvaluationGetInfoMap(row);
		return row;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> getActivityList(
			long companyId,
			String title,
			String activityStatus,
			String beginTimeQuery,
			String endTimeQuery,
			int page,
			int pageSize,
			String requestLangTag) {
		long nowSec = Instant.now().getEpochSecond();
		LambdaQueryWrapper<PointUpvaluation> wrapper =
				buildPointupvaluationListWrapper(
						companyId, title, activityStatus, beginTimeQuery, endTimeQuery, nowSec);

		Page<PointUpvaluation> pg = new Page<>(page, pageSize);
		pointUpvaluationMapper.selectPage(pg, wrapper);
		long total = pg.getTotal();

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		if (total == 0L) {
			result.put("list", Collections.emptyList());
			return result;
		}

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (PointUpvaluation entity : pg.getRecords()) {
			listMaps.add(toApiRow(entity));
		}
		promotionActivityMultiLangReadService.applyTitles(listMaps, requestLangTag);
		for (Map<String, Object> row : listMaps) {
			enrichListRowAfterQuery(row, nowSec);
		}
		result.put("list", listMaps);
		return result;
	}

	private long parseListFilterEpoch(String trimmed) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		String bad = msg("promotions.pointupvaluation.invalid_time_format", "时间格式不正确", locale);
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException(bad);
		}
		String t = trimmed.trim();
		if (!t.matches("^-?\\d+$")) {
			throw new BadRequestException(bad);
		}
		try {
			long raw = Long.parseLong(t);
			if (raw >= 1_000_000_000_000L) {
				raw = raw / 1000;
			}
			return raw;
		} catch (NumberFormatException e) {
			throw new BadRequestException(bad);
		}
	}

	private LambdaQueryWrapper<PointUpvaluation> buildPointupvaluationListWrapper(
			long companyId,
			String title,
			String activityStatus,
			String beginTimeQuery,
			String endTimeQuery,
			long nowSec) {
		boolean userRange =
				StringUtils.hasText(beginTimeQuery) && StringUtils.hasText(endTimeQuery);
		Long userBegin = null;
		Long userEnd = null;
		if (userRange) {
			userBegin = parseListFilterEpoch(beginTimeQuery.trim());
			userEnd = parseListFilterEpoch(endTimeQuery.trim());
		}

		String status = activityStatus == null ? "" : activityStatus.trim();
		LambdaQueryWrapper<PointUpvaluation> w =
				new LambdaQueryWrapper<PointUpvaluation>().eq(PointUpvaluation::getCompanyId, companyId);

		if (StringUtils.hasText(title)) {
			w.like(PointUpvaluation::getTitle, "%" + title.trim() + "%");
		}

		switch (status) {
			case "waiting" -> {
				if (userRange) {
					w.ge(PointUpvaluation::getBeginTime, userBegin)
							.ge(PointUpvaluation::getEndTime, nowSec)
							.le(PointUpvaluation::getEndTime, userEnd);
				} else {
					w.ge(PointUpvaluation::getBeginTime, nowSec).ge(PointUpvaluation::getEndTime, nowSec);
				}
			}
			case "ongoing" -> {
				w.le(PointUpvaluation::getBeginTime, nowSec).gt(PointUpvaluation::getEndTime, nowSec);
				if (userRange) {
					w.ge(PointUpvaluation::getBeginTime, userBegin).le(PointUpvaluation::getEndTime, userEnd);
				}
			}
			case "it_has_ended" -> {
				w.le(PointUpvaluation::getEndTime, nowSec);
				if (userRange) {
					w.ge(PointUpvaluation::getBeginTime, userBegin).le(PointUpvaluation::getEndTime, userEnd);
				}
			}
			default -> {
				if (userRange) {
					w.ge(PointUpvaluation::getBeginTime, userBegin).le(PointUpvaluation::getEndTime, userEnd);
				}
			}
		}

		w.orderByDesc(PointUpvaluation::getActivityId);
		return w;
	}

	private void enrichListRowAfterQuery(Map<String, Object> row, long nowSec) {
		Object bt = row.get("begin_time");
		Object et = row.get("end_time");
		if (!(bt instanceof Number) || !(et instanceof Number)) {
			throw new ResourceException("数据异常");
		}
		long btv = ((Number) bt).longValue();
		long etv = ((Number) et).longValue();
		row.put(
				"begin_date",
				Instant.ofEpochSecond(btv)
						.atZone(SYSTEM_ZONE)
						.toLocalDate()
						.format(DateTimeFormatter.ISO_LOCAL_DATE));
		row.put(
				"end_date",
				Instant.ofEpochSecond(etv)
						.atZone(SYSTEM_ZONE)
						.toLocalDate()
						.format(DateTimeFormatter.ISO_LOCAL_DATE));
		String rowStatus;
		if (btv >= nowSec && etv >= nowSec) {
			rowStatus = "waiting";
		} else if (btv <= nowSec && etv > nowSec) {
			rowStatus = "ongoing";
		} else {
			rowStatus = "it_has_ended";
		}
		row.put("activity_status", rowStatus);
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateStatus(Map<String, Object> data) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Long activityId = parseActivityIdForUpdateStatus(data.get("activity_id"), locale);

		LocalDate yesterday = LocalDate.now(SYSTEM_ZONE).minusDays(1);
		long endEpoch = yesterday.atTime(23, 59, 59).atZone(SYSTEM_ZONE).toEpochSecond();

		long companyId = ((Number) data.get("company_id")).longValue();
		LambdaQueryWrapper<PointUpvaluation> wrapper =
				new LambdaQueryWrapper<PointUpvaluation>().eq(PointUpvaluation::getCompanyId, companyId);
		if (activityId == null) {
			wrapper.isNull(PointUpvaluation::getActivityId);
		} else {
			wrapper.eq(PointUpvaluation::getActivityId, activityId.longValue());
		}
		PointUpvaluation existing = pointUpvaluationMapper.selectOne(wrapper);
		if (existing == null) {
			throw new ResourceException(
					msg("promotions.pointupvaluation.no_update_data_found", "未查询到更新数据", locale));
		}
		existing.setEndTime(endEpoch);
		existing.setUpdated((int) Instant.now().getEpochSecond());
		int rows = pointUpvaluationMapper.updateById(existing);
		if (rows == 0) {
			throw new ResourceException(
					msg("promotions.pointupvaluation.no_update_data_found", "未查询到更新数据", locale));
		}
	}

	/**
	 * Parses {@code activity_id} for status updates: {@code null} or blank input yields {@code null}
	 * (match rows with null activity id); non-numeric strings throw {@link ResourceException} with
	 * {@code promotions.pointupvaluation.no_update_data_found}. Numeric values return the parsed id.
	 */
	private Long parseActivityIdForUpdateStatus(Object raw, Locale locale) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String trimmed = String.valueOf(raw).trim();
		if (!StringUtils.hasText(trimmed)) {
			return null;
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException(
					msg("promotions.pointupvaluation.no_update_data_found", "未查询到更新数据", locale));
		}
	}

	private void enrichPointupvaluationGetInfoMap(Map<String, Object> result) {
		Object btTop = result.get("begin_time");
		Object etTop = result.get("end_time");
		if (!(btTop instanceof Number) || !(etTop instanceof Number)) {
			throw new ResourceException("数据异常");
		}
		DateTimeFormatter topFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		result.put(
				"begin_date",
				Instant.ofEpochSecond(((Number) btTop).longValue())
						.atZone(SYSTEM_ZONE)
						.format(topFormatter));
		result.put(
				"end_date",
				Instant.ofEpochSecond(((Number) etTop).longValue())
						.atZone(SYSTEM_ZONE)
						.format(topFormatter));

		Object tcObj = result.get("trigger_condition");
		if (!(tcObj instanceof Map<?, ?> tcRaw)) {
			throw new ResourceException("数据异常");
		}
		LinkedHashMap<String, Object> tc = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : tcRaw.entrySet()) {
			if (e.getKey() != null) {
				tc.put(e.getKey().toString(), e.getValue());
			}
		}

		Object ttObj = tc.get("trigger_time");
		LinkedHashMap<String, Object> triggerTime = new LinkedHashMap<>();
		if (ttObj instanceof Map<?, ?> ttRaw) {
			for (Map.Entry<?, ?> e : ttRaw.entrySet()) {
				if (e.getKey() != null) {
					triggerTime.put(e.getKey().toString(), e.getValue());
				}
			}
		} else {
			triggerTime.put("begin_date", "");
			triggerTime.put("end_date", "");
			tc.put("trigger_time", triggerTime);
			result.put("trigger_condition", tc);
			Object vg = result.get("valid_grade");
			if (vg == null) {
				result.put("valid_grade", Collections.emptyList());
			}
			return;
		}

		Object innerBt = triggerTime.get("begin_time");
		Object innerEt = triggerTime.get("end_time");
		if (triggerTimeFieldTruthy(innerBt) && triggerTimeFieldTruthy(innerEt)) {
			triggerTime.put("begin_date", formatTriggerInnerDate(innerBt));
			triggerTime.put("end_date", formatTriggerInnerDate(innerEt));
		} else {
			triggerTime.put("begin_date", "");
			triggerTime.put("end_date", "");
		}
		tc.put("trigger_time", triggerTime);
		result.put("trigger_condition", tc);

		Object vg = result.get("valid_grade");
		if (vg == null) {
			result.put("valid_grade", Collections.emptyList());
		}
	}

	private static boolean triggerTimeFieldTruthy(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (o instanceof String s) {
			String t = s.trim();
			return StringUtils.hasText(t) && !"0".equals(t);
		}
		if (o instanceof Boolean b) {
			return b;
		}
		return false;
	}

	private static OptionalLong parseEpochSecondsForTriggerDisplay(Object o) {
		if (o == null) {
			return OptionalLong.empty();
		}
		if (o instanceof Number n) {
			return OptionalLong.of(n.longValue());
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (!t.matches("^-?\\d+$")) {
				return OptionalLong.empty();
			}
			try {
				return OptionalLong.of(Long.parseLong(t));
			} catch (NumberFormatException e) {
				return OptionalLong.empty();
			}
		}
		return OptionalLong.empty();
	}

	private String formatTriggerInnerDate(Object o) {
		OptionalLong sec = parseEpochSecondsForTriggerDisplay(o);
		if (sec.isEmpty()) {
			return "";
		}
		return Instant.ofEpochSecond(sec.getAsLong())
				.atZone(SYSTEM_ZONE)
				.toLocalDate()
				.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	private Map<String, Object> toApiRow(PointUpvaluation row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("activity_id", row.getActivityId());
		m.put("company_id", row.getCompanyId());
		m.put("title", row.getTitle());
		m.put("upvaluation", row.getUpvaluation());
		m.put("max_up_point", row.getMaxUpPoint());
		m.put("begin_time", row.getBeginTime());
		m.put("end_time", row.getEndTime());
		Integer cr = row.getCreated();
		m.put("created", cr == null ? null : cr.longValue());
		Integer up = row.getUpdated();
		m.put("updated", up == null ? null : up.longValue());
		m.put("is_forever", Objects.equals(row.getEndTime(), FOREVER_END_EPOCH));

		String tcJson = row.getTriggerCondition();
		try {
			m.put(
					"trigger_condition",
					StringUtils.hasText(tcJson)
							? objectMapper.readValue(tcJson.trim(), new TypeReference<Map<String, Object>>() {})
							: new LinkedHashMap<>());
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据异常");
		}
		String vg = row.getValidGrade();
		try {
			m.put(
					"valid_grade",
					StringUtils.hasText(vg)
							? objectMapper.readValue(vg.trim(), new TypeReference<List<Object>>() {})
							: List.of());
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据异常");
		}
		String us = row.getUsedScene();
		try {
			m.put(
					"used_scene",
					StringUtils.hasText(us)
							? objectMapper.readValue(us.trim(), new TypeReference<List<Object>>() {})
							: List.of());
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据异常");
		}
		return m;
	}
}
