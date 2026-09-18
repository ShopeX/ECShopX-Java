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
import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageRowMapperService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsRowMapperService;
import cn.shopex.ecshopx.promotions.domain.LuckyDrawActivity;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableAdminSaveRules;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorCodes;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorMessages;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntablePrizeDataSchema;
import cn.shopex.ecshopx.promotions.mapper.LuckyDrawActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.LuckyDrawActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.multilang.LuckyDrawActivityOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TurntableConfigService {

	private static final Set<String> ALLOWED = Set.of(
			"company_id",
			"begin_time",
			"end_time",
			"cost_value",
			"activity_type",
			"activity_name",
			"activity_template_config",
			"prize_data",
			"intro",
			"created",
			"updated",
			"limit_total",
			"limit_day");

	private final LuckyDrawActivityMapper luckyDrawActivityMapper;
	private final LuckyDrawActivityOutsideMultiLangWriteService luckyDrawActivityOutsideMultiLangWriteService;
	private final LuckyDrawActivityOutsideMultiLangReadService luckyDrawActivityOutsideMultiLangReadService;
	private final DiscountCardsMapper discountCardsMapper;
	private final CardPackageMapper cardPackageMapper;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;
	private final CardPackageRowMapperService cardPackageRowMapperService;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public TurntableConfigService(
			LuckyDrawActivityMapper luckyDrawActivityMapper,
			LuckyDrawActivityOutsideMultiLangWriteService luckyDrawActivityOutsideMultiLangWriteService,
			LuckyDrawActivityOutsideMultiLangReadService luckyDrawActivityOutsideMultiLangReadService,
			DiscountCardsMapper discountCardsMapper,
			CardPackageMapper cardPackageMapper,
			DiscountCardsRowMapperService discountCardsRowMapperService,
			CardPackageRowMapperService cardPackageRowMapperService,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.luckyDrawActivityMapper = luckyDrawActivityMapper;
		this.luckyDrawActivityOutsideMultiLangWriteService = luckyDrawActivityOutsideMultiLangWriteService;
		this.luckyDrawActivityOutsideMultiLangReadService = luckyDrawActivityOutsideMultiLangReadService;
		this.discountCardsMapper = discountCardsMapper;
		this.cardPackageMapper = cardPackageMapper;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
		this.cardPackageRowMapperService = cardPackageRowMapperService;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	private String msg(String code, String zhCnFallback, Locale locale) {
		return messageSource.getMessage(code, null, zhCnFallback, locale);
	}

	private String requiredFieldsCannotBeEmpty(Locale locale) {
		return msg(
				TurntableAdminSaveRules.I18N_REQUIRED_FIELDS_CANNOT_BE_EMPTY,
				TurntableAdminSaveRules.FALLBACK_REQUIRED_FIELDS_CANNOT_BE_EMPTY,
				locale);
	}

	public long parseRequiredActivityId(String activityIdRaw) {
		Locale locale = LocaleContextHolder.getLocale();
		if (activityIdRaw == null) {
			throw new BadRequestException(
					msg("promotions.turntable.activity_id_required_error", "错误，活动id必传", locale));
		}
		String trimmed = activityIdRaw.trim();
		if (!StringUtils.hasText(trimmed) || "0".equals(trimmed)) {
			throw new BadRequestException(
					msg("promotions.turntable.activity_id_required_error", "错误，活动id必传", locale));
		}
		long actId;
		try {
			actId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.turntable.invalid_activity_id", "活动ID格式错误", locale));
		}
		if (actId <= 0L) {
			throw new BadRequestException(
					msg("promotions.turntable.activity_id_required_error", "错误，活动id必传", locale));
		}
		return actId;
	}

	@Transactional
	public void downLuckyDrawActivity(long companyId, String activityIdRaw) {
		Locale locale = LocaleContextHolder.getLocale();
		long actId = parseRequiredActivityId(activityIdRaw);
		LuckyDrawActivity existing =
				luckyDrawActivityMapper.selectOne(
						new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LuckyDrawActivity>()
								.eq(LuckyDrawActivity::getId, actId)
								.eq(LuckyDrawActivity::getCompanyId, companyId));
		if (existing == null) {
			throw new ResourceException(
					msg("promotions.turntable.no_update_data_found", "未查询到更新数据", locale));
		}
		long nowEpochSec = Instant.now().getEpochSecond();
		long begin = existing.getBeginTime() == null ? 0L : existing.getBeginTime();
		long end = existing.getEndTime() == null ? 0L : existing.getEndTime();
		if (TurntableAdminSaveRules.isEnded(begin, end, nowEpochSec)) {
			throw new BadRequestException(
					msg("promotions.turntable.already_ended", "活动已结束，不可再次结束", locale));
		}
		if (!TurntableAdminSaveRules.isInProgress(begin, end, nowEpochSec)) {
			throw new BadRequestException(
					msg("promotions.turntable.only_in_progress_can_end", "仅进行中活动可手动结束", locale));
		}
		int updatedSec = (int) nowEpochSec;
		com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<LuckyDrawActivity> wrapper =
				new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
		wrapper
				.eq(LuckyDrawActivity::getId, actId)
				.eq(LuckyDrawActivity::getCompanyId, companyId)
				.set(LuckyDrawActivity::getEndTime, nowEpochSec)
				.set(LuckyDrawActivity::getUpdated, updatedSec);
		int rows = luckyDrawActivityMapper.update(null, wrapper);
		if (rows == 0) {
			throw new ResourceException(
					msg("promotions.turntable.no_update_data_found", "未查询到更新数据", locale));
		}
	}

	/**
	 * Admin turntable config: loads the raw {@code lucky_draw_activity} row with multilingual
	 * field overlay. Prize list and template details are not loaded or merged into the result.
	 * Returns empty when no row exists for the given activity id; the controller may serialize
	 * that as an empty JSON array for the API contract.
	 */
	public Optional<Map<String, Object>> getTurntableConfig(
			long companyId, String idRaw, String requestLangTag) {
		Locale locale = LocaleContextHolder.getLocale();
		if (idRaw == null) {
			throw new ResourceException(
					msg("promotions.turntable.id_required_error", "错误，id必传", locale));
		}
		String trimmed = idRaw.trim();
		if (!StringUtils.hasText(trimmed) || "0".equals(trimmed)) {
			throw new ResourceException(
					msg("promotions.turntable.id_required_error", "错误，id必传", locale));
		}
		long activityId;
		try {
			activityId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
		LuckyDrawActivity row =
				luckyDrawActivityMapper.selectOne(
						new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LuckyDrawActivity>()
								.eq(LuckyDrawActivity::getId, activityId)
								.eq(LuckyDrawActivity::getCompanyId, companyId));
		if (row == null) {
			return Optional.empty();
		}
		LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
		detail.put("id", row.getId());
		detail.put("company_id", row.getCompanyId());
		detail.put("begin_time", row.getBeginTime());
		detail.put("end_time", row.getEndTime());
		detail.put("cost_value", row.getCostValue());
		detail.put("activity_type", row.getActivityType());
		detail.put("activity_name", row.getActivityName());
		detail.put("activity_template_config", row.getActivityTemplateConfig());
		detail.put("prize_data", row.getPrizeData());
		detail.put("intro", row.getIntro());
		detail.put("config_version", row.getConfigVersion() == null ? 1L : row.getConfigVersion());
		detail.put("created", row.getCreated());
		detail.put("updated", row.getUpdated());
		detail.put("limit_total", row.getLimitTotal());
		detail.put("limit_day", row.getLimitDay());
		luckyDrawActivityOutsideMultiLangReadService.applyActivityNameIntro(
				row.getCompanyId() == null ? 0L : row.getCompanyId(), activityId, detail, requestLangTag);
		return Optional.of(detail);
	}

	public Map<String, Object> getLuckyDrawDetail(long companyId, String idRaw, String requestLangTag) {
		Locale locale = LocaleContextHolder.getLocale();
		long activityId = parseRequiredActivityId(idRaw);
		LuckyDrawActivity row =
				luckyDrawActivityMapper.selectOne(
						new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LuckyDrawActivity>()
								.eq(LuckyDrawActivity::getId, activityId)
								.eq(LuckyDrawActivity::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.turntable.activity_not_exist_with_id",
							new Object[] {activityId},
							"错误，id为：{0} 的活动不存在",
							locale));
		}
		LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
		detail.put("id", row.getId());
		detail.put("company_id", row.getCompanyId());
		detail.put("begin_time", row.getBeginTime());
		detail.put("end_time", row.getEndTime());
		detail.put("cost_value", row.getCostValue());
		detail.put("activity_type", row.getActivityType());
		detail.put("activity_name", row.getActivityName());
		detail.put("activity_template_config", row.getActivityTemplateConfig());
		detail.put("prize_data", row.getPrizeData());
		detail.put("intro", row.getIntro());
		detail.put("config_version", row.getConfigVersion() == null ? 1L : row.getConfigVersion());
		detail.put("created", row.getCreated());
		detail.put("updated", row.getUpdated());
		detail.put("limit_total", row.getLimitTotal());
		detail.put("limit_day", row.getLimitDay());
		luckyDrawActivityOutsideMultiLangReadService.applyActivityNameIntro(
				row.getCompanyId() == null ? 0L : row.getCompanyId(), activityId, detail, requestLangTag);
		List<Map<String, Object>> prizeList = parseDetailPrizeData(detail, locale);
		Map<String, Object> configMap = parseDetailActivityTemplateConfig(detail, locale);
		List<Map<String, Object>> prizeListForEnrich =
				prizeList == null ? Collections.emptyList() : prizeList;
		List<Map<String, Object>> enrichedPrizes = getPrizeInfo(prizeListForEnrich);
		Map<String, Object> config = configMap == null ? new LinkedHashMap<>() : configMap;
		config.put("gameType", detail.get("activity_type"));
		Object gcObj = config.computeIfAbsent("gameConfig", k -> new LinkedHashMap<String, Object>());
		if (!(gcObj instanceof Map<?, ?>)) {
			throw new ResourceException(
					msg(
							"promotions.turntable.activity_template_config_invalid",
							"活动模板配置格式错误",
							locale));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> gameConfig = (Map<String, Object>) gcObj;
		gameConfig.put("prizes", enrichedPrizes);
		detail.put("prize_data", enrichedPrizes);
		detail.put("activity_template_config", config);
		return detail;
	}

	private List<Map<String, Object>> parseDetailPrizeData(Map<String, Object> detail, Locale locale) {
		Object prizeRaw = detail.get("prize_data");
		if (prizeRaw == null) {
			return null;
		}
		if (prizeRaw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			try {
				return objectMapper.readValue(t, new TypeReference<List<Map<String, Object>>>() {});
			} catch (JsonProcessingException e) {
				throw new ResourceException(
						msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
			}
		}
		if (prizeRaw instanceof List<?> rawList) {
			List<Map<String, Object>> prizeList = new ArrayList<>();
			for (Object elem : rawList) {
				if (!(elem instanceof Map<?, ?>)) {
					throw new ResourceException(
							msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) elem;
				prizeList.add(m);
			}
			return prizeList;
		}
		throw new ResourceException(
				msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
	}

	private Map<String, Object> parseDetailActivityTemplateConfig(Map<String, Object> detail, Locale locale) {
		Object tmplRaw = detail.get("activity_template_config");
		if (tmplRaw == null) {
			return new LinkedHashMap<>();
		}
		if (tmplRaw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return new LinkedHashMap<>();
			}
			try {
				Map<String, Object> m =
						objectMapper.readValue(t, new TypeReference<Map<String, Object>>() {});
				return m == null ? new LinkedHashMap<>() : m;
			} catch (JsonProcessingException e) {
				throw new ResourceException(
						msg(
								"promotions.turntable.activity_template_config_invalid",
								"活动模板配置格式错误",
								locale));
			}
		}
		if (tmplRaw instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> typed = (Map<String, Object>) m;
			return typed;
		}
		throw new ResourceException(
				msg(
						"promotions.turntable.activity_template_config_invalid",
						"活动模板配置格式错误",
						locale));
	}

	private List<Map<String, Object>> getPrizeInfo(List<Map<String, Object>> prizeDataList) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> src : prizeDataList) {
			LinkedHashMap<String, Object> prize = new LinkedHashMap<>(src);
			String prizeType =
					firstNonBlank(
							Objects.toString(prize.get("type"), ""),
							Objects.toString(prize.get("prize_type"), ""));
			Object prizeValue =
					prize.containsKey("value") ? prize.get("value") : prize.get("prize_value");
			if ("coupon".equals(prizeType)) {
				long cardId = parsePrizeValueAsLong(prizeValue);
				DiscountCards dc = discountCardsMapper.selectById(cardId);
				if (dc == null) {
					prize.put("prize_detail", Collections.emptyList());
				} else {
					prize.put(
							"prize_detail",
							new LinkedHashMap<>(discountCardsRowMapperService.toSnakeCaseMap(dc)));
				}
			} else if ("coupons".equals(prizeType)) {
				long packageId = parsePrizeValueAsLong(prizeValue);
				CardPackage pkg = cardPackageMapper.selectById(packageId);
				if (pkg == null) {
					prize.put("prize_detail", Collections.emptyList());
				} else {
					prize.put(
							"prize_detail",
							new LinkedHashMap<>(cardPackageRowMapperService.toSnakeCaseMap(pkg)));
				}
			}
			out.add(prize);
		}
		return out;
	}

	private static long parsePrizeValueAsLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a.trim();
		}
		if (StringUtils.hasText(b)) {
			return b.trim();
		}
		return "";
	}

	@Transactional
	public Map<String, Object> setTurntableConfig(long companyId, Map<String, Object> merged, String requestLangTag) {
		Locale locale = LocaleContextHolder.getLocale();
		List<Map<String, Object>> prizeList = parseAndValidatePrizeData(merged, locale);
		validateBeginEndTime(merged, locale);
		long begin = parseEpochSeconds(merged.get("begin_time"), locale);
		long end = parseEpochSeconds(merged.get("end_time"), locale);
		if (begin >= end) {
			throw new ResourceException(
					msg(
							"promotions.turntable.start_time_cannot_greater_equal_end_time",
							"错误，开始时间不能大于等于结束时间",
							locale));
		}
		long nowEpochSec = Instant.now().getEpochSecond();
		if (!TurntableAdminSaveRules.isEndTimeValidForNormalSave(end, nowEpochSec)) {
			throw new BadRequestException(
					msg("promotions.turntable.end_time_must_after_now", "结束时间必须大于当前时间", locale));
		}
		Long limitTotal = parseRequiredNonNegLong(merged.get("limit_total"), "limit_total", locale);
		Long limitDay = parseRequiredNonNegLong(merged.get("limit_day"), "limit_day", locale);
		List<String> limitErrors = TurntableAdminSaveRules.validateLimits(limitTotal, limitDay);
		if (!limitErrors.isEmpty()) {
			if (limitErrors.stream().anyMatch(e -> e.contains("必填"))) {
				throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
			}
			throw new BadRequestException(String.join("; ", limitErrors));
		}
		Object costValueRaw = merged.get("cost_value");
		if (costValueRaw == null) {
			throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
		}
		long costValue = parseEpochSeconds(costValueRaw, locale);
		if (costValue <= 0L) {
			throw new BadRequestException(
					msg("promotions.turntable.cost_value_must_positive", "抽奖消耗积分必须大于0", locale));
		}
		merged.put("limit_total", limitTotal);
		merged.put("limit_day", limitDay);
		merged.put("cost_value", costValue);
		merged.put("activity_type", "wheel");

		String prizeJson;
		try {
			prizeJson = objectMapper.writeValueAsString(prizeList);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(
					msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
		}
		merged.put("prize_data", prizeJson);

		if (isNoId(merged.get("id"))) {
			LuckyDrawActivity entity = new LuckyDrawActivity();
			applyWhitelistedFields(entity, merged, companyId, begin, end, true, locale);
			entity.setCostType(TurntableAdminSaveRules.COST_TYPE_POINT);
			entity.setAreaId(0L);
			entity.setConfigVersion(1L);
			luckyDrawActivityMapper.insert(entity);
			Long newId = entity.getId();
			luckyDrawActivityOutsideMultiLangWriteService.applyAfterInsert(
					newId == null ? 0L : newId, companyId, merged, requestLangTag);
			return Map.of("result", Boolean.TRUE, "id", newId == null ? 0L : newId, "config_version", 1L);
		}

		long id = parsePositiveId(merged.get("id"), locale);
		LuckyDrawActivity existing =
				luckyDrawActivityMapper.selectOne(
						new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LuckyDrawActivity>()
								.eq(LuckyDrawActivity::getId, id)
								.eq(LuckyDrawActivity::getCompanyId, companyId));
		if (existing == null) {
			throw new ResourceException(
					msg("promotions.turntable.no_update_data_found", "未查询到更新数据", locale));
		}
		long existBegin = existing.getBeginTime() == null ? 0L : existing.getBeginTime();
		long existEnd = existing.getEndTime() == null ? 0L : existing.getEndTime();
		if (TurntableAdminSaveRules.isEnded(existBegin, existEnd, nowEpochSec)) {
			throw new BadRequestException(
					msg("promotions.turntable.ended_readonly", "活动已结束，不可编辑", locale));
		}
		long clientVersion = parseClientConfigVersion(merged.get("config_version"), locale);
		long dbVersion = existing.getConfigVersion() == null ? 1L : existing.getConfigVersion();
		if (clientVersion != dbVersion) {
			throw new BadRequestException(
					TurntableErrorMessages.message(
							messageSource, TurntableErrorCodes.CONFIG_VERSION_CONFLICT, locale));
		}
		// 开始时间创建后不可改：强制沿用库内 begin
		begin = existBegin;
		if (begin >= end) {
			throw new ResourceException(
					msg(
							"promotions.turntable.start_time_cannot_greater_equal_end_time",
							"错误，开始时间不能大于等于结束时间",
							locale));
		}
		long frozenCost = existing.getCostValue() == null ? 0L : existing.getCostValue();
		applyWhitelistedFields(existing, merged, companyId, begin, end, false, locale);
		existing.setCostType(TurntableAdminSaveRules.COST_TYPE_POINT);
		int nowSec = (int) nowEpochSec;
		existing.setUpdated(nowSec);
		LambdaUpdateWrapper<LuckyDrawActivity> wrapper = new LambdaUpdateWrapper<>();
		wrapper
				.eq(LuckyDrawActivity::getId, id)
				.eq(LuckyDrawActivity::getCompanyId, companyId)
				.eq(LuckyDrawActivity::getConfigVersion, dbVersion)
				.set(LuckyDrawActivity::getEndTime, end)
				.set(LuckyDrawActivity::getCostValue, frozenCost)
				.set(LuckyDrawActivity::getLimitTotal, existing.getLimitTotal())
				.set(LuckyDrawActivity::getLimitDay, existing.getLimitDay())
				.set(LuckyDrawActivity::getActivityType, "wheel")
				.set(LuckyDrawActivity::getActivityName, existing.getActivityName())
				.set(LuckyDrawActivity::getActivityTemplateConfig, existing.getActivityTemplateConfig())
				.set(LuckyDrawActivity::getPrizeData, existing.getPrizeData())
				.set(LuckyDrawActivity::getIntro, existing.getIntro())
				.set(LuckyDrawActivity::getCostType, TurntableAdminSaveRules.COST_TYPE_POINT)
				.set(LuckyDrawActivity::getUpdated, nowSec)
				.set(LuckyDrawActivity::getConfigVersion, dbVersion + 1L);
		int rows = luckyDrawActivityMapper.update(null, wrapper);
		if (rows == 0) {
			throw new BadRequestException(
					TurntableErrorMessages.message(
							messageSource, TurntableErrorCodes.CONFIG_VERSION_CONFLICT, locale));
		}
		luckyDrawActivityOutsideMultiLangWriteService.applyAfterUpdate(id, companyId, merged, requestLangTag);
		return Map.of("result", Boolean.TRUE, "id", id, "config_version", dbVersion + 1L);
	}

	@Transactional
	public Map<String, Object> copyTurntableActivity(long companyId, String sourceIdRaw, String requestLangTag) {
		Locale locale = LocaleContextHolder.getLocale();
		long sourceId = parseRequiredActivityId(sourceIdRaw);
		LuckyDrawActivity source =
				luckyDrawActivityMapper.selectOne(
						new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LuckyDrawActivity>()
								.eq(LuckyDrawActivity::getId, sourceId)
								.eq(LuckyDrawActivity::getCompanyId, companyId));
		if (source == null) {
			throw new ResourceException(
					msg("promotions.turntable.no_update_data_found", "未查询到更新数据", locale));
		}
		List<Map<String, Object>> prizes;
		try {
			prizes =
					StringUtils.hasText(source.getPrizeData())
							? objectMapper.readValue(source.getPrizeData(), new TypeReference<>() {})
							: new ArrayList<>();
		} catch (JsonProcessingException e) {
			throw new BadRequestException(
					msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
		}
		List<Map<String, Object>> copiedPrizes = TurntableAdminSaveRules.regenerateAllPrizeIds(prizes);
		String prizeJson;
		try {
			prizeJson = objectMapper.writeValueAsString(copiedPrizes);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(
					msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
		}
		long now = Instant.now().getEpochSecond();
		LuckyDrawActivity entity = new LuckyDrawActivity();
		entity.setCompanyId(companyId);
		entity.setAreaId(0L);
		entity.setCostType(TurntableAdminSaveRules.COST_TYPE_POINT);
		entity.setBeginTime(source.getBeginTime());
		entity.setEndTime(source.getEndTime() != null && source.getEndTime() > now ? source.getEndTime() : now + 86400L);
		entity.setCostValue(source.getCostValue());
		entity.setLimitTotal(source.getLimitTotal());
		entity.setLimitDay(source.getLimitDay());
		entity.setActivityType("wheel");
		entity.setActivityName(source.getActivityName() == null ? "" : source.getActivityName() + " 副本");
		entity.setActivityTemplateConfig(source.getActivityTemplateConfig());
		entity.setPrizeData(prizeJson);
		entity.setIntro(source.getIntro());
		entity.setConfigVersion(1L);
		int nowSec = (int) now;
		entity.setCreated(nowSec);
		entity.setUpdated(nowSec);
		luckyDrawActivityMapper.insert(entity);
		Long newId = entity.getId();
		Map<String, Object> multi = new LinkedHashMap<>();
		multi.put("activity_name", entity.getActivityName());
		multi.put("intro", entity.getIntro());
		luckyDrawActivityOutsideMultiLangWriteService.applyAfterInsert(
				newId == null ? 0L : newId, companyId, multi, requestLangTag);
		return Map.of("result", Boolean.TRUE, "id", newId == null ? 0L : newId, "config_version", 1L);
	}

	private long parseClientConfigVersion(Object raw, Locale locale) {
		if (raw == null) {
			throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
		}
		try {
			long v = raw instanceof Number n ? n.longValue() : Long.parseLong(raw.toString().trim());
			if (v <= 0L) {
				throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
		}
	}

	private Long parseRequiredNonNegLong(Object raw, String field, Locale locale) {
		if (raw == null || (raw instanceof String s && !StringUtils.hasText(s.trim()))) {
			throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
		}
		try {
			long v = raw instanceof Number n ? n.longValue() : Long.parseLong(raw.toString().trim());
			if (v < 0L) {
				throw new BadRequestException(field + " 须为非负整数");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.turntable.numeric_field_format_error", "数值字段格式错误", locale));
		}
	}

	private long parsePositiveId(Object idRaw, Locale locale) {
		try {
			long id = idRaw instanceof Number n ? n.longValue() : Long.parseLong(idRaw.toString().trim());
			if (id <= 0L) {
				throw new BadRequestException(
						msg("promotions.turntable.invalid_activity_id", "活动ID格式错误", locale));
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.turntable.invalid_activity_id", "活动ID格式错误", locale));
		}
	}

	private static boolean isNoId(Object idRaw) {
		if (idRaw == null) {
			return true;
		}
		if (idRaw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return true;
			}
			if ("0".equals(t)) {
				return true;
			}
			try {
				return Long.parseLong(t) == 0L;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		if (idRaw instanceof Number n) {
			return n.longValue() == 0L;
		}
		return false;
	}

	private List<Map<String, Object>> parseAndValidatePrizeData(Map<String, Object> merged, Locale locale) {
		Object raw = merged.get("prize_data");
		if (isPrizeDataTopLevelEmpty(raw)) {
			throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
		}
		List<Map<String, Object>> prizeList = new ArrayList<>();
		if (raw instanceof String s) {
			String t = s.trim();
			JsonNode root;
			try {
				root = objectMapper.readTree(t);
			} catch (JsonProcessingException e) {
				throw new BadRequestException(
						msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
			}
			if (!root.isArray()) {
				throw new BadRequestException(
						msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
			}
			for (JsonNode n : root) {
				if (!n.isObject()) {
					throw new BadRequestException(
							msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
				}
				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> m = objectMapper.convertValue(n, Map.class);
					prizeList.add(m);
				} catch (IllegalArgumentException e) {
					throw new BadRequestException(
							msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
				}
			}
		} else if (raw instanceof List<?> rawList) {
			if (rawList.isEmpty()) {
				throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
			}
			for (Object elem : rawList) {
				if (!(elem instanceof Map<?, ?>)) {
					throw new BadRequestException(
							msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) elem;
				prizeList.add(m);
			}
		} else {
			throw new BadRequestException(
					msg("promotions.turntable.prize_data_invalid", "奖项数据格式错误", locale));
		}
		if (prizeList.isEmpty()) {
			throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
		}
		List<String> existingIds = List.of();
		Object idRaw = merged.get("id");
		if (!isNoId(idRaw)) {
			try {
				long id = parsePositiveId(idRaw, locale);
				LuckyDrawActivity existing = luckyDrawActivityMapper.selectById(id);
				if (existing != null) {
					existingIds = TurntableAdminSaveRules.extractPrizeIdsInOrder(existing.getPrizeData(), objectMapper);
				}
			} catch (BadRequestException ignored) {
				existingIds = List.of();
			}
		}
		List<Map<String, Object>> normalized =
				TurntableAdminSaveRules.normalizeAndAssignPrizeIds(prizeList, existingIds);
		List<String> schemaErrors = TurntablePrizeDataSchema.validatePrizeList(normalized);
		if (!schemaErrors.isEmpty()) {
			if (schemaErrors.stream().anyMatch(TurntablePrizeDataSchema::isRequiredFieldError)) {
				throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
			}
			throw new BadRequestException(String.join("; ", schemaErrors));
		}
		return normalized;
	}

	private static boolean isPrizeDataTopLevelEmpty(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		if (raw instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (raw instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}

	private void validateBeginEndTime(Map<String, Object> merged, Locale locale) {
		if (merged.get("begin_time") == null || merged.get("end_time") == null) {
			throw new BadRequestException(requiredFieldsCannotBeEmpty(locale));
		}
	}

	private long parseEpochSeconds(Object raw, Locale locale) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						msg("promotions.turntable.invalid_time_value", "开始或结束时间格式错误", locale));
			}
		}
		throw new BadRequestException(
				msg("promotions.turntable.invalid_time_value", "开始或结束时间格式错误", locale));
	}

	private void applyWhitelistedFields(
			LuckyDrawActivity entity,
			Map<String, Object> merged,
			long companyId,
			long begin,
			long end,
			boolean insertPath,
			Locale locale) {
		entity.setCompanyId(companyId);
		entity.setBeginTime(begin);
		entity.setEndTime(end);
		for (String key : ALLOWED) {
			if ("id".equals(key)
					|| "company_id".equals(key)
					|| "begin_time".equals(key)
					|| "end_time".equals(key)) {
				continue;
			}
			if (!merged.containsKey(key)) {
				continue;
			}
			if (("created".equals(key) || "updated".equals(key)) && merged.get(key) == null) {
				continue;
			}
			applySingleWhitelistKey(entity, key, merged, locale);
		}
		if (insertPath) {
			int nowSec = (int) (System.currentTimeMillis() / 1000L);
			if (entity.getCreated() == null) {
				entity.setCreated(nowSec);
			}
			if (entity.getUpdated() == null) {
				entity.setUpdated(nowSec);
			}
		}
	}

	private void applySingleWhitelistKey(LuckyDrawActivity entity, String key, Map<String, Object> merged, Locale locale) {
		switch (key) {
			case "cost_value", "limit_total", "limit_day" -> {
				Object v = merged.get(key);
				try {
					long n = v instanceof Number num ? num.longValue() : Long.parseLong(String.valueOf(v).trim());
					switch (key) {
						case "cost_value" -> entity.setCostValue(n);
						case "limit_total" -> entity.setLimitTotal(n);
						case "limit_day" -> entity.setLimitDay(n);
						default -> {
						}
					}
				} catch (NumberFormatException e) {
					throw new BadRequestException(
							msg("promotions.turntable.numeric_field_format_error", "数值字段格式错误", locale));
				}
			}
			case "activity_type" -> entity.setActivityType(stringOrEmpty(merged.get("activity_type")));
			case "activity_name" -> entity.setActivityName(stringOrEmpty(merged.get("activity_name")));
			case "activity_template_config" ->
				entity.setActivityTemplateConfig(nullableString(merged.get("activity_template_config")));
			case "prize_data" -> entity.setPrizeData(nullableString(merged.get("prize_data")));
			case "intro" -> entity.setIntro(nullableString(merged.get("intro")));
			case "created" -> entity.setCreated(parseIntegerOrThrow(merged.get("created"), locale));
			case "updated" -> entity.setUpdated(parseIntegerOrThrow(merged.get("updated"), locale));
			default -> {
			}
		}
	}

	private static String stringOrEmpty(Object v) {
		return v == null ? "" : Objects.toString(v, "");
	}

	private static String nullableString(Object v) {
		return v == null ? null : Objects.toString(v, null);
	}

	private int parseIntegerOrThrow(Object v, Locale locale) {
		try {
			return v instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.turntable.numeric_field_format_error", "数值字段格式错误", locale));
		}
	}
}
