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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import cn.shopex.ecshopx.employeepurchase.domain.Enterprises;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityEnterpriseBehaviorLogMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterprisesMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ActivityEnterpriseBehaviorStatsService {

	private final ActivityEnterpriseBehaviorLogMapper activityEnterpriseBehaviorLogMapper;
	private final EnterprisesMapper enterprisesMapper;

	public ActivityEnterpriseBehaviorStatsService(
			ActivityEnterpriseBehaviorLogMapper activityEnterpriseBehaviorLogMapper,
			EnterprisesMapper enterprisesMapper) {
		this.activityEnterpriseBehaviorLogMapper = activityEnterpriseBehaviorLogMapper;
		this.enterprisesMapper = enterprisesMapper;
	}

	public Map<Long, Map<String, Object>> loadActivityLevelStats(long companyId, List<Long> activityIds) {
		if (activityIds == null || activityIds.isEmpty()) {
			return Map.of();
		}
		List<LinkedHashMap<String, Object>> rows =
				activityEnterpriseBehaviorLogMapper.selectAggregatedStatsByActivityIds(companyId, activityIds);
		LinkedHashMap<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (LinkedHashMap<String, Object> row : rows) {
			long activityId = longVal(row.get("activity_id"));
			if (activityId > 0L) {
				out.put(activityId, normalizeStatsRow(row));
			}
		}
		return out;
	}

	public List<Map<String, Object>> loadEnterpriseStatsForActivity(long companyId, long activityId) {
		List<LinkedHashMap<String, Object>> rows =
				activityEnterpriseBehaviorLogMapper.selectAggregatedStatsByActivity(companyId, activityId);
		if (rows.isEmpty()) {
			return List.of();
		}
		Set<Long> enterpriseIds =
				rows.stream()
						.map(r -> longVal(r.get("enterprise_id")))
						.filter(id -> id > 0L)
						.collect(Collectors.toSet());
		Map<Long, Enterprises> enterpriseById = loadEnterprises(companyId, enterpriseIds);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (LinkedHashMap<String, Object> row : rows) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>(normalizeStatsRow(row));
			long enterpriseId = longVal(row.get("enterprise_id"));
			m.put("enterprise_id", enterpriseId);
			Enterprises ent = enterpriseById.get(enterpriseId);
			if (ent != null) {
				LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
				nested.put("id", ent.getId());
				nested.put("name", ent.getName());
				nested.put("enterprise_sn", ent.getEnterpriseSn());
				m.put("enterprise", nested);
			} else {
				m.put("enterprise", null);
			}
			out.add(m);
		}
		return out;
	}

	private static LinkedHashMap<String, Object> normalizeStatsRow(Map<String, Object> row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("scan_count", intVal(row.get("scan_count")));
		m.put("scan_user_count", intVal(row.get("scan_user_count")));
		m.put("passphrase_verify_user_count", intVal(row.get("passphrase_verify_user_count")));
		m.put("bind_user_count", intVal(row.get("bind_user_count")));
		m.put("order_user_count", intVal(row.get("order_user_count")));
		return m;
	}

	private Map<Long, Enterprises> loadEnterprises(long companyId, Set<Long> enterpriseIds) {
		if (enterpriseIds.isEmpty()) {
			return Map.of();
		}
		List<Enterprises> list =
				enterprisesMapper.selectList(
						Wrappers.<Enterprises>lambdaQuery()
								.eq(Enterprises::getCompanyId, companyId)
								.in(Enterprises::getId, enterpriseIds));
		LinkedHashMap<Long, Enterprises> out = new LinkedHashMap<>();
		for (Enterprises e : list) {
			if (e.getId() != null) {
				out.put(e.getId(), e);
			}
		}
		return out;
	}

	private static long longVal(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw == null) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
