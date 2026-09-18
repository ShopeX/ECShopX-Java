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

import cn.shopex.ecshopx.aftersales.dto.RefundLogExportQuery;
import cn.shopex.ecshopx.common.dispatch.RefundRecordCountExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class AftersalesRefundLogExportService {

	private final AftersalesRefundListService aftersalesRefundListService;
	private final RefundRecordCountExportFileJobDispatchPublisher refundRecordCountExportFileJobDispatchPublisher;

	public AftersalesRefundLogExportService(
			AftersalesRefundListService aftersalesRefundListService,
			RefundRecordCountExportFileJobDispatchPublisher refundRecordCountExportFileJobDispatchPublisher) {
		this.aftersalesRefundListService = aftersalesRefundListService;
		this.refundRecordCountExportFileJobDispatchPublisher = refundRecordCountExportFileJobDispatchPublisher;
	}

	public void logExport(HttpServletRequest request, RefundLogExportQuery q) {
		LinkedHashMap<String, Object> filter = aftersalesRefundListService.buildRefundLogExportFilter(q, request);
		long companyId = longVal(filter.get("company_id"));
		long count = aftersalesRefundListService.countByFilterMapForRefundLogExport(filter, companyId);
		if (count <= 0L) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (count > 15000L) {
			throw new ResourceException("导出有误，最高导出15000条数据");
		}
		AftersalesRefundListService.RefundLogExportOperator op =
				aftersalesRefundListService.readRefundLogExportOperator(request);
		refundRecordCountExportFileJobDispatchPublisher.publish(
				op.companyId(), op.operatorId(), new LinkedHashMap<>(filter));
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
