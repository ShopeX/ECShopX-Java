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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.companys.domain.OperatorLogs;
import cn.shopex.ecshopx.companys.mapper.OperatorLogsMapper;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.service.MerchantQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperatorLogsQueryService {

	private final OperatorLogsMapper operatorLogsMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final EmployeeService employeeService;
	private final MerchantQueryService merchantQueryService;

	public OperatorLogsQueryService(
			OperatorLogsMapper operatorLogsMapper,
			OperatorsQueryService operatorsQueryService,
			EmployeeService employeeService,
			MerchantQueryService merchantQueryService) {
		this.operatorLogsMapper = operatorLogsMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.employeeService = employeeService;
		this.merchantQueryService = merchantQueryService;
	}

	public Map<String, Object> getCompanysLogs(
			long companyId,
			Long jwtMerchantId,
			String jwtOperatorType,
			int page,
			int pageSize,
			boolean listFilterHasNonemptyMerchantId) {
		LambdaQueryWrapper<OperatorLogs> w = new LambdaQueryWrapper<>();
		w.eq(OperatorLogs::getCompanyId, companyId);
		if ("merchant".equals(jwtOperatorType)) {
			if (jwtMerchantId == null) {
				w.isNull(OperatorLogs::getMerchantId);
			} else {
				w.eq(OperatorLogs::getMerchantId, jwtMerchantId);
			}
		}
		w.orderByDesc(OperatorLogs::getCreated);

		Page<OperatorLogs> mpPage = new Page<>(page, pageSize);
		operatorLogsMapper.selectPage(mpPage, w);

		List<Map<String, Object>> listOfRowMaps = new ArrayList<>();
		for (OperatorLogs row : mpPage.getRecords()) {
			LinkedHashMap<String, Object> rowMap = new LinkedHashMap<>();
			rowMap.put("log_id", row.getLogId());
			rowMap.put("company_id", row.getCompanyId());
			rowMap.put("operator_id", row.getOperatorId());
			rowMap.put("ip", row.getIp());
			rowMap.put("operator_name", row.getOperatorName());
			rowMap.put("created", row.getCreated());
			rowMap.put("log_type", row.getLogType());
			rowMap.put("merchant_id", row.getMerchantId());

			Map<String, Object> operator =
					operatorsQueryService.getInfo(
							Map.of("operator_id", (long) row.getOperatorId(), "company_id", companyId));
			if (operator != null && !operator.isEmpty()) {
				String opType =
						operator.get("operator_type") == null ? "" : operator.get("operator_type").toString();
				if ("admin".equals(opType)) {
					rowMap.put("username", "超级管理员");
				} else if ("staff".equals(opType)) {
					Map<String, Object> employee =
							employeeService.getInfoStaff(row.getOperatorId(), companyId);
					String u =
							employee == null || employee.isEmpty() || employee.get("username") == null
									? null
									: employee.get("username").toString();
					rowMap.put("username", u != null ? u : "");
				} else if ("merchant".equals(opType)) {
					if (listFilterHasNonemptyMerchantId) {
						rowMap.put("username", "超级管理员");
					} else {
						Long mid = toLong(operator.get("merchant_id"));
						if (mid == null || mid <= 0) {
							rowMap.put("username", "");
						} else {
							Merchant m = merchantQueryService.getInfo(companyId, mid, false);
							rowMap.put(
									"username",
									m == null || m.getMerchantName() == null ? "" : m.getMerchantName());
						}
					}
				} else {
					rowMap.put(
							"username",
							operator.get("username") == null ? "" : operator.get("username").toString());
				}
			}

			listOfRowMaps.add(rowMap);
		}

		int totalCount = Math.toIntExact(mpPage.getTotal());
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listOfRowMaps);
		return out;
	}

	private static Long toLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
}
