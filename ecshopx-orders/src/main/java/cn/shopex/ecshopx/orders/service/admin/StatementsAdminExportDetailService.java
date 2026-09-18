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

import cn.shopex.ecshopx.common.dispatch.StatementDetailsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.Statements;
import cn.shopex.ecshopx.orders.mapper.StatementsMapper;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StatementsAdminExportDetailService {

	private final StatementsMapper statementsMapper;
	private final SupplierMapper supplierMapper;
	private final StatementDetailsExportFileJobDispatchPublisher statementDetailsExportFileJobDispatchPublisher;

	public StatementsAdminExportDetailService(
			StatementsMapper statementsMapper,
			SupplierMapper supplierMapper,
			StatementDetailsExportFileJobDispatchPublisher statementDetailsExportFileJobDispatchPublisher) {
		this.statementsMapper = statementsMapper;
		this.supplierMapper = supplierMapper;
		this.statementDetailsExportFileJobDispatchPublisher = statementDetailsExportFileJobDispatchPublisher;
	}

	public void exportDetail(
			long companyId,
			long operatorId,
			String operatorType,
			Long distributorIdOrNull,
			Long merchantIdOrNull,
			Map<String, Object> mergedParams) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		Object statementIdRaw = mergedParams.get("statement_id");
		if (statementIdLooksPresent(statementIdRaw)) {
			filter.put("statement_id", Long.parseLong(String.valueOf(statementIdRaw).trim()));
		}

		Object startRaw = mergedParams.get("start_time");
		if (filterParamLooksPresent(startRaw)) {
			Integer epoch = parseEpochSecondsOrNull(startRaw);
			if (epoch != null) {
				filter.put("created|gt", epoch);
			}
		}

		Object endRaw = mergedParams.get("end_time");
		if (filterParamLooksPresent(endRaw)) {
			Integer epoch = parseEpochSecondsOrNull(endRaw);
			if (epoch != null) {
				filter.put("created|lt", epoch);
			}
		}

		String opType = operatorType == null ? "" : operatorType.trim();
		if ("distributor".equals(opType)) {
			filter.put("distributor_id", distributorIdOrNull != null ? distributorIdOrNull : 0L);
		} else if ("merchant".equals(opType)) {
			filter.put("merchant_id", merchantIdOrNull != null ? merchantIdOrNull : 0L);
		} else if ("supplier".equals(opType)) {
			filter.put("supplier_id", resolveSupplierIdByOperator(companyId, operatorId));
		}

		if (!filter.containsKey("statement_id")) {
			filter.put("merchant_type", "distributor");
		} else {
			Object sid = filter.get("statement_id");
			Statements row =
					statementsMapper.selectOne(
							new LambdaQueryWrapper<Statements>()
									.eq(Statements::getCompanyId, companyId)
									.eq(Statements::getId, sid));
			String merchantType =
					(row != null && StringUtils.hasText(row.getMerchantType())) ? row.getMerchantType() : "distributor";
			filter.put("merchant_type", merchantType);
		}

		statementDetailsExportFileJobDispatchPublisher.enqueueDetailExport(
				companyId, operatorId, new LinkedHashMap<>(filter));
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

	private static boolean filterParamLooksPresent(Object o) {
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

	private static boolean statementIdLooksPresent(Object o) {
		if (!filterParamLooksPresent(o)) {
			return false;
		}
		try {
			long v = Long.parseLong(String.valueOf(o).trim());
			return v > 0;
		} catch (NumberFormatException e) {
			return false;
		}
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
