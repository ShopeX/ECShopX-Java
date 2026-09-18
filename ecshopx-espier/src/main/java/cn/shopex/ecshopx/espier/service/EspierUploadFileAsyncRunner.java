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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadExcelSheetReader;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileQueuedPayload;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadHeaderHandle;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler;
import cn.shopex.ecshopx.espier.service.upload.ImportDataJobDispatchPort;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EspierUploadFileAsyncRunner {

	private static final Logger log = LoggerFactory.getLogger(EspierUploadFileAsyncRunner.class);

	private final UploadeFileRepository uploadeFileRepository;
	private final EspierUploadFileHandlerRegistry registry;
	private final FileStorageService fileStorageService;
	private final ObjectMapper objectMapper;
	private final ImportDataJobDispatchPort importDataJobDispatchPort;

	public EspierUploadFileAsyncRunner(
			UploadeFileRepository uploadeFileRepository,
			EspierUploadFileHandlerRegistry registry,
			FileStorageService fileStorageService,
			ObjectMapper objectMapper,
			ImportDataJobDispatchPort importDataJobDispatchPort) {
		this.uploadeFileRepository = uploadeFileRepository;
		this.registry = registry;
		this.fileStorageService = fileStorageService;
		this.objectMapper = objectMapper;
		this.importDataJobDispatchPort = importDataJobDispatchPort;
	}

	public void runAfterCommit(EspierUploadFileQueuedPayload payload) {
		LinkedHashMap<String, Object> row = uploadeFileRepository.getInfoById(payload.getId());
		if (row.isEmpty()) {
			return;
		}
		row.put("operator_type", payload.getOperatorType());
		if (payload.getRelationId() > 0) {
			row.put("relation_id", payload.getRelationId());
		}
		runCore(row, payload.getStoragePath(), payload.getRequestFileType(), false);
	}

	public void runInline(Map<String, Object> persistedRow, String storagePath, String requestFileType) {
		runCore(new LinkedHashMap<>(persistedRow), storagePath, requestFileType, true);
	}

	private void runCore(Map<String, Object> data, String storagePath, String requestFileType, boolean executeSynchronously) {
		log.atDebug().log(
				"upload import maxMemory={}MB",
				Runtime.getRuntime().maxMemory() / (1024 * 1024));
		long id = toLong(data.get("id"));
		Map<String, Object> info = uploadeFileRepository.getInfoById(id);
		if (info.isEmpty()) {
			return;
		}
		if (!"wait".equals(String.valueOf(info.get("handle_status")))) {
			return;
		}
		uploadeFileRepository.updateOneBy(Map.of("id", id), Map.of("handle_status", "processing"));
		EspierUploadFileHandler handler = registry.requireHandler(requestFileType);
		if (!(handler instanceof EspierUploadTableHandler table)) {
			log.warn("No table handler for file_type={}, skip async import", requestFileType);
			uploadeFileRepository.updateStatusSimple(
					id,
					"finish",
					Map.of(
							"successLine",
							0,
							"errorLine",
							1,
							"errorlog",
							List.of(List.of("错误行数", "错误原因"), List.of("", "该文件类型不支持异步表格导入"))),
					0,
					System.currentTimeMillis() / 1000L);
			try {
				fileStorageService.delete("file", storagePath);
			} catch (Exception ex) {
				log.debug("delete storage path failed: {}", ex.toString());
			}
			handler.handleAfterInsertInRequestThread(
					toLong(data.get("company_id")),
					toLong(data.get("operator_id")),
					toLong(data.get("distributor_id")),
					toLong(data.get("supplier_id")),
					toLong(data.get("merchant_id")),
					data,
					storagePath,
					requestFileType);
			return;
		}
		byte[] bytes;
		try {
			bytes = fileStorageService.get("file", storagePath);
		} catch (Exception e) {
			log.error("read upload file failed id={}", id, e);
			storageReadFail(id, storagePath, table, data);
			return;
		}
		List<List<Object>> rows;
		try {
			rows = EspierUploadExcelSheetReader.readFirstSheet(bytes);
		} catch (IOException e) {
			headerFail(id, storagePath, table, data, e.getMessage());
			return;
		}
		if (rows.isEmpty()) {
			headerFail(id, storagePath, table, data, "空表格");
			return;
		}
		List<String> headerCells = new ArrayList<>();
		for (Object o : rows.get(0)) {
			headerCells.add(o == null ? "" : String.valueOf(o));
		}
		long companyId = toLong(data.get("company_id"));
		long relationId = toLong(data.get("relation_id"));
		Map<Integer, String> column;
		try {
			column = EspierUploadHeaderHandle.buildColumnMap(
					headerCells, table.getHeaderTitle(companyId, relationId));
		} catch (BadRequestException e) {
			headerFail(id, storagePath, table, data, e.getMessage());
			return;
		} catch (Exception e) {
			headerFail(id, storagePath, table, data, e.getMessage());
			return;
		}
		List<String> exportHeaderTitleColumns = buildExportHeaderTitles(headerCells);
		List<List<Object>> dataRows = new ArrayList<>(rows.subList(1, rows.size()));
		String operatorType =
				data.get("operator_type") == null ? "" : String.valueOf(data.get("operator_type")).trim();
		int excelRow = 2;
		List<Map<String, Object>> rowPayloads = new ArrayList<>();
		for (List<Object> row : dataRows) {
			if (!EspierUploadHeaderHandle.rowHasAnyValue(row)) {
				excelRow++;
				continue;
			}
			Map<String, Object> fileRowData = EspierUploadHeaderHandle.preRowHandle(column, row);
			fileRowData.put("__upload_file_id__", id);
			fileRowData.put("distributor_id", data.get("distributor_id"));
			fileRowData.put("relation_id", data.get("relation_id"));
			fileRowData.put("supplier_id", data.get("supplier_id"));
			fileRowData.put("operator_id", data.get("operator_id"));
			fileRowData.put("merchant_id", data.get("merchant_id"));
			fileRowData.put("__excel_row__", excelRow);
			fileRowData.put("__err_cells__", new ArrayList<>(row));
			rowPayloads.add(fileRowData);
			excelRow++;
		}

		LinkedHashMap<String, Object> uploadFileInfo = new LinkedHashMap<>(info);
		uploadFileInfo.putAll(data);
		uploadFileInfo.put("storage_path", storagePath);

		int numChunks = (rowPayloads.size() + 499) / 500;
		uploadeFileRepository.updateOneBy(Map.of("id", id), Map.of("left_job_num", numChunks));

		if (numChunks == 0) {
			persistEmptyTableImportResult(id, table, data, storagePath, requestFileType, companyId);
			return;
		}

		for (int k = 0; k < numChunks; k++) {
			int from = k * 500;
			int to = Math.min(from + 500, rowPayloads.size());
			List<Map<String, Object>> chunk = new ArrayList<>(rowPayloads.subList(from, to));
			int sort = numChunks - k;
			importDataJobDispatchPort.dispatchImportDataChunk(
					executeSynchronously, uploadFileInfo, chunk, column, sort, exportHeaderTitleColumns);
		}

		try {
			table.handleAfterInsertInRequestThread(
					companyId,
					toLong(data.get("operator_id")),
					toLong(data.get("distributor_id")),
					toLong(data.get("supplier_id")),
					toLong(data.get("merchant_id")),
					data,
					storagePath,
					requestFileType);
		} catch (Exception ex) {
			log.debug("afterInsert hook: {}", ex.toString());
		}
	}

	private void persistEmptyTableImportResult(
			long id,
			EspierUploadTableHandler table,
			Map<String, Object> data,
			String storagePath,
			String requestFileType,
			long companyId) {
		try {
			Map<String, Object> msg = new LinkedHashMap<>();
			msg.put("successLine", 0);
			msg.put("errorLine", 0);
			msg.put("errorlog", List.of());
			uploadeFileRepository.updateOneBy(
					Map.of("id", id),
					Map.of(
							"handle_status",
							"finish",
							"handle_message",
							objectMapper.writeValueAsString(msg),
							"handle_line_num",
							"0",
							"finish_time",
							System.currentTimeMillis() / 1000L,
							"updated",
							(int) (System.currentTimeMillis() / 1000L),
							"left_job_num",
							0));
		} catch (Exception e) {
			log.error("persist import result failed id={}", id, e);
		}
		try {
			fileStorageService.delete("file", storagePath);
		} catch (Exception ex) {
			log.debug("delete storage path failed: {}", ex.toString());
		}
		try {
			table.handleAfterInsertInRequestThread(
					companyId,
					toLong(data.get("operator_id")),
					toLong(data.get("distributor_id")),
					toLong(data.get("supplier_id")),
					toLong(data.get("merchant_id")),
					data,
					storagePath,
					requestFileType);
		} catch (Exception ex) {
			log.debug("afterInsert hook: {}", ex.toString());
		}
	}

	private static List<String> buildExportHeaderTitles(List<String> headerCells) {
		List<String> out = new ArrayList<>();
		for (String c : headerCells) {
			String normalized = EspierUploadHeaderHandle.normalizeHeaderCell(c);
			if (normalized.isEmpty()) {
				continue;
			}
			out.add(normalized);
		}
		return out;
	}

	private void storageReadFail(long id, String storagePath, EspierUploadTableHandler table, Map<String, Object> data) {
		try {
			List<List<Object>> errorlog = new ArrayList<>();
			errorlog.add(List.of("错误行数", "错误原因"));
			errorlog.add(List.of("", "读取上传文件失败"));
			Map<String, Object> hm = new LinkedHashMap<>();
			hm.put("successLine", 0);
			hm.put("errorLine", 1);
			hm.put("errorlog", errorlog);
			uploadeFileRepository.updateOneBy(
					Map.of("id", id),
					Map.of(
							"handle_status",
							"finish",
							"handle_message",
							objectMapper.writeValueAsString(hm),
							"handle_line_num",
							"1",
							"finish_time",
							System.currentTimeMillis() / 1000L,
							"updated",
							(int) (System.currentTimeMillis() / 1000L),
							"left_job_num",
							0));
		} catch (Exception e) {
			log.error("storageReadFail persist id={}", id, e);
		}
		try {
			fileStorageService.delete("file", storagePath);
		} catch (Exception ex) {
			log.debug("delete storage path failed: {}", ex.toString());
		}
		try {
			table.handleAfterInsertInRequestThread(
					toLong(data.get("company_id")),
					toLong(data.get("operator_id")),
					toLong(data.get("distributor_id")),
					toLong(data.get("supplier_id")),
					toLong(data.get("merchant_id")),
					data,
					storagePath,
					String.valueOf(data.get("file_type")));
		} catch (Exception ex) {
			log.debug("afterInsert hook: {}", ex.toString());
		}
	}

	private void headerFail(long id, String storagePath, EspierUploadTableHandler table, Map<String, Object> data, String msg) {
		try {
			List<List<Object>> errorlog = new ArrayList<>();
			errorlog.add(List.of("错误行数", "错误原因"));
			errorlog.add(List.of("", "头部标题或Excel解析错误: " + msg));
			Map<String, Object> hm = new LinkedHashMap<>();
			hm.put("successLine", 0);
			hm.put("errorLine", 1);
			hm.put("errorlog", errorlog);
			uploadeFileRepository.updateOneBy(
					Map.of("id", id),
					Map.of(
							"handle_status",
							"finish",
							"handle_message",
							objectMapper.writeValueAsString(hm),
							"handle_line_num",
							"1",
							"finish_time",
							System.currentTimeMillis() / 1000L,
							"updated",
							(int) (System.currentTimeMillis() / 1000L),
							"left_job_num",
							0));
		} catch (Exception e) {
			log.error("headerFail persist id={}", id, e);
		}
		try {
			fileStorageService.delete("file", storagePath);
		} catch (Exception ex) {
			log.debug("delete storage path failed: {}", ex.toString());
		}
		try {
			table.handleAfterInsertInRequestThread(
					toLong(data.get("company_id")),
					toLong(data.get("operator_id")),
					toLong(data.get("distributor_id")),
					toLong(data.get("supplier_id")),
					toLong(data.get("merchant_id")),
					data,
					storagePath,
					String.valueOf(data.get("file_type")));
		} catch (Exception ex) {
			log.debug("afterInsert hook: {}", ex.toString());
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
