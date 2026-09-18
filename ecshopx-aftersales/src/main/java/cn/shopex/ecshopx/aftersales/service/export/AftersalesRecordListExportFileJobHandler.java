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

package cn.shopex.ecshopx.aftersales.service.export;

import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AftersalesRecordListExportFileJobHandler {

	public static final String EXPORT_FILE_JOB_TYPE_AFTERSALE_RECORD_COUNT = "aftersale_record_count";

	private static final Logger log = LoggerFactory.getLogger(AftersalesRecordListExportFileJobHandler.class);

	private final AftersalesMapper aftersalesMapper;
	private final AftersalesRecordListCsvExportService csvExportService;
	private final ExportLogCreateService exportLogCreateService;
	private final OperatorsMapper operatorsMapper;

	public AftersalesRecordListExportFileJobHandler(
			AftersalesMapper aftersalesMapper,
			AftersalesRecordListCsvExportService csvExportService,
			ExportLogCreateService exportLogCreateService,
			OperatorsMapper operatorsMapper) {
		this.aftersalesMapper = aftersalesMapper;
		this.csvExportService = csvExportService;
		this.exportLogCreateService = exportLogCreateService;
		this.operatorsMapper = operatorsMapper;
	}

	public void run(AftersalesRecordListExportJobContext ctx) {
		LinkedHashMap<String, Object> work = new LinkedHashMap<>(ctx.filter());
		work.put("company_id", ctx.companyId());

		long supplierIdForLog = 0L;
		if (ctx.operatorId() > 0L) {
			Long ct =
					operatorsMapper.selectCount(
							new LambdaQueryWrapper<Operators>()
									.eq(Operators::getCompanyId, ctx.companyId())
									.eq(Operators::getOperatorId, ctx.operatorId())
									.eq(Operators::getOperatorType, "supplier"));
			if (ct != null && ct > 0L) {
				supplierIdForLog = ctx.operatorId();
				applySupplierFilterPatch(work, ctx.operatorId());
			}
		}

		long jobCount = aftersalesMapper.countAdminList(work);
		if (jobCount <= 0L) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}

		Optional<Map<String, String>> upload = csvExportService.runExport(work, ctx.companyId(), ctx.operatorId());
		if (upload.isEmpty()) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		Map<String, String> m = upload.get();
		String fileUrl = m.get("url");
		if (!StringUtils.hasText(fileUrl)) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		String fileName = m.get("filename");

		long merchantIdForLog = 0L;
		Object mid = work.get("merchant_id");
		if (mid != null && StringUtils.hasText(String.valueOf(mid))) {
			merchantIdForLog = longVal(mid);
		}

		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				merchantIdForLog,
				supplierIdForLog,
				EXPORT_FILE_JOB_TYPE_AFTERSALE_RECORD_COUNT,
				fileName != null ? fileName : "",
				fileUrl,
				Instant.now().getEpochSecond());
	}

	private static void applySupplierFilterPatch(LinkedHashMap<String, Object> filter, long operatorId) {
		filter.put("supplier_id", Long.valueOf(operatorId));
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
