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
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.datacube.service.DatacubeShopRoutePermissionService;
import cn.shopex.ecshopx.datacube.service.SourcesCreateService;
import cn.shopex.ecshopx.datacube.service.SourcesDeleteService;
import cn.shopex.ecshopx.datacube.service.SourcesDetailService;
import cn.shopex.ecshopx.datacube.service.SourcesListService;
import cn.shopex.ecshopx.datacube.service.SourcesPatchSaveTagsService;
import cn.shopex.ecshopx.datacube.service.SourcesUpdateService;
import cn.shopex.ecshopx.datacube.web.DatacubeAdminFlexibleInputMerge;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("datacubeAdminV1Sources")
@RequestMapping("/api/v1/datacube")
public class SourcesController {

	private final CompanysActivationService companysActivationService;
	private final DatacubeShopRoutePermissionService datacubeShopRoutePermissionService;
	private final SourcesCreateService sourcesCreateService;
	private final SourcesDeleteService sourcesDeleteService;
	private final SourcesUpdateService sourcesUpdateService;
	private final SourcesPatchSaveTagsService sourcesPatchSaveTagsService;
	private final SourcesListService sourcesListService;
	private final SourcesDetailService sourcesDetailService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public SourcesController(
			CompanysActivationService companysActivationService,
			DatacubeShopRoutePermissionService datacubeShopRoutePermissionService,
			SourcesCreateService sourcesCreateService,
			SourcesDeleteService sourcesDeleteService,
			SourcesUpdateService sourcesUpdateService,
			SourcesPatchSaveTagsService sourcesPatchSaveTagsService,
			SourcesListService sourcesListService,
			SourcesDetailService sourcesDetailService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.datacubeShopRoutePermissionService = datacubeShopRoutePermissionService;
		this.sourcesCreateService = sourcesCreateService;
		this.sourcesDeleteService = sourcesDeleteService;
		this.sourcesUpdateService = sourcesUpdateService;
		this.sourcesPatchSaveTagsService = sourcesPatchSaveTagsService;
		this.sourcesListService = sourcesListService;
		this.sourcesDetailService = sourcesDetailService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "source.create")
	@PostMapping(value = "/sources", name = "添加来源")
	public ResponseEntity<ApiResult<Map<String, Object>>> createSources(
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
		datacubeShopRoutePermissionService.assertSourceCreate(user);

		Map<String, Object> merged = DatacubeAdminFlexibleInputMerge.merge(request, body);

		Object snObj = merged.get("source_name");
		String sourceName = snObj != null ? String.valueOf(snObj).trim() : "";
		if (!StringUtils.hasText(sourceName)) {
			throw new ResourceException("添加来源出错: source_name 不能为空");
		}

		Object tagsIdRaw = merged.containsKey("tags_id") ? merged.get("tags_id") : "";

		Map<String, Object> dataRow = sourcesCreateService.addSources(companyId, sourceName, tagsIdRaw);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/datacube/sources");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "添加来源");
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

	@Activated(routeAlias = "source.list")
	@GetMapping(value = "/sources", name = "获取来源列表")
	public ResponseEntity<Map<String, Object>> getSourcesList(
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
		datacubeShopRoutePermissionService.assertSourceList(user);

		Map<String, Object> merged = DatacubeAdminFlexibleInputMerge.merge(request, body);
		String pageRaw = scalarToString(merged.get("page"));
		String pageSizeRaw = scalarToString(merged.get("pageSize"));

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		if (isTruthySourceNameFilter(merged.get("source_name"))) {
			filter.put("source_name", normalizeSourceNameFilterValue(merged.get("source_name")));
		}

		Map<String, Object> result = sourcesListService.getSourcesList(filter, pageRaw, pageSizeRaw);
		return ResponseEntity.ok(Map.of("data", result));
	}

	@Activated(routeAlias = "source.detail")
	@GetMapping(value = "/sources/{source_id}", name = "获取来源详情")
	public ResponseEntity<Map<String, Object>> getSourcesDetail(
			HttpServletRequest request, @PathVariable("source_id") String sourceId) {
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
		datacubeShopRoutePermissionService.assertSourceDetail(user);

		if (sourceId == null || !StringUtils.hasText(sourceId.trim())) {
			throw new ResourceException("获取来源详情出错: source_id 不能为空");
		}
		String trimmed = sourceId.trim();
		long id;
		try {
			id = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("获取来源详情出错: source_id 须为整数");
		}
		if (id < 1) {
			throw new ResourceException("获取来源详情出错: source_id 须大于 0");
		}

		Map<String, Object> result = sourcesDetailService.buildDetailMap(id);
		long rowCompany = toLong(result.get("company_id"));
		if (rowCompany != companyId) {
			throw new ResourceException("获取门店信息有误，请确认您的门店的ID.");
		}
		sourcesDetailService.appendCheckTagsIfTruthy(companyId, result);
		return ResponseEntity.ok(Map.of("data", result));
	}

	@Activated(routeAlias = "source.delete")
	@DeleteMapping(value = "/sources/{source_id}", name = "删除来源")
	public ResponseEntity<Void> deleteSources(HttpServletRequest request, @PathVariable("source_id") String sourceId) {
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
		datacubeShopRoutePermissionService.assertSourceDelete(user);

		if (sourceId == null || !StringUtils.hasText(sourceId.trim())) {
			throw new ResourceException("删除来源出错: source_id 不能为空");
		}
		String trimmed = sourceId.trim();
		long id;
		try {
			id = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("删除来源出错: source_id 须为整数");
		}
		if (id < 1) {
			throw new ResourceException("删除来源出错: source_id 须大于 0");
		}

		sourcesDeleteService.deleteSources(companyId, id);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/datacube/sources");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("source_id", sourceId)));
		} catch (Exception e) {
			logCtx.put("params", String.valueOf(Map.of("source_id", sourceId)));
		}
		logCtx.put("operator_name", "删除来源");
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

	@Activated(routeAlias = "source.update")
	@PutMapping(value = "/sources/{source_id}", name = "更新来源")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateSources(
			HttpServletRequest request,
			@PathVariable("source_id") String sourceId,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
		datacubeShopRoutePermissionService.assertSourceUpdate(user);

		Map<String, Object> merged = DatacubeAdminFlexibleInputMerge.merge(request, body);
		String trimmed = sourceId != null ? sourceId.trim() : "";
		merged.put("source_id", trimmed);

		Object snObj = merged.get("source_name");
		String sourceName = snObj != null ? String.valueOf(snObj).trim() : "";
		if (!StringUtils.hasText(sourceName)) {
			throw new ResourceException("更新来源出错: source_name 不能为空");
		}

		if (!StringUtils.hasText(trimmed)) {
			throw new ResourceException("更新来源出错: source_id 不能为空");
		}
		long id;
		try {
			id = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("更新来源出错: source_id 须为整数");
		}
		if (id < 1) {
			throw new ResourceException("更新来源出错: source_id 须大于 0");
		}

		Object tagsIdRaw = merged.containsKey("tags_id") ? merged.get("tags_id") : "";

		Map<String, Object> dataRow = sourcesUpdateService.updateSources(companyId, id, sourceName, tagsIdRaw);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/datacube/sources");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "更新来源");
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

	@Activated(routeAlias = "source.savetags")
	@PostMapping(value = "/savetags", name = "来源绑定会员标签")
	public ResponseEntity<Void> saveSourceTags(
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
		datacubeShopRoutePermissionService.assertSourceSaveTags(user);

		Map<String, Object> merged = DatacubeAdminFlexibleInputMerge.mergeQueryThenBody(request, body);
		sourcesPatchSaveTagsService.patchSaveTags(
				companyId, merged.get("tags_id"), merged.get("source_ids"));

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/datacube/savetags");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "来源绑定会员标签");
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

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String scalarToString(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof String s) {
			return s;
		}
		if (o instanceof Number || o instanceof Boolean) {
			return String.valueOf(o);
		}
		return String.valueOf(o);
	}

	private static boolean isTruthySourceNameFilter(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b && !b) {
			return false;
		}
		if (v instanceof Number n && n.doubleValue() == 0.0) {
			return false;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		String t = String.valueOf(v).trim();
		return !t.isEmpty() && !"0".equals(t);
	}

	private static String normalizeSourceNameFilterValue(Object v) {
		if (v instanceof String s) {
			return s.trim();
		}
		return String.valueOf(v).trim();
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}
}
