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

package cn.shopex.ecshopx.chinaumspay.api.admin.v1;

import cn.shopex.ecshopx.chinaumspay.service.DivisionErrorLogListService;
import cn.shopex.ecshopx.chinaumspay.service.DivisionErrorLogResubmitService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.chinaumspay.service.divisiondetail.DivisionDetailExportFilter;
import cn.shopex.ecshopx.chinaumspay.service.divisiondetail.DivisionDetailExportFilterBuilder;
import cn.shopex.ecshopx.chinaumspay.service.divisiondetail.DivisionDetailExportSubmitService;
import cn.shopex.ecshopx.chinaumspay.service.divisiondetail.DivisionDetailListService;
import cn.shopex.ecshopx.chinaumspay.service.divisionlist.DivisionListExportFilter;
import cn.shopex.ecshopx.chinaumspay.service.divisionlist.DivisionListExportFilterBuilder;
import cn.shopex.ecshopx.chinaumspay.service.divisionlist.DivisionListExportSubmitService;
import cn.shopex.ecshopx.chinaumspay.service.divisionlist.DivisionListService;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("chinaumsDivisionAdminV1")
@RequestMapping("/api/v1")
public class DivisionController {

	private final CompanysActivationService companysActivationService;
	private final DivisionErrorLogResubmitService divisionErrorLogResubmitService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;
	private final DivisionDetailExportFilterBuilder divisionDetailExportFilterBuilder;
	private final DivisionDetailExportSubmitService divisionDetailExportSubmitService;
	private final DivisionDetailListService divisionDetailListService;
	private final DivisionListExportFilterBuilder divisionListExportFilterBuilder;
	private final DivisionListExportSubmitService divisionListExportSubmitService;
	private final DivisionListService divisionListService;
	private final DivisionErrorLogListService divisionErrorLogListService;

	public DivisionController(
			CompanysActivationService companysActivationService,
			DivisionErrorLogResubmitService divisionErrorLogResubmitService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper,
			DivisionDetailExportFilterBuilder divisionDetailExportFilterBuilder,
			DivisionDetailExportSubmitService divisionDetailExportSubmitService,
			DivisionDetailListService divisionDetailListService,
			DivisionListExportFilterBuilder divisionListExportFilterBuilder,
			DivisionListExportSubmitService divisionListExportSubmitService,
			DivisionListService divisionListService,
			DivisionErrorLogListService divisionErrorLogListService) {
		this.companysActivationService = companysActivationService;
		this.divisionErrorLogResubmitService = divisionErrorLogResubmitService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
		this.divisionDetailExportFilterBuilder = divisionDetailExportFilterBuilder;
		this.divisionDetailExportSubmitService = divisionDetailExportSubmitService;
		this.divisionDetailListService = divisionDetailListService;
		this.divisionListExportFilterBuilder = divisionListExportFilterBuilder;
		this.divisionListExportSubmitService = divisionListExportSubmitService;
		this.divisionListService = divisionListService;
		this.divisionErrorLogListService = divisionErrorLogListService;
	}

	@Activated(routeAlias = "division.list")
	@GetMapping(value = "/division/list", name = "获取分账单列表")
	public ResponseEntity<Map<String, Object>> getList(HttpServletRequest request,
			@RequestParam(required = false) String back_status,
			@RequestParam(required = false) String time_start_begin,
			@RequestParam(required = false) String time_start_end,
			@RequestParam(required = false) Integer page,
			@RequestParam(name = "pageSize", required = false) Integer pageSize,
			@RequestParam(name = "page_size", required = false) Integer page_size) {
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

		Integer effectivePageSize = page_size != null ? page_size : pageSize;
		Map<String, Object> body = divisionListService.query(user, back_status, time_start_begin, time_start_end, page,
				effectivePageSize);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@Activated(routeAlias = "division.detail.list")
	@GetMapping(value = "/division/detail/list", name = "获取分账单详情列表")
	public ResponseEntity<Map<String, Object>> getDetailList(HttpServletRequest request,
			@RequestParam(required = false) String division_id,
			@RequestParam(required = false) String order_id,
			@RequestParam(required = false) String time_start_begin,
			@RequestParam(required = false) String time_start_end,
			@RequestParam(required = false) Integer page,
			@RequestParam(name = "pageSize", required = false) Integer pageSize,
			@RequestParam(name = "page_size", required = false) Integer page_size) {
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

		Integer effectivePageSize = page_size != null ? page_size : pageSize;
		Map<String, Object> body = divisionDetailListService.query(user, division_id, order_id, time_start_begin,
				time_start_end, page, effectivePageSize);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@Activated(routeAlias = "division.errorlog.list")
	@GetMapping(value = "/division/errorlog/list", name = "获取分账失败列表")
	public ResponseEntity<Map<String, Object>> errorlogList(HttpServletRequest request,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String order_id,
			@RequestParam(required = false) String time_start_begin,
			@RequestParam(required = false) String time_start_end,
			@RequestParam(required = false) Integer page,
			@RequestParam(name = "pageSize", required = false) Integer pageSize,
			@RequestParam(name = "page_size", required = false) Integer page_size) {
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

		Integer effectivePageSize = page_size != null ? page_size : pageSize;
		Map<String, Object> body = divisionErrorLogListService.query(user, status, order_id, time_start_begin,
				time_start_end, page, effectivePageSize);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO_FIXED)
	@Activated(routeAlias = "division.errorlog.resubmit")
	@PutMapping(value = "/division/errorlog/resubmit/{id}", name = "分账失败重试")
	public ResponseEntity<ApiResult<Map<String, Object>>> errrorlogResubmit(
			HttpServletRequest request, @PathVariable("id") String id) {
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

		Map<String, Object> data = divisionErrorLogResubmitService.resubmit(companyId, id);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/division/errorlog/resubmit/" + id);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("id", id)));
		} catch (Exception e) {
			logCtx.put("params", Map.of("id", id).toString());
		}
		logCtx.put("operator_name", "分账失败日志重新提交");
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

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO_FIXED)
	@Activated(routeAlias = "division.exportdata")
	@GetMapping(value = "/division/exportdata", name = "分账单导出")
	public ResponseEntity<Map<String, Object>> exportDivisionData(HttpServletRequest request,
			@RequestParam(required = false) String back_status,
			@RequestParam(required = false) String time_start_begin,
			@RequestParam(required = false) String time_start_end) {
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

		DivisionListExportFilter filter = divisionListExportFilterBuilder.build(user, back_status, time_start_begin,
				time_start_end);
		long operatorId = toLong(user.get("operator_id"));
		divisionListExportSubmitService.submit(filter, operatorId);

		return ResponseEntity.ok(Map.of("data", Map.of("status", Boolean.TRUE)));
	}

	@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO_FIXED)
	@Activated(routeAlias = "division.detail.exportdata")
	@GetMapping(value = "/division/detail/exportdata", name = "分账单明细导出")
	public ResponseEntity<Map<String, Object>> exportDivisionDetailData(HttpServletRequest request,
			@RequestParam(required = false) String division_id,
			@RequestParam(required = false) String order_id,
			@RequestParam(required = false) String time_start_begin,
			@RequestParam(required = false) String time_start_end) {
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

		DivisionDetailExportFilter filter = divisionDetailExportFilterBuilder.build(user, division_id, order_id,
				time_start_begin, time_start_end);
		long operatorId = toLong(ud.get("operator_id"));
		divisionDetailExportSubmitService.submit(filter, operatorId);

		return ResponseEntity.ok(Map.of("data", Map.of("status", Boolean.TRUE)));
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
}
