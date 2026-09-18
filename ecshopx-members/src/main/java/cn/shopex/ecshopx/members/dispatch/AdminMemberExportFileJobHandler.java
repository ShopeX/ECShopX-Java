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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.members.service.export.AdminMemberCsvExportRunService;
import cn.shopex.ecshopx.members.service.export.AdminMemberExportJobContext;
import org.springframework.stereotype.Component;

@Component
public class AdminMemberExportFileJobHandler {

	private final AdminMemberCsvExportRunService adminMemberCsvExportRunService;

	public AdminMemberExportFileJobHandler(AdminMemberCsvExportRunService adminMemberCsvExportRunService) {
		this.adminMemberCsvExportRunService = adminMemberCsvExportRunService;
	}

	public void run(AdminMemberExportJobContext ctx) {
		adminMemberCsvExportRunService.runExport(ctx);
	}
}
