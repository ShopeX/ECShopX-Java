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

package cn.shopex.ecshopx.datacube.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.datacube.service.DatacubeShopRoutePermissionService;
import cn.shopex.ecshopx.datacube.service.MonitorsAddService;
import cn.shopex.ecshopx.datacube.service.MonitorsDeleteService;
import cn.shopex.ecshopx.datacube.service.MonitorsDetailService;
import cn.shopex.ecshopx.datacube.service.MonitorsListService;
import cn.shopex.ecshopx.datacube.service.MonitorsRelSourcesService;
import cn.shopex.ecshopx.datacube.service.MonitorsStatsService;
import cn.shopex.ecshopx.datacube.service.MonitorsWxaCode64Service;
import cn.shopex.ecshopx.datacube.web.DatacubeAdminFlexibleInputMerge;
import cn.shopex.ecshopx.datacube.web.TrackFlexibleInputMerge;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@AdminAuth
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("datacubeAdminV1SourcesMonitors")
@RequestMapping("/api/v1/datacube")
public class SourcesMonitorsController {

	private final CompanysActivationService companysActivationService;
	private final DatacubeShopRoutePermissionService datacubeShopRoutePermissionService;
	private final MonitorsAddService monitorsAddService;
	private final MonitorsDeleteService monitorsDeleteService;
	private final MonitorsDetailService monitorsDetailService;
	private final MonitorsListService monitorsListService;
	private final MonitorsRelSourcesService monitorsRelSourcesService;
	private final MonitorsStatsService monitorsStatsService;
	private final MonitorsWxaCode64Service monitorsWxaCode64Service;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public SourcesMonitorsController(
			CompanysActivationService companysActivationService,
			DatacubeShopRoutePermissionService datacubeShopRoutePermissionService,
			MonitorsAddService monitorsAddService,
			MonitorsDeleteService monitorsDeleteService,
			MonitorsDetailService monitorsDetailService,
			MonitorsListService monitorsListService,
			MonitorsRelSourcesService monitorsRelSourcesService,
			MonitorsStatsService monitorsStatsService,
			MonitorsWxaCode64Service monitorsWxaCode64Service,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.datacubeShopRoutePermissionService = datacubeShopRoutePermissionService;
		this.monitorsAddService = monitorsAddService;
		this.monitorsDeleteService = monitorsDeleteService;
		this.monitorsDetailService = monitorsDetailService;
		this.monitorsListService = monitorsListService;
		this.monitorsRelSourcesService = monitorsRelSourcesService;
		this.monitorsStatsService = monitorsStatsService;
		this.monitorsWxaCode64Service = monitorsWxaCode64Service;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@ShopLog
	@Activated(routeAlias = "monitors.list")
	@GetMapping(value = "/monitors", name = "获取页面监控列表")
	public ResponseEntity<Map<String, Object>> getSourcesMonitors(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "pageSize", required = false) String pageSizeRaw) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertMonitorsList(user);

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		var pm = request.getParameterMap();
		if (pm.containsKey("wxappid")) {
			String v = request.getParameter("wxappid");
			filter.put("wxappid", v != null ? v : "");
		}
		if (pm.containsKey("regionauth_id")) {
			String v = request.getParameter("regionauth_id");
			filter.put("regionauth_id", v != null ? v : "");
		}
		if (pm.containsKey("monitorPath")) {
			String v = request.getParameter("monitorPath");
			filter.put("monitor_path", v != null ? v : "");
		}

		Map<String, Object> result = monitorsListService.getMonitorsList(filter, pageRaw, pageSizeRaw);
		return ResponseEntity.ok(Map.of("data", result));
	}

	@ShopLog
	@Activated(routeAlias = "monitors.detail")
	@GetMapping(value = "/monitors/{monitor_id}", name = "获取监控详情")
	public ResponseEntity<Map<String, Object>> getMonitorsDetail(
			HttpServletRequest request, @PathVariable("monitor_id") String monitorId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long jwtCompanyId = toLong(companyObj);
		if (jwtCompanyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(jwtCompanyId);
		datacubeShopRoutePermissionService.assertMonitorsList(user);

		String msg = "获取监控页面详情出错.";
		if (monitorId == null || !StringUtils.hasText(monitorId.trim())) {
			throw new ResourceException(msg + ": monitor_id 不能为空");
		}
		String trimmed = monitorId.trim();
		long parsed;
		try {
			parsed = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException(msg + ": monitor_id 须为整数");
		}
		if (parsed < 1L) {
			throw new ResourceException(msg + ": monitor_id 须大于 0");
		}

		Map<String, Object> result = monitorsDetailService.getDetailMapByMonitorId(parsed);
		long rowCompanyId = toLong(result.get("company_id"));
		if (jwtCompanyId != rowCompanyId) {
			throw new ResourceException("获取监控页面信息有误，请确认您的监控页面的ID.");
		}

		return ResponseEntity.ok(Map.of("data", result));
	}

	@ShopLog
	@Activated(routeAlias = "monitors.delete")
	@DeleteMapping(value = "/monitors/{monitor_id}", name = "删除监控页面")
	public ResponseEntity<Void> deleteMonitors(
			HttpServletRequest request, @PathVariable("monitor_id") String monitorId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertMonitorsDelete(user);

		String msg = "删除监控页面出错.";
		if (monitorId == null || !StringUtils.hasText(monitorId.trim())) {
			throw new ResourceException(msg + ": monitor_id 不能为空");
		}
		String trimmed = monitorId.trim();
		long parsed;
		try {
			parsed = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException(msg + ": monitor_id 须为整数");
		}
		if (parsed < 1L) {
			throw new ResourceException(msg + ": monitor_id 须大于 0");
		}

		monitorsDeleteService.deleteMonitor(companyId, parsed);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/datacube/monitors");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("monitor_id", monitorId)));
		} catch (Exception e) {
			logCtx.put("params", String.valueOf(Map.of("monitor_id", monitorId)));
		}
		logCtx.put("operator_name", "删除监控页面");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.noContent().build();
	}

	@ShopLog
	@Activated(routeAlias = "monitors.add")
	@PostMapping(value = "/monitors", name = "添加监控链接")
	public ResponseEntity<ApiResult<Map<String, Object>>> addMonitors(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertMonitorsAdd(user);

		Map<String, Object> merged = DatacubeAdminFlexibleInputMerge.merge(request, body);

		Map<String, List<String>> requiredFieldErrors = new LinkedHashMap<>();
		Object wxObj = merged.get("wxappid");
		String wxappid = wxObj != null ? String.valueOf(wxObj).trim() : "";
		if (!StringUtils.hasText(wxappid)) {
			requiredFieldErrors.put("wxappid", List.of("The wxappid field is required."));
		}

		Object pathObj = merged.get("monitor_path");
		String monitorPath = pathObj != null ? String.valueOf(pathObj).trim() : "";
		if (!StringUtils.hasText(monitorPath)) {
			requiredFieldErrors.put("monitor_path", List.of("The monitor path field is required."));
		}

		if (!requiredFieldErrors.isEmpty()) {
			throw new ResourceException(
					"添加监控链接出错: 缺少必填字段 " + String.join(", ", requiredFieldErrors.keySet()));
		}

		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		String encodedParams = "";
		if (merged.containsKey("monitor_path_params")) {
			Object mpp = merged.get("monitor_path_params");
			if (mpp != null) {
				if (!(mpp instanceof List<?> list)) {
					fieldErrors.put(
							"monitor_path_params", List.of("The monitor path params must be an array."));
				} else {
					LinkedHashMap<String, String> urlParams = new LinkedHashMap<>();
					for (int i = 0; i < list.size(); i++) {
						Object el = list.get(i);
						String pnKey = "monitor_path_params." + i + ".param_name";
						String pnRequiredMsg = "The monitor_path_params." + i + ".param_name field is required.";
						if (!(el instanceof Map<?, ?> itemMap)) {
							fieldErrors.put(pnKey, List.of(pnRequiredMsg));
							continue;
						}
						Object pnRaw = itemMap.get("param_name");
						String paramName = pnRaw != null ? String.valueOf(pnRaw).trim() : "";
						if (!StringUtils.hasText(paramName)) {
							fieldErrors.put(pnKey, List.of(pnRequiredMsg));
							continue;
						}
						if (!itemMap.containsKey("value") || itemMap.get("value") == null) {
							fieldErrors.put(
									"monitor_path_params." + i + ".value",
									List.of(
											"The monitor_path_params."
													+ i
													+ ".value field is required when monitor_path_params."
													+ i
													+ ".param_name is present."));
							continue;
						}
						Object valObj = itemMap.get("value");
						String valStr = String.valueOf(valObj);
						if (!StringUtils.hasText(valStr.trim())) {
							fieldErrors.put(
									"monitor_path_params." + i + ".value",
									List.of(
											"The monitor_path_params."
													+ i
													+ ".value field is required when monitor_path_params."
													+ i
													+ ".param_name is present."));
							continue;
						}
						urlParams.put(paramName, valStr);
					}
					if (fieldErrors.isEmpty() && !list.isEmpty()) {
						encodedParams = httpBuildQuery(urlParams);
					}
				}
			}
		}

		if (!fieldErrors.isEmpty()) {
			throw new ResourceException("添加监控链接出错.");
		}

		if (!inputHasMonitorIdKey(request, merged, body)) {
			throw new BadRequestException("缺少必填字段: monitor_id");
		}

		Object pageNameObj = merged.get("page_name");
		String pageName = pageNameObj != null ? String.valueOf(pageNameObj) : "";

		Object regionObj = merged.get("regionauth_id");
		String regionauthId = regionObj != null ? String.valueOf(regionObj) : "";

		Map<String, Object> dataRow =
				monitorsAddService.addMonitor(companyId, wxappid, monitorPath, encodedParams, pageName, regionauthId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/datacube/monitors");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "添加监控链接");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(dataRow));
	}

	private static String httpBuildQuery(LinkedHashMap<String, String> ordered) {
		List<String> parts = new ArrayList<>(ordered.size());
		for (Map.Entry<String, String> e : ordered.entrySet()) {
			String k = e.getKey();
			String v = e.getValue() != null ? e.getValue() : "";
			parts.add(
					URLEncoder.encode(k, StandardCharsets.UTF_8)
							+ "="
							+ URLEncoder.encode(v, StandardCharsets.UTF_8));
		}
		return String.join("&", parts);
	}

	@ShopLog
	@Activated(routeAlias = "monitors.relsources.detail")
	@PostMapping(value = "/monitorsRelSources", name = "监控页面关联来源")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> relSources(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertMonitorsRelSourcesDetail(user);

		Map<String, Object> merged = DatacubeAdminFlexibleInputMerge.merge(request, body);

		String relSourcesMsg = "保存监控页面关联来源出错.";

		Object monitorObj = merged.get("monitor_id");
		boolean monitorIdRequired = false;
		if (monitorObj == null) {
			monitorIdRequired = true;
		} else if (!(monitorObj instanceof Number)) {
			String monitorTrimmed = String.valueOf(monitorObj).trim();
			if (!StringUtils.hasText(monitorTrimmed)) {
				monitorIdRequired = true;
			}
		}

		boolean sourceIdsMissing = !merged.containsKey("sourceIds") || merged.get("sourceIds") == null;
		Object sourceIdsRaw = merged.get("sourceIds");
		if (!sourceIdsMissing && !(sourceIdsRaw instanceof List<?>)) {
			throw new BadRequestException("参数格式错误: source_ids 须为数组");
		}

		Map<String, List<String>> relRequiredErrors = new LinkedHashMap<>();
		if (monitorIdRequired) {
			relRequiredErrors.put("monitor_id", List.of("The monitor id field is required."));
		}
		if (sourceIdsMissing) {
			relRequiredErrors.put("sourceIds", List.of("The source ids field is required."));
		}
		if (!relRequiredErrors.isEmpty()) {
			throw new ResourceException(
					relSourcesMsg + ": 缺少必填字段 " + String.join(", ", relRequiredErrors.keySet()));
		}

		Object rawMonitorId;
		if (monitorObj instanceof Number n) {
			if (n.longValue() < 1L) {
				throw new ResourceException(relSourcesMsg + ": monitor_id 须大于 0");
			}
			rawMonitorId = monitorObj;
		} else {
			String monitorTrimmed = String.valueOf(monitorObj).trim();
			rawMonitorId = monitorObj instanceof String ? monitorTrimmed : monitorObj;
		}

		List<?> sourceList = (List<?>) sourceIdsRaw;
		if (sourceList.isEmpty()) {
			throw new ResourceException(relSourcesMsg + ": sourceIds 不能为空");
		}

		List<Map<String, Object>> rows =
				monitorsRelSourcesService.replaceRelSources(companyId, rawMonitorId, sourceList);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/datacube/monitorsRelSources");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "监控页面关联来源");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(rows));
	}

	@ShopLog
	@Activated(routeAlias = "monitors.relsources.list")
	@GetMapping(value = "/monitorsRelSources/{monitor_id}", name = "获取监控页面关联来源信息")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getRelSources(
			HttpServletRequest request, @PathVariable("monitor_id") String monitorId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertMonitorsRelSourcesList(user);

		String msg = "获取监控页面关联来源出错.";
		if (monitorId == null || !StringUtils.hasText(monitorId.trim())) {
			throw new ResourceException(msg + ": monitor_id 不能为空");
		}
		String trimmed = monitorId.trim();
		Object rawMonitorId;
		try {
			rawMonitorId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			rawMonitorId = trimmed;
		}

		List<Map<String, Object>> rows =
				monitorsRelSourcesService.listRelSourcesWithSourceNames(companyId, rawMonitorId);
		return ResponseEntity.ok(ApiResult.ok(rows));
	}

	@ShopLog
	@Activated(routeAlias = "monitors.relsources.delete")
	@DeleteMapping(value = "/monitorsRelSources/{monitor_id}/{source_id}", name = "删除监控页面的某个来源")
	public ResponseEntity<Void> deleteRelSources(
			HttpServletRequest request,
			@PathVariable("monitor_id") String monitorId,
			@PathVariable("source_id") String sourceId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertMonitorsRelSourcesDelete(user);

		Map<String, List<String>> pathErrors = new LinkedHashMap<>();
		if (monitorId == null || !StringUtils.hasText(monitorId.trim())) {
			pathErrors.put("monitor_id", List.of("The monitor id field is required."));
		}
		if (sourceId == null || !StringUtils.hasText(sourceId.trim())) {
			pathErrors.put("source_id", List.of("The source id field is required."));
		}
		if (!pathErrors.isEmpty()) {
			throw new ResourceException(
					"删除监控页面关联来源出错: 缺少必填字段 " + String.join(", ", pathErrors.keySet()));
		}

		String trimmedMonitor = monitorId.trim();
		Object rawMonitorId;
		try {
			rawMonitorId = Long.parseLong(trimmedMonitor);
		} catch (NumberFormatException e) {
			rawMonitorId = trimmedMonitor;
		}

		String trimmedSource = sourceId.trim();
		Object rawSourceId;
		try {
			rawSourceId = Long.parseLong(trimmedSource);
		} catch (NumberFormatException e) {
			rawSourceId = trimmedSource;
		}

		monitorsRelSourcesService.deleteOneRelSource(companyId, rawMonitorId, rawSourceId);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/datacube/monitorsRelSources");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put(
					"params",
					objectMapper.writeValueAsString(Map.of("monitor_id", monitorId, "source_id", sourceId)));
		} catch (Exception e) {
			logCtx.put("params", String.valueOf(Map.of("monitor_id", monitorId, "source_id", sourceId)));
		}
		logCtx.put("operator_name", "删除监控页面的某个来源");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok().build();
	}

	private static final Set<String> STATS_DATE_TYPES =
			Set.of("today", "yesterday", "before7days", "before30days", "beforemonth", "custom");

	@ShopLog
	@Activated(routeAlias = "monitors.stats")
	@GetMapping(value = "/monitorsstats", name = "获取监控页面的来源统计")
	public ResponseEntity<Map<String, Object>> getStats(
			HttpServletRequest request,
			@RequestParam(name = "monitor_id", required = false) String monitorIdRaw,
			@RequestParam(name = "date_type", required = false) String dateTypeRaw,
			@RequestParam(name = "begin_date", required = false) String beginDateRaw,
			@RequestParam(name = "end_date", required = false) String endDateRaw) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertMonitorsList(user);

		String statsMsg = "获取监控页面的来源统计出错.";
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

		if (monitorIdRaw == null || !StringUtils.hasText(monitorIdRaw.trim())) {
			fieldErrors.put("monitor_id", List.of("The monitor id field is required."));
		}

		if (dateTypeRaw == null || !StringUtils.hasText(dateTypeRaw.trim())) {
			fieldErrors.put("date_type", List.of("The date type field is required."));
		} else if (!STATS_DATE_TYPES.contains(dateTypeRaw.trim())) {
			fieldErrors.put("date_type", List.of("The selected date type is invalid."));
		}

		String dateType = dateTypeRaw != null ? dateTypeRaw.trim() : "";
		if ("custom".equals(dateType)) {
			if (beginDateRaw == null || !StringUtils.hasText(beginDateRaw.trim())) {
				fieldErrors.put(
						"begin_date", List.of("The begin date field is required when date type is custom."));
			}
			if (endDateRaw == null || !StringUtils.hasText(endDateRaw.trim())) {
				fieldErrors.put("end_date", List.of("The end date field is required when date type is custom."));
			}
		}

		if (!fieldErrors.isEmpty()) {
			throw new ResourceException(statsMsg);
		}

		String monitorTrimmed = monitorIdRaw.trim();
		String beginTrimmed = beginDateRaw != null ? beginDateRaw.trim() : "";
		String endTrimmed = endDateRaw != null ? endDateRaw.trim() : "";

		Map<String, Object> result =
				monitorsStatsService.buildStats(companyId, monitorTrimmed, dateType, beginTrimmed, endTrimmed);
		return ResponseEntity.ok(Map.of("data", result));
	}

	@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
	@GetMapping(value = "/monitorsWxaCode64", name = "获取监控的小程序码参数")
	public ResponseEntity<Map<String, Object>> getMonitorWxaCode64(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = TrackFlexibleInputMerge.merge(request, body);
		ParsedMonitorWxaCodeInput in = parseMonitorWxaCodeMerged(merged);
		Map<String, Object> data = monitorsWxaCode64Service.buildBase64Image(in.mid(), in.mScene(), in.sScene());
		return ResponseEntity.ok(Map.of("data", data));
	}

	@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
	@GetMapping(value = "/monitorsWxaCodeStream", name = "获取监控的小程序码流信息")
	public ResponseEntity<byte[]> getMonitorWxaCodeStream(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = TrackFlexibleInputMerge.merge(request, body);
		ParsedMonitorWxaCodeInput in = parseMonitorWxaCodeMerged(merged);
		byte[] bytes = monitorsWxaCode64Service.buildJpegBytesWithWidth1280(in.mid(), in.mScene(), in.sScene());
		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes);
	}

	private record ParsedMonitorWxaCodeInput(long mid, String mScene, String sScene) {}

	private static ParsedMonitorWxaCodeInput parseMonitorWxaCodeMerged(Map<String, Object> merged) {
		String validationMsg = "获取小程序码参数出错，请检查.";
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

		String monitorPresent = null;
		if (!merged.containsKey("monitor_id") || merged.get("monitor_id") == null) {
			fieldErrors.put("monitor_id", List.of("The monitor_id field is required."));
		} else {
			monitorPresent = scalarInputToValidationString(merged.get("monitor_id"));
			if (monitorPresent == null || monitorPresent.isEmpty()) {
				fieldErrors.put("monitor_id", List.of("The monitor_id field is required."));
			}
		}

		String sourcePresent = null;
		if (!merged.containsKey("source_id") || merged.get("source_id") == null) {
			fieldErrors.put("source_id", List.of("The source_id field is required."));
		} else {
			sourcePresent = scalarInputToValidationString(merged.get("source_id"));
			if (sourcePresent == null || sourcePresent.isEmpty()) {
				fieldErrors.put("source_id", List.of("The source_id field is required."));
			}
		}

		if (!fieldErrors.isEmpty()) {
			throw new ResourceException(validationMsg);
		}

		long mid;
		try {
			mid = Long.parseLong(monitorPresent.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(validationMsg + ": monitor_id 须为整数");
		}

		String mScene = monitorPresent.trim();
		String sScene = sourcePresent.trim();
		return new ParsedMonitorWxaCodeInput(mid, mScene, sScene);
	}

	private static String scalarInputToValidationString(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof String s) {
			return s.trim();
		}
		if (v instanceof Number || v instanceof Boolean) {
			return String.valueOf(v);
		}
		return String.valueOf(v).trim();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	/** Whether {@code monitor_id} appears on the merged map, the parsed body, or servlet parameters. */
	private static boolean inputHasMonitorIdKey(
			HttpServletRequest request, Map<String, Object> merged, Map<String, Object> body) {
		if (merged.containsKey("monitor_id")) {
			return true;
		}
		if (body != null && body.containsKey("monitor_id")) {
			return true;
		}
		return request.getParameterMap().containsKey("monitor_id");
	}
}
