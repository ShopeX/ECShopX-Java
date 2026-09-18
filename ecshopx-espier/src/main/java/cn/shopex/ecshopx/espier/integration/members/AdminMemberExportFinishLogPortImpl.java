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

package cn.shopex.ecshopx.espier.integration.members;

import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.members.service.export.port.AdminMemberExportFinishLogPort;
import org.springframework.stereotype.Service;

@Service
public class AdminMemberExportFinishLogPortImpl implements AdminMemberExportFinishLogPort {

	private final ExportLogCreateService exportLogCreateService;

	public AdminMemberExportFinishLogPortImpl(ExportLogCreateService exportLogCreateService) {
		this.exportLogCreateService = exportLogCreateService;
	}

	@Override
	public void createFinishLog(
			long companyId,
			long operatorId,
			long merchantId,
			long supplierId,
			String exportType,
			String fileName,
			String fileUrl,
			long finishTimeEpochSeconds) {
		exportLogCreateService.createFinishLog(
				companyId, operatorId, merchantId, supplierId, exportType, fileName, fileUrl, finishTimeEpochSeconds);
	}
}
