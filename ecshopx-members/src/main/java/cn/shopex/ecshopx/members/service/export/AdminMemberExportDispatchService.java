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

package cn.shopex.ecshopx.members.service.export;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.members.dispatch.AdminMemberExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberBatchOperatingMemberQueryFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberExportDispatchService {

	private final AdminMemberExportFilterAssembler adminMemberExportFilterAssembler;
	private final AdminMemberExportFileJobDispatchPublisher adminMemberExportFileJobDispatchPublisher;
	private final OperatorsQueryService operatorsQueryService;

	public AdminMemberExportDispatchService(
			AdminMemberExportFilterAssembler adminMemberExportFilterAssembler,
			AdminMemberExportFileJobDispatchPublisher adminMemberExportFileJobDispatchPublisher,
			OperatorsQueryService operatorsQueryService) {
		this.adminMemberExportFilterAssembler = adminMemberExportFilterAssembler;
		this.adminMemberExportFileJobDispatchPublisher = adminMemberExportFileJobDispatchPublisher;
		this.operatorsQueryService = operatorsQueryService;
	}

	public Map<String, Object> exportMemberData(
			HttpServletRequest request, Map<String, Object> merged, Map<?, ?> jwtMap) {
		long companyId = parseCompanyId(jwtMap);
		boolean datapassBlock = resolveDatapassBlock(request);
		long operatorId = parseOperatorId(jwtMap);

		AdminMemberBatchOperatingMemberQueryFilter built =
				adminMemberExportFilterAssembler.assemble(companyId, merged, jwtMap);

		long supplierId = 0L;
		if (operatorId > 0L) {
			Map<String, Object> opFilter = new LinkedHashMap<>();
			opFilter.put("company_id", companyId);
			opFilter.put("operator_id", operatorId);
			opFilter.put("operator_type", "supplier");
			Map<String, Object> opInfo = operatorsQueryService.getInfo(opFilter);
			if (opInfo != null && !opInfo.isEmpty()) {
				supplierId = operatorId;
			}
		}

		AdminMemberExportJobContext ctx =
				new AdminMemberExportJobContext(companyId, operatorId, supplierId, 0L, datapassBlock, built);
		adminMemberExportFileJobDispatchPublisher.enqueueAdminMemberExport(ctx);
		return Map.of("status", Boolean.TRUE);
	}

	private static long parseCompanyId(Map<?, ?> jwtMap) {
		Object o = jwtMap.get("company_id");
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static long parseOperatorId(Map<?, ?> jwtMap) {
		Object o = jwtMap.get("operator_id");
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static boolean resolveDatapassBlock(HttpServletRequest request) {
		Object attr = request.getAttribute("x-datapass-block");
		String raw = null;
		if (attr != null) {
			raw = String.valueOf(attr).trim();
		} else {
			String p = request.getParameter("x-datapass-block");
			if (p != null) {
				raw = p.trim();
			}
		}
		if (raw == null || !StringUtils.hasText(raw)) {
			return false;
		}
		if ("0".equals(raw) || "false".equalsIgnoreCase(raw)) {
			return false;
		}
		return true;
	}
}
