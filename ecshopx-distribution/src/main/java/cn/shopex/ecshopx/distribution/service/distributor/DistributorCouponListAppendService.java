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

package cn.shopex.ecshopx.distribution.service.distributor;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DistributorCouponListAppendService {

	private final DistributorMapper distributorMapper;
	private final DistributorSelfMetaService distributorSelfMetaService;

	public DistributorCouponListAppendService(
			DistributorMapper distributorMapper, DistributorSelfMetaService distributorSelfMetaService) {
		this.distributorMapper = distributorMapper;
		this.distributorSelfMetaService = distributorSelfMetaService;
	}

	public void appendDistributorList(long companyId, List<Map<String, Object>> list) {
		if (companyId < 1L || list == null || list.isEmpty()) {
			return;
		}
		Map<Long, Map<String, Object>> distributorData = loadDistributorMaps(companyId, list);
		for (Map<String, Object> item : list) {
			long distributorId = parseNonNegativeLong(item.get("distributor_id"));
			Map<String, Object> row = distributorData.get(distributorId);
			item.put("distributor_list", row != null ? List.of(row) : List.of());
		}
	}

	public void appendDistributorInfo(long companyId, List<Map<String, Object>> list) {
		if (companyId < 1L || list == null || list.isEmpty()) {
			return;
		}
		Map<Long, Map<String, Object>> distributorData = loadDistributorMaps(companyId, list);
		for (Map<String, Object> item : list) {
			long distributorId = parseNonNegativeLong(item.get("distributor_id"));
			Map<String, Object> row = distributorData.get(distributorId);
			item.put("distributor_info", row != null ? row : new LinkedHashMap<>());
		}
	}

	private Map<Long, Map<String, Object>> loadDistributorMaps(long companyId, List<Map<String, Object>> list) {
		Set<Long> distributorIds = new LinkedHashSet<>();
		for (Map<String, Object> item : list) {
			Object raw = item.get("distributor_id");
			if (raw != null && isNumericNonNegative(raw)) {
				long distributorId = parseNonNegativeLong(raw);
				if (distributorId > 0L) {
					distributorIds.add(distributorId);
				}
			}
		}
		Map<Long, Map<String, Object>> distributorData = new LinkedHashMap<>();
		if (!distributorIds.isEmpty()) {
			List<Distributor> rows = distributorMapper.selectList(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.in(Distributor::getDistributorId, distributorIds));
			for (Distributor d : rows) {
				if (d.getDistributorId() != null) {
					distributorData.put(d.getDistributorId(), toBriefRow(d, companyId));
				}
			}
		}
		distributorData.put(0L, distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId));
		return distributorData;
	}

	private static Map<String, Object> toBriefRow(Distributor d, long companyId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("distributor_id", d.getDistributorId() != null ? String.valueOf(d.getDistributorId()) : "0");
		m.put("company_id", String.valueOf(companyId));
		m.put("logo", d.getLogo() != null ? d.getLogo() : "");
		m.put("name", d.getName() != null ? d.getName() : "");
		return m;
	}

	private static boolean isNumericNonNegative(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue() >= 0L;
		}
		try {
			return Long.parseLong(raw.toString().trim()) >= 0L;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static long parseNonNegativeLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
