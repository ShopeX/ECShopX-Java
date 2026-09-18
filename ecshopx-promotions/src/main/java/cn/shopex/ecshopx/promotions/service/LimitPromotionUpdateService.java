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
import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("limitPromotionUpdateService")
public class LimitPromotionUpdateService {

	private final MessageSource messageSource;
	private final LimitPromotionPersistenceDelegate limitPromotionPersistenceDelegate;
	private final LimitPromotionUpdateWritesService limitPromotionUpdateWritesService;
	private final LimitPromotionsMapper limitPromotionsMapper;
	private final LimitItemPromotionsMapper limitItemPromotionsMapper;

	public LimitPromotionUpdateService(
			MessageSource messageSource,
			LimitPromotionPersistenceDelegate limitPromotionPersistenceDelegate,
			LimitPromotionUpdateWritesService limitPromotionUpdateWritesService,
			LimitPromotionsMapper limitPromotionsMapper,
			LimitItemPromotionsMapper limitItemPromotionsMapper) {
		this.messageSource = messageSource;
		this.limitPromotionPersistenceDelegate = limitPromotionPersistenceDelegate;
		this.limitPromotionUpdateWritesService = limitPromotionUpdateWritesService;
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
	}

	public Map<String, Object> updateLimitPromotion(Map<String, Object> params, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		try {
			String limitType = Objects.toString(params.get("limit_type"), "").trim();
			if (!StringUtils.hasText(limitType)) {
				limitType = "global";
				params.put("limit_type", limitType);
			}
			if ("shop".equalsIgnoreCase(limitType)) {
				params.put("day", 0);
				params.put("limit", 1);
				params.put("use_bound", "goods_import");
				int totalCount = parseIntDefault(params.get("total_count"), 0);
				long limitId = readLong(params.get("limit_id"));
				long companyId = readLong(params.get("company_id"));
				long existingItemRows =
						limitItemPromotionsMapper.selectCount(
								new LambdaQueryWrapper<LimitItemPromotions>()
										.eq(LimitItemPromotions::getCompanyId, companyId)
										.eq(LimitItemPromotions::getLimitId, limitId));
				if (totalCount + existingItemRows == 0) {
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

			limitPromotionPersistenceDelegate.normalizeListLikeKeys(params);
			params.put("start_time", start);
			params.put("end_time", end);

			long companyId = readLong(params.get("company_id"));
			long limitId = readLong(params.get("limit_id"));

			LimitPromotions existing =
					limitPromotionsMapper.selectOne(
							new LambdaQueryWrapper<LimitPromotions>()
									.eq(LimitPromotions::getLimitId, limitId)
									.eq(LimitPromotions::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (existing == null) {
				throw new ResourceException(messageSource.getMessage("promotions.limit.no_update_data_found", null, locale));
			}

			Map<String, Object> result =
					limitPromotionUpdateWritesService.applyUpdateWrites(
							limitId, companyId, params, requestLangTag, locale);

			if ("shop".equalsIgnoreCase(Objects.toString(params.get("limit_type"), "").trim())) {
				int nowTs = (int) (System.currentTimeMillis() / 1000L);
				limitItemPromotionsMapper.delete(
						new LambdaQueryWrapper<LimitItemPromotions>()
								.eq(LimitItemPromotions::getCompanyId, companyId)
								.eq(LimitItemPromotions::getLimitId, limitId)
								.eq(LimitItemPromotions::getDistributorId, 0L));
				limitItemPromotionsMapper.update(
						null,
						new LambdaUpdateWrapper<LimitItemPromotions>()
								.eq(LimitItemPromotions::getCompanyId, companyId)
								.eq(LimitItemPromotions::getLimitId, limitId)
								.set(LimitItemPromotions::getStartTime, start)
								.set(LimitItemPromotions::getEndTime, end)
								.set(LimitItemPromotions::getUpdated, nowTs));
			}

			return result;
		} catch (BadRequestException | ResourceException | UnauthorizedException | ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() == null ? "error" : e.getMessage());
		}
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
