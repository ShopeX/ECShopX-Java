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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class LimitPromotionCancelService {

	private static final Logger log = LoggerFactory.getLogger(LimitPromotionCancelService.class);

	private final LimitPromotionsMapper limitPromotionsMapper;
	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final LimitPromotionPersistenceDelegate limitPromotionPersistenceDelegate;
	private final MessageSource messageSource;

	public LimitPromotionCancelService(
			LimitPromotionsMapper limitPromotionsMapper,
			LimitItemPromotionsMapper limitItemPromotionsMapper,
			LimitPromotionPersistenceDelegate limitPromotionPersistenceDelegate,
			MessageSource messageSource) {
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.limitPromotionPersistenceDelegate = limitPromotionPersistenceDelegate;
		this.messageSource = messageSource;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> cancel(long limitId, long companyId) {
		Locale locale = Locale.SIMPLIFIED_CHINESE;
		try {
			locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
			int nowEpoch = (int) Instant.now().getEpochSecond();
			int endEpoch = nowEpoch - 1;

			LimitPromotions row =
					limitPromotionsMapper.selectOne(
							new LambdaQueryWrapper<LimitPromotions>()
									.eq(LimitPromotions::getLimitId, limitId)
									.eq(LimitPromotions::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (row == null) {
				throw new IllegalStateException("limit promotion cancel precondition not met");
			}

			LambdaUpdateWrapper<LimitPromotions> uw =
					new LambdaUpdateWrapper<LimitPromotions>()
							.eq(LimitPromotions::getLimitId, limitId)
							.eq(LimitPromotions::getCompanyId, companyId)
							.set(LimitPromotions::getEndTime, endEpoch)
							.set(LimitPromotions::getUpdated, nowEpoch);
			int mainRows = limitPromotionsMapper.update(null, uw);
			if (mainRows == 0) {
				throw new IllegalStateException("limit promotion cancel precondition not met");
			}

			limitItemPromotionsMapper.update(
					null,
					new LambdaUpdateWrapper<LimitItemPromotions>()
							.eq(LimitItemPromotions::getCompanyId, companyId)
							.eq(LimitItemPromotions::getLimitId, limitId)
							.set(LimitItemPromotions::getEndTime, endEpoch)
							.set(LimitItemPromotions::getUpdated, nowEpoch));

			LimitPromotions fresh = limitPromotionsMapper.selectById(limitId);
			if (fresh == null || !Objects.equals(fresh.getCompanyId(), companyId)) {
				throw new IllegalStateException("limit promotion cancel precondition not met");
			}

			String vg = fresh.getValidGrade();
			List<String> validGradeParts;
			if (vg == null || !StringUtils.hasText(vg.trim())) {
				validGradeParts = List.of();
			} else {
				validGradeParts =
						Arrays.stream(vg.split(","))
								.map(String::trim)
								.filter(StringUtils::hasText)
								.collect(Collectors.toList());
			}

			return limitPromotionPersistenceDelegate.buildLimitPromotionCancelWireMap(fresh, validGradeParts);
		} catch (Exception e) {
			log.error("limitPromotionCancel failed, limitId={}, companyId={}", limitId, companyId, e);
			throw new ResourceException(messageSource.getMessage("promotions.limit.cancel_failed", null, locale));
		}
	}
}
