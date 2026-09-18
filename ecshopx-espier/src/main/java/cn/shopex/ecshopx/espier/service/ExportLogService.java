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

import cn.shopex.ecshopx.common.cron.EspierExportHistoryZipFileRemover;
import cn.shopex.ecshopx.espier.domain.ExportLog;
import cn.shopex.ecshopx.espier.mapper.ExportLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportLogService {

	private static final int BATCH_LIMIT = 100;

	private final ExportLogMapper exportLogMapper;
	private final EspierExportHistoryZipFileRemover historyZipFileRemover;

	/**
	 * 清理已完成导出超过一定时间的 zip 行及对象存储；单次最多处理 {@value #BATCH_LIMIT} 条。
	 *
	 * @return 本次成功从库中物理删除的行数
	 */
	public int scheduleDeleteHistoryFile() {
		long nowSec = Instant.now().getEpochSecond();
		long time = nowSec - 3600L * 3L;
		LambdaQueryWrapper<ExportLog> wrapper = new LambdaQueryWrapper<ExportLog>()
				.isNotNull(ExportLog::getFinishTime)
				.le(ExportLog::getFinishTime, time)
				.last("LIMIT " + BATCH_LIMIT);
		List<ExportLog> rows = exportLogMapper.selectList(wrapper);
		if (rows == null || rows.isEmpty()) {
			return 0;
		}
		int processed = 0;
		for (ExportLog exportLog : rows) {
			String fileName = exportLog.getFileName() == null ? "" : exportLog.getFileName();
			String objectKey = "export/zip/" + fileName;
			historyZipFileRemover.removeExportHistoryZipObjectKey(objectKey);
			exportLogMapper.deleteById(exportLog.getLogId());
			processed++;
		}
		return processed;
	}
}
