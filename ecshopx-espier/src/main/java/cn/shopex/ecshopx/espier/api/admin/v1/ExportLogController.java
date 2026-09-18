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

package cn.shopex.ecshopx.espier.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.service.ExportLogFileDownResult;
import cn.shopex.ecshopx.espier.service.ExportLogFileDownService;
import cn.shopex.ecshopx.espier.service.ExportLogListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = false,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("espierAdminV1ExportLog")
@RequestMapping("/api/v1/espier/exportlog")
public class ExportLogController {

	private final ExportLogFileDownService exportLogFileDownService;
	private final ExportLogListService exportLogListService;

	public ExportLogController(
			ExportLogFileDownService exportLogFileDownService, ExportLogListService exportLogListService) {
		this.exportLogFileDownService = exportLogFileDownService;
		this.exportLogListService = exportLogListService;
	}

	@Activated(routeAlias = "espier.export.loglist")
	@GetMapping(value = "/list", name = "获取文件导出列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getExportLogList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> user =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		int page = parsePageIndex(pageRaw);
		int pageSize = parsePageSize(pageSizeRaw);
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		Long parsedOpId = EspierAdminJwtControllerSupport.parseLongOrNull(user == null ? null : user.get("operator_id"));
		long operatorId = parsedOpId == null ? 0L : parsedOpId;
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(user.get("operator_type"));
		String exportTypeRaw = request.getParameter("export_type");
		Map<String, Object> data =
				exportLogListService.getExportLogList(companyId, operatorId, operatorType, exportTypeRaw, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parsePageIndex(String raw) {
		long v = parseStrictLongIntLiteral(raw, "分页参数错误");
		if (v < 1L || v > Integer.MAX_VALUE) {
			throw new ResourceException("分页参数错误");
		}
		return (int) v;
	}

	private static int parsePageSize(String raw) {
		long v = parseStrictLongIntLiteral(raw, "每页最多查询50条数据");
		if (v > Integer.MAX_VALUE || v < 1L || v > 50L) {
			throw new ResourceException("每页最多查询50条数据");
		}
		return (int) v;
	}

	private static long parseStrictLongIntLiteral(String raw, String errorMsg) {
		if (raw == null || raw.trim().isEmpty()) {
			throw new ResourceException(errorMsg);
		}
		String t = raw.trim();
		if (t.charAt(0) == '+') {
			t = t.substring(1).trim();
			if (t.isEmpty()) {
				throw new ResourceException(errorMsg);
			}
		}
		if (t.contains(".") || t.toLowerCase().contains("e")) {
			throw new ResourceException(errorMsg);
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException(errorMsg);
		}
	}

	/**
	 * 注意：JVM 无按请求粒度的独立内存上限；将完整文件读入内存时，大体积载荷依赖节点堆大小与部署/运维约束，
	 * 超大场景需配合堆、超时与网关限制调优，后续可评估改为流式传输以降低峰值内存。
	 */
	@Activated(routeAlias = "espier.export.file.down")
	@GetMapping(value = "/file/down", name = "文件导出下载")
	public ResponseEntity<ApiResult<Map<String, Object>>> fileDown(
			@RequestParam(value = "log_id", required = false) String logIdRaw) {
		ExportLogFileDownResult result = exportLogFileDownService.fileDown(logIdRaw);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("csv_data", result.csvData());
		payload.put("file_name", result.fileName());
		return ResponseEntity.ok(ApiResult.ok(payload));
	}
}
