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
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.BargainPromotionMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BargainPromotionCreateService {

	private final MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final BargainPromotionsMapper bargainPromotionsMapper;
	private final UserBargainsMapper userBargainsMapper;
	private final BargainPromotionMultiLangWriteService bargainPromotionMultiLangWriteService;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public BargainPromotionCreateService(
			MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			BargainPromotionsMapper bargainPromotionsMapper,
			UserBargainsMapper userBargainsMapper,
			BargainPromotionMultiLangWriteService bargainPromotionMultiLangWriteService,
			MessageSource messageSource,
			ObjectMapper objectMapper) {
		this.marketingActivityCrossPromotionGuardService = marketingActivityCrossPromotionGuardService;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.bargainPromotionsMapper = bargainPromotionsMapper;
		this.userBargainsMapper = userBargainsMapper;
		this.bargainPromotionMultiLangWriteService = bargainPromotionMultiLangWriteService;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createBargain(Map<String, Object> params, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		validateParams(params, locale);
		long companyId = readLong(params.get("company_id"));
		Object itemIdRaw = params.get("item_id");
		String itemIdStr = String.valueOf(itemIdRaw).trim();
		long itemIdLong = Long.parseLong(itemIdStr);

		if (params.get("begin_time") == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_start_time_required", null, locale));
		}
		if (params.get("end_time") == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_end_time_required", null, locale));
		}
		long beginEpoch = requireFlexibleEpochSeconds(params.get("begin_time"), locale);
		long endEpoch = requireFlexibleEpochSeconds(params.get("end_time"), locale);
		if (beginEpoch >= endEpoch) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.end_time_must_greater_than_start", null, locale));
		}

		BigDecimal priceYuan = new BigDecimal(String.valueOf(params.get("price")).trim());
		BigDecimal mktPriceYuan = new BigDecimal(String.valueOf(params.get("mkt_price")).trim());
		int priceFen = priceYuan.movePointRight(2).setScale(0, RoundingMode.DOWN).intValueExact();
		int mktPriceFen = mktPriceYuan.movePointRight(2).setScale(0, RoundingMode.DOWN).intValueExact();
		int minPriceFen = 100;
		int maxCutPrice = mktPriceFen - priceFen;
		if (maxCutPrice < 100) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.price_difference_must_greater_than_1", null, locale));
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> peopleRangeMap = (Map<String, Object>) params.get("people_range");
		int minPeople = readPeopleCount(peopleRangeMap.get("min"), locale, true);
		int maxPeople = readPeopleCount(peopleRangeMap.get("max"), locale, false);
		if (minPeople < 1) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.min_help_people_required_min_1", null, locale));
		}
		if (maxPeople < 2) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.max_help_people_required_min_2", null, locale));
		}
		if (maxPeople <= minPeople) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.max_help_people_greater_than_min", null, locale));
		}

		Map<String, Object> guardParams = new LinkedHashMap<>();
		guardParams.put("company_id", companyId);
		guardParams.put("start_time", beginEpoch);
		guardParams.put("end_time", endEpoch);
		guardParams.put("use_bound", 1);
		guardParams.put("shop_ids", List.of());
		guardParams.put("source_id", 0L);
		guardParams.put("bargain_id", 0L);
		guardParams.put("item_ids", List.of(itemIdLong));
		guardParams.put("seckill_type", List.of("normal", "limited_time_sale"));
		marketingActivityCrossPromotionGuardService.checkActivityValidByBargain(guardParams);

		if (marketingActivityCatalogAccess.anyGiftItem(companyId, List.of(itemIdLong))) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.gift_product_check_error", null, locale));
		}

		List<Object> helpPicsRaw = new ArrayList<>((Collection<?>) params.get("help_pics"));
		List<String> helpPicsList = new ArrayList<>();
		for (Object o : helpPicsRaw) {
			helpPicsList.add(o == null ? "" : String.valueOf(o));
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		int now = (int) nowSec;
		BargainPromotions entity = new BargainPromotions();
		entity.setCompanyId(companyId);
		entity.setTitle(requiredTrim(params, "title", "promotions.bargain.activity_title_required", locale));
		entity.setAdPic(requiredTrim(params, "ad_pic", "promotions.bargain.activity_pic_required", locale));
		entity.setItemName(requiredTrim(params, "item_name", "promotions.bargain.item_name_required", locale));
		entity.setItemPics(requiredTrim(params, "item_pics", "promotions.bargain.item_pics_required", locale));
		Object intro = params.get("item_intro");
		entity.setItemIntro(intro == null ? null : String.valueOf(intro).trim());
		if (entity.getItemIntro() != null && entity.getItemIntro().isEmpty()) {
			entity.setItemIntro(null);
		}
		entity.setMktPrice(mktPriceFen);
		entity.setPrice(priceFen);
		entity.setLimitNum(readStrictPositiveInt(params.get("limit_num"), "promotions.bargain.limit_purchase_quantity_required", locale));
		entity.setOrderNum(0);
		entity.setCutdownRules(requiredTrim(params, "bargain_rules", "promotions.bargain.bargain_rules_required", locale));
		entity.setCutdownRange(null);
		try {
			entity.setPeopleRange(
					objectMapper.writeValueAsString(Map.of("min", minPeople, "max", maxPeople)));
			entity.setHelpPics(objectMapper.writeValueAsString(helpPicsList));
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据序列化失败");
		}
		entity.setMinPrice(minPriceFen);
		entity.setBeginTime(beginEpoch);
		entity.setEndTime(endEpoch);
		entity.setShareMsg(requiredTrim(params, "share_msg", "promotions.bargain.share_content_required", locale));
		entity.setItemId(itemIdStr);
		entity.setCreated(now);
		entity.setUpdated(now);

		bargainPromotionsMapper.insert(entity);
		long id = entity.getBargainId();
		String resolvedLangTag =
				(requestLangTag != null && StringUtils.hasText(requestLangTag.trim()))
						? requestLangTag.trim()
						: "zh-CN";
		bargainPromotionMultiLangWriteService.addForNewBargain(id, companyId, params, resolvedLangTag);
		return entityToResponseMap(entity, nowSec);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateBargain(Map<String, Object> params, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		long bargainId = parseBargainIdForUpdate(params.get("bargain_id"), locale);
		validateUpdateParams(params, locale);
		long companyId = readLong(params.get("company_id"));
		Object itemIdRaw = params.get("item_id");
		String itemIdStr = String.valueOf(itemIdRaw).trim();
		long itemIdLong = Long.parseLong(itemIdStr);

		if (params.get("begin_time") == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_start_time_required", null, locale));
		}
		if (params.get("end_time") == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_end_time_required", null, locale));
		}
		long beginEpoch = requireFlexibleEpochSeconds(params.get("begin_time"), locale);
		long endEpoch = requireFlexibleEpochSeconds(params.get("end_time"), locale);
		if (beginEpoch >= endEpoch) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.end_time_must_greater_than_start", null, locale));
		}

		BigDecimal priceYuan = new BigDecimal(String.valueOf(params.get("price")).trim());
		BigDecimal mktPriceYuan = new BigDecimal(String.valueOf(params.get("mkt_price")).trim());
		int priceFen = priceYuan.movePointRight(2).setScale(0, RoundingMode.DOWN).intValueExact();
		int mktPriceFen = mktPriceYuan.movePointRight(2).setScale(0, RoundingMode.DOWN).intValueExact();
		int minPriceFen = 100;
		int maxCutPrice = mktPriceFen - priceFen;
		if (maxCutPrice < 100) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.price_difference_must_greater_than_1", null, locale));
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> peopleRangeMap = (Map<String, Object>) params.get("people_range");
		int minPeople = readPeopleCountForUpdateMin(peopleRangeMap.get("min"), locale);
		int maxPeople = readPeopleCountForUpdateMax(peopleRangeMap.get("max"), locale);
		if (minPeople < 1) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.min_help_people_required", null, locale));
		}
		if (maxPeople < 1) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.max_help_people_required_min_0", null, locale));
		}
		if (maxPeople <= minPeople) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.max_help_people_greater_than_min", null, locale));
		}

		Map<String, Object> guardParams = new LinkedHashMap<>();
		guardParams.put("company_id", companyId);
		guardParams.put("start_time", beginEpoch);
		guardParams.put("end_time", endEpoch);
		guardParams.put("use_bound", 1);
		guardParams.put("shop_ids", List.of());
		guardParams.put("source_id", 0L);
		guardParams.put("bargain_id", bargainId);
		guardParams.put("item_ids", List.of(itemIdLong));
		guardParams.put("seckill_type", List.of("normal", "limited_time_sale"));
		marketingActivityCrossPromotionGuardService.checkActivityValidByBargain(guardParams);

		if (marketingActivityCatalogAccess.anyGiftItem(companyId, List.of(itemIdLong))) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.gift_product_check_error", null, locale));
		}

		long userCount =
				userBargainsMapper.selectCount(
						new LambdaQueryWrapper<UserBargains>().eq(UserBargains::getBargainId, bargainId));
		if (userCount > 0L) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.bargain_activity_has_user_cannot_edit", null, locale));
		}

		BargainPromotions existing = bargainPromotionsMapper.selectById(bargainId);
		if (existing == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_not_exist_with_id", new Object[] {bargainId}, locale));
		}

		long nowSeconds = System.currentTimeMillis() / 1000L;
		if (existing.getEndTime() != null && existing.getEndTime() < nowSeconds) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.bargain_activity_ended_cannot_edit", null, locale));
		}

		List<Object> helpPicsRaw = new ArrayList<>((Collection<?>) params.get("help_pics"));
		List<String> helpPicsList = new ArrayList<>();
		for (Object o : helpPicsRaw) {
			helpPicsList.add(o == null ? "" : String.valueOf(o));
		}

		int nowInt = (int) nowSeconds;
		BargainPromotions entity = new BargainPromotions();
		entity.setBargainId(bargainId);
		entity.setCompanyId(companyId);
		entity.setTitle(requiredTrim(params, "title", "promotions.bargain.activity_title_required", locale));
		entity.setAdPic(requiredTrim(params, "ad_pic", "promotions.bargain.activity_pic_required", locale));
		entity.setItemName(requiredTrim(params, "item_name", "promotions.bargain.item_name_required", locale));
		entity.setItemPics(requiredTrim(params, "item_pics", "promotions.bargain.item_pics_required", locale));
		Object intro = params.get("item_intro");
		entity.setItemIntro(intro == null ? null : String.valueOf(intro).trim());
		if (entity.getItemIntro() != null && entity.getItemIntro().isEmpty()) {
			entity.setItemIntro(null);
		}
		entity.setMktPrice(mktPriceFen);
		entity.setPrice(priceFen);
		entity.setLimitNum(readStrictPositiveInt(params.get("limit_num"), "promotions.bargain.limit_purchase_quantity_required", locale));
		entity.setOrderNum(existing.getOrderNum());
		entity.setCutdownRules(requiredTrim(params, "bargain_rules", "promotions.bargain.bargain_rules_required", locale));
		entity.setCutdownRange(existing.getCutdownRange());
		try {
			entity.setPeopleRange(objectMapper.writeValueAsString(Map.of("min", minPeople, "max", maxPeople)));
			entity.setHelpPics(objectMapper.writeValueAsString(helpPicsList));
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据序列化失败");
		}
		entity.setMinPrice(minPriceFen);
		entity.setBeginTime(beginEpoch);
		entity.setEndTime(endEpoch);
		entity.setShareMsg(requiredTrim(params, "share_msg", "promotions.bargain.share_content_required", locale));
		entity.setItemId(itemIdStr);
		entity.setCreated(existing.getCreated());
		entity.setUpdated(nowInt);

		int affected = bargainPromotionsMapper.updateById(entity);
		if (affected == 0) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_not_exist_with_id", new Object[] {bargainId}, locale));
		}

		String resolvedLangTag =
				(requestLangTag != null && StringUtils.hasText(requestLangTag.trim()))
						? requestLangTag.trim()
						: "zh-CN";
		bargainPromotionMultiLangWriteService.addForNewBargain(bargainId, companyId, params, resolvedLangTag);

		BargainPromotions refreshed = bargainPromotionsMapper.selectById(bargainId);
		if (refreshed == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_not_exist_with_id", new Object[] {bargainId}, locale));
		}
		return entityToResponseMap(refreshed, nowSeconds);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> terminateBargain(String bargainIdRaw, Locale locale) {
		String terminateMsg =
				messageSource.getMessage("promotions.bargain.terminate_bargain_error", null, locale);
		if (bargainIdRaw == null || !StringUtils.hasText(bargainIdRaw.trim())) {
			throw new ResourceException(terminateMsg, Map.of("bargain_id", List.of("validation.required")));
		}
		String trim = bargainIdRaw.trim();
		long id;
		try {
			id = Long.parseLong(trim);
		} catch (NumberFormatException e) {
			throw new ResourceException(terminateMsg, Map.of("bargain_id", List.of("validation.integer")));
		}
		if (id < 1L) {
			throw new ResourceException(terminateMsg, Map.of("bargain_id", List.of("validation.min.numeric")));
		}

		long nowEpochSeconds = System.currentTimeMillis() / 1000L;
		BargainPromotions row = bargainPromotionsMapper.selectById(id);
		if (row == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.bargain_activity_not_exist", null, locale));
		}

		if (row.getEndTime() != null && row.getEndTime() > nowEpochSeconds) {
			int nowInt = (int) nowEpochSeconds;
			LambdaUpdateWrapper<BargainPromotions> wrapper = new LambdaUpdateWrapper<>();
			wrapper
					.eq(BargainPromotions::getBargainId, id)
					.set(BargainPromotions::getEndTime, nowEpochSeconds)
					.set(BargainPromotions::getUpdated, nowInt);
			int affected = bargainPromotionsMapper.update(null, wrapper);
			if (affected == 0) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.bargain.bargain_activity_not_exist_with_id", new Object[] {id}, locale));
			}
			row = bargainPromotionsMapper.selectById(id);
			if (row == null) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.bargain.bargain_activity_not_exist_with_id", new Object[] {id}, locale));
			}
		}

		return entityToResponseMap(row, nowEpochSeconds);
	}

	private long parseBargainIdForUpdate(Object raw, Locale locale) {
		if (raw == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_id_required", null, locale));
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_id_required", null, locale));
		}
		long bargainId;
		try {
			bargainId = Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_id_invalid", null, locale));
		}
		if (bargainId < 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.bargain_id_invalid", null, locale));
		}
		return bargainId;
	}

	private void validateUpdateParams(Map<String, Object> params, Locale locale) {
		requiredTrim(params, "title", "promotions.bargain.activity_title_required", locale);
		requiredTrim(params, "ad_pic", "promotions.bargain.activity_pic_required", locale);
		requiredTrim(params, "item_name", "promotions.bargain.item_name_required", locale);
		requiredTrim(params, "item_pics", "promotions.bargain.item_pics_required", locale);
		assertStrictPositiveDecimal(params.get("price"), "promotions.bargain.item_price_required_min_0", locale);
		assertStrictPositiveDecimal(params.get("mkt_price"), "promotions.bargain.item_discount_required_min_0", locale);
		assertStrictPositiveIntParam(params.get("limit_num"), "promotions.bargain.limit_purchase_quantity_required", locale);
		requiredTrim(params, "bargain_rules", "promotions.bargain.bargain_rules_required", locale);
		validateUpdatePeopleRangeStructure(params, locale);
		requiredTrim(params, "share_msg", "promotions.bargain.share_content_required", locale);
		validateHelpPics(params, locale);
		validateItemId(params, locale);
	}

	private void validateUpdatePeopleRangeStructure(Map<String, Object> params, Locale locale) {
		Object raw = params.get("people_range");
		if (!(raw instanceof Map<?, ?> map)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.min_help_people_required", null, locale));
		}
		if (!map.containsKey("min") || map.get("min") == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.min_help_people_required", null, locale));
		}
		if (!map.containsKey("max") || map.get("max") == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.max_help_people_required_min_0", null, locale));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cast = (Map<String, Object>) raw;
		params.put("people_range", cast);
	}

	private int readPeopleCountForUpdateMin(Object v, Locale locale) {
		if (v == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.min_help_people_required", null, locale));
		}
		try {
			if (v instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.min_help_people_required", null, locale));
		}
	}

	private int readPeopleCountForUpdateMax(Object v, Locale locale) {
		if (v == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.max_help_people_required_min_0", null, locale));
		}
		try {
			if (v instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.max_help_people_required_min_0", null, locale));
		}
	}

	private void validateParams(Map<String, Object> params, Locale locale) {
		requiredTrim(params, "title", "promotions.bargain.activity_title_required", locale);
		requiredTrim(params, "ad_pic", "promotions.bargain.activity_pic_required", locale);
		requiredTrim(params, "item_name", "promotions.bargain.item_name_required", locale);
		requiredTrim(params, "item_pics", "promotions.bargain.item_pics_required", locale);
		assertStrictPositiveDecimal(params.get("price"), "promotions.bargain.item_price_required_min_0", locale);
		assertStrictPositiveDecimal(params.get("mkt_price"), "promotions.bargain.item_discount_required_min_0", locale);
		assertStrictPositiveIntParam(params.get("limit_num"), "promotions.bargain.limit_purchase_quantity_required", locale);
		requiredTrim(params, "bargain_rules", "promotions.bargain.bargain_rules_required", locale);
		validatePeopleRangeStructure(params, locale);
		requiredTrim(params, "share_msg", "promotions.bargain.share_content_required", locale);
		validateHelpPics(params, locale);
		validateItemId(params, locale);
	}

	private void validateItemId(Map<String, Object> params, Locale locale) {
		Object v = params.get("item_id");
		if (v == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.item_id_required", null, locale));
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.item_id_required", null, locale));
		}
		try {
			Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.item_id_invalid", null, locale));
		}
	}

	private void validateHelpPics(Map<String, Object> params, Locale locale) {
		Object v = params.get("help_pics");
		if (!(v instanceof Collection<?>) || ((Collection<?>) v).isEmpty()) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.flip_card_pics_required", null, locale));
		}
	}

	private void validatePeopleRangeStructure(Map<String, Object> params, Locale locale) {
		Object raw = params.get("people_range");
		if (!(raw instanceof Map<?, ?> map)) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.min_help_people_required_min_1", null, locale));
		}
		if (!map.containsKey("min") || map.get("min") == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.min_help_people_required_min_1", null, locale));
		}
		if (!map.containsKey("max") || map.get("max") == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.bargain.max_help_people_required_min_2", null, locale));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cast = (Map<String, Object>) raw;
		params.put("people_range", cast);
	}

	private int readPeopleCount(Object v, Locale locale, boolean minField) {
		String key =
				minField
						? "promotions.bargain.min_help_people_required_min_1"
						: "promotions.bargain.max_help_people_required_min_2";
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage(key, null, locale));
		}
		try {
			if (v instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(key, null, locale));
		}
	}

	private String requiredTrim(Map<String, Object> params, String field, String messageKey, Locale locale) {
		Object v = params.get(field);
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
		return s;
	}

	private void assertStrictPositiveDecimal(Object v, String messageKey, Locale locale) {
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
		try {
			BigDecimal d = new BigDecimal(String.valueOf(v).trim());
			if (d.compareTo(BigDecimal.ZERO) <= 0) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
			}
		} catch (NumberFormatException | ArithmeticException e) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
	}

	private void assertStrictPositiveIntParam(Object v, String messageKey, Locale locale) {
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
		try {
			int n;
			if (v instanceof Number num) {
				n = num.intValue();
			} else {
				n = Integer.parseInt(String.valueOf(v).trim());
			}
			if (n <= 0) {
				throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
	}

	private int readStrictPositiveInt(Object v, String messageKey, Locale locale) {
		if (v == null) {
			throw new BadRequestException(messageSource.getMessage(messageKey, null, locale));
		}
		try {
			int n;
			if (v instanceof Number num) {
				n = num.intValue();
			} else {
				n = Integer.parseInt(String.valueOf(v).trim());
			}
			if (n <= 0) {
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
			} catch (DateTimeParseException e) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
			}
		}
		throw new BadRequestException(
				messageSource.getMessage("promotions.automation.invalid_time_format", null, locale));
	}

	private Map<String, Object> entityToResponseMap(BargainPromotions e, long nowEpochSeconds) {
		return toBargainListRow(e, nowEpochSeconds);
	}

	public Map<String, Object> toBargainListRow(BargainPromotions entity, long nowEpochSeconds) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("bargain_id", entity.getBargainId());
		m.put("company_id", entity.getCompanyId());
		m.put("title", entity.getTitle());
		m.put("ad_pic", entity.getAdPic());
		m.put("item_name", entity.getItemName());
		m.put("item_pics", entity.getItemPics());
		m.put("item_intro", entity.getItemIntro());
		m.put("mkt_price", entity.getMktPrice());
		m.put("price", entity.getPrice());
		m.put("limit_num", entity.getLimitNum());
		m.put("order_num", entity.getOrderNum());
		m.put("bargain_rules", entity.getCutdownRules());
		m.put("bargain_range", cutdownRangeForResponse(entity.getCutdownRange()));
		m.put("people_range", readJsonMapPeopleRangeNullable(entity.getPeopleRange()));
		m.put("min_price", entity.getMinPrice());
		m.put("begin_time", entity.getBeginTime());
		m.put("end_time", entity.getEndTime());
		m.put("share_msg", entity.getShareMsg());
		m.put("help_pics", readHelpPicsListForRow(entity.getHelpPics()));
		m.put("created", entity.getCreated());
		m.put("updated", entity.getUpdated());
		boolean expired = entity.getEndTime() == null ? false : (entity.getEndTime() < nowEpochSeconds);
		m.put("is_expired", expired);
		m.put("item_id", entity.getItemId());
		return m;
	}

	private Map<String, Object> readJsonMapPeopleRangeNullable(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return null;
		}
		try {
			return objectMapper.readValue(raw.trim(), new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据解析失败");
		}
	}

	private List<String> readHelpPicsListForRow(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return Collections.emptyList();
		}
		try {
			return objectMapper.readValue(raw.trim(), new TypeReference<List<String>>() {});
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据解析失败");
		}
	}

	private Object cutdownRangeForResponse(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new ArrayList<>();
		}
		String t = raw.trim();
		try {
			JsonNode node = objectMapper.readTree(t);
			if (node == null || node.isNull()) {
				return new ArrayList<>();
			}
			if (node.isArray()) {
				return objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
			}
			if (node.isObject()) {
				if (node.size() == 0) {
					return new ArrayList<>();
				}
				return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
			}
		} catch (JsonProcessingException ignored) {
		}
		return new ArrayList<>();
	}

}