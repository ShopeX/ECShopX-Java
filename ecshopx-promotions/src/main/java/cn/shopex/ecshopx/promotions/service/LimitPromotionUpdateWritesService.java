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
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.LimitPromotionMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("limitPromotionUpdateWritesService")
public class LimitPromotionUpdateWritesService {

	private final LimitPromotionPersistenceDelegate delegate;
	private final LimitPromotionsMapper limitPromotionsMapper;
	private final LimitPromotionMultiLangWriteService limitPromotionMultiLangWriteService;
	private final MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public LimitPromotionUpdateWritesService(
			LimitPromotionPersistenceDelegate delegate,
			LimitPromotionsMapper limitPromotionsMapper,
			LimitPromotionMultiLangWriteService limitPromotionMultiLangWriteService,
			MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService,
			MessageSource messageSource,
			ObjectMapper objectMapper) {
		this.delegate = delegate;
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.limitPromotionMultiLangWriteService = limitPromotionMultiLangWriteService;
		this.marketingActivityCrossPromotionGuardService = marketingActivityCrossPromotionGuardService;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> applyUpdateWrites(
			long limitId,
			long companyId,
			Map<String, Object> params,
			String requestLangTag,
			Locale locale)
			throws JsonProcessingException {
		List<Long> itemIds = readLongList(params.get("items"));
		String ubStr = Objects.toString(params.get("use_bound"), "").trim();
		delegate.checkActivityForMutation(companyId, ubStr, itemIds, params, locale);

		LimitPromotions dbRow = limitPromotionsMapper.selectById(limitId);
		if (dbRow == null || !Objects.equals(dbRow.getCompanyId(), companyId)) {
			throw new ResourceException(messageSource.getMessage("promotions.limit.no_update_data_found", null, locale));
		}

		String limitType = Objects.toString(params.get("limit_type"), "").trim();
		String limitName = Objects.toString(params.get("limit_name"), "").trim();
		int dayInt = readNonNegativeInt(params.get("day"), "promotions.limit.day_invalid", locale);
		int limitInt = readMinOneInt(params.get("limit"), "promotions.limit.limit_invalid", locale);
		int start = (int) readLong(params.get("start_time"));
		int end = (int) readLong(params.get("end_time"));
		int nowTs = (int) (System.currentTimeMillis() / 1000L);

		LimitPromotions toUpdate = new LimitPromotions();
		toUpdate.setLimitName(limitName);
		toUpdate.setLimitType(limitType);
		List<String> validGradeList = readStringListParam(params.get("valid_grade"));
		toUpdate.setValidGrade(String.join(",", validGradeList));
		toUpdate.setRule(objectMapper.writeValueAsString(Map.of("day", dayInt, "limit", limitInt)));
		toUpdate.setStartTime(start);
		toUpdate.setEndTime(end);
		toUpdate.setUpdated(nowTs);
		if ("global".equalsIgnoreCase(limitType)) {
			toUpdate.setErrorDesc("");
			toUpdate.setTotalItemNum(0);
			toUpdate.setValidItemNum(0);
		}

		boolean patchUseBoundColumns = applyUseBoundParamsForUpdate(params, toUpdate, ubStr, locale);

		Integer resolvedUseBound =
				patchUseBoundColumns ? toUpdate.getUseBound() : dbRow.getUseBound();

		LinkedHashMap<String, Object> guardParams = new LinkedHashMap<>();
		guardParams.put("company_id", companyId);
		guardParams.put("start_time", start);
		guardParams.put("end_time", end);
		guardParams.put("use_bound", resolvedUseBound);
		guardParams.put("item_category", params.get("item_category"));
		guardParams.put("tag_ids", readLongList(params.get("tag_ids")));
		guardParams.put("brand_ids", readLongList(params.get("brand_ids")));
		guardParams.put("shop_ids", List.of());
		guardParams.put("item_ids", itemIds);
		guardParams.put("limit_id", limitId);
		guardParams.put("source_id", dbRow.getSourceId() == null ? 0L : dbRow.getSourceId());
		marketingActivityCrossPromotionGuardService.checkActivityValidByLimit(guardParams);

		LambdaUpdateWrapper<LimitPromotions> mainPatch = new LambdaUpdateWrapper<LimitPromotions>()
				.eq(LimitPromotions::getLimitId, limitId)
				.eq(LimitPromotions::getCompanyId, companyId)
				.set(LimitPromotions::getLimitName, limitName)
				.set(LimitPromotions::getLimitType, limitType)
				.set(LimitPromotions::getValidGrade, String.join(",", validGradeList))
				.set(LimitPromotions::getRule, objectMapper.writeValueAsString(Map.of("day", dayInt, "limit", limitInt)))
				.set(LimitPromotions::getStartTime, start)
				.set(LimitPromotions::getEndTime, end)
				.set(LimitPromotions::getUpdated, nowTs);
		if ("global".equalsIgnoreCase(limitType)) {
			mainPatch
					.set(LimitPromotions::getErrorDesc, "")
					.set(LimitPromotions::getTotalItemNum, 0)
					.set(LimitPromotions::getValidItemNum, 0);
		}
		if (patchUseBoundColumns) {
			mainPatch
					.set(LimitPromotions::getUseBound, toUpdate.getUseBound())
					.set(LimitPromotions::getTagIds, toUpdate.getTagIds())
					.set(LimitPromotions::getBrandIds, toUpdate.getBrandIds());
		}
		int rows = limitPromotionsMapper.update(null, mainPatch);
		if (rows == 0) {
			throw new ResourceException(messageSource.getMessage("promotions.limit.no_update_data_found", null, locale));
		}

		limitPromotionMultiLangWriteService.addForNewLimit(limitId, companyId, params, requestLangTag);

		delegate.deleteCategoriesForLimit(companyId, limitId);
		int effectiveUseBound = patchUseBoundColumns ? toUpdate.getUseBound() : dbRow.getUseBound();
		if (effectiveUseBound == 2 && !readLongList(params.get("item_category")).isEmpty()) {
			delegate.insertCategoryRowsForLimit(limitId, companyId, readLongList(params.get("item_category")));
		}

		List<Map<String, Object>> itemRows = List.of();
		if ("global".equalsIgnoreCase(limitType)) {
			delegate.deleteAllItemsForLimit(companyId, limitId);
			LimitPromotions dbRowAfterUpdate = limitPromotionsMapper.selectById(limitId);
			itemRows = delegate.insertGlobalItemRelations(dbRowAfterUpdate, params, itemIds, limitInt, locale);
		}

		LimitPromotions freshEntity = limitPromotionsMapper.selectById(limitId);
		return delegate.buildResponseMap(freshEntity, itemRows, readStringListParam(params.get("valid_grade")));
	}

	private boolean applyUseBoundParamsForUpdate(
			Map<String, Object> params, LimitPromotions entity, String ubStr, Locale locale)
			throws JsonProcessingException {
		if ("goods_import".equalsIgnoreCase(ubStr)) {
			return false;
		}
		if ("goods".equalsIgnoreCase(ubStr)) {
			entity.setUseBound(1);
			entity.setTagIds("[]");
			entity.setBrandIds("[]");
		} else if ("category".equalsIgnoreCase(ubStr)) {
			List<Long> cats = readLongList(params.get("item_category"));
			if (cats.isEmpty()) {
				throw new ResourceException(
						messageSource.getMessage("promotions.limit.item_category_required", null, locale));
			}
			entity.setUseBound(2);
			entity.setTagIds("[]");
			entity.setBrandIds("[]");
		} else if ("tag".equalsIgnoreCase(ubStr)) {
			List<Long> tagIds = readLongList(params.get("tag_ids"));
			if (tagIds.isEmpty()) {
				throw new ResourceException(messageSource.getMessage("promotions.limit.tag_ids_required", null, locale));
			}
			entity.setUseBound(3);
			entity.setTagIds(objectMapper.writeValueAsString(tagIds));
			entity.setBrandIds("[]");
		} else if ("brand".equalsIgnoreCase(ubStr)) {
			List<Long> brandIds = readLongList(params.get("brand_ids"));
			if (brandIds.isEmpty()) {
				throw new ResourceException(
						messageSource.getMessage("promotions.limit.brand_ids_required", null, locale));
			}
			entity.setUseBound(4);
			entity.setBrandIds(objectMapper.writeValueAsString(brandIds));
			entity.setTagIds("[]");
		} else {
			entity.setUseBound(1);
			entity.setTagIds("[]");
			entity.setBrandIds("[]");
		}
		return true;
	}

	private List<String> readStringListParam(Object v) {
		if (!(v instanceof List<?> l)) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (Object o : l) {
			if (o == null) {
				continue;
			}
			if (o instanceof Number n) {
				out.add(String.valueOf(n.longValue()));
				continue;
			}
			String s = o.toString().trim();
			if (StringUtils.hasText(s)) {
				out.add(s);
			}
		}
		return out;
	}

	private static List<Long> readLongList(Object v) {
		if (!(v instanceof List<?> l)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object o : l) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o != null && StringUtils.hasText(o.toString())) {
				try {
					out.add(Long.parseLong(o.toString().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return out;
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
}
