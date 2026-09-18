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
import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PromotionsItemsTag;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.event.SeckillActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.event.SeckillActivityEndCommittedEvent;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.SeckillActivityOutsideMultiLangWriteService;
import cn.shopex.ecshopx.promotions.util.SeckillShopIdCsvParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class SeckillActivityCreateService {

	private static final ZoneId DATETIME_PARSE_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter LOCAL_DATETIME_SECONDS =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter SECKILL_LISTING_DATETIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DATETIME_PARSE_ZONE);

	private final MessageSource messageSource;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;
	private final SeckillActivityMapper seckillActivityMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final PromotionsItemsTagMapper promotionsItemsTagMapper;
	private final SeckillActivityOutsideMultiLangWriteService seckillActivityOutsideMultiLangWriteService;
	private final SeckillActivityItemStoreWriteService seckillActivityItemStoreWriteService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final ObjectMapper objectMapper;
	private final SavePromotionItemTagJobDispatchPublisher savePromotionItemTagJobDispatchPublisher;
	private final SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher;

	public SeckillActivityCreateService(
			MessageSource messageSource,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService,
			SeckillActivityMapper seckillActivityMapper,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			PromotionsItemsTagMapper promotionsItemsTagMapper,
			SeckillActivityOutsideMultiLangWriteService seckillActivityOutsideMultiLangWriteService,
			SeckillActivityItemStoreWriteService seckillActivityItemStoreWriteService,
			ApplicationEventPublisher applicationEventPublisher,
			ObjectMapper objectMapper,
			SavePromotionItemTagJobDispatchPublisher savePromotionItemTagJobDispatchPublisher,
			SalespersonItemsShelvesJobDispatchPublisher salespersonItemsShelvesJobDispatchPublisher) {
		this.messageSource = messageSource;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.marketingActivityCrossPromotionGuardService = marketingActivityCrossPromotionGuardService;
		this.seckillActivityMapper = seckillActivityMapper;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.promotionsItemsTagMapper = promotionsItemsTagMapper;
		this.seckillActivityOutsideMultiLangWriteService = seckillActivityOutsideMultiLangWriteService;
		this.seckillActivityItemStoreWriteService = seckillActivityItemStoreWriteService;
		this.applicationEventPublisher = applicationEventPublisher;
		this.objectMapper = objectMapper;
		this.savePromotionItemTagJobDispatchPublisher = savePromotionItemTagJobDispatchPublisher;
		this.salespersonItemsShelvesJobDispatchPublisher = salespersonItemsShelvesJobDispatchPublisher;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createSeckillActivity(Map<String, Object> params, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		normalizeItemsJson(params, locale);
		normalizeLimitMoneyFields(params);
		normalizeSeckillType(params);

		String activityName = Objects.toString(params.get("activity_name"), "").trim();
		if (!StringUtils.hasText(activityName)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.activity_name_required", null, locale));
		}

		long activityStartEpoch = requireFlexibleEpochSeconds(params.get("activity_start_time"), locale);
		long activityEndEpoch = requireFlexibleEpochSeconds(params.get("activity_end_time"), locale);
		long activityReleaseEpoch = requireFlexibleEpochSeconds(params.get("activity_release_time"), locale);
		int activityStart = (int) activityStartEpoch;
		int activityEnd = (int) activityEndEpoch;
		int activityRelease = (int) activityReleaseEpoch;

		assertLimitMoneyDigits(params.get("limit_total_money"), locale);
		assertLimitMoneyDigits(params.get("limit_money"), locale);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
		if (items == null || items.isEmpty()) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.bind_product_param_missing", null, locale));
		}

		if (activityRelease > activityStart) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.release_time_after_start", null, locale));
		}
		if (activityStart >= activityEnd) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.start_must_before_end", null, locale));
		}

		Object vpObj = params.get("validity_period");
		if (!(vpObj instanceof Number) || ((Number) vpObj).intValue() <= 1) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.validity_period_invalid", null, locale));
		}

		long companyId = readLong(params.get("company_id"));
		String seckillTypeStr = Objects.toString(params.get("seckill_type"), "normal").trim();
		if (!StringUtils.hasText(seckillTypeStr)) {
			seckillTypeStr = "normal";
		}

		List<Long> itemIds = validateSeckillItemsForAdminWrite(companyId, seckillTypeStr, items, locale);

		if (Objects.equals("limited_time_sale", seckillTypeStr)) {
			assertNoOverlappingLimitedTimeSaleForShops(companyId, activityStart, activityEnd, params);
		}

		List<Long> shopIds =
				SeckillShopIdCsvParser.parsePositiveShopIdsFromCsv(Objects.toString(params.get("distributor_id"), "").trim());
		LinkedHashMap<String, Object> guardParams = new LinkedHashMap<>();
		guardParams.put("company_id", companyId);
		guardParams.put("start_time", activityStart);
		guardParams.put("end_time", activityEnd);
		guardParams.put("use_bound", 1);
		guardParams.put("shop_ids", shopIds);
		guardParams.put("source_id", readLong(params.get("source_id")));
		guardParams.put("item_ids", itemIds);
		guardParams.put("seckill_type", seckillTypeStr);
		marketingActivityCrossPromotionGuardService.checkActivityValidBySecKill(guardParams);

		long limitTotalFen = moneyYuanToFenIfNonZero(params.get("limit_total_money"));
		long limitMoneyFen = moneyYuanToFenIfNonZero(params.get("limit_money"));
		int useBound = resolveUseBound(params);

		int now = (int) (System.currentTimeMillis() / 1000L);
		SeckillActivity entity = new SeckillActivity();
		entity.setCompanyId(companyId);
		entity.setDistributorId(wrapDistributorIdCsv(params.get("distributor_id")));
		entity.setActivityName(activityName);
		entity.setAdPic(nullIfBlank(Objects.toString(params.get("ad_pic"), null)));
		entity.setActivityStartTime(activityStart);
		entity.setActivityEndTime(activityEnd);
		entity.setActivityReleaseTime(activityRelease);
		entity.setIsActivityRebate(readBooleanWithDefaultFalse(params.get("is_activity_rebate")));
		entity.setIsFreeShipping(readBooleanWithDefaultFalse(params.get("is_free_shipping")));
		entity.setLimitTotalMoney(limitTotalFen);
		entity.setLimitMoney(limitMoneyFen);
		entity.setValidityPeriod(((Number) params.get("validity_period")).intValue());
		entity.setOtherext(nullIfBlank(Objects.toString(params.get("otherext"), null)));
		entity.setDescription(nullIfBlank(Objects.toString(params.get("description"), null)));
		entity.setSeckillType(seckillTypeStr);
		entity.setItemType(nullIfBlankElseDefault(Objects.toString(params.get("item_type"), null), "normal"));
		entity.setUseBound(useBound);
		entity.setCreated(now);
		entity.setUpdated(now);
		entity.setSourceType(nullIfBlank(Objects.toString(params.get("source_type"), null)));
		entity.setSourceId(readLongOrZero(params.get("source_id")));
		entity.setDisabled(false);

		seckillActivityMapper.insert(entity);
		Long seckillId = entity.getSeckillId();
		if (seckillId == null || seckillId <= 0L) {
			throw new ResourceException("活动创建失败");
		}

		seckillActivityOutsideMultiLangWriteService.addForNewSeckill(seckillId, companyId, params, requestLangTag);

		seckillRelGoodsMapper.delete(
				new LambdaQueryWrapper<SeckillRelGoods>()
						.eq(SeckillRelGoods::getCompanyId, companyId)
						.eq(SeckillRelGoods::getSeckillId, seckillId));

		Map<String, Object> skuResult = marketingActivityCatalogAccess.loadSkuItemsList(companyId, itemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) skuResult.get("list");
		if (skuList == null) {
			skuList = List.of();
		}
		Set<Long> existingSku =
				skuList.stream().map(m -> readLong(m.get("item_id"))).filter(id -> id > 0L).collect(Collectors.toSet());
		Map<Long, Map<String, Object>> skuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> m : skuList) {
			long iid = readLong(m.get("item_id"));
			if (iid > 0L) {
				skuByItemId.put(iid, m);
			}
		}

		Map<Long, Boolean> defaultItemShowTracker = new LinkedHashMap<>();
		List<Map<String, Object>> insertedItemRows = new ArrayList<>();
		Map<Long, BigDecimal> activityPriceByItemId = new LinkedHashMap<>();
		boolean wroteRedis = false;
		for (Map<String, Object> row : items) {
			long rowItemId = readLong(row.get("item_id"));
			if (!existingSku.contains(rowItemId)) {
				continue;
			}
			Map<String, Object> skuRow = skuByItemId.get(rowItemId);
			long defId = readLong(skuRow.get("default_item_id"));
			if (defId <= 0L) {
				defId = rowItemId;
			}
			boolean isShow = defaultItemShowTracker.putIfAbsent(defId, Boolean.TRUE) == null;

			String title = Objects.toString(row.get("item_title"), "");
			if (!StringUtils.hasText(title)) {
				title = Objects.toString(skuRow.get("item_name"), "");
			}
			String itemPic = Objects.toString(row.get("item_pic"), "");
			if (!StringUtils.hasText(itemPic)) {
				itemPic = Objects.toString(skuRow.get("pics"), "");
			}
			BigDecimal priceYuan = readActivityPriceYuan(row.get("activity_price"), title, locale);
			int priceFen = priceYuan.movePointRight(2).setScale(0, RoundingMode.DOWN).intValue();
			int activityStoreInt = readInt(row.get("activity_store"));
			int limitNum = readInt(row.get("limit_num"));
			int sort = readIntWithDefault(row.get("sort"), 0);
			String itemType = Objects.toString(skuRow.get("type"), "normal");
			if (!StringUtils.hasText(itemType)) {
				itemType = "normal";
			}

			SeckillRelGoods rel = new SeckillRelGoods();
			rel.setSeckillId(seckillId);
			rel.setCompanyId(companyId);
			rel.setSeckillType(seckillTypeStr);
			rel.setItemId(rowItemId);
			rel.setItemType(itemType);
			rel.setIsShow(isShow);
			rel.setActivityStartTime(activityStart);
			rel.setActivityEndTime(activityEnd);
			rel.setActivityReleaseTime(activityRelease);
			rel.setItemTitle(title);
			rel.setItemPic(itemPic);
			rel.setActivityPrice(priceFen);
			rel.setActivityStore(activityStoreInt);
			rel.setLimitNum(limitNum);
			rel.setSort(sort);
			rel.setCreated(now);
			rel.setUpdated(now);
			rel.setDisabled(false);
			seckillRelGoodsMapper.insert(rel);
			insertedItemRows.add(relGoodsToMap(rel));
			activityPriceByItemId.put(rowItemId, BigDecimal.valueOf(priceFen).movePointLeft(2));

			if (!"limited_time_sale".equals(seckillTypeStr)) {
				seckillActivityItemStoreWriteService.hsetStore(companyId, seckillId, rowItemId, activityStoreInt);
				wroteRedis = true;
			}
		}

		if (wroteRedis) {
			seckillActivityItemStoreWriteService.expireAtEndPlusOneDay(companyId, seckillId, activityEnd);
		}

		savePromotionItemTagJobDispatchPublisher.publishSavePromotionItemTag(
				companyId,
				seckillId,
				seckillTypeStr,
				activityRelease,
				activityEnd,
				entity.getItemType(),
				itemIds,
				activityPriceByItemId);

		boolean isNormalSeckill = "normal".equals(seckillTypeStr);
		String activityTypeRaw = isNormalSeckill ? "seckill" : "limited_time_sale";
		SeckillActivityCommittedEvent committedEvent =
				new SeckillActivityCommittedEvent(companyId, seckillId, List.copyOf(itemIds), false);
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						salespersonItemsShelvesJobDispatchPublisher.publish(companyId, seckillId, activityTypeRaw);
						applicationEventPublisher.publishEvent(committedEvent);
					}
				});

		return buildResponseMap(entity, insertedItemRows);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateSeckillActivity(Map<String, Object> params, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		normalizeItemsJson(params, locale);
		normalizeLimitMoneyFields(params);
		normalizeSeckillType(params);

		long companyId = readLong(params.get("company_id"));
		long seckillId = readLongOrZero(params.get("seckill_id"));
		if (seckillId <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.seckill_id_required", null, locale));
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) params.get("items");
		if (items == null || items.isEmpty()) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.bind_product_param_missing", null, locale));
		}

		String seckillTypeStr = Objects.toString(params.get("seckill_type"), "normal").trim();
		if (!StringUtils.hasText(seckillTypeStr)) {
			seckillTypeStr = "normal";
		}

		List<Long> itemIds = validateSeckillItemsForAdminWrite(companyId, seckillTypeStr, items, locale);

		SeckillActivity existing =
				seckillActivityMapper.selectOne(
						new LambdaQueryWrapper<SeckillActivity>()
								.eq(SeckillActivity::getCompanyId, companyId)
								.eq(SeckillActivity::getSeckillId, seckillId));
		if (existing == null) {
			throw new ResourceException(messageSource.getMessage("promotions.seckill.no_update_data_found", null, locale));
		}

		long activityStartEpoch = requireFlexibleEpochSeconds(params.get("activity_start_time"), locale);
		long activityEndEpoch = requireFlexibleEpochSeconds(params.get("activity_end_time"), locale);
		long activityReleaseEpoch = requireFlexibleEpochSeconds(params.get("activity_release_time"), locale);
		int activityStart = (int) activityStartEpoch;
		int activityEnd = (int) activityEndEpoch;
		int activityRelease = (int) activityReleaseEpoch;

		String activityName = Objects.toString(params.get("activity_name"), "").trim();
		if (!StringUtils.hasText(activityName)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.activity_name_required", null, locale));
		}

		if (activityRelease > activityStart) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.release_time_after_start", null, locale));
		}
		if (activityStart >= activityEnd) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.start_must_before_end", null, locale));
		}

		Object vpObj = params.get("validity_period");
		if (!(vpObj instanceof Number) || ((Number) vpObj).intValue() <= 1) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.validity_period_invalid", null, locale));
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		String computedStatus = computeSeckillLifecycleStatus(existing, now);
		if ("normal".equals(seckillTypeStr) && !"waiting".equals(computedStatus)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.seckill.current_activity_not_editable", null, locale));
		}

		if (Objects.equals("limited_time_sale", seckillTypeStr)) {
			assertNoOverlappingLimitedTimeSaleForShops(companyId, activityStart, activityEnd, params);
		}

		List<Long> shopIds =
				SeckillShopIdCsvParser.parsePositiveShopIdsFromCsv(Objects.toString(params.get("distributor_id"), "").trim());
		LinkedHashMap<String, Object> guardParams = new LinkedHashMap<>();
		guardParams.put("company_id", companyId);
		guardParams.put("start_time", activityStart);
		guardParams.put("end_time", activityEnd);
		guardParams.put("use_bound", 1);
		guardParams.put("shop_ids", shopIds);
		guardParams.put("source_id", readLong(params.get("source_id")));
		guardParams.put("item_ids", itemIds);
		guardParams.put("seckill_type", seckillTypeStr);
		guardParams.put("exclude_seckill_id", seckillId);
		marketingActivityCrossPromotionGuardService.checkActivityValidBySecKill(guardParams);

		long limitTotalFen = moneyYuanToFenIfNonZero(params.get("limit_total_money"));
		long limitMoneyFen = moneyYuanToFenIfNonZero(params.get("limit_money"));
		int useBound = resolveUseBound(params);

		existing.setActivityName(activityName);
		existing.setAdPic(nullIfBlank(Objects.toString(params.get("ad_pic"), null)));
		existing.setActivityStartTime(activityStart);
		existing.setActivityEndTime(activityEnd);
		existing.setActivityReleaseTime(activityRelease);
		existing.setIsActivityRebate(readBooleanWithDefaultFalse(params.get("is_activity_rebate")));
		existing.setIsFreeShipping(readBooleanWithDefaultFalse(params.get("is_free_shipping")));
		existing.setLimitTotalMoney(limitTotalFen);
		existing.setLimitMoney(limitMoneyFen);
		existing.setValidityPeriod(((Number) params.get("validity_period")).intValue());
		existing.setOtherext(nullIfBlank(Objects.toString(params.get("otherext"), null)));
		existing.setDescription(nullIfBlank(Objects.toString(params.get("description"), null)));
		existing.setSeckillType(seckillTypeStr);
		existing.setItemType(nullIfBlankElseDefault(Objects.toString(params.get("item_type"), null), "normal"));
		existing.setUseBound(useBound);
		existing.setDistributorId(wrapDistributorIdCsv(params.get("distributor_id")));
		existing.setUpdated(now);
		existing.setSourceType(nullIfBlank(Objects.toString(params.get("source_type"), null)));
		existing.setSourceId(readLongOrZero(params.get("source_id")));

		int rows = seckillActivityMapper.updateById(existing);
		if (rows == 0) {
			throw new ResourceException(messageSource.getMessage("promotions.seckill.no_update_data_found", null, locale));
		}

		seckillActivityOutsideMultiLangWriteService.updateForSeckill(seckillId, companyId, params, requestLangTag);

		seckillRelGoodsMapper.delete(
				new LambdaQueryWrapper<SeckillRelGoods>()
						.eq(SeckillRelGoods::getCompanyId, companyId)
						.eq(SeckillRelGoods::getSeckillId, seckillId));

		Map<String, Object> skuResult = marketingActivityCatalogAccess.loadSkuItemsList(companyId, itemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList = (List<Map<String, Object>>) skuResult.get("list");
		if (skuList == null) {
			skuList = List.of();
		}
		Set<Long> existingSku =
				skuList.stream().map(m -> readLong(m.get("item_id"))).filter(id -> id > 0L).collect(Collectors.toSet());
		Map<Long, Map<String, Object>> skuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> m : skuList) {
			long iid = readLong(m.get("item_id"));
			if (iid > 0L) {
				skuByItemId.put(iid, m);
			}
		}

		Map<Long, Boolean> defaultItemShowTracker = new LinkedHashMap<>();
		List<Map<String, Object>> insertedItemRows = new ArrayList<>();
		Map<Long, BigDecimal> activityPriceByItemId = new LinkedHashMap<>();
		boolean wroteRedis = false;
		for (Map<String, Object> row : items) {
			long rowItemId = readLong(row.get("item_id"));
			if (!existingSku.contains(rowItemId)) {
				seckillRelGoodsMapper.delete(
						new LambdaQueryWrapper<SeckillRelGoods>()
								.eq(SeckillRelGoods::getCompanyId, companyId)
								.eq(SeckillRelGoods::getSeckillId, seckillId));
				continue;
			}
			Map<String, Object> skuRow = skuByItemId.get(rowItemId);
			long defId = readLong(skuRow.get("default_item_id"));
			if (defId <= 0L) {
				defId = rowItemId;
			}
			boolean isShow = defaultItemShowTracker.putIfAbsent(defId, Boolean.TRUE) == null;

			String title = Objects.toString(row.get("item_title"), "");
			if (!StringUtils.hasText(title)) {
				title = Objects.toString(skuRow.get("item_name"), "");
			}
			String itemPic = Objects.toString(row.get("item_pic"), "");
			if (!StringUtils.hasText(itemPic)) {
				itemPic = Objects.toString(skuRow.get("pics"), "");
			}
			BigDecimal priceYuan = readActivityPriceYuan(row.get("activity_price"), title, locale);
			int priceFen = priceYuan.movePointRight(2).setScale(0, RoundingMode.DOWN).intValue();
			int activityStoreInt = readInt(row.get("activity_store"));
			int limitNum = readInt(row.get("limit_num"));
			int sort = readIntWithDefault(row.get("sort"), 0);
			String itemType = Objects.toString(skuRow.get("type"), "normal");
			if (!StringUtils.hasText(itemType)) {
				itemType = "normal";
			}

			SeckillRelGoods rel = new SeckillRelGoods();
			rel.setSeckillId(seckillId);
			rel.setCompanyId(companyId);
			rel.setSeckillType(seckillTypeStr);
			rel.setItemId(rowItemId);
			rel.setItemType(itemType);
			rel.setIsShow(isShow);
			rel.setActivityStartTime(activityStart);
			rel.setActivityEndTime(activityEnd);
			rel.setActivityReleaseTime(activityRelease);
			rel.setItemTitle(title);
			rel.setItemPic(itemPic);
			rel.setActivityPrice(priceFen);
			rel.setActivityStore(activityStoreInt);
			rel.setLimitNum(limitNum);
			rel.setSort(sort);
			rel.setCreated(now);
			rel.setUpdated(now);
			rel.setDisabled(false);
			seckillRelGoodsMapper.insert(rel);
			insertedItemRows.add(relGoodsToMap(rel));
			activityPriceByItemId.put(rowItemId, BigDecimal.valueOf(priceFen).movePointLeft(2));

			if (!"limited_time_sale".equals(seckillTypeStr)) {
				seckillActivityItemStoreWriteService.hsetStore(companyId, seckillId, rowItemId, activityStoreInt);
				wroteRedis = true;
			}
		}

		if (wroteRedis) {
			seckillActivityItemStoreWriteService.expireAtEndPlusOneDay(companyId, seckillId, activityEnd);
		}

		savePromotionItemTagJobDispatchPublisher.publishSavePromotionItemTag(
				companyId,
				seckillId,
				seckillTypeStr,
				activityRelease,
				activityEnd,
				existing.getItemType(),
				itemIds,
				activityPriceByItemId);

		boolean isNormalSeckillUpdate = "normal".equals(seckillTypeStr);
		String activityTypeRawUpdate = isNormalSeckillUpdate ? "seckill" : "limited_time_sale";
		SeckillActivityCommittedEvent committedEventUpdate =
				new SeckillActivityCommittedEvent(companyId, seckillId, List.copyOf(itemIds), false);
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						salespersonItemsShelvesJobDispatchPublisher.publish(
								companyId, seckillId, activityTypeRawUpdate);
						applicationEventPublisher.publishEvent(committedEventUpdate);
					}
				});

		SeckillActivity reloaded = seckillActivityMapper.selectById(seckillId);
		if (reloaded == null) {
			throw new ResourceException(messageSource.getMessage("promotions.seckill.no_update_data_found", null, locale));
		}
		return enrichUpdateResponse(reloaded, insertedItemRows);
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateStatus(Map<String, Object> params, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long companyId = readLong(params.get("company_id"));

		long seckillId = 0L;
		try {
			Object sidObj = params.get("seckill_id");
			if (sidObj instanceof Number n) {
				seckillId = n.longValue();
			} else if (sidObj != null && StringUtils.hasText(sidObj.toString().trim())) {
				seckillId = Long.parseLong(sidObj.toString().trim());
			}
		} catch (NumberFormatException ignored) {
			seckillId = 0L;
		}
		if (seckillId <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.seckill_id_required", null, locale));
		}

		SeckillActivity row =
				seckillActivityMapper.selectOne(
						new LambdaQueryWrapper<SeckillActivity>()
								.eq(SeckillActivity::getCompanyId, companyId)
								.eq(SeckillActivity::getSeckillId, seckillId));
		if (row == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.seckill.no_update_data_found", null, locale));
		}

		row.setDisabled(Boolean.TRUE);
		row.setUpdated((int) (System.currentTimeMillis() / 1000L));
		int n = seckillActivityMapper.updateById(row);
		if (n == 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.seckill.no_update_data_found", null, locale));
		}
		Map<String, Object> resultPayload = new LinkedHashMap<>();
		resultPayload.put("seckill_type", row.getSeckillType());
		boolean stepOk = n > 0;
		if (stepOk) {
			Map<String, Object> langPayload = new LinkedHashMap<>();
			langPayload.put("activity_name", row.getActivityName());
			langPayload.put("description", row.getDescription());
			langPayload.put("ad_pic", row.getAdPic());
			seckillActivityOutsideMultiLangWriteService.updateForSeckill(
					seckillId, companyId, langPayload, requestLangTag);

			seckillRelGoodsMapper.update(
					null,
					new LambdaUpdateWrapper<SeckillRelGoods>()
							.eq(SeckillRelGoods::getCompanyId, companyId)
							.eq(SeckillRelGoods::getSeckillId, seckillId)
							.set(SeckillRelGoods::getDisabled, true)
							.set(SeckillRelGoods::getUpdated, row.getUpdated()));

			String tagTypeRaw = row.getSeckillType();
			String tagTypeForDelete;
			if (tagTypeRaw == null || !StringUtils.hasText(tagTypeRaw.trim())) {
				tagTypeForDelete = "normal";
			} else {
				tagTypeForDelete = tagTypeRaw.trim();
			}
			promotionsItemsTagMapper.delete(
					new LambdaQueryWrapper<PromotionsItemsTag>()
							.eq(PromotionsItemsTag::getPromotionId, seckillId)
							.eq(PromotionsItemsTag::getCompanyId, companyId)
							.eq(PromotionsItemsTag::getTagType, tagTypeForDelete));

			applicationEventPublisher.publishEvent(
					new SeckillActivityEndCommittedEvent(companyId, seckillId, row.getSeckillType()));
		}
	}

	private Map<String, Object> enrichUpdateResponse(SeckillActivity e, List<Map<String, Object>> itemsOut) {
		return assembleAdminSeckillReadPayload(e, itemsOut);
	}

	/**
	 * Assembles admin read payload for a seckill activity (listing status, dates, distributor display array).
	 */
	public Map<String, Object> assembleAdminSeckillReadPayload(
			SeckillActivity activity, List<Map<String, Object>> itemMaps) {
		Map<String, Object> m = buildResponseMap(activity, itemMaps);
		applySeckillListingStatusAndDates(m, activity, (int) (System.currentTimeMillis() / 1000L));
		return m;
	}

	/**
	 * H5 getinfo：主活动 + rel 行 Map + rel 分页 total，含列表态 status / *_date / distributor_id 数组化（与 assembleAdminSeckillReadPayload 一致，满足小程序端历史字段）。
	 */
	public Map<String, Object> assembleWxappH5GetInfoPayload(
			SeckillActivity activity, List<Map<String, Object>> relItemMaps, long relTotalCount) {
		Map<String, Object> m = buildResponseMap(activity, relItemMaps);
		applySeckillListingStatusAndDates(m, activity, (int) (System.currentTimeMillis() / 1000L));
		m.put("total_count", relTotalCount);
		return m;
	}

	/**
	 * H5 getlist：单行活动 + 全量本页 rel 行 Map（无 rel 分页 total_count 根字段）。
	 */
	public Map<String, Object> assembleWxappH5GetListRowPayload(
			SeckillActivity activity, List<Map<String, Object>> relItemMaps) {
		Map<String, Object> m = buildResponseMap(activity, relItemMaps);
		applySeckillListingStatusAndDates(m, activity, (int) (System.currentTimeMillis() / 1000L));
		return m;
	}

	/** 商品详情 {@code activity_info}（秒杀/限时特惠，不含关联 SKU 列表）。 */
	public Map<String, Object> buildGoodsDetailSeckillActivityInfo(SeckillActivity activity) {
		Map<String, Object> m = buildResponseMap(activity, List.of());
		applySeckillListingStatusAndDates(m, activity, (int) (System.currentTimeMillis() / 1000L));
		m.remove("items");
		return m;
	}

	/** Maps a persisted rel row to the same keys as create/update item rows in API responses. */
	public static Map<String, Object> relGoodsEntityToAdminRow(SeckillRelGoods r) {
		return relGoodsToMap(r);
	}

	private void applySeckillListingStatusAndDates(Map<String, Object> m, SeckillActivity e, int nowTime) {
		m.put("distributor_id", distributorIdToResponseArray(e.getDistributorId()));
		m.put("otherext", parseOtherextForResponse(e.getOtherext()));
		int activityEnd = nzEpoch(e.getActivityEndTime());
		int activityStart = nzEpoch(e.getActivityStartTime());
		int activityRelease = nzEpoch(e.getActivityReleaseTime());
		boolean disabled = Boolean.TRUE.equals(e.getDisabled());
		m.remove("last_seconds");
		if (nowTime >= activityEnd || disabled) {
			m.put("status", "it_has_ended");
		} else if (nowTime >= activityStart && nowTime < activityEnd && !disabled) {
			m.put("status", "in_sale");
			m.put("last_seconds", Math.max(0, activityEnd - nowTime));
		} else if (nowTime >= activityRelease && nowTime < activityStart && !disabled) {
			m.put("status", "in_the_notice");
			m.put("last_seconds", Math.max(0, activityStart - nowTime));
		} else if (nowTime < activityRelease && !disabled) {
			m.put("status", "waiting");
		} else {
			m.put("status", "it_has_ended");
		}
		m.put("activity_start_date", formatSeckillListingDateTime(activityStart));
		m.put("activity_end_date", formatSeckillListingDateTime(activityEnd));
		m.put("activity_release_date", formatSeckillListingDateTime(activityRelease));
		m.put("created_date", formatSeckillListingDateTime(nzEpoch(e.getCreated())));
		m.put("updated_date", formatSeckillListingDateTime(nzEpoch(e.getUpdated())));
	}

	private static String computeSeckillLifecycleStatus(SeckillActivity existing, int now) {
		int activityEnd = nzEpoch(existing.getActivityEndTime());
		int activityStart = nzEpoch(existing.getActivityStartTime());
		int activityRelease = nzEpoch(existing.getActivityReleaseTime());
		boolean disabled = Boolean.TRUE.equals(existing.getDisabled());
		if (now >= activityEnd || disabled) {
			return "it_has_ended";
		}
		if (now >= activityStart && now < activityEnd && !disabled) {
			return "in_sale";
		}
		if (now >= activityRelease && now < activityStart && !disabled) {
			return "in_the_notice";
		}
		if (now < activityRelease && !disabled) {
			return "waiting";
		}
		return "it_has_ended";
	}

	private static int nzEpoch(Integer t) {
		return t == null ? 0 : t;
	}

	private String formatSeckillListingDateTime(int epochSec) {
		if (epochSec <= 0) {
			return "";
		}
		return SECKILL_LISTING_DATETIME.format(Instant.ofEpochSecond(epochSec));
	}

	private Object parseOtherextForResponse(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return objectMapper.readValue(raw.trim(), new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private static List<String> distributorIdToResponseArray(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String t = raw.trim();
		while (t.startsWith(",")) {
			t = t.substring(1);
		}
		while (t.endsWith(",")) {
			t = t.substring(0, t.length() - 1);
		}
		if (!StringUtils.hasText(t)) {
			return null;
		}
		List<String> parts = new ArrayList<>();
		for (String p : t.split(",")) {
			parts.add(p);
		}
		return parts;
	}

	private Map<String, Object> buildResponseMap(SeckillActivity e, List<Map<String, Object>> itemsOut) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("seckill_id", e.getSeckillId());
		m.put("company_id", e.getCompanyId());
		m.put("activity_name", e.getActivityName());
		m.put("activity_start_time", e.getActivityStartTime());
		m.put("activity_end_time", e.getActivityEndTime());
		m.put("activity_release_time", e.getActivityReleaseTime());
		m.put("distributor_id", e.getDistributorId());
		m.put("seckill_type", e.getSeckillType());
		m.put("limit_total_money", e.getLimitTotalMoney());
		m.put("limit_money", e.getLimitMoney());
		m.put("validity_period", e.getValidityPeriod());
		m.put("ad_pic", e.getAdPic());
		m.put("description", e.getDescription());
		m.put("is_activity_rebate", e.getIsActivityRebate());
		m.put("is_free_shipping", e.getIsFreeShipping());
		m.put("item_type", e.getItemType());
		m.put("use_bound", e.getUseBound());
		m.put("otherext", e.getOtherext());
		m.put("source_type", e.getSourceType());
		m.put("source_id", e.getSourceId());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("disabled", e.getDisabled());
		m.put("items", itemsOut);
		return m;
	}

	static Map<String, Object> relGoodsToMap(SeckillRelGoods r) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", r.getId());
		m.put("seckill_id", r.getSeckillId());
		m.put("seckill_type", r.getSeckillType());
		m.put("item_id", r.getItemId());
		m.put("item_type", r.getItemType());
		m.put("is_show", r.getIsShow());
		m.put("item_spec_desc", r.getItemSpecDesc());
		m.put("company_id", r.getCompanyId());
		m.put("activity_start_time", r.getActivityStartTime());
		m.put("activity_end_time", r.getActivityEndTime());
		m.put("activity_release_time", r.getActivityReleaseTime());
		m.put("item_title", r.getItemTitle());
		m.put("item_pic", r.getItemPic());
		m.put("activity_price", r.getActivityPrice());
		m.put("activity_store", r.getActivityStore());
		m.put("limit_num", r.getLimitNum());
		m.put("sales_store", r.getSalesStore());
		m.put("sort", r.getSort());
		m.put("created", r.getCreated());
		m.put("updated", r.getUpdated());
		m.put("disabled", r.getDisabled());
		return m;
	}

	private void assertNoOverlappingLimitedTimeSaleForShops(
			long companyId, int activityStart, int activityEnd, Map<String, Object> params) {
		List<Long> shopIds =
				SeckillShopIdCsvParser.parsePositiveShopIdsFromCsv(Objects.toString(params.get("distributor_id"), "").trim());
		if (shopIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		int overlapFloor = Math.max(activityStart, now);
		LambdaQueryWrapper<SeckillActivity> w = new LambdaQueryWrapper<>();
		w.eq(SeckillActivity::getCompanyId, companyId)
				.eq(SeckillActivity::getSeckillType, "limited_time_sale")
				.eq(SeckillActivity::getDisabled, false)
				.le(SeckillActivity::getActivityStartTime, activityEnd)
				.ge(SeckillActivity::getActivityEndTime, overlapFloor);
		long seckillIdForExclude = readLongOrZero(params.get("seckill_id"));
		if (seckillIdForExclude > 0L) {
			w.ne(SeckillActivity::getSeckillId, seckillIdForExclude);
		}
		List<SeckillActivity> hits = seckillActivityMapper.selectList(w);
		if (hits == null || hits.isEmpty()) {
			return;
		}
		for (SeckillActivity row : hits) {
			Set<Long> rowShops =
					new LinkedHashSet<>(
							SeckillShopIdCsvParser.parsePositiveShopIdsFromCsv(Objects.toString(row.getDistributorId(), "")));
			for (Long sid : shopIds) {
				if (rowShops.contains(sid)) {
					throw new ResourceException("店铺id=" + sid + "已存在有效活动");
				}
			}
		}
	}

	/**
	 * Validates seckill item rows for admin create/update: required fields per row, distinct
	 * positive item IDs, catalog constraints, and type-specific numeric rules (inventory, price,
	 * per-user limit).
	 *
	 * @return ordered distinct positive {@code item_id} values for persistence and guards
	 */
	private List<Long> validateSeckillItemsForAdminWrite(
			long companyId, String seckillTypeStr, List<Map<String, Object>> items, Locale locale) {
		for (Map<String, Object> row : items) {
			assertSeckillItemRowRequiredFields(row, locale);
		}
		List<Long> itemIds = collectOrderedDistinctPositiveItemIds(items);
		if (itemIds.isEmpty()) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.bind_product_param_missing", null, locale));
		}

		if (marketingActivityCatalogAccess.anyGiftItem(companyId, itemIds)) {
			throw new ResourceException(messageSource.getMessage("promotions.seckill.gift_exists", null, locale));
		}

		if ("normal".equals(seckillTypeStr)) {
			for (Map<String, Object> row : items) {
				int store = readInt(row.get("activity_store"));
				if (store <= 0) {
					String title = Objects.toString(row.get("item_title"), "").trim();
					throw new BadRequestException(
							messageSource.getMessage(
									"promotions.seckill.item_store_invalid", new Object[] {title}, locale));
				}
			}
		}

		for (Map<String, Object> row : items) {
			String title = Objects.toString(row.get("item_title"), "").trim();
			BigDecimal priceYuan = readActivityPriceYuan(row.get("activity_price"), title, locale);
			if (priceYuan.compareTo(BigDecimal.ZERO) <= 0) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.seckill.item_price_invalid", new Object[] {title}, locale));
			}
			int lim = readInt(row.get("limit_num"));
			if (lim <= 0) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.seckill.item_limit_invalid", new Object[] {title}, locale));
			}
		}
		return itemIds;
	}

	/**
	 * Asserts required keys {@code item_id}, {@code item_title}, {@code activity_price}, and
	 * {@code activity_store} are present; string-like values must be non-blank after trim.
	 */
	private void assertSeckillItemRowRequiredFields(Map<String, Object> row, Locale locale) {
		Object itemIdRaw = row.get("item_id");
		if (isRequiredFieldMissing(itemIdRaw)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.item_id_param_missing", null, locale));
		}
		long itemId;
		try {
			itemId = readLong(itemIdRaw);
		} catch (Exception e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.item_id_param_missing", null, locale));
		}
		if (itemId <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.item_id_param_missing", null, locale));
		}
		String itemTitle = Objects.toString(row.get("item_title"), "").trim();
		if (!StringUtils.hasText(itemTitle)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.item_title_param_missing", null, locale));
		}
		Object priceRaw = row.get("activity_price");
		if (isRequiredFieldMissing(priceRaw)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.item_activity_price_param_missing", null, locale));
		}
		Object storeRaw = row.get("activity_store");
		if (isRequiredFieldMissing(storeRaw)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.item_activity_store_param_missing", null, locale));
		}
	}

	/** True when {@code v} is null or a string that is empty or whitespace-only after trim. */
	private static boolean isRequiredFieldMissing(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		return false;
	}

	private void normalizeItemsJson(Map<String, Object> params, Locale locale) {
		if (!params.containsKey("items")) {
			return;
		}
		Object raw = params.get("items");
		if (raw instanceof List<?> listRaw) {
			List<Map<String, Object>> ok = new ArrayList<>();
			for (Object o : listRaw) {
				if (!(o instanceof Map<?, ?> m)) {
					throw new BadRequestException(
							messageSource.getMessage(
									"promotions.seckill.items_json_invalid", null, "Invalid items list format", locale));
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> row = (Map<String, Object>) m;
				ok.add(row);
			}
			params.put("items", ok);
			return;
		}
		if (raw instanceof String s) {
			try {
				List<Map<String, Object>> parsed =
						objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
				if (parsed == null) {
					throw new BadRequestException(
							messageSource.getMessage(
									"promotions.seckill.items_json_invalid", null, "Invalid items list format", locale));
				}
				params.put("items", parsed);
			} catch (Exception e) {
				if (e instanceof BadRequestException be) {
					throw be;
				}
				throw new BadRequestException(
						messageSource.getMessage(
								"promotions.seckill.items_json_invalid", null, "Invalid items list format", locale));
			}
			return;
		}
		throw new BadRequestException(
				messageSource.getMessage("promotions.seckill.items_json_invalid", null, "Invalid items list format", locale));
	}

	private void normalizeLimitMoneyFields(Map<String, Object> params) {
		normalizeOneLimitMoney(params, "limit_total_money");
		normalizeOneLimitMoney(params, "limit_money");
	}

	private static void normalizeOneLimitMoney(Map<String, Object> params, String key) {
		Object v = params.get(key);
		if (v == null || (v instanceof Boolean b && !b) || (v instanceof String s && !StringUtils.hasText(s.trim()))) {
			params.put(key, 0L);
			return;
		}
	}

	private void assertLimitMoneyDigits(Object v, Locale locale) {
		if (v == null) {
			return;
		}
		if (v instanceof Boolean) {
			return;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		if (s.length() > 13) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.amount_cannot_exceed_13_digits", null, locale));
		}
	}

	private static void normalizeSeckillType(Map<String, Object> params) {
		String t = Objects.toString(params.get("seckill_type"), "").trim();
		if (!StringUtils.hasText(t)) {
			params.put("seckill_type", "normal");
		} else {
			params.put("seckill_type", t);
		}
	}

	private static long moneyYuanToFenIfNonZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Boolean b && !b) {
			return 0L;
		}
		if (v instanceof String s && !StringUtils.hasText(s.trim())) {
			return 0L;
		}
		try {
			BigDecimal bd = new BigDecimal(String.valueOf(v).trim());
			if (bd.compareTo(BigDecimal.ZERO) == 0) {
				return 0L;
			}
			return bd.movePointRight(2).setScale(0, RoundingMode.DOWN).longValue();
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int resolveUseBound(Map<String, Object> params) {
		Object ub = params.get("use_bound");
		if (ub == null) {
			return 1;
		}
		if ("goods".equals(ub) || "goods".equals(Objects.toString(ub, "").trim())) {
			return 1;
		}
		try {
			if (ub instanceof Number n) {
				return n.intValue();
			}
			String t = String.valueOf(ub).trim();
			if (StringUtils.hasText(t)) {
				return Integer.parseInt(t);
			}
		} catch (NumberFormatException ignored) {
		}
		return 1;
	}

	private static List<Long> collectOrderedDistinctPositiveItemIds(List<Map<String, Object>> items) {
		LinkedHashSet<Long> seen = new LinkedHashSet<>();
		List<Long> out = new ArrayList<>();
		for (Map<String, Object> row : items) {
			try {
				long id = readLong(row.get("item_id"));
				if (id > 0L && seen.add(id)) {
					out.add(id);
				}
			} catch (Exception ignored) {
			}
		}
		return out;
	}

	private BigDecimal readActivityPriceYuan(Object raw, String itemTitle, Locale locale) {
		if (raw == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.item_price_invalid", new Object[] {itemTitle}, locale));
		}
		try {
			return new BigDecimal(String.valueOf(raw).trim());
		} catch (Exception e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.item_price_invalid", new Object[] {itemTitle}, locale));
		}
	}

	private static int readInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return (int) Double.parseDouble(String.valueOf(v).trim());
	}

	private static int readIntWithDefault(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof String s && !StringUtils.hasText(s.trim())) {
			return def;
		}
		try {
			return readInt(v);
		} catch (Exception e) {
			return def;
		}
	}

	private static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static long readLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		try {
			return readLong(v);
		} catch (Exception e) {
			return 0L;
		}
	}

	private static boolean readBooleanWithDefaultFalse(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
			return true;
		}
		if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		return false;
	}

	private static String wrapDistributorIdCsv(Object raw) {
		if (raw == null) {
			return null;
		}
		String inner;
		if (raw instanceof List<?> list) {
			inner =
					list.stream()
							.filter(Objects::nonNull)
							.map(Object::toString)
							.map(String::trim)
							.filter(StringUtils::hasText)
							.collect(Collectors.joining(","));
		} else {
			inner = raw.toString().trim();
		}
		if (!StringUtils.hasText(inner)) {
			return null;
		}
		while (inner.startsWith(",")) {
			inner = inner.substring(1);
		}
		while (inner.endsWith(",")) {
			inner = inner.substring(0, inner.length() - 1);
		}
		inner = inner.trim();
		if (!StringUtils.hasText(inner)) {
			return null;
		}
		return "," + inner + ",";
	}

	private static String nullIfBlank(String s) {
		return StringUtils.hasText(s) ? s : null;
	}

	private static String nullIfBlankElseDefault(String s, String def) {
		if (!StringUtils.hasText(s)) {
			return def;
		}
		return s;
	}

	public long requireFlexibleEpochSeconds(Object v, Locale locale) {
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.date_required", null, locale));
		}
		if (v instanceof Boolean || v instanceof Map<?, ?> || v instanceof Iterable<?>) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.date_invalid", null, locale));
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
				throw new BadRequestException(messageSource.getMessage("promotions.groups.date_required", null, locale));
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
					throw new BadRequestException(messageSource.getMessage("promotions.groups.date_invalid", null, locale));
				}
			}
			try {
				LocalDate localDate = LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
				return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();
			} catch (DateTimeParseException ignored) {
				try {
					LocalDateTime ldt = LocalDateTime.parse(t, LOCAL_DATETIME_SECONDS);
					return ldt.atZone(DATETIME_PARSE_ZONE).toEpochSecond();
				} catch (DateTimeParseException e) {
					throw new BadRequestException(messageSource.getMessage("promotions.groups.date_invalid", null, locale));
				}
			}
		}
		throw new BadRequestException(messageSource.getMessage("promotions.groups.date_invalid", null, locale));
	}
}
