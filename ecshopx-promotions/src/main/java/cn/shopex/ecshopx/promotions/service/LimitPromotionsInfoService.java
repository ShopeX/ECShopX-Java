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
import cn.shopex.ecshopx.promotions.domain.LimitCategoryPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitCategoryPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.port.LimitPromotionAdminGoodsSupportPort;
import cn.shopex.ecshopx.promotions.service.multilang.LimitPromotionListMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LimitPromotionsInfoService {

	private final LimitPromotionsMapper limitPromotionsMapper;
	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final LimitCategoryPromotionsMapper limitCategoryPromotionsMapper;
	private final LimitPromotionListMultiLangReadService limitPromotionListMultiLangReadService;
	private final MarketingActivityInfoAssemblySupport marketingActivityInfoAssemblySupport;
	private final LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public LimitPromotionsInfoService(
			LimitPromotionsMapper limitPromotionsMapper,
			LimitItemPromotionsMapper limitItemPromotionsMapper,
			LimitCategoryPromotionsMapper limitCategoryPromotionsMapper,
			LimitPromotionListMultiLangReadService limitPromotionListMultiLangReadService,
			MarketingActivityInfoAssemblySupport marketingActivityInfoAssemblySupport,
			LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.limitCategoryPromotionsMapper = limitCategoryPromotionsMapper;
		this.limitPromotionListMultiLangReadService = limitPromotionListMultiLangReadService;
		this.marketingActivityInfoAssemblySupport = marketingActivityInfoAssemblySupport;
		this.limitPromotionAdminGoodsSupportPort = limitPromotionAdminGoodsSupportPort;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> info(long companyId, long limitId, String requestLangTag) {
		Locale loc =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		LambdaQueryWrapper<LimitPromotions> lw = new LambdaQueryWrapper<>();
		lw.eq(LimitPromotions::getLimitId, limitId).eq(LimitPromotions::getCompanyId, companyId);
		LimitPromotions entity = limitPromotionsMapper.selectOne(lw);
		if (entity == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.limit.no_update_data_found", null, loc));
		}

		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("limit_id", wireBigint(entity.getLimitId()));
		detail.put("company_id", wireBigint(entity.getCompanyId()));
		detail.put("limit_name", entity.getLimitName());
		detail.put("limit_type", entity.getLimitType());
		detail.put("total_item_num", entity.getTotalItemNum());
		detail.put("valid_item_num", entity.getValidItemNum());
		detail.put("error_desc", wireNullableText(entity.getErrorDesc()));
		detail.put("valid_grade", parseValidGradeList(entity.getValidGrade()));
		detail.put("rule", entity.getRule());
		int startEpoch = entity.getStartTime() != null ? entity.getStartTime() : 0;
		int endEpoch = entity.getEndTime() != null ? entity.getEndTime() : 0;
		detail.put("start_time", startEpoch);
		detail.put("end_time", endEpoch);
		detail.put("use_bound", entity.getUseBound());
		detail.put("tag_ids", parseJsonArrayOrEmpty(entity.getTagIds()));
		detail.put("brand_ids", parseJsonArrayOrEmpty(entity.getBrandIds()));
		detail.put("created", entity.getCreated());
		detail.put("updated", entity.getUpdated());
		detail.put("source_type", entity.getSourceType());
		detail.put("source_id", wireBigint(entity.getSourceId()));

		marketingActivityInfoAssemblySupport.applyStartEndTimeStringOverrides(detail, startEpoch, endEpoch);

		LambdaQueryWrapper<LimitItemPromotions> iw = new LambdaQueryWrapper<>();
		iw.eq(LimitItemPromotions::getLimitId, limitId)
				.eq(LimitItemPromotions::getCompanyId, companyId)
				.eq(LimitItemPromotions::getItemType, "normal")
				.eq(LimitItemPromotions::getDistributorId, 0L)
				.orderByDesc(LimitItemPromotions::getCreated)
				.last("LIMIT 1000");
		List<LimitItemPromotions> relLists = limitItemPromotionsMapper.selectList(iw);

		List<Long> itemIds =
				relLists.stream().map(LimitItemPromotions::getItemId).filter(Objects::nonNull).toList();

		Map<String, Object> skuPack =
				limitPromotionAdminGoodsSupportPort.loadSkuItemsListForPromotionDetail(companyId, itemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) skuPack.get("list");
		if (items == null) {
			items = List.of();
		}
		detail.put("items", items);

		Map<Long, Map<String, Object>> skuById = new LinkedHashMap<>();
		for (Map<String, Object> row : items) {
			Long iid = toLongObject(row.get("item_id"));
			if (iid != null) {
				skuById.put(iid, row);
			}
		}

		List<Map<String, Object>> relItems = new ArrayList<>();
		for (LimitItemPromotions rel : relLists) {
			Long itemId = rel.getItemId();
			if (itemId != null && skuById.containsKey(itemId)) {
				relItems.add(new LinkedHashMap<>(skuById.get(itemId)));
			}
		}

		if (relItems.isEmpty()) {
			detail.put("itemTreeLists", List.of());
		} else {
			detail.put("itemTreeLists", limitPromotionAdminGoodsSupportPort.formatItemsList(relItems));
		}

		LambdaQueryWrapper<LimitCategoryPromotions> cw = new LambdaQueryWrapper<>();
		cw.eq(LimitCategoryPromotions::getCompanyId, companyId).eq(LimitCategoryPromotions::getLimitId, limitId);
		List<LimitCategoryPromotions> categoryRows = limitCategoryPromotionsMapper.selectList(cw);
		marketingActivityInfoAssemblySupport.putLimitCategorySplitsFromRows(categoryRows, detail);
		if (!detail.containsKey("rel_category_ids")) {
			detail.put("rel_category_ids", List.of());
		}
		if (!detail.containsKey("item_category")) {
			detail.put("item_category", List.of());
		}

		detail.put("rel_tag_ids", detail.get("tag_ids"));
		detail.put(
				"tag_list",
				limitPromotionAdminGoodsSupportPort.loadTagList(companyId, parseLongIds(detail.get("tag_ids"))));

		detail.put("rel_brand_ids", detail.get("brand_ids"));
		detail.put(
				"brand_list",
				limitPromotionAdminGoodsSupportPort.loadBrandList(companyId, parseLongIds(detail.get("brand_ids"))));

		List<Map<String, Object>> single = List.of(detail);
		limitPromotionListMultiLangReadService.applyLimitNames(companyId, single, requestLangTag);

		return detail;
	}

	private List<String> parseValidGradeList(String validGrade) {
		String vg = validGrade;
		if (vg == null || !StringUtils.hasText(vg.trim())) {
			return List.of();
		}
		return Arrays.stream(vg.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
	}

	private List<Object> parseJsonArrayOrEmpty(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return List.of();
		}
		try {
			return objectMapper.readValue(raw.trim(), new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	private static List<Long> parseLongIds(Object raw) {
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object el : list) {
			if (el == null) {
				continue;
			}
			if (el instanceof Number n) {
				out.add(n.longValue());
				continue;
			}
			try {
				String s = el.toString().trim();
				if (StringUtils.hasText(s)) {
					out.add(Long.parseLong(s));
				}
			} catch (NumberFormatException ignored) {
				// skip invalid token
			}
		}
		return out;
	}

	private static String wireBigint(Long v) {
		return v == null ? null : Long.toString(v.longValue());
	}

	private static Object wireNullableText(String s) {
		if (s == null || !StringUtils.hasText(s.trim())) {
			return null;
		}
		return s;
	}

	private static Long toLongObject(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return null;
		}
		try {
			long v = Long.parseLong(o.toString().trim());
			return v;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
