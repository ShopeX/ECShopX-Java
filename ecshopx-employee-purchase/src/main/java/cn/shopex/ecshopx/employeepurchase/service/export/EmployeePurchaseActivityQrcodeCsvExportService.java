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

package cn.shopex.ecshopx.employeepurchase.service.export;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.service.dto.ActivityAdminExportQuery;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseActivityQrcodeCsvExportService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(CN);
	private static final String EXPORT_TYPE = "employee_purchase_activity_qrcode";

	private static final LinkedHashMap<String, String> TITLE_HEADERS = new LinkedHashMap<>();

	static {
		TITLE_HEADERS.put("enterprise_name", "企业名称");
		TITLE_HEADERS.put("enterprise_sn", "企业编码");
		TITLE_HEADERS.put("passphrase_code", "企业口令码");
		TITLE_HEADERS.put("participate_quota", "可参与名额");
		TITLE_HEADERS.put("passphrase_limitfee", "口令码额度(元)");
		TITLE_HEADERS.put("qrcode_url", "企业小程序码下载地址");
	}

	private final EmployeePurchaseActivityQrcodeExportRowBuilder rowBuilder;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public EmployeePurchaseActivityQrcodeCsvExportService(
			EmployeePurchaseActivityQrcodeExportRowBuilder rowBuilder,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.rowBuilder = rowBuilder;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(ActivityAdminExportQuery query) {
		List<Map<String, String>> rows =
				rowBuilder.buildRows(query.companyId(), query.activityId(), query.distributorId());
		if (rows.isEmpty()) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		String fileBase =
				FILE_TS.format(ZonedDateTime.now(CN)) + "_activity_" + query.activityId() + "_qrcode";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBase, TITLE_HEADERS, rows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			return;
		}
		long finishSec = Instant.now().getEpochSecond();
		exportLogCreateService.createFinishLog(
				query.companyId(),
				query.operatorId(),
				EXPORT_TYPE,
				uploaded.get("filename"),
				uploaded.get("url"),
				finishSec);
	}

	public void validateExportable(ActivityAdminExportQuery query) {
		List<Map<String, String>> rows =
				rowBuilder.buildRows(query.companyId(), query.activityId(), query.distributorId());
		if (rows.isEmpty()) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
	}
}
