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

package cn.shopex.ecshopx.orders.service.offline.export;

import cn.shopex.ecshopx.common.dispatch.OfflinePaymentExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.service.offline.OfflinePaymentAdminQueryFilterBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OfflinePaymentExportService {

	private final OfflinePaymentAdminQueryFilterBuilder offlinePaymentAdminQueryFilterBuilder;
	private final OfflinePaymentMapper offlinePaymentMapper;
	private final OfflinePaymentExportFileJobDispatchPublisher offlinePaymentExportFileJobDispatchPublisher;

	public OfflinePaymentExportService(
			OfflinePaymentAdminQueryFilterBuilder offlinePaymentAdminQueryFilterBuilder,
			OfflinePaymentMapper offlinePaymentMapper,
			OfflinePaymentExportFileJobDispatchPublisher offlinePaymentExportFileJobDispatchPublisher) {
		this.offlinePaymentAdminQueryFilterBuilder = offlinePaymentAdminQueryFilterBuilder;
		this.offlinePaymentMapper = offlinePaymentMapper;
		this.offlinePaymentExportFileJobDispatchPublisher = offlinePaymentExportFileJobDispatchPublisher;
	}

	public void exportData(long companyId, long operatorId, Map<String, Object> params) {
		LinkedHashMap<String, Object> filter = offlinePaymentAdminQueryFilterBuilder.buildFilter(companyId, params);

		long count = offlinePaymentMapper.selectCount(OfflinePaymentExportQuerySupport.toWrapper(filter));
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (count > 15000) {
			throw new ResourceException("导出有误，最高导出15000条数据");
		}
		offlinePaymentExportFileJobDispatchPublisher.enqueueOfflinePaymentExport(companyId, operatorId, filter);
	}
}
