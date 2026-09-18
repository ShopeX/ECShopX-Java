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

package cn.shopex.ecshopx.orders.service.statement;

import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatementsSummarizedAdminFilterAssembler {

	private final SupplierMapper supplierMapper;

	public StatementsSummarizedAdminFilterAssembler(SupplierMapper supplierMapper) {
		this.supplierMapper = supplierMapper;
	}

	public LinkedHashMap<String, Object> assembleListFilter(
			long companyId,
			long operatorId,
			String operatorType,
			Long jwtDistributorIdOrNull,
			Long jwtMerchantIdOrNull,
			Map<String, Object> requestParams) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		filter.put("merchant_type", "distributor");
		Object merchantTypeRaw = requestParams.get("merchant_type");
		if (isTruthyFilterParam(merchantTypeRaw)) {
			filter.put("merchant_type", String.valueOf(merchantTypeRaw).trim());
		}

		Object supplierRaw = requestParams.get("supplier_id");
		if (isTruthyFilterParam(supplierRaw)) {
			filter.put(
					"supplier_id",
					floatValPrefixToLongForSupplierFilter(String.valueOf(supplierRaw).trim()));
		}

		Object distributorRaw = requestParams.get("distributor_id");
		if (isTruthyFilterParam(distributorRaw)) {
			Long v = parseLongStrictOrSkip(distributorRaw);
			if (v != null) {
				filter.put("distributor_id", v);
			}
		}

		Object merchantRaw = requestParams.get("merchant_id");
		if (isTruthyFilterParam(merchantRaw)) {
			Long v = parseLongStrictOrSkip(merchantRaw);
			if (v != null) {
				filter.put("merchant_id", v);
			}
		}

		Object statusRaw = requestParams.get("statement_status");
		if (isTruthyFilterParam(statusRaw)) {
			String trimmed = String.valueOf(statusRaw).trim();
			if ("ready".equals(trimmed)) {
				filter.put("statement_status", List.of("ready", "confirmed"));
			} else {
				filter.put("statement_status", trimmed);
			}
		}

		Object startRaw = requestParams.get("start_time");
		if (isTruthyFilterParam(startRaw)) {
			Integer epoch = parseEpochSecondsOrNull(startRaw);
			if (epoch != null) {
				filter.put("start_time|gt", epoch);
			}
		}

		Object endRaw = requestParams.get("end_time");
		if (isTruthyFilterParam(endRaw)) {
			Integer epoch = parseEpochSecondsOrNull(endRaw);
			if (epoch != null) {
				filter.put("end_time|lt", epoch);
			}
		}

		String opType = operatorType == null ? "" : operatorType.trim();
		if ("distributor".equals(opType)) {
			filter.put("distributor_id", jwtDistributorIdOrNull != null ? jwtDistributorIdOrNull : 0L);
		} else if ("merchant".equals(opType)) {
			filter.put("merchant_id", jwtMerchantIdOrNull != null ? jwtMerchantIdOrNull : 0L);
		} else if ("supplier".equals(opType)) {
			filter.put("supplier_id", resolveSupplierIdByOperator(companyId, operatorId));
		}

		return filter;
	}

	private long resolveSupplierIdByOperator(long companyId, long operatorId) {
		if (operatorId == 0L) {
			return 0L;
		}
		Supplier row =
				supplierMapper.selectOne(
						new LambdaQueryWrapper<Supplier>()
								.eq(Supplier::getCompanyId, companyId)
								.eq(Supplier::getOperatorId, operatorId)
								.last("LIMIT 1"));
		if (row == null || row.getId() == null) {
			return 0L;
		}
		return row.getId();
	}

	private static long floatValPrefixToLongForSupplierFilter(String s) {
		if (s.isEmpty()) {
			return 0L;
		}
		int end = s.length();
		while (end > 0) {
			try {
				double d = Double.parseDouble(s.substring(0, end));
				if (Double.isFinite(d)) {
					return (long) d;
				}
			} catch (NumberFormatException ignored) {
				// continue shrinking prefix
			}
			end--;
		}
		return 0L;
	}

	private static Long parseLongStrictOrSkip(Object o) {
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isTruthyFilterParam(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (o instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		return true;
	}

	private static Integer parseEpochSecondsOrNull(Object raw) {
		try {
			String s = String.valueOf(raw).trim();
			long v = Long.parseLong(s);
			if (v > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			if (v < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			return (int) v;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
