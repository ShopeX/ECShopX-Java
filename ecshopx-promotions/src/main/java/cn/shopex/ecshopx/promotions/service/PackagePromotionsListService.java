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

import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.LimitPromotionListDistributorNameReadService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PackagePromotionsListService {

	private final PackagePromotionsMapper packagePromotionsMapper;
	private final LimitPromotionListDistributorNameReadService limitPromotionListDistributorNameReadService;

	public PackagePromotionsListService(
			PackagePromotionsMapper packagePromotionsMapper,
			LimitPromotionListDistributorNameReadService limitPromotionListDistributorNameReadService) {
		this.packagePromotionsMapper = packagePromotionsMapper;
		this.limitPromotionListDistributorNameReadService = limitPromotionListDistributorNameReadService;
	}

	public Map<String, Object> lists(
			long companyId,
			String operatorType,
			long distributorIdFromQuery,
			String status,
			int page,
			int pageSize,
			String requestLangTag) {
		long now = System.currentTimeMillis() / 1000L;
		long total =
				packagePromotionsMapper.countAdminPackageList(
						companyId, status, operatorType, distributorIdFromQuery, now);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}
		long offset = (long) (page - 1) * (long) pageSize;
		int limit = Math.max(0, pageSize);
		List<PackagePromotions> entities =
				packagePromotionsMapper.selectAdminPackageListPage(
						companyId, status, operatorType, distributorIdFromQuery, now, offset, limit);
		List<Map<String, Object>> rows = new ArrayList<>();
		for (PackagePromotions e : entities) {
			rows.add(entityToListRow(e, now));
		}

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
				if (rowSourceId != distributorIdFromQuery) {
					if ("staff".equals(rowSourceType) && distributorIdFromQuery == 0L) {
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

	private Map<String, Object> entityToListRow(PackagePromotions e, long now) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("package_id", e.getPackageId());
		row.put("company_id", e.getCompanyId());
		row.put("goods_id", e.getGoodsId());
		row.put("main_item_id", e.getMainItemId());
		row.put("main_item_price", e.getMainItemPrice());
		row.put("package_name", e.getPackageName());
		row.put("valid_grade", e.getValidGrade());
		row.put("used_platform", e.getUsedPlatform());
		row.put("free_postage", e.getFreePostage());
		row.put("package_total_price", e.getPackageTotalPrice());
		row.put("start_time", e.getStartTime());
		row.put("end_time", e.getEndTime());
		row.put("package_status", e.getPackageStatus());
		row.put("reason", e.getReason());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		row.put("source_type", e.getSourceType());
		row.put("source_id", e.getSourceId() != null ? e.getSourceId() : 0L);

		long st = e.getStartTime() == null ? 0L : e.getStartTime().longValue();
		long en = e.getEndTime() == null ? 0L : e.getEndTime().longValue();
		String displayStatus;
		if (st > now) {
			displayStatus = "waiting";
		} else if (en < now) {
			displayStatus = "end";
		} else {
			displayStatus = "ongoing";
		}
		row.put("status", displayStatus);

		return row;
	}
}
