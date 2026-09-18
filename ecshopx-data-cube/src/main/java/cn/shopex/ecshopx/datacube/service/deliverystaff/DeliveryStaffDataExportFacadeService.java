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

package cn.shopex.ecshopx.datacube.service.deliverystaff;

import cn.shopex.ecshopx.companys.service.deliverystaff.AdminDeliveryStaffDataExportFilter;
import cn.shopex.ecshopx.common.dispatch.DeliveryStaffDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.datacube.dispatch.DeliveryStaffDataExportFileJobPayloadSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeliveryStaffDataExportFacadeService {

	private final DeliveryStaffDataExportFileJobDispatchPublisher deliveryStaffDataExportFileJobDispatchPublisher;

	private final DeliveryStaffDataCsvExportService deliveryStaffDataCsvExportService;

	private final boolean syncInline;

	public DeliveryStaffDataExportFacadeService(
			DeliveryStaffDataExportFileJobDispatchPublisher deliveryStaffDataExportFileJobDispatchPublisher,
			DeliveryStaffDataCsvExportService deliveryStaffDataCsvExportService,
			@Value("${datacube.cron.delivery-staff-export.sync-inline:false}") boolean syncInline) {
		this.deliveryStaffDataExportFileJobDispatchPublisher = deliveryStaffDataExportFileJobDispatchPublisher;
		this.deliveryStaffDataCsvExportService = deliveryStaffDataCsvExportService;
		this.syncInline = syncInline;
	}

	public void submitExport(AdminDeliveryStaffDataExportFilter filter) {
		if (syncInline) {
			deliveryStaffDataCsvExportService.runExport(filter);
			return;
		}
		deliveryStaffDataExportFileJobDispatchPublisher.enqueueDeliveryStaffDataExport(
				filter.getCompanyId(),
				filter.getOperatorId(),
				DeliveryStaffDataExportFileJobPayloadSupport.filterToMap(filter));
	}
}
