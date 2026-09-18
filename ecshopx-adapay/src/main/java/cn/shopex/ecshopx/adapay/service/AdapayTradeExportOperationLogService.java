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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayTradeExportOperationLogService {

	private final AdapayDealerOperatorResolveHelper adapayDealerOperatorResolveHelper;
	private final AdapayOperationLogRecordPort adapayOperationLogRecordPort;
	private final DistributorListQueryService distributorListQueryService;
	private final OperatorsQueryService operatorsQueryService;

	public AdapayTradeExportOperationLogService(
			AdapayDealerOperatorResolveHelper adapayDealerOperatorResolveHelper,
			AdapayOperationLogRecordPort adapayOperationLogRecordPort,
			DistributorListQueryService distributorListQueryService,
			OperatorsQueryService operatorsQueryService) {
		this.adapayDealerOperatorResolveHelper = adapayDealerOperatorResolveHelper;
		this.adapayOperationLogRecordPort = adapayOperationLogRecordPort;
		this.distributorListQueryService = distributorListQueryService;
		this.operatorsQueryService = operatorsQueryService;
	}

	public void recordTradeExportRequest(long companyId, Map<String, Object> jwtMap) {
		String operatorType = str(jwtMap.get("operator_type")).trim().toLowerCase(Locale.ROOT);
		long jwtOperatorId = parseLong(jwtMap.get("operator_id"));
		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("company_id", companyId);

		String sourceType;
		long relId;

		if ("distributor".equals(operatorType)) {
			long distributorId = parseLong(jwtMap.get("distributor_id"));
			List<Distributor> ds = distributorListQueryService.listByIdsAndCompany(companyId, List.of(distributorId));
			if (ds.isEmpty()) {
				throw new ResourceException("导出有误,暂无数据导出");
			}
			String name = ds.get(0).getName() != null ? ds.get(0).getName() : "";
			logParams.put("name", name);
			relId = distributorId;
			sourceType = "distributor";
		} else {
			relId = adapayDealerOperatorResolveHelper.resolveRelOperatorIdForLogOrThrow(jwtOperatorId);
			Map<String, Object> opInfo =
					operatorsQueryService.getInfo(Map.of("company_id", companyId, "operator_id", jwtOperatorId));
			if (opInfo == null || opInfo.isEmpty()) {
				throw new ResourceException("操作者信息为空");
			}
			String username = str(opInfo.get("username"));
			String mobile = str(opInfo.get("mobile"));
			String name = StringUtils.hasText(username) ? username : mobile;
			if (!StringUtils.hasText(name)) {
				throw new ResourceException("操作者信息为空");
			}
			logParams.put("name", name);
			if ("dealer".equals(operatorType)) {
				sourceType = "dealer";
			} else {
				sourceType = "merchant";
			}
		}

		adapayOperationLogRecordPort.logRecord(logParams, relId, "trade/exportdata", sourceType, jwtOperatorId);
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long parseLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
