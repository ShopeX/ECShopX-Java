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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.dispatch.BspayWithdrawDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.bspay.service.export.BspayWithdrawDataExportContext;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BspayWithdrawExportDataService {

	private final WithdrawApplyExportQueryService withdrawApplyExportQueryService;
	private final BspayWithdrawDataExportFileJobDispatchPublisher bspayWithdrawDataExportFileJobDispatchPublisher;
	private final WithdrawApplyAdminFilterBuilder withdrawApplyAdminFilterBuilder;

	public BspayWithdrawExportDataService(
			WithdrawApplyExportQueryService withdrawApplyExportQueryService,
			BspayWithdrawDataExportFileJobDispatchPublisher bspayWithdrawDataExportFileJobDispatchPublisher,
			WithdrawApplyAdminFilterBuilder withdrawApplyAdminFilterBuilder) {
		this.withdrawApplyExportQueryService = withdrawApplyExportQueryService;
		this.bspayWithdrawDataExportFileJobDispatchPublisher = bspayWithdrawDataExportFileJobDispatchPublisher;
		this.withdrawApplyAdminFilterBuilder = withdrawApplyAdminFilterBuilder;
	}

	public void exportWithdrawData(Map<String, Object> jwtMap, HttpServletRequest request) {
		Object rawCompany = jwtMap.get("company_id");
		if (rawCompany == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = parseLong(rawCompany);
		if (companyId <= 0) {
			throw new UnauthorizedException("未登录");
		}
		long jwtOperatorId = parseLong(jwtMap.get("operator_id"));

		LinkedHashMap<String, Object> filter =
				withdrawApplyAdminFilterBuilder.buildForExport(companyId, jwtOperatorId, jwtMap, request);

		long count = withdrawApplyExportQueryService.countWithdrawApplies(filter);
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (count > 15000) {
			throw new ResourceException("导出有误，当前导出数据为 " + count + " 条，最高导出 15000 条数据");
		}

		bspayWithdrawDataExportFileJobDispatchPublisher.enqueueBspayWithdrawDataExport(
				new BspayWithdrawDataExportContext(companyId, jwtOperatorId, new LinkedHashMap<>(filter)));
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
