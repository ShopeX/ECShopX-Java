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

package cn.shopex.ecshopx.chinaumspay.service.divisionlist;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivision;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DivisionListService {

	private final DivisionListExportFilterBuilder divisionListExportFilterBuilder;
	private final ChinaumsDivisionListExportQueryService chinaumsDivisionListExportQueryService;

	public DivisionListService(DivisionListExportFilterBuilder divisionListExportFilterBuilder,
			ChinaumsDivisionListExportQueryService chinaumsDivisionListExportQueryService) {
		this.divisionListExportFilterBuilder = divisionListExportFilterBuilder;
		this.chinaumsDivisionListExportQueryService = chinaumsDivisionListExportQueryService;
	}

	public Map<String, Object> query(Map<String, Object> jwtUser, String backStatus, String timeStartBegin,
			String timeStartEnd, Integer page, Integer pageSize) {
		int pageNum = normalizePage(page);
		int size = normalizePageSize(pageSize);

		DivisionListExportFilter filter =
				divisionListExportFilterBuilder.build(jwtUser, backStatus, timeStartBegin, timeStartEnd);
		int total = chinaumsDivisionListExportQueryService.countByFilter(filter);

		List<Map<String, Object>> rows;
		if (total == 0) {
			rows = Collections.emptyList();
		} else {
			List<ChinaumspayDivision> records =
					chinaumsDivisionListExportQueryService.listPageByFilter(filter, pageNum, size);
			rows = new ArrayList<>(records.size());
			for (ChinaumspayDivision r : records) {
				rows.add(toSnakeRow(r));
			}
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", (long) total);
		body.put("list", rows);
		return body;
	}

	private static int normalizePage(Integer page) {
		if (page == null || page < 1) {
			return 1;
		}
		return page;
	}

	private static int normalizePageSize(Integer pageSize) {
		if (pageSize == null || pageSize < 1) {
			return 20;
		}
		return pageSize;
	}

	private static Map<String, Object> toSnakeRow(ChinaumspayDivision e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		m.put("total_fee", e.getTotalFee());
		m.put("actual_fee", e.getActualFee());
		m.put("commission_rate_fee", e.getCommissionRateFee());
		m.put("division_fee", e.getDivisionFee());
		m.put("backsucc_fee", e.getBacksuccFee());
		m.put("rate_fee", e.getRateFee());
		m.put("back_status", e.getBackStatus());
		m.put("create_time", e.getCreateTime());
		m.put("update_time", e.getUpdateTime());
		return m;
	}
}
