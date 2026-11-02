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

package cn.shopex.ecshopx.espier.dispatch;

import cn.shopex.ecshopx.common.dispatch.ExportFileJobTypeHandler;
import cn.shopex.ecshopx.companys.service.posaccount.PosStoreAccountExportService;
import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PosStoreAccountExportFileJobTypeHandler implements ExportFileJobTypeHandler {

	private static final Logger log = LoggerFactory.getLogger(PosStoreAccountExportFileJobTypeHandler.class);
	private static final DateTimeFormatter TS =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Shanghai"));

	private final PosStoreAccountExportService exportService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public PosStoreAccountExportFileJobTypeHandler(
			PosStoreAccountExportService exportService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.exportService = exportService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	@Override
	public String exportType() {
		return PosStoreAccountExportService.EXPORT_TYPE;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		try {
			List<Map<String, String>> rows = exportService.csvRows(payload);
			LinkedHashMap<String, String> headers = new LinkedHashMap<>();
			headers.put("name", "姓名");
			headers.put("employee_no", "员工号");
			headers.put("email", "邮箱");
			headers.put("mobile", "手机号");
			headers.put("password", "密码");
			headers.put("role", "角色");
			headers.put("shop_code", "门店编码");
			headers.put("shop_name", "门店");
			headers.put("status", "状态");
			String base = "pos_store_account_" + TS.format(Instant.now());
			Map<String, String> file = exportCsvFileService.exportCsv(base, headers, rows);
			if (file == null || file.isEmpty()) {
				return;
			}
			long operatorId = 0L;
			Object rawOp = payload.get("operator_id");
			if (rawOp instanceof Number n) {
				operatorId = n.longValue();
			}
			long companyId = 0L;
			Object rawCo = payload.get("company_id");
			if (rawCo instanceof Number n) {
				companyId = n.longValue();
			}
			exportLogCreateService.createFinishLog(
					companyId,
					operatorId,
					PosStoreAccountExportService.EXPORT_TYPE,
					file.get("filename"),
					file.get("url"),
					Instant.now().getEpochSecond());
		} catch (RuntimeException e) {
			log.debug("pos store account export job failed: {}", e.toString());
			throw e;
		}
	}
}
