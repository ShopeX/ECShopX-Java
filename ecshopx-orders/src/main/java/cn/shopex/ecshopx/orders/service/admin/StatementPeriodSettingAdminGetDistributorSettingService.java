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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.orders.domain.StatementPeriodSetting;
import cn.shopex.ecshopx.orders.mapper.StatementPeriodSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class StatementPeriodSettingAdminGetDistributorSettingService {

	private final StatementPeriodSettingMapper statementPeriodSettingMapper;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final ObjectMapper objectMapper;

	public StatementPeriodSettingAdminGetDistributorSettingService(
			StatementPeriodSettingMapper statementPeriodSettingMapper,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			ObjectMapper objectMapper) {
		this.statementPeriodSettingMapper = statementPeriodSettingMapper;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getDistributorSetting(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String distributorIdRaw,
			String merchantIdRaw) {
		int page = parsePageOneBased(pageRaw, 1);
		int pageSize = parsePageSize(pageSizeRaw, 20);

		LambdaQueryWrapper<StatementPeriodSetting> w =
				new LambdaQueryWrapper<StatementPeriodSetting>()
						.eq(StatementPeriodSetting::getCompanyId, companyId)
						.eq(StatementPeriodSetting::getMerchantType, "distributor")
						.gt(StatementPeriodSetting::getDistributorId, 0L);
		Long filterDistributorId = parseOptionalTruthyPositiveLong(distributorIdRaw);
		if (filterDistributorId != null) {
			w.eq(StatementPeriodSetting::getDistributorId, filterDistributorId);
		}
		Long filterMerchantId = parseOptionalTruthyPositiveLong(merchantIdRaw);
		if (filterMerchantId != null) {
			w.eq(StatementPeriodSetting::getMerchantId, filterMerchantId);
		}

		long totalLong = statementPeriodSettingMapper.selectCount(w);
		int totalCount = totalLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalLong;

		if (totalCount == 0) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0);
			empty.put("list", List.of());
			return empty;
		}

		Page<StatementPeriodSetting> p = new Page<>(page, pageSize, false);
		Page<StatementPeriodSetting> result = statementPeriodSettingMapper.selectPage(p, w);
		List<StatementPeriodSetting> records = result.getRecords();

		Set<Long> distributorIds = new LinkedHashSet<>();
		Set<Long> merchantIds = new LinkedHashSet<>();
		for (StatementPeriodSetting r : records) {
			if (r.getDistributorId() != null) {
				distributorIds.add(r.getDistributorId());
			}
			if (r.getMerchantId() != null) {
				merchantIds.add(r.getMerchantId());
			}
		}

		Map<Long, String> distributorNameById = loadDistributorNames(companyId, distributorIds);
		Map<Long, String> merchantNameById = loadMerchantNames(companyId, merchantIds);

		List<Map<String, Object>> listRows = new ArrayList<>();
		for (StatementPeriodSetting r : records) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", r.getId());
			row.put("company_id", r.getCompanyId());
			row.put("merchant_id", r.getMerchantId());
			row.put("distributor_id", r.getDistributorId());
			row.put("supplier_id", r.getSupplierId());
			row.put("merchant_type", r.getMerchantType());
			row.put("period", decodePeriodLenient(r.getPeriod()));
			row.put("created", r.getCreated());
			row.put("updated", r.getUpdated());
			long did = r.getDistributorId() == null ? 0L : r.getDistributorId();
			long mid = r.getMerchantId() == null ? 0L : r.getMerchantId();
			row.put("distributor_name", distributorNameById.getOrDefault(did, ""));
			row.put("merchant_name", merchantNameById.getOrDefault(mid, ""));
			listRows.add(row);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listRows);
		return out;
	}

	private static boolean queryParamLooksPresent(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		return !t.isEmpty() && !"0".equals(t);
	}

	private static Long parseOptionalTruthyPositiveLong(String raw) {
		if (!queryParamLooksPresent(raw)) {
			return null;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int parsePageOneBased(String raw, int defaultValue) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static int parsePageSize(String raw, int defaultValue) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v < 1 ? defaultValue : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private Object decodePeriodLenient(String periodStr) {
		if (periodStr == null || periodStr.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(periodStr, new TypeReference<List<Object>>() {});
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private Map<Long, String> loadDistributorNames(long companyId, Set<Long> distributorIds) {
		Map<Long, String> out = new HashMap<>();
		if (distributorIds.isEmpty()) {
			return out;
		}
		List<Long> idList = new ArrayList<>(distributorIds);
		List<Map<String, Object>> rows =
				distributionDistributorSelfReadMapper.listDistributorNamesByCompanyAndIds(companyId, idList);
		for (Map<String, Object> dbRow : rows) {
			Long did = longFromCell(dbRow.get("distributor_id"));
			if (did != null) {
				Object nameObj = dbRow.get("name");
				out.put(did, nameObj == null ? "" : String.valueOf(nameObj));
			}
		}
		return out;
	}

	private Map<Long, String> loadMerchantNames(long companyId, Set<Long> merchantIds) {
		Map<Long, String> out = new HashMap<>();
		if (merchantIds.isEmpty()) {
			return out;
		}
		List<Long> idList = new ArrayList<>(merchantIds);
		List<Map<String, Object>> dbRows =
				distributionDistributorSelfReadMapper.listMerchantNamesByCompanyAndIds(companyId, idList);
		for (Map<String, Object> dbRow : dbRows) {
			Long mid = longFromCell(dbRow.get("merchant_id"));
			if (mid != null) {
				Object nameObj = dbRow.get("merchant_name");
				out.put(mid, nameObj == null ? "" : String.valueOf(nameObj));
			}
		}
		return out;
	}

	private static Long longFromCell(Object cell) {
		if (cell == null) {
			return null;
		}
		if (cell instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(cell).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
