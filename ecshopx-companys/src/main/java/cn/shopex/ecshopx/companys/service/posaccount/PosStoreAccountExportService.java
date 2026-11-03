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

package cn.shopex.ecshopx.companys.service.posaccount;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.service.employee.EmployeeOrchestrationSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PosStoreAccountExportService {

	public static final String EXPORT_TYPE = "pos_store_account";

	private final PosStoreAccountQueryService queryService;
	private final PosStoreAccountPasswordService passwordService;
	private final DistributionDistributorSelfReadMapper shopMapper;
	private final PosStoreAccountExportDispatchPublisher publisher;

	public PosStoreAccountExportService(
			PosStoreAccountQueryService queryService,
			PosStoreAccountPasswordService passwordService,
			DistributionDistributorSelfReadMapper shopMapper,
			PosStoreAccountExportDispatchPublisher publisher) {
		this.queryService = queryService;
		this.passwordService = passwordService;
		this.shopMapper = shopMapper;
		this.publisher = publisher;
	}

	public Map<String, Object> enqueue(Map<String, Object> jwt, Map<String, Object> filters) {
		if (!PosStoreAccountScope.isAdmin(jwt)) {
			throw new ForbiddenException("仅超级管理员可导出账号");
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", EXPORT_TYPE);
		payload.put("company_id", PosStoreAccountScope.companyId(jwt));
		payload.put("operator_id", EmployeeOrchestrationSupport.longClaimOrNull(jwt, "operator_id", "operatorId"));
		payload.put("operator_type", EmployeeOrchestrationSupport.strClaim(jwt, "operator_type", "operatorType"));
		payload.put("distributor_ids", jwt.get("distributor_ids"));
		if (filters != null) {
			payload.put("keyword", filters.get("keyword"));
			payload.put("role", filters.get("role"));
			payload.put("distributor_id", filters.get("distributor_id"));
			payload.put("status", filters.get("status"));
		}
		publisher.enqueue(payload);
		return Map.of("status", true);
	}

	public List<Map<String, String>> csvRows(Map<String, Object> payload) {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", payload.get("company_id"));
		jwt.put("operator_type", payload.get("operator_type"));
		jwt.put("distributor_ids", payload.get("distributor_ids"));
		Long shopId = toLong(payload.get("distributor_id"));
		List<PosStoreAccount> rows =
				queryService.listAll(
						jwt,
						str(payload.get("keyword")),
						str(payload.get("role")),
						shopId,
						str(payload.get("status")));
		long companyId = PosStoreAccountScope.companyId(jwt);
		Map<Long, Map<String, Object>> shops = shops(companyId, rows);
		List<Map<String, String>> out = new ArrayList<>();
		for (PosStoreAccount row : rows) {
			Map<String, Object> shop = shops.get(row.getDistributorId());
			Map<String, String> csv = new LinkedHashMap<>();
			csv.put("name", blank(row.getName()));
			csv.put("employee_no", blank(row.getEmployeeNo()));
			csv.put("email", blank(row.getEmail()));
			csv.put("mobile", blank(row.getMobile()));
			csv.put("password", passwordService.decryptAlways(row.getPasswordCipher()));
			csv.put("role", PosStoreAccountRules.roleLabel(row.getRole()));
			csv.put("shop_code", shop == null ? "" : blank(shop.get("shop_code")));
			csv.put("shop_name", shop == null ? "" : blank(shop.get("name")));
			csv.put("status", PosStoreAccountRules.statusLabel(row.getStatus()));
			out.add(csv);
		}
		return out;
	}

	private Map<Long, Map<String, Object>> shops(long companyId, List<PosStoreAccount> rows) {
		List<Long> ids = new ArrayList<>();
		for (PosStoreAccount row : rows) {
			if (row.getDistributorId() != null && row.getDistributorId() > 0) {
				ids.add(row.getDistributorId());
			}
		}
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		if (ids.isEmpty()) {
			return out;
		}
		List<Map<String, Object>> found = shopMapper.listDistributorNamesByCompanyAndIds(companyId, ids);
		if (found == null) {
			return out;
		}
		for (Map<String, Object> shop : found) {
			Object id = shop.get("distributor_id");
			if (id instanceof Number n) {
				out.put(n.longValue(), shop);
			}
		}
		return out;
	}

	private static String str(Object v) {
		return v == null ? null : v.toString();
	}

	private static String blank(Object v) {
		return v == null ? "" : v.toString();
	}

	private static Long toLong(Object v) {
		if (v instanceof Number n && n.longValue() > 0) {
			return n.longValue();
		}
		if (v == null || v.toString().isBlank()) {
			return null;
		}
		try {
			long n = Long.parseLong(v.toString().trim());
			return n > 0 ? n : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
