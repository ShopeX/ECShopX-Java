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

package cn.shopex.ecshopx.orders.service.offline;

import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.service.offline.export.OfflinePaymentExportQuerySupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OfflinePaymentGetListService {

	private final OfflinePaymentAdminQueryFilterBuilder offlinePaymentAdminQueryFilterBuilder;
	private final OfflinePaymentMapper offlinePaymentMapper;
	private final OfflinePaymentAdminListRowAssembler offlinePaymentAdminListRowAssembler;

	public OfflinePaymentGetListService(
			OfflinePaymentAdminQueryFilterBuilder offlinePaymentAdminQueryFilterBuilder,
			OfflinePaymentMapper offlinePaymentMapper,
			OfflinePaymentAdminListRowAssembler offlinePaymentAdminListRowAssembler) {
		this.offlinePaymentAdminQueryFilterBuilder = offlinePaymentAdminQueryFilterBuilder;
		this.offlinePaymentMapper = offlinePaymentMapper;
		this.offlinePaymentAdminListRowAssembler = offlinePaymentAdminListRowAssembler;
	}

	public Map<String, Object> getList(long companyId, Map<String, Object> queryParams) {
		LinkedHashMap<String, Object> filter =
				offlinePaymentAdminQueryFilterBuilder.buildFilter(companyId, queryParams);
		LambdaQueryWrapper<OfflinePayment> wrapper =
				OfflinePaymentExportQuerySupport.toWrapper(filter).orderByDesc(OfflinePayment::getId);
		long totalCount = offlinePaymentMapper.selectCount(wrapper);
		List<Map<String, Object>> list = new ArrayList<>();
		if (totalCount > 0) {
			int pageNo = queryIntWithDefault(queryParams, "page", 1);
			int pageSize = queryIntWithDefault(queryParams, "pageSize", 20);
			Page<OfflinePayment> page = new Page<>(pageNo, pageSize, false);
			Page<OfflinePayment> pageResult = offlinePaymentMapper.selectPage(page, wrapper);
			for (OfflinePayment row : pageResult.getRecords()) {
				list.add(offlinePaymentAdminListRowAssembler.toListRowMap(row));
			}
		}
		Map<String, Object> result = new LinkedHashMap<>();
		int totalCountInt =
				totalCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalCount;
		result.put("total_count", totalCountInt);
		result.put("list", list);
		return result;
	}

	private static int queryIntWithDefault(
			Map<String, Object> params, String key, int defaultWhenKeyAbsent) {
		if (!params.containsKey(key) || params.get(key) == null) {
			return defaultWhenKeyAbsent;
		}
		return scalarToIntLeadingDigits(params.get(key));
	}

	private static int scalarToIntLeadingDigits(Object v) {
		if (v instanceof Number n) {
			return (int) n.longValue();
		}
		String s = String.valueOf(v).trim();
		long l = LeadingNumberParser.parseAsLong(s);
		return (int) l;
	}
}
