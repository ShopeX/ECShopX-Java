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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseActivityUsersQueryMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseActivityUsersService {

	private final EmployeePurchaseActivityUsersQueryMapper activityUsersQueryMapper;

	public EmployeePurchaseActivityUsersService(EmployeePurchaseActivityUsersQueryMapper activityUsersQueryMapper) {
		this.activityUsersQueryMapper = activityUsersQueryMapper;
	}

	public Map<String, Object> getActivityUsers(
			long companyId,
			long activityId,
			String employeeMobile,
			String relativeMobile,
			int page,
			int pageSize) {
		String normEmployeeMobile = StringUtils.hasText(employeeMobile) ? employeeMobile : null;
		String normRelativeMobile = StringUtils.hasText(relativeMobile) ? relativeMobile : null;

		long total = activityUsersQueryMapper.countActivityUsers(
				companyId, activityId, normEmployeeMobile, normRelativeMobile);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		if (total == 0) {
			result.put("list", List.of());
			return result;
		}

		long offset = pageSize > 0 ? Math.max(0L, (long) (page - 1) * (long) pageSize) : 0L;
		List<LinkedHashMap<String, Object>> rows = activityUsersQueryMapper.selectActivityUsers(
				companyId, activityId, normEmployeeMobile, normRelativeMobile, pageSize, offset);

		List<Map<String, Object>> processed = new ArrayList<>(rows.size());
		for (LinkedHashMap<String, Object> row : rows) {
			normalizeRowScalars(row);
			processed.add(row);
		}
		result.put("list", processed);
		return result;
	}

	private static void normalizeRowScalars(Map<String, Object> row) {
		putDisabledAsZeroOrOne(row);
		normalizeNumericScalar(row, "employee_user_id");
		normalizeNumericScalar(row, "relative_user_id");
		normalizeNumericScalar(row, "created");
		normalizeAggregateFee(row);
	}

	private static void putDisabledAsZeroOrOne(Map<String, Object> row) {
		Object dis = row.get("disabled");
		if (dis == null) {
			row.put("disabled", 0);
			return;
		}
		int v;
		if (dis instanceof Boolean b) {
			v = b ? 1 : 0;
		} else if (dis instanceof Number n) {
			v = n.intValue() != 0 ? 1 : 0;
		} else {
			String s = dis.toString().trim();
			v = ("1".equals(s) || "true".equalsIgnoreCase(s)) ? 1 : 0;
		}
		row.put("disabled", v);
	}

	private static void normalizeNumericScalar(Map<String, Object> row, String key) {
		Object v = row.get(key);
		if (v == null) {
			return;
		}
		if (v instanceof BigDecimal bd) {
			row.put(key, bd.longValue());
		} else if (v instanceof Number n && !(v instanceof Long) && !(v instanceof Integer)) {
			row.put(key, n.longValue());
		}
	}

	private static void normalizeAggregateFee(Map<String, Object> row) {
		Object fee = row.get("aggregate_fee");
		if (fee == null) {
			return;
		}
		if (fee instanceof BigDecimal bd) {
			row.put("aggregate_fee", bd.longValue());
		} else if (fee instanceof Number n && !(fee instanceof Integer) && !(fee instanceof Long)) {
			row.put("aggregate_fee", n.longValue());
		}
	}
}
