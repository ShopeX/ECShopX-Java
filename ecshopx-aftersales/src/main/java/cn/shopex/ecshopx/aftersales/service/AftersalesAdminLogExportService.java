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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.aftersales.dto.AftersalesLogExportQuery;
import cn.shopex.ecshopx.common.dispatch.AftersaleRecordCountExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class AftersalesAdminLogExportService {

	private final AftersalesAdminListService aftersalesAdminListService;
	private final AftersaleRecordCountExportFileJobDispatchPublisher aftersaleRecordCountExportFileJobDispatchPublisher;

	public AftersalesAdminLogExportService(
			AftersalesAdminListService aftersalesAdminListService,
			AftersaleRecordCountExportFileJobDispatchPublisher aftersaleRecordCountExportFileJobDispatchPublisher) {
		this.aftersalesAdminListService = aftersalesAdminListService;
		this.aftersaleRecordCountExportFileJobDispatchPublisher = aftersaleRecordCountExportFileJobDispatchPublisher;
	}

	public void logExport(HttpServletRequest request, AftersalesLogExportQuery q) {
		long count = aftersalesAdminListService.countForLogExport(q, request);
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (count > 15000) {
			throw new ResourceException("导出有误，最高导出15000条数据");
		}
		LinkedHashMap<String, Object> sqlFilter = aftersalesAdminListService.buildLogExportSqlFilter(q, request);
		if (sqlFilter == null) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		AftersalesAdminListService.AdminOperatorSnapshot op = aftersalesAdminListService.readAdminOperatorSnapshot(request);
		aftersaleRecordCountExportFileJobDispatchPublisher.publish(
				op.companyId(), op.operatorId(), new LinkedHashMap<>(sqlFilter));
	}
}
