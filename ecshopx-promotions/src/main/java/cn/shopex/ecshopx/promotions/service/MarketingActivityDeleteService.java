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

import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.domain.MarketingGiftItems;
import cn.shopex.ecshopx.promotions.domain.PromotionsItemsTag;
import cn.shopex.ecshopx.promotions.event.MarketingActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class MarketingActivityDeleteService {

	private final MarketingActivityItemListActivityQuerySupport marketingActivityItemListActivityQuerySupport;
	private final MarketingActivityListMultiLangReadService marketingActivityListMultiLangReadService;
	private final MarketingActivityMapper marketingActivityMapper;
	private final MarketingActivityItemsMapper marketingActivityItemsMapper;
	private final MarketingGiftItemsMapper marketingGiftItemsMapper;
	private final PromotionsItemsTagMapper promotionsItemsTagMapper;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher;
	private final MessageSource messageSource;

	public MarketingActivityDeleteService(
			MarketingActivityItemListActivityQuerySupport marketingActivityItemListActivityQuerySupport,
			MarketingActivityListMultiLangReadService marketingActivityListMultiLangReadService,
			MarketingActivityMapper marketingActivityMapper,
			MarketingActivityItemsMapper marketingActivityItemsMapper,
			MarketingGiftItemsMapper marketingGiftItemsMapper,
			PromotionsItemsTagMapper promotionsItemsTagMapper,
			ApplicationEventPublisher applicationEventPublisher,
			SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher,
			MessageSource messageSource) {
		this.marketingActivityItemListActivityQuerySupport = marketingActivityItemListActivityQuerySupport;
		this.marketingActivityListMultiLangReadService = marketingActivityListMultiLangReadService;
		this.marketingActivityMapper = marketingActivityMapper;
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
		this.marketingGiftItemsMapper = marketingGiftItemsMapper;
		this.promotionsItemsTagMapper = promotionsItemsTagMapper;
		this.applicationEventPublisher = applicationEventPublisher;
		this.salespersonItemsShelvesJobDispatchPublisher = salespersonItemsShelvesJobDispatchPublisher;
		this.messageSource = messageSource;
	}

	@Transactional(rollbackFor = Exception.class)
	public Object deleteMarketingActivity(
			long companyId, long marketingId, boolean physicalDeleteBranch, String requestLangTag) {
		String resolvedLang = (requestLangTag == null || requestLangTag.isBlank()) ? "zh-CN" : requestLangTag.trim();
		Locale locale = Locale.forLanguageTag(resolvedLang.replace('_', '-'));
		if (locale.getLanguage().isEmpty()) {
			locale = Locale.SIMPLIFIED_CHINESE;
		}
		if (physicalDeleteBranch) {
			return deletePhysical(companyId, marketingId, resolvedLang, locale);
		}
		return endActivity(companyId, marketingId, locale);
	}

	private String msg(String code, String defaultZh, Locale locale) {
		return messageSource.getMessage(code, null, defaultZh, locale);
	}

	private Map<String, Object> deletePhysical(
			long companyId, long marketingId, String resolvedLang, Locale locale) {
		Optional<MarketingActivity> opt =
				marketingActivityItemListActivityQuerySupport.loadActivityRow(companyId, marketingId);
		if (opt.isEmpty()) {
			throw new ResourceException(msg("promotions.marketing_activity.not_found", "活动不存在", locale));
		}
		MarketingActivity row = opt.get();
		Map<String, Object> snapshot =
				new LinkedHashMap<>(
						marketingActivityItemListActivityQuerySupport.buildActivityPayloadForItemList(row));
		marketingActivityListMultiLangReadService.applyListTranslations(companyId, List.of(snapshot), resolvedLang);
		String st = snapshot.get("status") instanceof String s ? s : "";
		if (!"waiting".equals(st)) {
			throw new ResourceException(
					msg("promotions.marketing_activity.cannot_delete_activity", "该活动不能删除", locale));
		}
		marketingActivityMapper.delete(
				new LambdaQueryWrapper<MarketingActivity>()
						.eq(MarketingActivity::getCompanyId, companyId)
						.eq(MarketingActivity::getMarketingId, marketingId));
		marketingActivityItemsMapper.delete(
				new LambdaQueryWrapper<MarketingActivityItems>()
						.eq(MarketingActivityItems::getCompanyId, companyId)
						.eq(MarketingActivityItems::getMarketingId, marketingId));
		marketingGiftItemsMapper.delete(
				new LambdaQueryWrapper<MarketingGiftItems>()
						.eq(MarketingGiftItems::getCompanyId, companyId)
						.eq(MarketingGiftItems::getMarketingId, marketingId));
		String tagType = row.getMarketingType() != null ? row.getMarketingType() : "";
		promotionsItemsTagMapper.delete(
				new LambdaQueryWrapper<PromotionsItemsTag>()
						.eq(PromotionsItemsTag::getPromotionId, marketingId)
						.eq(PromotionsItemsTag::getCompanyId, companyId)
						.eq(PromotionsItemsTag::getTagType, tagType));
		int activityStart = row.getStartTime() != null ? row.getStartTime() : 0;
		int activityEnd = row.getEndTime() != null ? row.getEndTime() : 0;
		MarketingActivityCommittedEvent event =
				new MarketingActivityCommittedEvent(
						companyId,
						marketingId,
						tagType,
						activityStart,
						activityEnd,
						false,
						null,
						List.of(),
						Map.of(),
						false);
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						salespersonItemsShelvesJobDispatchPublisher.publish(companyId, marketingId, tagType);
						applicationEventPublisher.publishEvent(event);
					}
				});
		return snapshot;
	}

	private Boolean endActivity(long companyId, long marketingId, Locale locale) {
		Optional<MarketingActivity> opt =
				marketingActivityItemListActivityQuerySupport.loadActivityRow(companyId, marketingId);
		if (opt.isEmpty()) {
			throw new ResourceException(
					msg("promotions.marketing_activity.no_update_data_found", "未查询到更新数据", locale));
		}
		MarketingActivity row = opt.get();
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		row.setEndTime(nowSec);
		row.setUpdated(nowSec);
		int n = marketingActivityMapper.updateById(row);
		if (n <= 0) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			return Boolean.FALSE;
		}
		LambdaUpdateWrapper<MarketingActivityItems> uw = new LambdaUpdateWrapper<>();
		uw.eq(MarketingActivityItems::getCompanyId, companyId)
				.eq(MarketingActivityItems::getMarketingId, marketingId);
		uw.set(MarketingActivityItems::getEndTime, nowSec).set(MarketingActivityItems::getUpdated, nowSec);
		marketingActivityItemsMapper.update(null, uw);
		String tagType = row.getMarketingType() != null ? row.getMarketingType() : "";
		promotionsItemsTagMapper.delete(
				new LambdaQueryWrapper<PromotionsItemsTag>()
						.eq(PromotionsItemsTag::getPromotionId, marketingId)
						.eq(PromotionsItemsTag::getCompanyId, companyId)
						.eq(PromotionsItemsTag::getTagType, tagType));
		int activityStart = row.getStartTime() != null ? row.getStartTime() : 0;
		MarketingActivityCommittedEvent event =
				new MarketingActivityCommittedEvent(
						companyId,
						marketingId,
						tagType,
						activityStart,
						nowSec,
						false,
						null,
						List.of(),
						Map.of(),
						false);
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						salespersonItemsShelvesJobDispatchPublisher.publish(companyId, marketingId, tagType);
						applicationEventPublisher.publishEvent(event);
					}
				});
		return Boolean.TRUE;
	}
}
