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
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExportLogCreateService {

	private static final Logger log = LoggerFactory.getLogger(ExportLogCreateService.class);

	private final ExportLogMapper exportLogMapper;

	public ExportLogCreateService(ExportLogMapper exportLogMapper) {
		this.exportLogMapper = exportLogMapper;
	}

	public void createFinishLog(long companyId, long operatorId, String exportType, String fileName, String fileUrl,
			long finishTimeEpochSeconds) {
		createFinishLog(companyId, operatorId, 0L, exportType, fileName, fileUrl, finishTimeEpochSeconds);
	}

	public void createFinishLog(long companyId, long operatorId, long merchantId, String exportType, String fileName,
			String fileUrl, long finishTimeEpochSeconds) {
		createFinishLog(companyId, operatorId, merchantId, 0L, exportType, fileName, fileUrl, finishTimeEpochSeconds);
	}

	public void createFinishLog(long companyId, long operatorId, long merchantId, long supplierId, String exportType,
			String fileName, String fileUrl, long finishTimeEpochSeconds) {
		try {
			int now = (int) Instant.now().getEpochSecond();
			ExportLog e = new ExportLog();
			e.setCompanyId(companyId);
			e.setOperatorId(operatorId);
			e.setMerchantId(merchantId);
			e.setSupplierId(supplierId);
			e.setExportType(exportType);
			e.setFileName(fileName);
			e.setFileUrl(fileUrl);
			e.setHandleStatus("finish");
			e.setFinishTime(finishTimeEpochSeconds);
			e.setCreated(now);
			e.setUpdated(now);
			exportLogMapper.insert(e);
		} catch (Exception ex) {
			log.debug("队列导出: 导出日志完成状态更新失败", ex);
		}
	}
}
