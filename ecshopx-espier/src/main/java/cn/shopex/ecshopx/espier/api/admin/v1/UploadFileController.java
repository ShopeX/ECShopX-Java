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
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.service.UploadFileService;
import cn.shopex.ecshopx.espier.service.upload.EspierUploadShouldQueueParser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("espierAdminV1UploadFile")
@RequestMapping("/api/v1/espier")
public class UploadFileController {

	private final UploadFileService uploadFileService;
	private final Environment environment;

	public UploadFileController(UploadFileService uploadFileService, Environment environment) {
		this.uploadFileService = uploadFileService;
		this.environment = environment;
	}

	@Activated(routeAlias = "espier.upload")
	@PostMapping(
			value = "/upload_file",
			produces = MediaType.APPLICATION_JSON_VALUE,
			name = "上传文件")
	public ResponseEntity<Map<String, Object>> handleUploadFile(
			HttpServletRequest request,
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "file_type", required = false) String fileType,
			@RequestParam(value = "should_queue", required = false, defaultValue = "1") String shouldQueueRaw,
			@RequestParam(value = "relation_id", required = false, defaultValue = "0") String relationIdRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> user =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		long operatorId = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("operator_id")) == null
				? 0L
				: EspierAdminJwtControllerSupport.parseLongOrNull(user.get("operator_id"));
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(user.get("operator_type"));
		if (operatorType == null) {
			operatorType = "";
		}
		long supplierId = "supplier".equalsIgnoreCase(operatorType) ? operatorId : 0L;
		long distributorId =
				EspierAdminJwtControllerSupport.parseDistributorId(
						resolveDistributorIdRaw(request, distributorIdRaw));
		long relationId = parseRelationIdOrZero(relationIdRaw);
			if (isEmployeePurchaseUploadFileType(fileType)) {
			if (relationId <= 0) {
				throw new cn.shopex.ecshopx.common.exception.BadRequestException("关联id不能为空");
			}
			// 对齐 PHP：门店 ID 取自当前登录用户，不再依赖请求参数
			Long jwtDistributorId = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("distributor_id"));
			distributorId = jwtDistributorId == null ? 0L : Math.max(0L, jwtDistributorId);
		}
		boolean shouldQueue = EspierUploadShouldQueueParser.parseShouldQueue(shouldQueueRaw);
		if (environment.acceptsProfiles(Profiles.of("local"))) {
			shouldQueue = false;
		}
		Map<String, Object> result = uploadFileService.handleUploadFile(
				companyId,
				operatorId,
				operatorType,
				supplierId,
				distributorId,
				relationId,
				fileType,
				file,
				shouldQueue);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("data", result));
	}

	@Activated(routeAlias = "espier.upload.list")
	@GetMapping(value = "/upload_files", name = "获取上传文件列表")
	public ResponseEntity<Map<String, Object>> getUploadLists(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "file_type", required = false) String fileType,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "relation_id", required = false, defaultValue = "0") String relationIdRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> user =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		Long merchantId = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("merchant_id"));
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(user.get("operator_type"));
		if (operatorType == null) {
			operatorType = "";
		}
		long operatorId = EspierAdminJwtControllerSupport.parseLongOrNull(user.get("operator_id")) == null
				? 0L
				: EspierAdminJwtControllerSupport.parseLongOrNull(user.get("operator_id"));
		Map<String, Object> data = uploadFileService.getUploadLists(
				companyId,
				merchantId,
				operatorType,
				operatorId,
				fileType,
				resolveDistributorIdRaw(request, distributorIdRaw),
				relationIdRaw,
				pageRaw,
				pageSizeRaw);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("data", data));
	}

	/**
	 * Prefer Activated-injected distributor_id when present (selected store for distributor accounts).
	 */
	private static String resolveDistributorIdRaw(HttpServletRequest request, String distributorIdRaw) {
		Object activated = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID);
		if (activated != null) {
			String text = String.valueOf(activated).trim();
			if (!text.isEmpty() && !"0".equals(text)) {
				return text;
			}
		}
		return distributorIdRaw == null || distributorIdRaw.isEmpty() ? "0" : distributorIdRaw;
	}

	private static long parseRelationIdOrZero(String relationIdRaw) {
		if (relationIdRaw == null || relationIdRaw.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(relationIdRaw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean isEmployeePurchaseUploadFileType(String fileType) {
		if (fileType == null) {
			return false;
		}
		return "employee_purchase_activity_items".equals(fileType)
				|| "employee_purchase_activity_items_sort".equals(fileType);
	}

	@Activated(routeAlias = "espier.upload.error.export")
	@GetMapping(value = "/upload_error_file_export/{id}", name = "上传文件执行后错误信息")
	public ResponseEntity<Map<String, Object>> exportUploadErrorFile(
			@PathVariable("id") String id,
			@RequestParam(value = "file_type", required = false) String fileType) {
		Map<String, String> payload = uploadFileService.exportUploadErrorFile(id, fileType);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("data", payload));
	}

	@Activated(routeAlias = "espier.upload.template.export")
	@GetMapping(value = "/upload_template", name = "获取上传文件模版")
	public ResponseEntity<Map<String, Object>> exportUploadTemplate(
			HttpServletRequest request,
			@RequestParam(value = "file_type", required = false) String fileTypeRaw,
			@RequestParam(value = "file_name", required = false) String fileNameRaw,
			@RequestParam(value = "relation_id", required = false, defaultValue = "0") String relationIdRaw) {
		@SuppressWarnings("unchecked")
		Map<String, Object> user =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		String operatorType = EspierAdminJwtControllerSupport.optionalTrimmedString(user.get("operator_type"));
		if (operatorType == null) {
			operatorType = "";
		}
		Map<String, String> inner =
				uploadFileService.exportUploadTemplate(
						companyId, operatorType, fileTypeRaw, fileNameRaw, parseRelationIdOrZero(relationIdRaw));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("data", inner));
	}
}
