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

package cn.shopex.ecshopx.workwechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.wechat.WorkWechatDistributorDepartmentSyncPort;
import cn.shopex.ecshopx.common.wechat.WorkWechatSalespersonSyncPort;
import cn.shopex.ecshopx.workwechat.service.WorkWechatConfigService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatCorpUserApiService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatDepartmentTreeService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatDistributorJsSdkService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatRelListService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatRelLogsListService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatReportUserListService;
import cn.shopex.ecshopx.workwechat.service.WorkWechatVerifyDomainFileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true
)
@AdminAuth
@ShopLog
@RestController("workWechatAdminV1WorkWechat")
@RequestMapping("/api/v1/workwechat")
public class WorkWechatController {

	private static final Pattern VERIFY_DOMAIN_FILENAME = Pattern
			.compile("^([a-zA-Z0-9_]{10,50})\\.txt$");

	private final WorkWechatConfigService workWechatConfigService;
	private final WorkWechatDistributorJsSdkService workWechatDistributorJsSdkService;
	private final WorkWechatVerifyDomainFileService workWechatVerifyDomainFileService;
	private final WorkWechatDistributorDepartmentSyncPort workWechatDistributorDepartmentSyncPort;
	private final WorkWechatSalespersonSyncPort workWechatSalespersonSyncPort;
	private final WorkWechatCorpUserApiService workWechatCorpUserApiService;
	private final WorkWechatRelListService workWechatRelListService;
	private final WorkWechatRelLogsListService workWechatRelLogsListService;
	private final WorkWechatDepartmentTreeService workWechatDepartmentTreeService;
	private final WorkWechatReportUserListService workWechatReportUserListService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public WorkWechatController(
			WorkWechatConfigService workWechatConfigService,
			WorkWechatDistributorJsSdkService workWechatDistributorJsSdkService,
			WorkWechatVerifyDomainFileService workWechatVerifyDomainFileService,
			WorkWechatDistributorDepartmentSyncPort workWechatDistributorDepartmentSyncPort,
			WorkWechatSalespersonSyncPort workWechatSalespersonSyncPort,
			WorkWechatCorpUserApiService workWechatCorpUserApiService,
			WorkWechatRelListService workWechatRelListService,
			WorkWechatRelLogsListService workWechatRelLogsListService,
			WorkWechatDepartmentTreeService workWechatDepartmentTreeService,
			WorkWechatReportUserListService workWechatReportUserListService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.workWechatConfigService = workWechatConfigService;
		this.workWechatDistributorJsSdkService = workWechatDistributorJsSdkService;
		this.workWechatVerifyDomainFileService = workWechatVerifyDomainFileService;
		this.workWechatDistributorDepartmentSyncPort = workWechatDistributorDepartmentSyncPort;
		this.workWechatSalespersonSyncPort = workWechatSalespersonSyncPort;
		this.workWechatCorpUserApiService = workWechatCorpUserApiService;
		this.workWechatRelListService = workWechatRelListService;
		this.workWechatRelLogsListService = workWechatRelLogsListService;
		this.workWechatDepartmentTreeService = workWechatDepartmentTreeService;
		this.workWechatReportUserListService = workWechatReportUserListService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "workwechat.config.info")
	@GetMapping(value = "/config", name = "获取企业微信配置")
	public ApiResult<Map<String, Object>> getConfig(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> result = workWechatConfigService.getViewConfig(companyId);

		Object agentsObj = result.get("agents");
		if (agentsObj instanceof Map<?, ?> agents) {
			Object customerObj = agents.get("customer");
			if (customerObj instanceof Map<?, ?> customerRaw && customerRaw.containsKey("URL")) {
				@SuppressWarnings("unchecked")
				Map<String, Object> customer = (Map<String, Object>) customerRaw;
				Object urlObj = customer.get("URL");
				String relativeUrl = urlObj == null ? "" : String.valueOf(urlObj);
				String fullUrl = request.getRequestURL().toString();
				int idx = fullUrl.indexOf("/workwechat/config");
				if (idx >= 0) {
					customer.put("URL", fullUrl.substring(0, idx) + relativeUrl);
				}
			}
		}

		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "workwechat.config.save")
	@PostMapping(value = "/config", name = "保存企业微信配置")
	public ApiResult<Map<String, Object>> setConfig(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> fullRequestBody = body == null ? Collections.emptyMap() : body;
		Map<String, Object> result = workWechatConfigService.saveWorkWechatConfig(companyId, fullRequestBody);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "workwechat.report.info")
	@GetMapping(value = "/report", name = "获取企业微信通讯录")
	public ApiResult<List<Map<String, Object>>> getReport(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		List<Map<String, Object>> tree = workWechatDepartmentTreeService.getDepartmentTree(companyId);
		return ApiResult.ok(tree);
	}

	@Activated(routeAlias = "workwechat.report.userlists")
	@GetMapping(value = "/report/{department_id}", name = "部门成员列表")
	public ApiResult<List<Map<String, Object>>> getReportUserLists(HttpServletRequest request,
			@PathVariable("department_id") String departmentIdRaw) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long departmentId;
		try {
			departmentId = Long.parseLong(departmentIdRaw == null ? "" : departmentIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数格式错误");
		}
		List<Map<String, Object>> data = workWechatReportUserListService.listUsersForDepartment(companyId,
				departmentId);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true
	)
	@Activated(routeAlias = "workwechat.report.distributor.sync")
	@PostMapping(value = "/report/syncDistributor", name = "同步部门到店铺")
	public ApiResult<List<Map<String, Object>>> syncDistributor(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorIdRaw,
			@RequestParam(value = "department_id", required = false, defaultValue = "") String departmentIdJson) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readCompanyId(ud);
		String requestLangTag = RequestLangTag.current(langueProperties);

		String deptTrimmed = departmentIdJson == null ? "" : departmentIdJson.trim();
		if (!StringUtils.hasText(deptTrimmed)) {
			throw new ResourceException("请至少选择一个部门");
		}

		List<Map<String, Object>> departments;
		try {
			JsonNode root = objectMapper.readTree(deptTrimmed);
			if (root == null) {
				throw new BadRequestException("参数格式错误");
			}
			if (root.isObject()) {
				departments = new ArrayList<>();
			} else if (root.isArray()) {
				departments = new ArrayList<>();
				for (JsonNode n : root) {
					if (n == null || !n.isObject()) {
						throw new BadRequestException("参数格式错误");
					}
					@SuppressWarnings("unchecked")
					Map<String, Object> m = objectMapper.convertValue(n, Map.class);
					departments.add(m);
				}
			} else {
				throw new BadRequestException("参数格式错误");
			}
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数格式错误");
		}

		String didRaw = distributorIdRaw == null ? "" : distributorIdRaw.trim();
		long distributorId = 0L;
		if (StringUtils.hasText(didRaw) && !"0".equals(didRaw)) {
			try {
				distributorId = Long.parseLong(didRaw);
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数格式错误");
			}
		}

		List<Map<String, Object>> result;
		if (distributorId != 0L) {
			result = workWechatDistributorDepartmentSyncPort.updateDepartmentToDistributor(
					companyId, departments, distributorId, requestLangTag);
		} else {
			result = workWechatDistributorDepartmentSyncPort.syncDepartmentToDistributor(
					companyId, departments, requestLangTag);
		}
		return ApiResult.ok(result);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true
	)
	@Activated(routeAlias = "workwechat.report.salesperson.sync")
	@PostMapping(value = "/report/syncSalesperson", name = "同步成员到导购")
	public ApiResult<Map<String, Object>> syncSalesperson(
			HttpServletRequest request,
			@RequestParam(value = "department_id", required = false, defaultValue = "0") String departmentIdRaw,
			@RequestParam(value = "user_ids", required = false, defaultValue = "") String userIdsJson) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readCompanyId(ud);

		String idsTrim = userIdsJson == null ? "" : userIdsJson.trim();
		List<Map<String, Object>> userData = new ArrayList<>();

		if (StringUtils.hasText(idsTrim)) {
			JsonNode root;
			try {
				root = objectMapper.readTree(idsTrim);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("参数格式错误");
			}
			if (root == null) {
				throw new BadRequestException("参数格式错误");
			}
			if (root.isArray()) {
				for (JsonNode n : root) {
					if (n == null || !n.isTextual()) {
						throw new BadRequestException("参数格式错误");
					}
					String uid = n.asText();
					Map<String, Object> userArr = workWechatCorpUserApiService.getUser(companyId, uid);
					if (userArr != null && !userArr.isEmpty()) {
						userData.add(userArr);
					}
				}
			} else if (!root.isObject()) {
				throw new BadRequestException("参数格式错误");
			}
		} else {
			long departmentId;
			try {
				departmentId = Long.parseLong(departmentIdRaw == null ? "0" : departmentIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("参数格式错误");
			}
			userData.addAll(workWechatCorpUserApiService.getDetailedDepartmentUsers(companyId, departmentId));
		}

		Map<String, Object> result = workWechatSalespersonSyncPort.syncUserToSalesperson(companyId, userData);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "workwechat.rel.list")
	@GetMapping(value = "/rellist/{salespersonId}", name = "导购企微关联")
	public ApiResult<Map<String, Object>> getWorkWechatList(
			@PathVariable("salespersonId") String salespersonId,
			@RequestParam(value = "page", required = false, defaultValue = "1") String page,
			@RequestParam(value = "page_size", required = false, defaultValue = "20") String pageSize,
			@RequestParam(value = "is_friend", required = false, defaultValue = "0") String isFriend,
			@RequestParam(value = "is_bind", required = false, defaultValue = "0") String isBind) {
		Map<String, Object> data = workWechatRelListService.getWorkWechatRelList(salespersonId, page, pageSize, isFriend,
				isBind);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "workwechat.rel.log")
	@GetMapping(value = "/rellogs/{userId}", name = "导购企微关联日志")
	public ApiResult<Map<String, Object>> getWorkWechatLogsList(
			@PathVariable("userId") String userId,
			@RequestParam(value = "page", required = false, defaultValue = "1") String page,
			@RequestParam(value = "page_size", required = false, defaultValue = "20") String pageSize,
			@RequestParam(value = "is_friend", required = false, defaultValue = "0") String isFriend) {
		return ApiResult.ok(
				workWechatRelLogsListService.getWorkWechatRelLogsList(userId, page, pageSize, isFriend));
	}

	@Activated(routeAlias = "workwechat.distributor.js.config")
	@PostMapping(value = "/distributor/js/config", name = "JsSDK")
	public ApiResult<Map<String, Object>> getDistributorJsConfig(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Object urlObj = body == null ? null : body.get("url");
		String url = urlObj == null ? "" : String.valueOf(urlObj).trim();
		if (!StringUtils.hasText(url)) {
			throw new BadRequestException("当前页面url必填");
		}
		Map<String, Object> result = workWechatDistributorJsSdkService.buildDistributorJsConfig(companyId, url);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "workwechat.domain.verify")
	@PostMapping(value = "/domain/verify", name = "校验域名文件")
	public ApiResult<Map<String, Object>> verifyDomain(
			HttpServletRequest request,
			@RequestParam(value = "file", required = false) MultipartFile file) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readRequiredLong(ud, "operator_id");

		if (file == null || file.isEmpty()) {
			throw new BadRequestException("请选择上传文件");
		}
		String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
		if (ext == null || !"txt".equalsIgnoreCase(ext)) {
			throw new BadRequestException("上传文件只支持 txt 格式");
		}
		if (file.getSize() > 5000) {
			throw new BadRequestException("上传文件过大");
		}
		String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
		Matcher matcher = VERIFY_DOMAIN_FILENAME.matcher(fileName);
		if (!matcher.matches()) {
			throw new BadRequestException("不允许的文件名");
		}
		String stem = matcher.group(1);
		String contents;
		try {
			contents = new String(file.getBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new BadRequestException("读取上传文件失败");
		}

		workWechatVerifyDomainFileService.saveVerifyInfo(companyId, operatorId, stem, contents);
		return ApiResult.ok(Map.of("status", true));
	}

	private static long readRequiredLong(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new BadRequestException(key + " 缺失或无效");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException(key + " 缺失或无效");
		}
	}

	private static long readCompanyId(Map<?, ?> ud) {
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("未登录");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

}
