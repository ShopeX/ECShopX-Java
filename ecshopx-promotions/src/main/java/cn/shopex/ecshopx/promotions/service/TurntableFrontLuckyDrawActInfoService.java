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
import cn.shopex.ecshopx.point.domain.PointMember;
import cn.shopex.ecshopx.point.mapper.PointMemberMapper;
import cn.shopex.ecshopx.promotions.domain.LuckyDrawActivity;
import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDayKeyResolver;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStatus;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorCodes;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorMessages;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntablePublicConfigRules;
import cn.shopex.ecshopx.promotions.mapper.LuckyDrawActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import cn.shopex.ecshopx.promotions.service.multilang.LuckyDrawActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TurntableFrontLuckyDrawActInfoService {

	private final LuckyDrawActivityMapper luckyDrawActivityMapper;
	private final TurntableLogMapper turntableLogMapper;
	private final PointMemberMapper pointMemberMapper;
	private final LuckyDrawActivityOutsideMultiLangReadService multiLangReadService;
	private final TurntableDayKeyResolver dayKeyResolver;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public TurntableFrontLuckyDrawActInfoService(
			LuckyDrawActivityMapper luckyDrawActivityMapper,
			TurntableLogMapper turntableLogMapper,
			PointMemberMapper pointMemberMapper,
			LuckyDrawActivityOutsideMultiLangReadService multiLangReadService,
			TurntableDayKeyResolver dayKeyResolver,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.luckyDrawActivityMapper = luckyDrawActivityMapper;
		this.turntableLogMapper = turntableLogMapper;
		this.pointMemberMapper = pointMemberMapper;
		this.multiLangReadService = multiLangReadService;
		this.dayKeyResolver = dayKeyResolver;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	private String msg(String code, String zhCnFallback, Locale locale) {
		return messageSource.getMessage(code, null, zhCnFallback, locale);
	}

	/**
	 * @param companyIdFromClaims 来自 H5 claims；&lt;=0 时尝试用积分账户公司
	 */
	public Map<String, Object> getLuckyDrawActInfo(
			long userId, long companyIdFromClaims, String idRaw, String requestLangTag) {
		Locale locale = LocaleContextHolder.getLocale();
		if (idRaw == null || !StringUtils.hasText(idRaw.trim()) || "0".equals(idRaw.trim())) {
			throw new BadRequestException(
					msg("promotions.turntable.activity_id_required_error", "错误，活动id必传", locale));
		}
		long activityId;
		try {
			activityId = Long.parseLong(idRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					msg("promotions.turntable.invalid_activity_id", "活动ID格式错误", locale));
		}

		PointMember pointRow =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.last("LIMIT 1"));
		long companyId = companyIdFromClaims > 0L
				? companyIdFromClaims
				: (pointRow == null || pointRow.getCompanyId() == null ? 0L : pointRow.getCompanyId());
		if (companyId <= 0L) {
			throw new ResourceException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ACTIVITY_NOT_FOUND, locale));
		}

		LuckyDrawActivity row =
				luckyDrawActivityMapper.selectOne(
						new LambdaQueryWrapper<LuckyDrawActivity>()
								.eq(LuckyDrawActivity::getId, activityId)
								.eq(LuckyDrawActivity::getCompanyId, companyId));
		if (row == null) {
			throw new ResourceException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ACTIVITY_NOT_FOUND, locale));
		}

		long now = Instant.now().getEpochSecond();
		long begin = row.getBeginTime() == null ? 0L : row.getBeginTime();
		long end = row.getEndTime() == null ? 0L : row.getEndTime();
		long limitTotal = row.getLimitTotal() == null ? 0L : row.getLimitTotal();
		long limitDay = row.getLimitDay() == null ? 0L : row.getLimitDay();

		long usedTotal = countEffectiveDraws(userId, activityId, null, null);
		ZoneId bizZone = dayKeyResolver.resolveZone(companyId);
		ZonedDateTime dayStartZdt = LocalDate.parse(dayKeyResolver.dayKey(companyId)).atStartOfDay(bizZone);
		long dayStart = dayStartZdt.toEpochSecond();
		long dayEnd = dayStart + 86400L;
		long usedDay = countEffectiveDraws(userId, activityId, dayStart, dayEnd);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("id", row.getId());
		data.put("company_id", row.getCompanyId());
		data.put("begin_time", row.getBeginTime());
		data.put("end_time", row.getEndTime());
		data.put("cost_value", row.getCostValue());
		data.put("activity_type", row.getActivityType());
		data.put("activity_name", row.getActivityName());
		data.put("activity_template_config", row.getActivityTemplateConfig());
		data.put("intro", row.getIntro());
		data.put("limit_total", limitTotal);
		data.put("limit_day", limitDay);
		data.put("status", TurntablePublicConfigRules.displayStatus(begin, end, now));
		data.put("remain_total", TurntablePublicConfigRules.remainCount(limitTotal, usedTotal));
		data.put("remain_day", TurntablePublicConfigRules.remainCount(limitDay, usedDay));
		data.put("limit_total_unlimited", TurntablePublicConfigRules.isUnlimited(limitTotal));
		data.put("limit_day_unlimited", TurntablePublicConfigRules.isUnlimited(limitDay));

		List<Map<String, Object>> prizes = parsePrizeData(row.getPrizeData());
		data.put("prize_data", TurntablePublicConfigRules.sanitizePrizesForPublic(prizes));

		multiLangReadService.applyActivityNameIntro(companyId, activityId, data, requestLangTag);

		if (userId <= 0L || pointRow == null) {
			data.put("user_info", Collections.emptyList());
		} else {
			LinkedHashMap<String, Object> ui = new LinkedHashMap<>();
			ui.put("user_id", pointRow.getUserId());
			ui.put("company_id", pointRow.getCompanyId());
			ui.put("point", pointRow.getPoint());
			data.put("user_info", ui);
		}
		return data;
	}

	/** 兼容旧调用签名。 */
	public Map<String, Object> getLuckyDrawActInfo(long userId, String idRaw, String requestLangTag) {
		return getLuckyDrawActInfo(userId, 0L, idRaw, requestLangTag);
	}

	private long countEffectiveDraws(long userId, long actId, Long createdFrom, Long createdTo) {
		LambdaQueryWrapper<TurntableLog> w = new LambdaQueryWrapper<>();
		w.eq(TurntableLog::getUserId, userId)
				.eq(TurntableLog::getActId, actId)
				.and(
						q ->
								q.in(
												TurntableLog::getStatus,
												TurntableDrawStatus.SUCCESS,
												TurntableDrawStatus.GRANT_FAILED)
										.or()
										.isNull(TurntableLog::getStatus));
		if (createdFrom != null) {
			w.ge(TurntableLog::getCreated, createdFrom.intValue());
		}
		if (createdTo != null) {
			w.lt(TurntableLog::getCreated, createdTo.intValue());
		}
		Long c = turntableLogMapper.selectCount(w);
		return c == null ? 0L : c;
	}

	private List<Map<String, Object>> parsePrizeData(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			List<Map<String, Object>> list =
					objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
			return list == null ? List.of() : list;
		} catch (Exception e) {
			return List.of();
		}
	}
}
