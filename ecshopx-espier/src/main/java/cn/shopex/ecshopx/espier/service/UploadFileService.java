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

import cn.shopex.ecshopx.common.espier.upload.EspierUploadRelationValidatePort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.storage.StorageException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileHandlerRegistry;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTableHandler;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTemplateExtension;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadTemplateXlsxBuilder;
import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileJobEnqueuePort;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileQueuedPayload;
import cn.shopex.ecshopx.common.cron.EspierScheduledUploadSourceFileRemover;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadFileStorageWriter;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadFileService {

	private static final Logger log = LoggerFactory.getLogger(UploadFileService.class);

	private static final Pattern LEGACY_QUERY_INTEGER = Pattern.compile("^(?:\\+|[-])?(?:0|[1-9]\\d*)$");

	/**
	 * 当对象存储读取失败时，导出接口仍返回与成功下载相近的响应形态（含文件名与文件内容字段）；
	 * 文件字段使用该稳定、体积很小的 UTF-8 HTML 占位内容编码后填入，避免空响应或结构突变。
	 */
	private static final byte[] ERROR_EXPORT_FALLBACK_HTML_BYTES =
			java.util.HexFormat.of()
					.parseHex(
							"3c21444f43545950452068746d6c3e3c68746d6c3e3c626f64793e343034204e6f7420466f756e643c2f626f64793e3c2f68746d6c3e");

	private final EspierUploadFileHandlerRegistry registry;
	private final UploadeFileRepository uploadeFileRepository;
	private final OperatorsQueryService operatorsQueryService;
	private final EspierUploadFileStorageWriter storageWriter;
	private final EspierUploadFileJobEnqueuePort espierUploadFileJobEnqueuePort;
	private final EspierUploadFileAsyncRunner espierUploadFileAsyncRunner;
	private final FileStorageService fileStorageService;
	private final EspierScheduledUploadSourceFileRemover scheduledUploadSourceFileRemover;
	private final List<EspierUploadRelationValidatePort> relationValidators;

	public UploadFileService(
			EspierUploadFileHandlerRegistry registry,
			UploadeFileRepository uploadeFileRepository,
			OperatorsQueryService operatorsQueryService,
			EspierUploadFileStorageWriter storageWriter,
			EspierUploadFileJobEnqueuePort espierUploadFileJobEnqueuePort,
			EspierUploadFileAsyncRunner espierUploadFileAsyncRunner,
			FileStorageService fileStorageService,
			EspierScheduledUploadSourceFileRemover scheduledUploadSourceFileRemover,
			List<EspierUploadRelationValidatePort> relationValidators) {
		this.registry = registry;
		this.uploadeFileRepository = uploadeFileRepository;
		this.operatorsQueryService = operatorsQueryService;
		this.storageWriter = storageWriter;
		this.espierUploadFileJobEnqueuePort = espierUploadFileJobEnqueuePort;
		this.espierUploadFileAsyncRunner = espierUploadFileAsyncRunner;
		this.fileStorageService = fileStorageService;
		this.scheduledUploadSourceFileRemover = scheduledUploadSourceFileRemover;
		this.relationValidators = relationValidators == null ? List.of() : relationValidators;
	}

	public int scheduleDeleteErrorFile() {
		long cutoff = Instant.now().minus(15, ChronoUnit.DAYS).getEpochSecond();
		long totalCount = uploadeFileRepository.countScheduleDeleteErrorFileCandidates(cutoff);
		if (totalCount == 0) {
			return 0;
		}
		int totalPage = (int) Math.ceil(totalCount / 100.0);
		int processed = 0;
		for (int page = 1; page <= totalPage; page++) {
			LinkedHashMap<String, Object> data =
					uploadeFileRepository.listScheduleDeleteErrorFileCandidates(cutoff, page, 100);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
			if (list == null) {
				continue;
			}
			for (Map<String, Object> row : list) {
				Object hmObj = row.get("handle_message");
				if (!(hmObj instanceof Map)) {
					continue;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> handleMessage = (Map<String, Object>) hmObj;
				if (!hasTruthyErrorLine(handleMessage)) {
					continue;
				}
				String fileType = row.get("file_type") == null ? "" : String.valueOf(row.get("file_type"));
				long companyId = parseLongLoose(row.get("company_id"));
				Object createdObj = row.get("created");
				int createdSec =
						createdObj instanceof Number n
								? n.intValue()
								: Integer.parseInt(String.valueOf(createdObj));
				String fileName = row.get("file_name") == null ? "upload.xlsx" : String.valueOf(row.get("file_name"));
				String relativePath =
						storageWriter.buildRelativePathForImport(fileType, companyId, fileName, createdSec);
				try {
					scheduledUploadSourceFileRemover.removeRelativePath(relativePath);
				} catch (Exception e) {
					log.debug("删除上传文件处理错误信息文件失败：{}", e.getMessage());
				}
				processed++;
			}
		}
		return processed;
	}

	private static boolean hasTruthyErrorLine(Map<String, Object> handleMessage) {
		Object el = handleMessage.get("errorLine");
		if (el == null) {
			return false;
		}
		if (el instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		if (el instanceof String s) {
			if (s.isEmpty() || "0".equals(s)) {
				return false;
			}
			try {
				return Long.parseLong(s.trim(), 10) != 0L;
			} catch (NumberFormatException e) {
				return true;
			}
		}
		if (el instanceof Boolean b) {
			return b;
		}
		return false;
	}

	private static long parseLongLoose(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o).trim());
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> handleUploadFile(
			long companyId,
			long operatorId,
			String operatorType,
			long supplierId,
			long distributorId,
			long relationId,
			String fileType,
			MultipartFile file,
			boolean shouldQueue) {
		String normalizedType = EspierAdminJwtControllerSupport.optionalTrimmedString(fileType);
		if (normalizedType == null || normalizedType.isEmpty()) {
			throw new BadRequestException("file_type 必填");
		}
		validateRelationIdIfRequired(companyId, normalizedType, relationId);
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("上传文件内容必填");
		}
		EspierUploadFileHandler handler = registry.requireHandler(normalizedType);
		if (handler == null) {
			throw new BadRequestException("不支持的 file_type: " + normalizedType);
		}
		handler.check(file);
		Optional<Map<String, Object>> sync = handler.trySyncProcess(
				companyId, operatorId, distributorId, supplierId, file);
		if (sync.isPresent()) {
			return sync.get();
		}
		byte[] bytes = readUploadBytes(file);
		int uploadEpochSeconds = Math.toIntExact(Instant.now().getEpochSecond());
		String storageRelativePath;
		try {
			storageRelativePath =
					storageWriter.writeImportFile(normalizedType, companyId, file, bytes, uploadEpochSeconds);
		} catch (StorageException e) {
			log.warn("import file storage write failed file_type={}", normalizedType, e);
			throw new ResourceException("上传失败");
		}
		Map<String, Object> opRow =
				operatorsQueryService.getInfo(Map.of("company_id", companyId, "operator_id", operatorId));
		long merchantId = 0L;
		if (opRow != null) {
			Long m = EspierAdminJwtControllerSupport.parseLongOrNull(opRow.get("merchant_id"));
			merchantId = m == null ? 0L : m;
		}
		LinkedHashMap<String, Object> row = uploadeFileRepository.create(
				companyId,
				operatorId,
				supplierId,
				distributorId,
				merchantId,
				relationId,
				normalizedType,
				file,
				storageRelativePath,
				shouldQueue,
				uploadEpochSeconds);
		Map<String, Object> result = new LinkedHashMap<>(row);
		result.put("file_type", normalizedType);
		result.put("operator_type", operatorType == null ? "" : operatorType);
		result.put("relation_id", relationId);
		if (shouldQueue) {
			espierUploadFileJobEnqueuePort.publishAfterCommit(
					EspierUploadFileQueuedPayload.fromPersistedRow(result, storageRelativePath, normalizedType));
		} else {
			espierUploadFileAsyncRunner.runInline(result, storageRelativePath, normalizedType);
		}
		return result;
	}

	public Map<String, Object> getUploadLists(
			long companyId,
			Long merchantIdFromJwt,
			String operatorType,
			long operatorIdFromJwt,
			String fileTypeRaw,
			String distributorIdRaw,
			String relationIdRaw,
			String pageRaw,
			String pageSizeRaw) {
		int page = parseRequiredBoundedPageQuery(pageRaw, true);
		int pageSize = parseRequiredBoundedPageQuery(pageSizeRaw, false);
		String fileType = EspierAdminJwtControllerSupport.optionalTrimmedString(fileTypeRaw);
		if (fileType == null) {
			fileType = "member_info";
		}
		long supplierId = "supplier".equalsIgnoreCase(operatorType) ? operatorIdFromJwt : 0L;
		Long merchantFilter = null;
		if ("merchant".equalsIgnoreCase(operatorType) && merchantIdFromJwt != null) {
			merchantFilter = merchantIdFromJwt;
		}
		Long distributorFilterOrNull = resolveDistributorFilterOrNull(distributorIdRaw);
		Long relationFilterOrNull = resolveRelationFilterOrNull(relationIdRaw);
		LinkedHashMap<String, Object> data =
				uploadeFileRepository.lists(
						companyId,
						fileType,
						supplierId,
						merchantFilter,
						distributorFilterOrNull,
						relationFilterOrNull,
						page,
						pageSize);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
		stripErrorlogFromListItems(list);
		if (list != null) {
			for (Map<String, Object> row : list) {
				coerceUploadListRowJsonTypes(row);
			}
		}
		return data;
	}

	private static boolean isUnsetQueryParam(String raw) {
		return raw == null || raw.isEmpty() || "0".equals(raw);
	}

	public Map<String, String> exportUploadTemplate(
			long companyId, String operatorType, String fileTypeRaw, String fileNameRaw, long relationId) {
		if (isUnsetQueryParam(fileTypeRaw)) {
			throw new ResourceException("文件类型不能为空！");
		}
		if (isUnsetQueryParam(fileNameRaw)) {
			throw new ResourceException("文件名称不能为空！");
		}
		String normalizedType =
				"supplier".equalsIgnoreCase(operatorType) && "normal_goods".equals(fileTypeRaw)
						? "supplier_goods"
						: fileTypeRaw;
		validateRelationIdIfRequired(companyId, normalizedType, relationId);
		EspierUploadFileHandler h = registry.requireHandler(normalizedType);
		if (h == null) {
			throw new BadRequestException("不支持的文件类型");
		}
		if (!(h instanceof EspierUploadTableHandler table)) {
			throw new BadRequestException("不支持的文件类型");
		}
		UploadHeaderTitle title = table.getHeaderTitle(companyId, relationId);
		Optional<List<List<Object>>> demo =
				h instanceof EspierUploadTemplateExtension ext ? ext.templateDemoRows(companyId, operatorType) : Optional.empty();
		Optional<List<String>> textCols =
				h instanceof EspierUploadTemplateExtension ext2
						? ext2.templateTextColumnHeaders(companyId, operatorType)
						: Optional.empty();
		if ("distributor_info".equals(normalizedType) && (textCols.isEmpty() || textCols.get().isEmpty())) {
			textCols = Optional.of(List.of("经营开始时间", "经营结束时间"));
		}
		byte[] xlsx;
		try {
			xlsx = EspierUploadTemplateXlsxBuilder.buildTemplateBytes(fileNameRaw, title, demo, textCols);
		} catch (UncheckedIOException e) {
			throw new ResourceException("模板生成失败");
		}
		String prefix = "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,";
		String dataUrl = prefix + Base64.getEncoder().encodeToString(xlsx);
		LinkedHashMap<String, String> out = new LinkedHashMap<>();
		out.put("name", fileNameRaw + ".xlsx");
		out.put("file", dataUrl);
		return out;
	}

	private static int parseRequiredBoundedPageQuery(String raw, boolean pageField) {
		String t = raw == null ? "" : raw.trim();
		String pageMsg = "分页参数错误";
		String pageSizeMsg = "每页最多查询50条数据";
		if (t.isEmpty()) {
			throw new BadRequestException(pageField ? pageMsg : pageSizeMsg);
		}
		if (!LEGACY_QUERY_INTEGER.matcher(t).matches()) {
			throw new BadRequestException(pageField ? pageMsg : pageSizeMsg);
		}
		BigInteger bi = new BigInteger(t);
		if (bi.bitLength() > 31) {
			throw new BadRequestException(pageField ? pageMsg : pageSizeMsg);
		}
		if (pageField) {
			if (bi.compareTo(BigInteger.ONE) < 0) {
				throw new BadRequestException(pageMsg);
			}
			return bi.intValueExact();
		}
		if (bi.compareTo(BigInteger.ONE) < 0 || bi.compareTo(BigInteger.valueOf(50)) > 0) {
			throw new BadRequestException(pageSizeMsg);
		}
		return bi.intValueExact();
	}

	private static Long resolveDistributorFilterOrNull(String distributorIdRaw) {
		String s = distributorIdRaw == null ? "" : distributorIdRaw.trim();
		if (s.isEmpty() || "0".equals(s)) {
			return null;
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 格式错误");
		}
		if (v == 0L) {
			return null;
		}
		return v;
	}

	private static Long resolveRelationFilterOrNull(String relationIdRaw) {
		String s = relationIdRaw == null ? "" : relationIdRaw.trim();
		if (s.isEmpty() || "0".equals(s)) {
			return null;
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("relation_id 格式错误");
		}
		if (v == 0L) {
			return null;
		}
		return v;
	}

	private void validateRelationIdIfRequired(long companyId, String fileType, long relationId) {
		for (EspierUploadRelationValidatePort validator : relationValidators) {
			if (!validator.supports(fileType)) {
				continue;
			}
			if (relationId <= 0) {
				throw new BadRequestException("关联id不能为空");
			}
			validator.validate(companyId, relationId);
			return;
		}
	}

	@SuppressWarnings("unchecked")
	private static void stripErrorlogFromListItems(List<Map<String, Object>> list) {
		if (list == null) {
			return;
		}
		for (Map<String, Object> row : list) {
			if (row == null) {
				continue;
			}
			Object hm = row.get("handle_message");
			if (!(hm instanceof Map)) {
				continue;
			}
			Map<String, Object> msgMap = (Map<String, Object>) hm;
			if (!msgMap.containsKey("errorlog")) {
				continue;
			}
			try {
				msgMap.remove("errorlog");
			} catch (UnsupportedOperationException ex) {
				LinkedHashMap<String, Object> copy = new LinkedHashMap<>(msgMap);
				copy.remove("errorlog");
				row.put("handle_message", copy);
			}
		}
	}

	private static void coerceUploadListRowJsonTypes(Map<String, Object> row) {
		if (row == null) {
			return;
		}
		coerceStringKeyToLong(row, "id");
		coerceStringKeyToLong(row, "company_id");
		coerceStringKeyToLong(row, "operator_id");
		coerceStringKeyToLong(row, "merchant_id");
	}

	private static void coerceStringKeyToLong(Map<String, Object> row, String key) {
		Object v = row.get(key);
		if (v == null) {
			return;
		}
		if (v instanceof String s) {
			if (s.isEmpty()) {
				row.put(key, null);
			} else {
				row.put(key, Long.parseLong(s));
			}
		}
	}

	public Map<String, String> exportUploadErrorFile(String idRaw, String fileTypeRaw) {
		String normalizedType = EspierAdminJwtControllerSupport.optionalTrimmedString(fileTypeRaw);
		if (normalizedType == null || normalizedType.isEmpty()) {
			throw new BadRequestException("file_type 必填");
		}
		EspierUploadFileHandler handler = registry.requireHandler(normalizedType);
		if (handler == null) {
			throw new BadRequestException("不支持的 file_type: " + normalizedType);
		}
		String routeId = idRaw == null ? "" : idRaw.trim();

		LinkedHashMap<String, Object> info = uploadeFileRepository.getInfoByRouteId(routeId);
		Object hm = info.get("handle_message");
		boolean needRegenerate = false;
		List<?> errorRows = null;
		if (hm instanceof Map<?, ?> msgMap) {
			Object el = msgMap.get("errorlog");
			if (el instanceof List<?> list && !list.isEmpty()) {
				needRegenerate = true;
				errorRows = list;
			}
		}

		String objectKey = "uploadFileError/" + routeId + "/error.xlsx";
		if (needRegenerate) {
			try {
				byte[] bytes = buildErrorXlsxBytes(errorRows);
				fileStorageService.put("file", objectKey, bytes);
			} catch (RuntimeException e) {
				log.warn(
						"exportUploadErrorFile: regenerate put failed for key={}, continuing to read storage",
						objectKey,
						e);
			}
		}

		byte[] content;
		try {
			content = fileStorageService.get("file", objectKey);
		} catch (RuntimeException e) {
			log.warn("exportUploadErrorFile: storage get failed for key={}, returning fallback payload", objectKey, e);
			return buildErrorExportSuccessShapedResponse(null);
		}
		if (content == null || content.length == 0) {
			return buildErrorExportSuccessShapedResponse(null);
		}
		return buildErrorExportSuccessShapedResponse(content);
	}

	private static Map<String, String> buildErrorExportSuccessShapedResponse(byte[] content) {
		byte[] payload = content == null || content.length == 0 ? ERROR_EXPORT_FALLBACK_HTML_BYTES : content;
		String prefix = "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,";
		String dataUrl = prefix + Base64.getEncoder().encodeToString(payload);
		Map<String, String> out = new LinkedHashMap<>();
		out.put("name", "error.xlsx");
		out.put("file", dataUrl);
		return out;
	}

	private static byte[] buildErrorXlsxBytes(List<?> errorData) {
		try (XSSFWorkbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("导入错误信息");
			if (errorData == null || errorData.isEmpty()) {
				workbook.write(baos);
				return baos.toByteArray();
			}
			for (int r = 0; r < errorData.size(); r++) {
				Object rowObj = errorData.get(r);
				Row row = sheet.createRow(r);
				if (rowObj instanceof List<?> cells) {
					for (int c = 0; c < cells.size(); c++) {
						Object v = cells.get(c);
						row.createCell(c).setCellValue(v == null ? "" : String.valueOf(v));
					}
				} else {
					row.createCell(0).setCellValue(String.valueOf(rowObj));
				}
			}
			workbook.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static byte[] readUploadBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			if (isLikelyMultipartSizeProblem(e)) {
				throw new BadRequestException("上传文件大小超出限制");
			}
			log.warn("Failed to read uploaded file bytes", e);
			throw new BadRequestException("文件读取失败");
		} catch (MultipartException e) {
			if (isLikelyMultipartSizeProblem(e)) {
				throw new BadRequestException("上传文件大小超出限制");
			}
			throw e;
		} catch (IllegalStateException e) {
			if (isLikelyMultipartSizeProblem(e)) {
				throw new BadRequestException("上传文件大小超出限制");
			}
			throw e;
		}
	}

	private static boolean isLikelyMultipartSizeProblem(Throwable t) {
		for (Throwable c = t; c != null; c = c.getCause()) {
			if (c instanceof MaxUploadSizeExceededException
					|| c instanceof SizeLimitExceededException
					|| c instanceof FileSizeLimitExceededException) {
				return true;
			}
		}
		return false;
	}
}
