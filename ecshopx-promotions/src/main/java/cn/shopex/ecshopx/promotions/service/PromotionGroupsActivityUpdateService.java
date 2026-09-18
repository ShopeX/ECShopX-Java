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

import cn.shopex.ecshopx.common.dispatch.SavePromotionItemTagJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsRelGoodsWriteService.NormalizedGroupSkuItem;
import cn.shopex.ecshopx.promotions.service.event.PromotionGroupActivityCommittedEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsActivityUpdateService {

	private final MessageSource messageSource;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionGroupsActivityGroupParamValidationService promotionGroupsActivityGroupParamValidationService;
	private final MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final PromotionGroupsActivityUpdateWritesService promotionGroupsActivityUpdateWritesService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final MarketingActivityPostCommitJobsService marketingActivityPostCommitJobsService;
	private final SavePromotionItemTagJobDispatchPublisher savePromotionItemTagJobDispatchPublisher;
	private final PromotionGroupsRelGoodsWriteService promotionGroupsRelGoodsWriteService;

	public PromotionGroupsActivityUpdateService(
			MessageSource messageSource,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionGroupsActivityGroupParamValidationService promotionGroupsActivityGroupParamValidationService,
			MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			PromotionGroupsActivityUpdateWritesService promotionGroupsActivityUpdateWritesService,
			ApplicationEventPublisher applicationEventPublisher,
			MarketingActivityPostCommitJobsService marketingActivityPostCommitJobsService,
			SavePromotionItemTagJobDispatchPublisher savePromotionItemTagJobDispatchPublisher,
			PromotionGroupsRelGoodsWriteService promotionGroupsRelGoodsWriteService) {
		this.messageSource = messageSource;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionGroupsActivityGroupParamValidationService = promotionGroupsActivityGroupParamValidationService;
		this.marketingActivityCrossPromotionGuardService = marketingActivityCrossPromotionGuardService;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.promotionGroupsActivityUpdateWritesService = promotionGroupsActivityUpdateWritesService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.marketingActivityPostCommitJobsService = marketingActivityPostCommitJobsService;
		this.savePromotionItemTagJobDispatchPublisher = savePromotionItemTagJobDispatchPublisher;
		this.promotionGroupsRelGoodsWriteService = promotionGroupsRelGoodsWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updatePromotionGroupsActivity(
			Map<String, Object> params, String groupId, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		if (groupId == null || !StringUtils.hasText(groupId.trim())) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		String trimmed = groupId.trim();
		long groupsActivityId;
		try {
			groupsActivityId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}

		long companyId = PromotionGroupsActivityGroupParamValidationService.readLong(params.get("company_id"));

		promotionGroupsActivityGroupParamValidationService.validateRequestParams(params, locale);

		PromotionGroupsActivity existing =
				promotionGroupsActivityMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsActivity>()
								.eq(PromotionGroupsActivity::getGroupsActivityId, groupsActivityId)
								.eq(PromotionGroupsActivity::getCompanyId, companyId));
		if (existing == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		Long existingBegin = existing.getBeginTime();
		Long existingEnd = existing.getEndTime();
		if (existingBegin == null || existingEnd == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		if (now >= existingEnd) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.activity_started_cannot_edit", null, locale));
		}
		boolean inProgress = now >= existingBegin && now < existingEnd;
		if (!inProgress) {
			promotionGroupsActivityGroupParamValidationService.assertActivityNotYetStarted(existing, locale);
		}

		String actNameTrimmed = Objects.toString(params.get("act_name"), "").trim();
		long goodsId =
				promotionGroupsActivityGroupParamValidationService.readPositiveLongParam(
						params.get("goods_id"), "promotions.groups.goods_id_required", locale);
		if (inProgress && !Objects.equals(goodsId, existing.getGoodsId())) {
			throw new ResourceException(messageSource.getMessage("promotions.groups.invalid_product", null, locale));
		}

		int limitBuyNum =
				promotionGroupsActivityGroupParamValidationService.readIntInRange(
						params.get("limit_buy_num"), 0, 999, "promotions.groups.limit_buy_num_invalid", locale);
		int personNum =
				promotionGroupsActivityGroupParamValidationService.readIntInRange(
						params.get("person_num"), 2, 999, "promotions.groups.person_num_invalid", locale);
		int limitTime =
				promotionGroupsActivityGroupParamValidationService.readIntInRange(
						params.get("limit_time"), 1, 99, "promotions.groups.limit_time_invalid", locale);

		List<NormalizedGroupSkuItem> skuItems =
				promotionGroupsRelGoodsWriteService.normalizeAndValidateItems(params, companyId, locale);
		long actPriceFen =
				skuItems.stream().mapToLong(NormalizedGroupSkuItem::activityPriceFen).min().orElseThrow();
		long storeSum = skuItems.stream().mapToLong(NormalizedGroupSkuItem::activityStore).sum();

		Object dateRaw = params.get("date");
		if (!(dateRaw instanceof List<?> dateList) || dateList.size() != 2) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.date_required", null, locale));
		}
		long beginEpoch =
				promotionGroupsActivityGroupParamValidationService.requireFlexibleEpochSeconds(dateList.get(0), locale);
		long endEpoch =
				promotionGroupsActivityGroupParamValidationService.requireFlexibleEpochSeconds(dateList.get(1), locale);
		int begin = (int) beginEpoch;
		int end = (int) endEpoch;

		if ((long) end - begin < (long) limitTime * 3600L) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.activity_time_longer_than_group_time", null, locale));
		}

		Map<String, Object> guardParams = new LinkedHashMap<>();
		guardParams.put("company_id", companyId);
		guardParams.put("start_time", begin);
		guardParams.put("end_time", end);
		guardParams.put("use_bound", 1);
		guardParams.put("shop_ids", List.of());
		guardParams.put("source_id", 0L);
		guardParams.put("groups_activity_id", groupsActivityId);
		guardParams.put(
				"item_ids",
				skuItems.stream().map(NormalizedGroupSkuItem::itemId).toList());
		guardParams.put("seckill_type", List.of("normal", "limited_time_sale"));
		marketingActivityCrossPromotionGuardService.checkActivityValidByGroup(guardParams);

		promotionGroupsActivityGroupParamValidationService.assertDuplicateNameForUpdate(
				companyId, actNameTrimmed, groupsActivityId, locale);

		if (!inProgress) {
			int beginTime = begin > now ? begin : now;
			if (beginTime < now) {
				throw new ResourceException(
						messageSource.getMessage("promotions.groups.start_time_greater_than_current", null, locale));
			}
			if (beginTime >= end) {
				throw new ResourceException(
						messageSource.getMessage("promotions.groups.end_time_greater_than_start", null, locale));
			}
		}

		if (promotionGroupsActivityGroupParamValidationService.hasAnyNonDeletedDistributor(companyId)) {
			for (NormalizedGroupSkuItem sku : skuItems) {
				Long hitDistributorId =
						promotionGroupsActivityGroupParamValidationService.findLowPricedDistributorItem(
								companyId, sku.itemId(), (int) sku.activityPriceFen());
				if (hitDistributorId != null) {
					throw new ResourceException(
							messageSource.getMessage(
									"promotions.groups.group_price_cannot_greater_than_store_price",
									new Object[] {hitDistributorId},
									locale));
				}
			}
		}

		boolean freePost = promotionGroupsActivityGroupParamValidationService.readTriStateBoolean(params, "free_post", locale);
		boolean rigUp = promotionGroupsActivityGroupParamValidationService.readTriStateBoolean(params, "rig_up", locale);
		boolean robot = promotionGroupsActivityGroupParamValidationService.readTriStateBoolean(params, "robot", locale);

		String groupGoodsType = resolveGroupGoodsType(companyId, goodsId);

		existing.setActName(actNameTrimmed);
		existing.setGoodsId(goodsId);
		existing.setGroupGoodsType(groupGoodsType);
		existing.setPics(Objects.toString(params.get("pics"), "").trim());
		existing.setActPrice(actPriceFen);
		existing.setPersonNum((long) personNum);
		existing.setBeginTime((long) begin);
		existing.setEndTime((long) end);
		existing.setLimitBuyNum((long) limitBuyNum);
		existing.setLimitTime(limitTime);
		existing.setStore(storeSum);
		existing.setFreePost(freePost);
		existing.setRigUp(rigUp);
		existing.setRobot(robot);
		existing.setShareDesc(Objects.toString(params.get("share_desc"), "").trim());
		existing.setUpdated(now);

		if (inProgress) {
			promotionGroupsRelGoodsWriteService.updatePricesAndStoresInPlace(
					groupsActivityId, companyId, skuItems, now, locale);
		} else {
			promotionGroupsRelGoodsWriteService.replaceRelGoods(groupsActivityId, companyId, skuItems, now);
		}

		Map<String, Object> row =
				promotionGroupsActivityUpdateWritesService.applyUpdateWrites(
						existing, params, requestLangTag, locale);

		if (groupsActivityId > 0L) {
			Map<Long, BigDecimal> priceMap = new LinkedHashMap<>();
			List<Long> itemIds = new ArrayList<>();
			for (NormalizedGroupSkuItem s : skuItems) {
				itemIds.add(s.itemId());
				priceMap.put(s.itemId(), BigDecimal.valueOf(s.activityPriceFen()).movePointLeft(2));
			}
			savePromotionItemTagJobDispatchPublisher.publishSavePromotionItemTag(
					companyId,
					groupsActivityId,
					"single_group",
					begin,
					end,
					groupGoodsType,
					itemIds,
					priceMap);
		}

		if (groupsActivityId > 0L) {
			applicationEventPublisher.publishEvent(new PromotionGroupActivityCommittedEvent(companyId, groupsActivityId));
		}

		marketingActivityPostCommitJobsService.enqueueGroupSalespersonItemsShelves(companyId, groupsActivityId);

		return row;
	}

	private String resolveGroupGoodsType(long companyId, long goodsId) {
		Map<String, Object> skuPack = marketingActivityCatalogAccess.loadSkuItemsList(companyId, List.of(goodsId));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) skuPack.get("list");
		if (skuList != null && !skuList.isEmpty()) {
			Map<String, Object> itemRow = skuList.get(0);
			if (itemRow.get("item_type") != null && StringUtils.hasText(itemRow.get("item_type").toString().trim())) {
				return itemRow.get("item_type").toString().trim();
			}
		}
		return "services";
	}
}
