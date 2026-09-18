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

import cn.shopex.ecshopx.promotions.domain.LimitPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitPromotionsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.LimitPromotionListDistributorNameReadService;
import cn.shopex.ecshopx.promotions.service.multilang.LimitPromotionListMultiLangReadService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LimitPromotionsListService {

	private final LimitPromotionsMapper limitPromotionsMapper;
	private final LimitPromotionListMultiLangReadService limitPromotionListMultiLangReadService;
	private final LimitPromotionListDistributorNameReadService limitPromotionListDistributorNameReadService;

	public LimitPromotionsListService(
			LimitPromotionsMapper limitPromotionsMapper,
			LimitPromotionListMultiLangReadService limitPromotionListMultiLangReadService,
			LimitPromotionListDistributorNameReadService limitPromotionListDistributorNameReadService) {
		this.limitPromotionsMapper = limitPromotionsMapper;
		this.limitPromotionListMultiLangReadService = limitPromotionListMultiLangReadService;
		this.limitPromotionListDistributorNameReadService = limitPromotionListDistributorNameReadService;
	}

	public Map<String, Object> lists(
			long companyId,
			String sourceType,
			long sourceIdFromQuery,
			String status,
			int page,
			int pageSize,
			String requestLangTag) {
		Map<String, Object> out = new LinkedHashMap<>();
		long now = System.currentTimeMillis() / 1000L;
		long total =
				limitPromotionsMapper.countAdminLimitList(
						companyId, status, sourceType, sourceIdFromQuery, now);
		out.put("total_count", total);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}
		long offset = (long) (page - 1) * (long) pageSize;
		int limit = Math.max(0, pageSize);
		List<LimitPromotions> entities =
				limitPromotionsMapper.selectAdminLimitListPage(
						companyId, status, sourceType, sourceIdFromQuery, now, offset, limit);
		List<Map<String, Object>> rows = new ArrayList<>();
		for (LimitPromotions e : entities) {
			rows.add(entityToRow(e, now));
		}
		limitPromotionListMultiLangReadService.applyLimitNames(companyId, rows, requestLangTag);

		Set<Long> distributorSourceIds = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			if ("distributor".equals(String.valueOf(row.get("source_type")))) {
				Object sid = row.get("source_id");
				if (sid instanceof Number n && n.longValue() > 0L) {
					distributorSourceIds.add(n.longValue());
				}
			}
		}
		Map<Long, String> distributorNames =
				limitPromotionListDistributorNameReadService.mapDistributorIdToDisplayName(
						companyId, new ArrayList<>(distributorSourceIds), requestLangTag);
		for (Map<String, Object> row : rows) {
			if ("distributor".equals(String.valueOf(row.get("source_type")))) {
				Object sid = row.get("source_id");
				long id = sid instanceof Number ? ((Number) sid).longValue() : 0L;
				row.put("source_name", distributorNames.getOrDefault(id, ""));
			} else {
				row.put("source_name", "");
			}
		}

		if (!rows.isEmpty()) {
			for (Map<String, Object> row : rows) {
				long rowSourceId =
						row.get("source_id") instanceof Number
								? ((Number) row.get("source_id")).longValue()
								: 0L;
				String rowSourceType = String.valueOf(row.get("source_type"));
				String editBtn;
				if (rowSourceId != sourceIdFromQuery) {
					if ("staff".equals(rowSourceType) && sourceIdFromQuery == 0L) {
						editBtn = "Y";
					} else {
						editBtn = "N";
					}
				} else {
					editBtn = "Y";
				}
				row.put("edit_btn", editBtn);
			}
		}

		out.put("list", rows);
		return out;
	}

	private Map<String, Object> entityToRow(LimitPromotions e, long now) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("limit_id", e.getLimitId());
		row.put("company_id", e.getCompanyId());
		row.put("limit_name", e.getLimitName());
		row.put("total_item_num", e.getTotalItemNum());
		row.put("valid_item_num", e.getValidItemNum());
		String err = e.getErrorDesc();
		if (!StringUtils.hasText(err == null ? "" : err.trim())) {
			row.put("error_desc", "");
		} else {
			int n = err.split(";", -1).length;
			row.put("error_desc", n + "个错误");
		}
		row.put("limit_type", e.getLimitType());
		row.put("valid_grade", e.getValidGrade());
		row.put("rule", e.getRule());
		row.put("start_time", e.getStartTime());
		row.put("end_time", e.getEndTime());
		row.put("use_bound", e.getUseBound() == null ? 1 : e.getUseBound());
		row.put("tag_ids", tagOrBrandIdsColumnString(e.getTagIds()));
		row.put("brand_ids", tagOrBrandIdsColumnString(e.getBrandIds()));
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		row.put("source_type", e.getSourceType());
		row.put("source_id", e.getSourceId() != null ? e.getSourceId() : 0L);

		long st = e.getStartTime() == null ? 0L : e.getStartTime().longValue();
		long en = e.getEndTime() == null ? 0L : e.getEndTime().longValue();
		String displayStatus;
		if (now < st) {
			displayStatus = "waiting";
		} else if (now > st && now < en) {
			displayStatus = "ongoing";
		} else if (now >= en) {
			displayStatus = "end";
		} else {
			displayStatus = "waiting";
		}
		row.put("status", displayStatus);
		return row;
	}

	private static String tagOrBrandIdsColumnString(String raw) {
		return raw != null ? raw : "[]";
	}
}
