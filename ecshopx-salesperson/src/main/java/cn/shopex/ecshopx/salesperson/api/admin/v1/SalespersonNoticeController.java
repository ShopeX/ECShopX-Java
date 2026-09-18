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

package cn.shopex.ecshopx.salesperson.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.salesperson.service.SalespersonNoticeAddService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true
)
@AdminAuth
@ShopLog
@RestController("salespersonAdminV1SalespersonNotice")
@RequestMapping("/api/v1/salespersonotice")
public class SalespersonNoticeController {

	private final SalespersonNoticeAddService salespersonNoticeAddService;

	public SalespersonNoticeController(SalespersonNoticeAddService salespersonNoticeAddService) {
		this.salespersonNoticeAddService = salespersonNoticeAddService;
	}

	@Activated(routeAlias = "salesperson.notice.add")
	@PostMapping(value = "/notice", name = "添加导购员通知")
	public ResponseEntity<Map<String, Object>> addNotice(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) raw;
		Object cid = userData.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, arr) -> {
			if ("distributorIds".equals(k) || "distributor_ids".equals(k)) {
				return;
			}
			if (arr != null && arr.length > 0 && StringUtils.hasText(arr[0])) {
				merged.put(k, arr[0]);
			}
		});
		putDistributorQueryParams(request, merged, "distributorIds");
		putDistributorQueryParams(request, merged, "distributor_ids");
		if (body != null) {
			merged.putAll(body);
		}

		Map<String, Object> inner = salespersonNoticeAddService.addNotice(companyId, merged, userData);
		return ResponseEntity.ok(Map.of("data", inner));
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		return dingo422(ex.getMessage(), ex.getFieldErrors(), statusCode);
	}

	@ExceptionHandler(ResourceException.class)
	public ResponseEntity<?> handleResource(ResourceException ex) {
		int statusCode = ex.getEmbeddedStatusCode() != null ? ex.getEmbeddedStatusCode() : 422;
		return dingo422(ex.getMessage(), ex.getFieldErrors(), statusCode);
	}

	private static ResponseEntity<?> dingo422(String message, Map<String, List<String>> errors, int statusCode) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		data.put("status_code", statusCode);
		if (errors != null && !errors.isEmpty()) {
			data.put("errors", errors);
		}
		return ResponseEntity.ok(Map.of("data", data));
	}

	private static Object readRequestLayerDistributorIds(HttpServletRequest request) {
		Object attr = request.getAttribute("distributorIds");
		if (attr != null) {
			return attr;
		}
		String[] arr = request.getParameterValues("distributorIds");
		if (arr == null || arr.length == 0) {
			return null;
		}
		if (arr.length == 1) {
			String s0 = arr[0];
			return s0 == null ? "" : s0.trim();
		}
		List<String> list = new ArrayList<>();
		for (String s : arr) {
			list.add(s == null ? "" : s.trim());
		}
		return list;
	}

	private static long parseNoticeIdForSend(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择通知", 422);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				throw new BadRequestException("请选择通知", 422);
			}
			try {
				long v = Long.parseLong(t);
				if (v == 0) {
					throw new BadRequestException("请选择通知", 422);
				}
				return v;
			} catch (NumberFormatException e) {
				throw new BadRequestException("请选择通知", 422);
			}
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v == 0) {
				throw new BadRequestException("请选择通知", 422);
			}
			return v;
		}
		String t = raw.toString().trim();
		if (t.isEmpty() || "0".equals(t)) {
			throw new BadRequestException("请选择通知", 422);
		}
		try {
			long v = Long.parseLong(t);
			if (v == 0) {
				throw new BadRequestException("请选择通知", 422);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("请选择通知", 422);
		}
	}

	private static long parseNoticeIdForWithdraw(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择要撤回的通知", 422);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				throw new BadRequestException("请选择要撤回的通知", 422);
			}
			try {
				long v = Long.parseLong(t);
				if (v == 0) {
					throw new BadRequestException("请选择要撤回的通知", 422);
				}
				return v;
			} catch (NumberFormatException e) {
				throw new BadRequestException("请选择要撤回的通知", 422);
			}
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v == 0) {
				throw new BadRequestException("请选择要撤回的通知", 422);
			}
			return v;
		}
		String t = raw.toString().trim();
		if (t.isEmpty() || "0".equals(t)) {
			throw new BadRequestException("请选择要撤回的通知", 422);
		}
		try {
			long v = Long.parseLong(t);
			if (v == 0) {
				throw new BadRequestException("请选择要撤回的通知", 422);
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("请选择要撤回的通知", 422);
		}
	}

	private static void putDistributorQueryParams(HttpServletRequest request, LinkedHashMap<String, Object> merged,
			String logicalKey) {
		String[] arr = request.getParameterValues(logicalKey);
		if (arr == null || arr.length == 0) {
			return;
		}
		if (arr.length == 1) {
			String s0 = arr[0];
			merged.put(logicalKey, s0 == null ? "" : s0.trim());
			return;
		}
		List<String> list = new ArrayList<>();
		for (String s : arr) {
			list.add(s == null ? "" : s.trim());
		}
		merged.put(logicalKey, list);
	}

	@Activated(routeAlias = "salesperson.notice.send")
	@PostMapping(value = "/sendnotice", name = "发送导购员通知")
	public ResponseEntity<Map<String, Object>> sendNotice(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawUser = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUser instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) rawUser;
		Object cid = userData.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		if (SalespersonNoticeAddService.distributorIdsExplicitPresent(readRequestLayerDistributorIds(request))) {
			throw new ResourceException("您没有此操作的权限");
		}

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, arr) -> {
			if ("distributorIds".equals(k) || "distributor_ids".equals(k)) {
				return;
			}
			if (arr != null && arr.length > 0 && StringUtils.hasText(arr[0])) {
				merged.put(k, arr[0]);
			}
		});
		putDistributorQueryParams(request, merged, "distributorIds");
		putDistributorQueryParams(request, merged, "distributor_ids");
		if (body != null) {
			merged.putAll(body);
		}

		long noticeId = parseNoticeIdForSend(merged.get("notice_id"));
		Map<String, Object> inner = salespersonNoticeAddService.sendNotice(companyId, noticeId, merged.get("distributor_id"));
		return ResponseEntity.ok(Map.of("data", inner));
	}

	@Activated(routeAlias = "salesperson.notice.withdraw")
	@PostMapping(value = "/withdrawnotice", name = "撤回导购员通知")
	public ResponseEntity<Map<String, Object>> withdrawNotice(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawUser = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUser instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) rawUser;
		Object cid = userData.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		if (SalespersonNoticeAddService.distributorIdsExplicitPresent(readRequestLayerDistributorIds(request))) {
			throw new ResourceException("您没有此操作的权限");
		}

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, arr) -> {
			if ("distributorIds".equals(k) || "distributor_ids".equals(k)) {
				return;
			}
			if (arr != null && arr.length > 0 && StringUtils.hasText(arr[0])) {
				merged.put(k, arr[0]);
			}
		});
		putDistributorQueryParams(request, merged, "distributorIds");
		putDistributorQueryParams(request, merged, "distributor_ids");
		if (body != null) {
			merged.putAll(body);
		}

		long noticeId = parseNoticeIdForWithdraw(merged.get("notice_id"));
		Map<String, Object> inner = salespersonNoticeAddService.withdrawNotice(companyId, noticeId);
		return ResponseEntity.ok(Map.of("data", inner));
	}

	@Activated(routeAlias = "salesperson.notice.list")
	@GetMapping(value = "/list", name = "导购员通知列表")
	public ResponseEntity<Map<String, Object>> getNoticeList(
			HttpServletRequest request,
			@RequestParam(name = "title", required = false) String title,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "page_size", defaultValue = "10") int pageSize) {
		Object rawUser = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUser instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) rawUser;
		Object cid = userData.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		int statusFilter = parseNoticeListStatusFilter(statusParam);
		if (page < 1 || pageSize < 1) {
			throw new ResourceException("分页参数不合法");
		}

		String titleForQuery = StringUtils.hasText(title) ? title.trim() : null;
		Map<String, Object> payload = salespersonNoticeAddService.getNoticeList(companyId, titleForQuery, statusFilter,
				page, pageSize);
		return ResponseEntity.ok(Map.of("data", payload));
	}

	private static int parseNoticeListStatusFilter(String statusParam) {
		if (statusParam == null || statusParam.trim().isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(statusParam.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Activated(routeAlias = "salesperson.notice.info")
	@GetMapping(value = "/detail", name = "导购员通知详情")
	public ResponseEntity<Map<String, Object>> getNoticeDetail(HttpServletRequest request) {
		Object rawUser = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUser instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) rawUser;
		Object cid = userData.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		long noticeId = parseNoticeIdForSend(request.getParameter("notice_id"));
		int withLog = parseWithLogQuery(request);
		Object inner = salespersonNoticeAddService.getNoticeDetail(companyId, noticeId, withLog);
		return ResponseEntity.ok(Map.of("data", inner));
	}

	private static int parseWithLogQuery(HttpServletRequest request) {
		String raw = request.getParameter("with_log");
		if (raw == null) {
			return 0;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(trimmed);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Activated(routeAlias = "salesperson.notice.delete")
	@DeleteMapping(value = "/notice", name = "删除导购员通知")
	public ResponseEntity<Map<String, Object>> deleteNotice(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawUser = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUser instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) rawUser;
		Object cid = userData.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		if (SalespersonNoticeAddService.distributorIdsExplicitPresent(readRequestLayerDistributorIds(request))) {
			throw new ResourceException("您没有此操作的权限");
		}

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, arr) -> {
			if ("distributorIds".equals(k) || "distributor_ids".equals(k)) {
				return;
			}
			if (arr != null && arr.length > 0 && StringUtils.hasText(arr[0])) {
				merged.put(k, arr[0]);
			}
		});
		putDistributorQueryParams(request, merged, "distributorIds");
		putDistributorQueryParams(request, merged, "distributor_ids");
		if (body != null) {
			merged.putAll(body);
		}

		long noticeId = parseNoticeIdForSend(merged.get("notice_id"));
		Map<String, Object> inner = salespersonNoticeAddService.deleteNotice(companyId, noticeId);
		return ResponseEntity.ok(Map.of("data", inner));
	}

	@Activated(routeAlias = "salesperson.notice.update")
	@PutMapping(value = "/notice", name = "修改导购员通知")
	public ResponseEntity<Map<String, Object>> updateNotice(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawUser = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawUser instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) rawUser;
		Object cid = userData.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString());

		if (SalespersonNoticeAddService.distributorIdsExplicitPresent(readRequestLayerDistributorIds(request))) {
			throw new ResourceException("您没有此操作的权限");
		}

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, arr) -> {
			if ("distributorIds".equals(k) || "distributor_ids".equals(k)) {
				return;
			}
			if (arr != null && arr.length > 0 && StringUtils.hasText(arr[0])) {
				merged.put(k, arr[0]);
			}
		});
		putDistributorQueryParams(request, merged, "distributorIds");
		putDistributorQueryParams(request, merged, "distributor_ids");
		if (body != null) {
			merged.putAll(body);
		}

		salespersonNoticeAddService.validateUpdateNoticeFields(merged);
		long noticeId = parseNoticeIdForSend(merged.get("notice_id"));
		Map<String, Object> inner = salespersonNoticeAddService.updateNotice(companyId, noticeId, merged);
		return ResponseEntity.ok(Map.of("data", inner));
	}
}
