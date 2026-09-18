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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadRowContext;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class UploadFileImportDataService {

	private static final Logger log = LoggerFactory.getLogger(UploadFileImportDataService.class);

	private final UploadeFileRepository uploadeFileRepository;
	private final ObjectMapper objectMapper;
	private final EspierUploadFileHandlerRegistry registry;
	private final FileStorageService fileStorageService;
	private final TransactionTemplate rowTransactionTemplate;

	public UploadFileImportDataService(
			UploadeFileRepository uploadeFileRepository,
			ObjectMapper objectMapper,
			EspierUploadFileHandlerRegistry registry,
			FileStorageService fileStorageService,
			PlatformTransactionManager platformTransactionManager) {
		this.uploadeFileRepository = uploadeFileRepository;
		this.objectMapper = objectMapper;
		this.registry = registry;
		this.fileStorageService = fileStorageService;
		TransactionTemplate rowTx = new TransactionTemplate(platformTransactionManager);
		rowTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.rowTransactionTemplate = rowTx;
	}

	public void handleImportData(
			Map<String, Object> uploadFileInfo,
			List<Map<String, Object>> params,
			Map<Integer, String> column,
			int sort,
			List<String> exportHeaderTitleColumns) {
		long id = toLong(uploadFileInfo.get("id"));
		String requestFileType = String.valueOf(uploadFileInfo.get("file_type"));
		String storagePath =
				uploadFileInfo.get("storage_path") == null ? "" : String.valueOf(uploadFileInfo.get("storage_path"));

		EspierUploadFileHandler handler = registry.requireHandler(requestFileType);
		if (!(handler instanceof EspierUploadTableHandler table)) {
			throw new BadRequestException("该文件类型不支持表格导入");
		}
		long companyId = toLong(uploadFileInfo.get("company_id"));
		String operatorType =
				uploadFileInfo.get("operator_type") == null ? "" : String.valueOf(uploadFileInfo.get("operator_type")).trim();
		EspierUploadRowContext ctx = new EspierUploadRowContext(
				companyId,
				toLong(uploadFileInfo.get("operator_id")),
				toLong(uploadFileInfo.get("distributor_id")),
				toLong(uploadFileInfo.get("supplier_id")),
				toLong(uploadFileInfo.get("merchant_id")),
				requestFileType,
				operatorType);

		int successLine = 0;
		int errorLine = 0;
		List<List<Object>> errorlog = new ArrayList<>();
		long relationId = toLong(uploadFileInfo.get("relation_id"));
		List<String> errorHeaderTitles = exportHeaderTitleColumns == null || exportHeaderTitleColumns.isEmpty()
				? new ArrayList<>(table.getHeaderTitle(companyId, relationId).all().keySet())
				: exportHeaderTitleColumns;
		for (Map<String, Object> rawRow : params) {
			Map<String, Object> fileRowData = rowForHandle(rawRow);
			try {
				rowTransactionTemplate.executeWithoutResult(status -> table.handleRow(ctx, fileRowData));
				successLine++;
			} catch (Exception ex) {
				errorLine++;
				List<Object> err = alignedErrRow(errCells(rawRow), errorHeaderTitles);
				Object excelRow = rawRow.get("__excel_row__");
				err.add(excelRow == null ? 0 : excelRow);
				err.add(ex.getMessage());
				errorlog.add(err);
				log.debug("import row error: {}", ex.toString());
			}
		}

		mergeFinishState(
				id,
				sort,
				successLine,
				errorLine,
				errorlog,
				new ArrayList<>(errorHeaderTitles),
				storagePath,
				companyId,
				table);
	}

	private static Map<String, Object> rowForHandle(Map<String, Object> raw) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>(raw);
		m.remove("__excel_row__");
		m.remove("__err_cells__");
		return m;
	}

	@SuppressWarnings("unchecked")
	private static List<Object> errCells(Map<String, Object> raw) {
		Object v = raw.get("__err_cells__");
		if (v instanceof List<?> list) {
			return new ArrayList<>((List<Object>) list);
		}
		return new ArrayList<>();
	}

	/** Clip/pad raw Excel cells to the filtered header width so 错误行数/错误原因 stay aligned. */
	private static List<Object> alignedErrRow(List<Object> errCells, List<String> exportHeaderTitleColumns) {
		int n = exportHeaderTitleColumns == null ? 0 : exportHeaderTitleColumns.size();
		List<Object> err = new ArrayList<>(n + 2);
		for (int i = 0; i < n; i++) {
			err.add(i < errCells.size() ? errCells.get(i) : "");
		}
		return err;
	}

	private void mergeFinishState(
			long id,
			int sort,
			int chunkSuccess,
			int chunkError,
			List<List<Object>> chunkErrorlog,
			List<String> exportHeaderTitleColumns,
			String storagePath,
			long companyId,
			EspierUploadTableHandler table) {
		int triesRemaining = -1;
		int spin = 0;
		while (true) {
			Map<String, Object> data = uploadeFileRepository.getInfoById(id);
			if (data.isEmpty()) {
				return;
			}
			int left = toInt(data.get("left_job_num"));
			if (left <= 0) {
				return;
			}
			if (sort < left) {
				spin++;
				if (spin > 10_000) {
					log.warn("import merge waited too long id={} sort={} left={}", id, sort, left);
					return;
				}
				Thread.onSpinWait();
				continue;
			}
			if (triesRemaining < 0) {
				triesRemaining = left;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> prevMsg = data.get("handle_message") instanceof Map<?, ?> hm
					? (Map<String, Object>) hm
					: null;
			int mergedSuccess = chunkSuccess + intFromMsg(prevMsg, "successLine");
			int mergedError = chunkError + intFromMsg(prevMsg, "errorLine");
			List<List<Object>> mergedErrorlog = new ArrayList<>();
			boolean prevHadErrorlog = false;
			if (prevMsg != null && prevMsg.get("errorlog") instanceof List<?> p) {
				prevHadErrorlog = !p.isEmpty();
				for (Object o : p) {
					if (o instanceof List<?> row) {
						mergedErrorlog.add(new ArrayList<>(castRow(row)));
					}
				}
			}
			if (prevHadErrorlog) {
				mergedErrorlog.addAll(chunkErrorlog);
			} else if (!chunkErrorlog.isEmpty()) {
				List<String> exportTitles = exportHeaderTitleColumns;
				if (exportTitles.isEmpty()) {
					exportTitles = new ArrayList<>(
							table.getHeaderTitle(companyId, toLong(data.get("relation_id"))).all().keySet());
				}
				List<Object> title = new ArrayList<>();
				for (String h : exportTitles) {
					title.add(h);
				}
				title.add("错误行数");
				title.add("错误原因");
				List<List<Object>> withTitle = new ArrayList<>();
				withTitle.add(title);
				withTitle.addAll(chunkErrorlog);
				mergedErrorlog = withTitle;
			}

			int leftJobNum = left > 1 ? left - 1 : 0;
			String status = leftJobNum == 0 ? "finish" : "processing";
			try {
				Map<String, Object> msg = new LinkedHashMap<>();
				msg.put("successLine", mergedSuccess);
				msg.put("errorLine", mergedError);
				msg.put("errorlog", mergedErrorlog);
				uploadeFileRepository.updateOneBy(
						Map.of("id", id, "left_job_num", left),
						Map.of(
								"handle_status",
								status,
								"handle_message",
								objectMapper.writeValueAsString(msg),
								"handle_line_num",
								String.valueOf(mergedSuccess + mergedError),
								"finish_time",
								System.currentTimeMillis() / 1000L,
								"updated",
								(int) (System.currentTimeMillis() / 1000L),
								"left_job_num",
								leftJobNum));
			} catch (ResourceException ex) {
				triesRemaining--;
				if (triesRemaining <= 0) {
					log.error("import merge update exhausted id={}", id, ex);
					return;
				}
				continue;
			} catch (Exception e) {
				log.error("persist import merge failed id={}", id, e);
				return;
			}

			try {
				if (!storagePath.isBlank()) {
					fileStorageService.delete("file", storagePath);
				}
			} catch (Exception ex) {
				log.debug("delete storage path failed: {}", ex.toString());
			}
			return;
		}
	}

	private static List<Object> castRow(List<?> row) {
		List<Object> out = new ArrayList<>();
		for (Object o : row) {
			out.add(o);
		}
		return out;
	}

	private static int intFromMsg(Map<String, Object> prevMsg, String key) {
		if (prevMsg == null) {
			return 0;
		}
		return toInt(prevMsg.get(key));
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(o));
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
