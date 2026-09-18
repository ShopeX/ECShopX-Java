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

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.service.UploadFileImportDataService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ImportDataJobHandler implements DispatchHandler {

	private final UploadFileImportDataService uploadFileImportDataService;

	public ImportDataJobHandler(UploadFileImportDataService uploadFileImportDataService) {
		this.uploadFileImportDataService = uploadFileImportDataService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Map<String, Object> uploadFileInfo = requireMap(payload, "upload_file_info");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> params = (List<Map<String, Object>>) payload.get("params");
		if (params == null) {
			throw new BadRequestException("导入任务缺少 params");
		}
		Map<Integer, String> column = parseColumn(requireMap(payload, "column"));
		Object sortObj = payload.get("sort");
		if (sortObj == null) {
			throw new BadRequestException("导入任务缺少 sort");
		}
		int sort = ((Number) sortObj).intValue();
		@SuppressWarnings("unchecked")
		List<String> exportHeaderTitleColumns = (List<String>) payload.get("export_header_title_columns");
		if (exportHeaderTitleColumns == null) {
			throw new BadRequestException("导入任务缺少 export_header_title_columns");
		}
		uploadFileImportDataService.handleImportData(
				uploadFileInfo, params, column, sort, exportHeaderTitleColumns);
	}

	private static Map<String, Object> requireMap(Map<String, Object> payload, String key) {
		Object v = payload.get(key);
		if (!(v instanceof Map<?, ?> m)) {
			throw new BadRequestException("导入任务缺少或类型错误: " + key);
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static Map<Integer, String> parseColumn(Map<String, Object> rawColumn) {
		Map<Integer, String> column = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : rawColumn.entrySet()) {
			int idx = Integer.parseInt(e.getKey());
			column.put(idx, e.getValue() == null ? "" : String.valueOf(e.getValue()));
		}
		return column;
	}
}
