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
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("limitPromotionLimitItemUpdateService")
public class LimitPromotionLimitItemUpdateService {

	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final LimitPromotionPersistenceDelegate limitPromotionPersistenceDelegate;
	private final MessageSource messageSource;

	public LimitPromotionLimitItemUpdateService(
			LimitItemPromotionsMapper limitItemPromotionsMapper,
			LimitPromotionPersistenceDelegate limitPromotionPersistenceDelegate,
			MessageSource messageSource) {
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.limitPromotionPersistenceDelegate = limitPromotionPersistenceDelegate;
		this.messageSource = messageSource;
	}

	public Map<String, Object> updateLimitItem(Map<String, Object> params) {
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		try {
			long companyId = readLong(params.get("company_id"));
			long limitId = readLong(params.get("limit_id"));

			requirePresentText(
					params,
					"limit_num",
					"promotions.limit.update_item_limit_num_required",
					locale);
			requirePresentText(
					params, "item_id", "promotions.limit.update_item_item_id_required", locale);
			requirePresentText(
					params,
					"distributor_id",
					"promotions.limit.update_item_distributor_id_required",
					locale);

			long limitNumLong =
					readMinOneLong(
							params.get("limit_num"), "promotions.limit.limit_invalid", locale);
			long itemId =
					readMinOneLong(
							params.get("item_id"),
							"promotions.limit.update_item_item_id_invalid",
							locale);
			long distributorId =
					readMinOneLong(
							params.get("distributor_id"),
							"promotions.limit.update_item_distributor_id_invalid",
							locale);

			LimitItemPromotions row = selectOneRow(companyId, distributorId, itemId, limitId);
			if (row == null) {
				throw new ResourceException(
						messageSource.getMessage("promotions.limit.no_update_data_found", null, locale));
			}

			int nowTs = (int) (System.currentTimeMillis() / 1000L);
			int affected =
					limitItemPromotionsMapper.update(
							null,
							new LambdaUpdateWrapper<LimitItemPromotions>()
									.eq(LimitItemPromotions::getCompanyId, companyId)
									.eq(LimitItemPromotions::getDistributorId, distributorId)
									.eq(LimitItemPromotions::getItemId, itemId)
									.eq(LimitItemPromotions::getLimitId, limitId)
									.set(LimitItemPromotions::getLimitNum, limitNumLong)
									.set(LimitItemPromotions::getUpdated, nowTs));
			if (affected == 0) {
				throw new ResourceException(
						messageSource.getMessage("promotions.limit.no_update_data_found", null, locale));
			}

			LimitItemPromotions refreshed = selectOneRow(companyId, distributorId, itemId, limitId);
			if (refreshed == null) {
				throw new ResourceException(
						messageSource.getMessage("promotions.limit.no_update_data_found", null, locale));
			}
			return limitPromotionPersistenceDelegate.toLimitItemApiMap(refreshed);
		} catch (BadRequestException | ResourceException | UnauthorizedException | ForbiddenException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() == null ? "error" : e.getMessage());
		}
	}

	private LimitItemPromotions selectOneRow(
			long companyId, long distributorId, long itemId, long limitId) {
		return limitItemPromotionsMapper.selectOne(
				new LambdaQueryWrapper<LimitItemPromotions>()
						.eq(LimitItemPromotions::getCompanyId, companyId)
						.eq(LimitItemPromotions::getDistributorId, distributorId)
						.eq(LimitItemPromotions::getItemId, itemId)
						.eq(LimitItemPromotions::getLimitId, limitId)
						.last("LIMIT 1"));
	}

	private void requirePresentText(
			Map<String, Object> params, String key, String messageKey, Locale locale) {
		if (!params.containsKey(key)
				|| params.get(key) == null
				|| !StringUtils.hasText(Objects.toString(params.get(key), "").trim())) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
	}

	private long readMinOneLong(Object v, String messageKey, Locale locale) {
		try {
			long n;
			if (v instanceof Number num) {
				n = num.longValue();
			} else {
				n = Long.parseLong(String.valueOf(v).trim());
			}
			if (n < 1L) {
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
}
