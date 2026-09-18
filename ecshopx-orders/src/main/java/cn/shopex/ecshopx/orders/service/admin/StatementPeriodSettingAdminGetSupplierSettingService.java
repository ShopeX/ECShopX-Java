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

import cn.shopex.ecshopx.orders.domain.StatementPeriodSetting;
import cn.shopex.ecshopx.orders.mapper.StatementPeriodSettingMapper;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class StatementPeriodSettingAdminGetSupplierSettingService {

	private final StatementPeriodSettingMapper statementPeriodSettingMapper;
	private final SupplierMapper supplierMapper;
	private final ObjectMapper objectMapper;

	public StatementPeriodSettingAdminGetSupplierSettingService(
			StatementPeriodSettingMapper statementPeriodSettingMapper,
			SupplierMapper supplierMapper,
			ObjectMapper objectMapper) {
		this.statementPeriodSettingMapper = statementPeriodSettingMapper;
		this.supplierMapper = supplierMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getSupplierSetting(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String supplierNameRaw,
			String supplierIdRaw) {
		int page = parsePageOneBased(pageRaw, 1);
		int pageSize = parsePageSize(pageSizeRaw, 20);
		String nameKey = supplierNameRaw == null ? "" : supplierNameRaw.trim();
		String sidParam = supplierIdRaw == null ? "" : supplierIdRaw.trim();

		List<Long> nameIdsFromSearch = null;
		if (!nameKey.isEmpty()) {
			String escaped = escapeLikeForContains(nameKey);
			LambdaQueryWrapper<Supplier> sw =
					new LambdaQueryWrapper<Supplier>().like(Supplier::getSupplierName, "%" + escaped + "%");
			List<Supplier> nameRows = supplierMapper.selectList(sw);
			if (nameRows.isEmpty()) {
				return emptyTotalList();
			}
			nameIdsFromSearch =
					nameRows.stream().map(Supplier::getId).filter(Objects::nonNull).distinct().toList();
		}

		LambdaQueryWrapper<StatementPeriodSetting> w =
				new LambdaQueryWrapper<StatementPeriodSetting>()
						.eq(StatementPeriodSetting::getCompanyId, companyId)
						.eq(StatementPeriodSetting::getMerchantType, "supplier")
						.gt(StatementPeriodSetting::getSupplierId, 0L);

		if (nameIdsFromSearch != null) {
			if (sidParam.isEmpty()) {
				return emptyTotalList();
			}
			Long matched = null;
			for (Long id : nameIdsFromSearch) {
				if (id != null && Long.toString(id).equals(sidParam)) {
					matched = id;
					break;
				}
			}
			if (matched == null) {
				return emptyTotalList();
			}
			w.eq(StatementPeriodSetting::getSupplierId, matched);
		} else {
			Long single = parseOptionalTruthyPositiveLong(supplierIdRaw);
			if (single == null) {
				return emptyTotalList();
			}
			w.eq(StatementPeriodSetting::getSupplierId, single);
		}

		long totalLong = statementPeriodSettingMapper.selectCount(w);
		int totalCount = totalLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalLong;

		if (totalCount == 0) {
			return emptyTotalList();
		}

		Page<StatementPeriodSetting> p = new Page<>(page, pageSize, false);
		Page<StatementPeriodSetting> pageResult = statementPeriodSettingMapper.selectPage(p, w);
		List<StatementPeriodSetting> records = pageResult.getRecords();

		LinkedHashSet<Long> sidSet = new LinkedHashSet<>();
		for (StatementPeriodSetting r : records) {
			if (r.getSupplierId() != null) {
				sidSet.add(r.getSupplierId());
			}
		}
		Map<Long, String> nameById = new LinkedHashMap<>();
		if (!sidSet.isEmpty()) {
			List<Long> sidList = new ArrayList<>(sidSet);
			List<Supplier> supplierRows =
					supplierMapper.selectList(new LambdaQueryWrapper<Supplier>().in(Supplier::getId, sidList));
			for (Supplier s : supplierRows) {
				if (s.getId() != null) {
					String n = s.getSupplierName();
					nameById.put(s.getId(), n == null ? "" : n);
				}
			}
		}

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
			long supplierId = r.getSupplierId() == null ? 0L : r.getSupplierId();
			row.put("supplier_name", nameById.getOrDefault(supplierId, ""));
			listRows.add(row);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", Integer.valueOf(totalCount));
		out.put("list", listRows);
		return out;
	}

	private static LinkedHashMap<String, Object> emptyTotalList() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("total_count", Integer.valueOf(0));
		m.put("list", List.of());
		return m;
	}

	private static String escapeLikeForContains(String raw) {
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
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
}
