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

package cn.shopex.ecshopx.point.service.export;

import cn.shopex.ecshopx.common.dispatch.MemberPointLogsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.point.service.PointMemberListService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Coordinates member point log exports: either enqueues a background job or runs the CSV export on the current thread.
 */
@Service
public class PointMemberLogExportOrchestratorService {

	private final PointMemberListService pointMemberListService;
	private final PointMemberLogCsvExportService pointMemberLogCsvExportService;
	private final MemberPointLogsExportFileJobDispatchPublisher memberPointLogsExportFileJobDispatchPublisher;
	private final OperatorsQueryService operatorsQueryService;

	public PointMemberLogExportOrchestratorService(
			PointMemberListService pointMemberListService,
			PointMemberLogCsvExportService pointMemberLogCsvExportService,
			MemberPointLogsExportFileJobDispatchPublisher memberPointLogsExportFileJobDispatchPublisher,
			OperatorsQueryService operatorsQueryService) {
		this.pointMemberListService = pointMemberListService;
		this.pointMemberLogCsvExportService = pointMemberLogCsvExportService;
		this.memberPointLogsExportFileJobDispatchPublisher = memberPointLogsExportFileJobDispatchPublisher;
		this.operatorsQueryService = operatorsQueryService;
	}

	public void export(long companyId, long operatorId, String operatorType,
			int page, int pageSize, long userIdParam, String mobile, String username, String name,
			Long dateBegin, Long dateEnd, boolean shouldQueue) {
		long total = pointMemberListService.countLocalPointMemberLogs(
				companyId, userIdParam, mobile, username, name, dateBegin, dateEnd);
		if (total <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		String normType = operatorType != null ? operatorType.trim().toLowerCase(Locale.ROOT) : "";
		boolean datapassBlock = "admin".equals(normType) || "staff".equals(normType);

		long supplierId = 0L;
		if (operatorId > 0) {
			Map<String, Object> filter = new LinkedHashMap<>();
			filter.put("company_id", companyId);
			filter.put("operator_id", operatorId);
			filter.put("operator_type", "supplier");
			Map<String, Object> info = operatorsQueryService.getInfo(filter);
			if (info != null && !info.isEmpty()) {
				supplierId = operatorId;
			}
		}

		PointMemberLogExportContext ctx = new PointMemberLogExportContext(
				companyId,
				operatorId,
				supplierId,
				userIdParam,
				mobile,
				username,
				name,
				dateBegin,
				dateEnd,
				datapassBlock);
		if (shouldQueue) {
			memberPointLogsExportFileJobDispatchPublisher.enqueueMemberPointLogsExport(ctx);
		} else {
			pointMemberLogCsvExportService.runExport(ctx);
		}
	}
}
