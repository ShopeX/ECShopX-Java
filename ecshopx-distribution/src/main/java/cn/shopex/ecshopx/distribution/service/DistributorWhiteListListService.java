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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListExportFilter;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListListRow;
import cn.shopex.ecshopx.distribution.service.export.DistributorWhiteListExportService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorWhiteListListService {

	private final DistributorWhiteListExportService distributorWhiteListExportService;
	private final DistributorWhiteListExportQueryService distributorWhiteListExportQueryService;

	public DistributorWhiteListListService(
			DistributorWhiteListExportService distributorWhiteListExportService,
			DistributorWhiteListExportQueryService distributorWhiteListExportQueryService) {
		this.distributorWhiteListExportService = distributorWhiteListExportService;
		this.distributorWhiteListExportQueryService = distributorWhiteListExportQueryService;
	}

	@SuppressWarnings("unused")
	public Map<String, Object> getWhiteList(
			long companyId,
			String operatorType,
			Long jwtDistributorId,
			Map<String, Object> merged,
			int page,
			int pageSize) {
		DistributorWhiteListExportFilter filter =
				distributorWhiteListExportService.buildExportFilterForWhiteListGet(
						companyId, operatorType, jwtDistributorId, merged);
		long total = distributorWhiteListExportQueryService.countGroupedByMobile(filter);
		List<DistributorWhiteListListRow> rows =
				distributorWhiteListExportQueryService.listGroupedByMobileWithoutPageSizeLimit(filter);
		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (DistributorWhiteListListRow row : rows) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", row.id());
			item.put("company_id", row.companyId());
			item.put("created", row.created());
			item.put("updated", row.updated());
			item.put("mobile", row.mobile());
			item.put("username", row.username());
			item.put("distributor_info", row.distributorInfo());
			list.add(item);
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		data.put("total_count", String.valueOf(total));
		return data;
	}
}
