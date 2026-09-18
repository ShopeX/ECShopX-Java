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
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.LimitCategoryPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitCategoryPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("limitPromotionPersistenceDelegate")
public class LimitPromotionPersistenceDelegate {

	private final MessageSource messageSource;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final LimitCategoryPromotionsMapper limitCategoryPromotionsMapper;
	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final ObjectMapper objectMapper;

	public LimitPromotionPersistenceDelegate(
			MessageSource messageSource,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			LimitCategoryPromotionsMapper limitCategoryPromotionsMapper,
			LimitItemPromotionsMapper limitItemPromotionsMapper,
			ObjectMapper objectMapper) {
		this.messageSource = messageSource;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.limitCategoryPromotionsMapper = limitCategoryPromotionsMapper;
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.objectMapper = objectMapper;
	}

	public void normalizeListLikeKeys(Map<String, Object> params) {
		normalizeListKey(params, "valid_grade");
		normalizeListKey(params, "items");
		normalizeListKey(params, "item_category");
		normalizeListKey(params, "tag_ids");
		normalizeListKey(params, "brand_ids");
	}

	public void checkActivityForMutation(
			long companyId, String ubStr, List<Long> itemIds, Map<String, Object> params, Locale locale) {
		if ("goods".equalsIgnoreCase(ubStr) && itemIds.isEmpty()) {
			throw new ResourceException(messageSource.getMessage("promotions.limit.no_activity_items", null, locale));
		}
		if ("goods".equalsIgnoreCase(ubStr) && marketingActivityCatalogAccess.anyGiftItem(companyId, itemIds)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.limit.gift_items_not_allowed", null, locale));
		}
		List<String> validGradeList = readStringListParam(params.get("valid_grade"));
		if (validGradeList.isEmpty()) {
			throw new ResourceException(messageSource.getMessage("promotions.limit.valid_grade_required", null, locale));
		}
	}

	public void deleteCategoriesForLimit(long companyId, long limitId) {
		limitCategoryPromotionsMapper.delete(
				new LambdaQueryWrapper<LimitCategoryPromotions>()
						.eq(LimitCategoryPromotions::getCompanyId, companyId)
						.eq(LimitCategoryPromotions::getLimitId, limitId));
	}

	public void insertCategoryRowsForLimit(long limitId, long companyId, List<Long> categoryIds) {
		if (categoryIds == null) {
			return;
		}
		for (Long cid : categoryIds) {
			if (cid == null) {
				continue;
			}
			limitCategoryPromotionsMapper.insertCategoryRow(limitId, cid, companyId, 0);
		}
	}

	public int deleteAllItemsForLimit(long companyId, long limitId) {
		return limitItemPromotionsMapper.delete(
				new LambdaQueryWrapper<LimitItemPromotions>()
						.eq(LimitItemPromotions::getCompanyId, companyId)
						.eq(LimitItemPromotions::getLimitId, limitId));
	}

	public List<Map<String, Object>> insertGlobalItemRelations(
			LimitPromotions limit, Map<String, Object> params, List<Long> itemIds, int limitPurchase, Locale locale)
			throws JsonProcessingException {
		List<Map<String, Object>> result = new ArrayList<>();
		String ubStr = Objects.toString(params.get("use_bound"), "").trim();
		if (!"goods".equalsIgnoreCase(ubStr)) {
			List<Long> ids;
			String itemType;
			if ("category".equalsIgnoreCase(ubStr)) {
				ids = readLongList(params.get("item_category"));
				itemType = "category";
			} else if ("tag".equalsIgnoreCase(ubStr)) {
				ids = readLongList(params.get("tag_ids"));
				itemType = "tag";
			} else if ("brand".equalsIgnoreCase(ubStr)) {
				ids = readLongList(params.get("brand_ids"));
				itemType = "brand";
			} else {
				return result;
			}
			long companyId = limit.getCompanyId();
			int ts = (int) (System.currentTimeMillis() / 1000L);
			for (Long vid : ids) {
				if (vid == null) {
					continue;
				}
				LimitItemPromotions row = new LimitItemPromotions();
				row.setLimitId(limit.getLimitId());
				row.setDistributorId(0L);
				row.setItemId(vid);
				row.setItemType(itemType);
				row.setLimitNum((long) limitPurchase);
				row.setItemName("");
				row.setCompanyId(companyId);
				row.setStartTime(limit.getStartTime());
				row.setEndTime(limit.getEndTime());
				row.setPics("");
				row.setPrice(0);
				row.setItemSpecDesc("");
				row.setCreated(ts);
				row.setUpdated(ts);
				limitItemPromotionsMapper.insert(row);
				result.add(itemRowToMap(row));
			}
			return result;
		}
		long companyId = limit.getCompanyId();
		Map<String, Object> sku = marketingActivityCatalogAccess.loadSkuItemsList(companyId, itemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) sku.get("list");
		if (list == null) {
			list = List.of();
		}
		int ts = (int) (System.currentTimeMillis() / 1000L);
		for (Map<String, Object> v : list) {
			long itemId = readLong(v.get("item_id"));
			String itemName = v.get("item_name") != null ? v.get("item_name").toString() : "";
			long overlapCnt =
					limitItemPromotionsMapper.selectCount(
							new LambdaQueryWrapper<LimitItemPromotions>()
									.eq(LimitItemPromotions::getCompanyId, companyId)
									.eq(LimitItemPromotions::getDistributorId, 0L)
									.eq(LimitItemPromotions::getItemId, itemId)
									.le(LimitItemPromotions::getStartTime, limit.getEndTime())
									.ge(LimitItemPromotions::getEndTime, limit.getStartTime()));
			if (overlapCnt > 0) {
				throw new ResourceException(
						messageSource.getMessage("promotions.limit.item_already_limited", new Object[] {itemName}, locale));
			}
			LimitItemPromotions row = new LimitItemPromotions();
			row.setLimitId(limit.getLimitId());
			row.setDistributorId(0L);
			row.setItemId(itemId);
			row.setLimitNum((long) limitPurchase);
			row.setItemName(itemName);
			row.setCompanyId(companyId);
			row.setItemSpecDesc(Objects.toString(v.get("item_spec_desc"), ""));
			Object pics = v.get("pics");
			String picStr = "";
			if (pics instanceof List<?> pl && !pl.isEmpty()) {
				picStr = Objects.toString(pl.get(0), "");
			}
			row.setPics(picStr);
			Object price = v.get("price");
			if (price instanceof Number n) {
				row.setPrice(n.intValue());
			} else {
				row.setPrice(0);
			}
			row.setStartTime(limit.getStartTime());
			row.setEndTime(limit.getEndTime());
			row.setCreated(ts);
			row.setUpdated(ts);
			limitItemPromotionsMapper.insert(row);
			result.add(itemRowToMap(row));
		}
		return result;
	}

	public Map<String, Object> toLimitItemApiMap(LimitItemPromotions row) {
		return itemRowToMap(row);
	}

	public Map<String, Object> buildResponseMap(
			LimitPromotions e, List<Map<String, Object>> itemRows, List<String> validGradeParts)
			throws JsonProcessingException {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("limit_id", wireBigint(e.getLimitId()));
		m.put("company_id", wireBigint(e.getCompanyId()));
		m.put("limit_name", e.getLimitName());
		m.put("limit_type", e.getLimitType());
		m.put("total_item_num", e.getTotalItemNum());
		m.put("valid_item_num", e.getValidItemNum());
		m.put("error_desc", wireNullableText(e.getErrorDesc()));
		m.put("valid_grade", validGradeParts);
		m.put("rule", e.getRule());
		m.put("start_time", e.getStartTime());
		m.put("end_time", e.getEndTime());
		m.put("use_bound", e.getUseBound());
		m.put("tag_ids", parseJsonArrayOrEmpty(e.getTagIds()));
		m.put("brand_ids", parseJsonArrayOrEmpty(e.getBrandIds()));
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("source_type", e.getSourceType());
		m.put("source_id", wireBigint(e.getSourceId()));
		m.put("items", itemRows);
		return m;
	}

	public Map<String, Object> buildLimitPromotionCancelWireMap(LimitPromotions e, List<String> validGradeParts)
			throws JsonProcessingException {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("limit_id", wireBigint(e.getLimitId()));
		m.put("company_id", wireBigint(e.getCompanyId()));
		m.put("limit_name", e.getLimitName());
		m.put("limit_type", e.getLimitType());
		m.put("total_item_num", e.getTotalItemNum());
		m.put("valid_item_num", e.getValidItemNum());
		m.put("error_desc", wireNullableText(e.getErrorDesc()));
		m.put("valid_grade", validGradeParts);
		m.put("rule", e.getRule());
		m.put("start_time", e.getStartTime());
		m.put("end_time", e.getEndTime());
		m.put("use_bound", e.getUseBound());
		m.put("tag_ids", parseJsonArrayOrEmpty(e.getTagIds()));
		m.put("brand_ids", parseJsonArrayOrEmpty(e.getBrandIds()));
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("source_type", e.getSourceType());
		m.put("source_id", wireBigint(e.getSourceId()));
		return m;
	}

	private Map<String, Object> itemRowToMap(LimitItemPromotions row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("limit_id", wireBigint(row.getLimitId()));
		m.put("distributor_id", wireBigint(row.getDistributorId() != null ? row.getDistributorId() : 0L));
		m.put("item_id", wireBigint(row.getItemId()));
		String itemType = row.getItemType();
		m.put("item_type", StringUtils.hasText(itemType) ? itemType : "normal");
		m.put("limit_num", row.getLimitNum());
		m.put("company_id", wireBigint(row.getCompanyId()));
		m.put("item_name", row.getItemName() != null ? row.getItemName() : "");
		m.put("pics", row.getPics() != null ? row.getPics() : "");
		m.put("price", row.getPrice() != null ? row.getPrice() : 0);
		m.put("item_spec_desc", row.getItemSpecDesc() != null ? row.getItemSpecDesc() : "");
		m.put("start_time", row.getStartTime());
		m.put("end_time", row.getEndTime());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		return m;
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

	private List<Object> parseJsonArrayOrEmpty(String raw) throws JsonProcessingException {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return List.of();
		}
		return objectMapper.readValue(raw.trim(), new TypeReference<List<Object>>() {});
	}

	private void normalizeListKey(Map<String, Object> params, String key) {
		Object v = params.get(key);
		if (v == null) {
			params.put(key, new ArrayList<>());
			return;
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				params.put(key, new ArrayList<>());
				return;
			}
			List<Object> parts =
					Arrays.stream(s.split(","))
							.map(String::trim)
							.filter(StringUtils::hasText)
							.collect(Collectors.toList());
			params.put(key, parts);
			return;
		}
		if (v instanceof List<?> l) {
			List<Object> copy = new ArrayList<>(l);
			params.put(key, copy);
			return;
		}
		params.put(key, List.of(v));
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

	private static long readLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
