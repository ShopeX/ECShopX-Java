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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.salesperson.domain.ProfitStatistics;
import cn.shopex.ecshopx.salesperson.mapper.ProfitStatisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfitWithdrawalListQueryService {

	private static final String KEY_NAME_CONTAINS = "nameContains";

	private final ProfitStatisticsMapper profitStatisticsMapper;
	private final ObjectMapper objectMapper;

	public ProfitWithdrawalListQueryService(ProfitStatisticsMapper profitStatisticsMapper,
			ObjectMapper objectMapper) {
		this.profitStatisticsMapper = profitStatisticsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> query(Map<String, Object> filter, Integer page, Integer pageSize) {
		LambdaQueryWrapper<ProfitStatistics> wrapper = buildWrapper(filter);
		Long total = profitStatisticsMapper.selectCount(wrapper);
		long totalCount = total == null ? 0L : total;

		if (totalCount <= 0) {
			return Map.of("total_count", totalCount, "list", Collections.emptyList());
		}

		List<ProfitStatistics> records;
		if (pageSize == null || pageSize <= 0) {
			records = profitStatisticsMapper.selectList(wrapper);
		} else {
			int pageCoerced = page == null ? 0 : page;
			Page<ProfitStatistics> p = new Page<>(pageCoerced, pageSize, false);
			records = profitStatisticsMapper.selectPage(p, wrapper).getRecords();
		}

		List<Map<String, Object>> rowMaps = new ArrayList<>(records.size());
		for (ProfitStatistics row : records) {
			rowMaps.add(toRowMap(row));
		}
		return Map.of("total_count", totalCount, "list", rowMaps);
	}

	private LambdaQueryWrapper<ProfitStatistics> buildWrapper(Map<String, Object> filter) {
		LambdaQueryWrapper<ProfitStatistics> w = new LambdaQueryWrapper<>();
		for (Map.Entry<String, Object> e : filter.entrySet()) {
			String key = e.getKey();
			if (KEY_NAME_CONTAINS.equals(key)) {
				continue;
			}
			Object val = e.getValue();
			if ("date".equals(key)) {
				if (val != null && StringUtils.hasText(String.valueOf(val).trim())) {
					w.eq(ProfitStatistics::getDate, String.valueOf(val).trim());
				}
				continue;
			}
			if ("profit_user_type".equals(key)) {
				if (val == null) {
					w.isNull(ProfitStatistics::getProfitUserType);
				} else if (val instanceof Long l) {
					w.eq(ProfitStatistics::getProfitUserType, l);
				} else {
					w.eq(ProfitStatistics::getProfitUserType, Long.parseLong(val.toString().trim()));
				}
				continue;
			}
		}
		Object nameContains = filter.get(KEY_NAME_CONTAINS);
		if (nameContains != null && StringUtils.hasText(nameContains.toString().trim())) {
			String v = nameContains.toString().trim();
			w.like(ProfitStatistics::getName, "%" + v + "%");
		}
		return w;
	}

	private Map<String, Object> toRowMap(ProfitStatistics e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("date", e.getDate());
		m.put("company_id", e.getCompanyId());
		m.put("profit_user_id", e.getProfitUserId());
		m.put("profit_user_type", e.getProfitUserType());
		m.put("withdrawals_fee", e.getWithdrawalsFee());
		m.put("name", e.getName());
		m.put("params", parseParams(e.getParams()));
		return m;
	}

	private Map<String, Object> parseParams(String paramsJson) {
		if (!StringUtils.hasText(paramsJson)) {
			return Map.of();
		}
		try {
			Map<String, Object> parsed = objectMapper.readValue(paramsJson, new TypeReference<Map<String, Object>>() {
			});
			return parsed != null ? parsed : Map.of();
		} catch (Exception ignored) {
			return Map.of();
		}
	}
}
