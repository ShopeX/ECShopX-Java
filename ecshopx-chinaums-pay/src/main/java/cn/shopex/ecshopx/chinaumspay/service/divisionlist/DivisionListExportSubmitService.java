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

package cn.shopex.ecshopx.chinaumspay.service.divisionlist;

import cn.shopex.ecshopx.chinaumspay.dispatch.ChinaumsDivisionListExportFileJobPayloadSupport;
import cn.shopex.ecshopx.common.dispatch.ChinaumsDivisionListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DivisionListExportSubmitService {

	private final ChinaumsDivisionListExportQueryService queryService;
	private final ChinaumsDivisionListExportFileJobDispatchPublisher dispatchPublisher;
	private final DivisionListCsvExportService divisionListCsvExportService;
	private final boolean syncInline;

	public DivisionListExportSubmitService(
			ChinaumsDivisionListExportQueryService queryService,
			ChinaumsDivisionListExportFileJobDispatchPublisher dispatchPublisher,
			DivisionListCsvExportService divisionListCsvExportService,
			@Value("${chinaums.cron.division-list-export.sync-inline:false}") boolean syncInline) {
		this.queryService = queryService;
		this.dispatchPublisher = dispatchPublisher;
		this.divisionListCsvExportService = divisionListCsvExportService;
		this.syncInline = syncInline;
	}

	public void submit(DivisionListExportFilter filter, long operatorId) {
		int count = queryService.countByFilter(filter);
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (syncInline) {
			divisionListCsvExportService.runExport(new DivisionListExportContext(filter.getCompanyId(), operatorId, filter));
			return;
		}
		dispatchPublisher.enqueueChinaumsDivisionListExport(
				filter.getCompanyId(), operatorId, ChinaumsDivisionListExportFileJobPayloadSupport.filterToMap(filter));
	}
}
