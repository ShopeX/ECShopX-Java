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

package cn.shopex.ecshopx.espier.service;

import cn.shopex.ecshopx.espier.domain.ExportLog;
import cn.shopex.ecshopx.espier.mapper.ExportLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExportLogListService {

	private static final ZoneId ASIA_SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FINISH_DATE_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ExportLogMapper exportLogMapper;

	public ExportLogListService(ExportLogMapper exportLogMapper) {
		this.exportLogMapper = exportLogMapper;
	}

	public Map<String, Object> getExportLogList(
			long companyId,
			long operatorId,
			String operatorType,
			String exportTypeRaw,
			int page,
			int pageSize) {
		String ot = operatorType == null ? "" : operatorType.trim();
		long supplierId = "supplier".equalsIgnoreCase(ot) ? operatorId : 0L;

		LambdaQueryWrapper<ExportLog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ExportLog::getCompanyId, companyId);
		wrapper.eq(ExportLog::getOperatorId, operatorId);
		wrapper.eq(ExportLog::getSupplierId, supplierId);
		if (exportTypeRaw == null) {
			wrapper.isNull(ExportLog::getExportType);
		} else {
			wrapper.eq(ExportLog::getExportType, exportTypeRaw);
		}

		long total = exportLogMapper.selectCount(wrapper);
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("total_count", total);
		if (total == 0) {
			res.put("list", List.of());
			return res;
		}

		wrapper.orderByDesc(ExportLog::getCreated);
		Page<ExportLog> p = new Page<>(page, pageSize, false);
		exportLogMapper.selectPage(p, wrapper);
		res.put("list", p.getRecords().stream().map(this::toListRow).toList());
		return res;
	}

	private Map<String, Object> toListRow(ExportLog e) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("log_id", e.getLogId());
		row.put("company_id", e.getCompanyId());
		row.put("file_name", e.getFileName());
		row.put("file_url", e.getFileUrl());
		row.put("export_type", e.getExportType());
		row.put("handle_status", e.getHandleStatus());
		row.put("error_msg", e.getErrorMsg());
		row.put("finish_time", e.getFinishTime());
		if (e.getFinishTime() == null) {
			row.put("finish_date", null);
		} else {
			row.put(
					"finish_date",
					Instant.ofEpochSecond(e.getFinishTime())
							.atZone(ASIA_SHANGHAI)
							.format(FINISH_DATE_FMT));
		}
		row.put("distributor_id", e.getDistributorId());
		row.put("operator_id", e.getOperatorId());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		row.put("merchant_id", e.getMerchantId());
		row.put("supplier_id", e.getSupplierId());
		return row;
	}
}
