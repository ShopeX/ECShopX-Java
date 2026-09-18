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

package cn.shopex.ecshopx.chinaumspay.dispatch;

import cn.shopex.ecshopx.common.dispatch.ExportFileJobTypeHandler;
import cn.shopex.ecshopx.chinaumspay.service.divisiondetail.ChinaumsDivisionDetailExportFileJobHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ChinaumsDivisionDetailExportFileJobTypeHandler implements ExportFileJobTypeHandler {

	private final ChinaumsDivisionDetailExportFileJobHandler jobHandler;

	public ChinaumsDivisionDetailExportFileJobTypeHandler(ChinaumsDivisionDetailExportFileJobHandler jobHandler) {
		this.jobHandler = jobHandler;
	}

	@Override
	public String exportType() {
		return ChinaumsDivisionListExportFileJobTypes.TYPE_CHINAUMS_DIVISION_DETAIL;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		jobHandler.run(ChinaumsDivisionDetailExportFileJobPayloadSupport.contextFromPayload(payload));
	}
}
