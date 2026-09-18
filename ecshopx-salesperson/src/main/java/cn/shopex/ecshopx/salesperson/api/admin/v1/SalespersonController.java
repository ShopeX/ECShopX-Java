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
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.salesperson.service.AdminSalespersonCreateService;
import cn.shopex.ecshopx.salesperson.service.AdminSalespersonDeleteService;
import cn.shopex.ecshopx.salesperson.service.AdminSalespersonUpdateService;
import cn.shopex.ecshopx.salesperson.service.DistributorSalesmanListService;
import cn.shopex.ecshopx.salesperson.service.DistributorShoppingGuideAddService;
import cn.shopex.ecshopx.salesperson.service.DistributorShoppingGuideUpdateService;
import cn.shopex.ecshopx.salesperson.service.SalemanCustomerComplaintListService;
import cn.shopex.ecshopx.salesperson.service.SalemanCustomerComplaintReplyService;
import cn.shopex.ecshopx.salesperson.service.SalespersonGetInfoService;
import cn.shopex.ecshopx.salesperson.service.SalespersonListService;
import cn.shopex.ecshopx.salesperson.service.SalespersonRelShopListService;
import cn.shopex.ecshopx.salesperson.service.SalespersonRoleListService;
import cn.shopex.ecshopx.salesperson.service.SalespersonRoleUpdateService;
import cn.shopex.ecshopx.salesperson.service.SalespersonSignLogListService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RestController("salespersonAdminV1Salesperson")
@RequestMapping("/api/v1")
public class SalespersonController {

	private final AdminSalespersonCreateService adminSalespersonCreateService;
	private final AdminSalespersonUpdateService adminSalespersonUpdateService;
	private final AdminSalespersonDeleteService adminSalespersonDeleteService;
	private final SalemanCustomerComplaintReplyService salemanCustomerComplaintReplyService;
	private final SalemanCustomerComplaintListService salemanCustomerComplaintListService;
	private final DistributorShoppingGuideAddService distributorShoppingGuideAddService;
	private final DistributorShoppingGuideUpdateService distributorShoppingGuideUpdateService;
	private final SalespersonRoleUpdateService salespersonRoleUpdateService;
	private final SalespersonRoleListService salespersonRoleListService;
	private final DistributorSalesmanListService distributorSalesmanListService;
	private final SalespersonGetInfoService salespersonGetInfoService;
	private final SalespersonRelShopListService salespersonRelShopListService;
	private final SalespersonSignLogListService salespersonSignLogListService;
	private final SalespersonListService salespersonListService;

	public SalespersonController(AdminSalespersonCreateService adminSalespersonCreateService,
			AdminSalespersonUpdateService adminSalespersonUpdateService,
			AdminSalespersonDeleteService adminSalespersonDeleteService,
			SalemanCustomerComplaintReplyService salemanCustomerComplaintReplyService,
			SalemanCustomerComplaintListService salemanCustomerComplaintListService,
			DistributorShoppingGuideAddService distributorShoppingGuideAddService,
			DistributorShoppingGuideUpdateService distributorShoppingGuideUpdateService,
			SalespersonRoleUpdateService salespersonRoleUpdateService,
			SalespersonRoleListService salespersonRoleListService,
			DistributorSalesmanListService distributorSalesmanListService,
			SalespersonGetInfoService salespersonGetInfoService,
			SalespersonRelShopListService salespersonRelShopListService,
			SalespersonSignLogListService salespersonSignLogListService,
			SalespersonListService salespersonListService) {
		this.adminSalespersonCreateService = adminSalespersonCreateService;
		this.adminSalespersonUpdateService = adminSalespersonUpdateService;
		this.adminSalespersonDeleteService = adminSalespersonDeleteService;
		this.salemanCustomerComplaintReplyService = salemanCustomerComplaintReplyService;
		this.salemanCustomerComplaintListService = salemanCustomerComplaintListService;
		this.distributorShoppingGuideAddService = distributorShoppingGuideAddService;
		this.distributorShoppingGuideUpdateService = distributorShoppingGuideUpdateService;
		this.salespersonRoleUpdateService = salespersonRoleUpdateService;
		this.salespersonRoleListService = salespersonRoleListService;
		this.distributorSalesmanListService = distributorSalesmanListService;
		this.salespersonGetInfoService = salespersonGetInfoService;
		this.salespersonRelShopListService = salespersonRelShopListService;
		this.salespersonSignLogListService = salespersonSignLogListService;
		this.salespersonListService = salespersonListService;
	}

	@Activated(routeAlias = "distribution.salesman.list.get")
	@GetMapping(value = "/distributor/salesmans", name = "获取店铺导购员列表")
	public ResponseEntity<Map<String, Object>> getSalesmanList(HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "salesman_name", required = false) String salesmanName,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "is_valid", required = false) String isValid,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize) {
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

		Map<String, Object> body = distributorSalesmanListService.getSalesmanList(userData, distributorId,
				salesmanName, mobile, isValid, page, pageSize);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@Activated(routeAlias = "distribution.salesman.update")
	@PutMapping(value = "/distributor/salesman/{salesmanId}", name = "更新店铺导购员")
	public ResponseEntity<Map<String, Object>> updateSalesman(HttpServletRequest request,
			@PathVariable("salesmanId") String salesmanId,
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

		Map<String, Object> merged = mergeInputForCreateSalesperson(request, body);
		String mobile = trimToOptional(merged.get("mobile"));
		String name = trimToOptional(merged.get("salesman_name"));
		List<Long> distributorIds = mergeIdList(request, merged, "distributor_id");
		String isValid = trimToOptional(merged.get("is_valid"));

		Object roleRaw = merged.get("role");
		String roleText = roleRaw == null ? "" : String.valueOf(roleRaw).trim();
		long roleVal = roleText.isEmpty() ? 0L : LeadingNumberParser.parseAsLong(roleText);
		Long roleOpt = roleVal != 0L ? roleVal : null;

		Map<String, Object> payload = distributorShoppingGuideUpdateService.updateSalesman(companyId, salesmanId,
				mobile, name, isValid, roleOpt, distributorIds);
		return ResponseEntity.ok(Map.of("data", payload));
	}

	@Activated(routeAlias = "distribution.salesman.role.list")
	@GetMapping(value = "/distributor/salesman/role", name = "获取店铺导购员权限集合")
	public ResponseEntity<Map<String, Object>> getSalesmanRoleList() {
		return ResponseEntity.ok(Map.of("data", salespersonRoleListService.getSalesmanRoleList()));
	}

	@Activated(routeAlias = "distribution.salesman.role.update")
	@PutMapping(value = "/distributor/salesman/role/{salesmanId}", name = "更新店铺导购员权限")
	public ResponseEntity<Map<String, Object>> updateSalesmanRole(HttpServletRequest request,
			@PathVariable("salesmanId") String salesmanId,
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

		Map<String, Object> merged = mergeInputForCreateSalesperson(request, body);
		String roleStr;
		if (!merged.containsKey("role") || merged.get("role") == null) {
			roleStr = "0";
		} else {
			roleStr = String.valueOf(merged.get("role"));
		}

		salespersonRoleUpdateService.updateSalesmanRole(companyId, salesmanId, roleStr);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@Activated(routeAlias = "distribution.salesman.add")
	@PostMapping(value = "/distributor/salesman", name = "新增店铺导购员")
	public ResponseEntity<Map<String, Object>> addSalesman(HttpServletRequest request) {
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

		String mobile = firstNonBlank(request.getParameterValues("mobile"));
		String salesmanName = firstNonBlank(request.getParameterValues("salesman_name"));
		String role = firstOrNull(request.getParameterValues("role"));
		String employeeStatus = firstOrNull(request.getParameterValues("employee_status"));

		Map<String, Object> data = distributorShoppingGuideAddService.addSalesman(companyId, mobile, salesmanName,
				request.getParameterValues("distributor_id"), role, employeeStatus);
		return ResponseEntity.ok(Map.of("data", data));
	}

	@Activated(routeAlias = "distribution.salesmancustomercomplaints.list.get")
	@GetMapping(value = "/distributor/salemanCustomerComplaints", name = "导购员客诉列表")
	@SuppressWarnings("unused")
	public ResponseEntity<Map<String, Object>> getSalemanCustomerComplaintsList(HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "user_name", required = false) String userName,
			@RequestParam(value = "user_mobile", required = false) String userMobile,
			@RequestParam(value = "saleman_name", required = false) String salemanName,
			@RequestParam(value = "saleman_mobile", required = false) String salemanMobile,
			@RequestParam(value = "reply_status", required = false) String replyStatus) {
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

		String pageTrim = page == null ? "" : page.trim();
		String pageSizeTrim = pageSize == null ? "" : pageSize.trim();

		String userNameForFilter = filterContainsRawIfPresent(request, "user_name");
		String userMobileForFilter = filterContainsRawIfPresent(request, "user_mobile");
		String salemanNameForFilter = filterContainsRawIfPresent(request, "saleman_name");
		String salemanMobileForFilter = filterContainsRawIfPresent(request, "saleman_mobile");

		String rawReplyStatus = request.getParameter("reply_status");
		String replyStatusForFilter = null;
		if (rawReplyStatus != null && !rawReplyStatus.isEmpty()) {
			replyStatusForFilter = rawReplyStatus;
		}

		Map<String, Object> body = salemanCustomerComplaintListService.getSalemanCustomerComplaintsList(companyId,
				userNameForFilter, userMobileForFilter, salemanNameForFilter, salemanMobileForFilter,
				replyStatusForFilter, pageTrim, pageSizeTrim);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@Activated(routeAlias = "distribution.salesmancustomercomplaints.reply")
	@PostMapping(value = "/distributor/salemanCustomerComplaints", name = "回复导购员客诉")
	public ResponseEntity<Map<String, Object>> replySalemanCustomerComplaints(HttpServletRequest request,
			@RequestParam(value = "reply_id", required = false) String replyId,
			@RequestParam(value = "reply_content", required = false) String replyContent) {
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

		String replyIdTrim = replyId == null ? "" : replyId.trim();
		String replyContentTrim = replyContent == null ? "" : replyContent.trim();

		if (StringUtils.hasText(replyContentTrim)
				&& replyContentTrim.codePointCount(0, replyContentTrim.length()) > 255) {
			throw new ResourceException("回复内容不能超过255个字符");
		}
		if (!StringUtils.hasText(replyIdTrim)) {
			throw new ResourceException("未知的回复对象");
		}
		if (!StringUtils.hasText(replyContentTrim)) {
			throw new ResourceException("请输入回复内容");
		}

		int id = parseComplaintReplyIdOrThrow(replyIdTrim);

		Map<String, Object> row = salemanCustomerComplaintReplyService.replySalemanCustomerComplaints(id,
				replyContentTrim, userData);
		return ResponseEntity.ok(Map.of("data", row));
	}

	@Activated(routeAlias = "shop.salesperson.create")
	@PostMapping(value = "/shops/salesperson", name = "添加门店人员")
	public ResponseEntity<Map<String, Object>> createSalesperson(HttpServletRequest request,
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

		Map<String, Object> merged = mergeInputForCreateSalesperson(request, body);
		String name = trimToOptional(merged.get("name"));
		String mobile = trimToOptional(merged.get("mobile"));
		String salespersonType = trimToOptional(merged.get("salesperson_type"));
		if (!StringUtils.hasText(salespersonType)) {
			salespersonType = "admin";
		}
		List<Long> shopIds = mergeIdList(request, merged, "shop_id");
		List<Long> distributorIds = mergeIdList(request, merged, "distributor_id");

		adminSalespersonCreateService.createSalesperson(companyId, userData, name, mobile, salespersonType, shopIds,
				distributorIds);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
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

	private static Map<String, Object> mergeInputForCreateSalesperson(HttpServletRequest request,
			Map<String, Object> body) {
		LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
		if (body != null) {
			input.putAll(body);
		}
		return input;
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
			String k = e.getKey();
			if ("shop_id".equals(k) || "distributor_id".equals(k)) {
				List<String> vals = collectTrimmedParameterValues(request, k);
				if (!vals.isEmpty()) {
					m.put(k, vals.size() == 1 ? vals.get(0) : vals);
				}
			} else {
				String[] v = e.getValue();
				if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
					m.put(k, v[0]);
				}
			}
		}
		return m;
	}

	private static List<String> collectTrimmedParameterValues(HttpServletRequest request, String name) {
		String[] arr = request.getParameterValues(name);
		List<String> vals = new ArrayList<>();
		if (arr == null) {
			return vals;
		}
		for (String s : arr) {
			if (s != null && StringUtils.hasText(s.trim())) {
				vals.add(s.trim());
			}
		}
		return vals;
	}

	private static List<Long> mergeIdList(HttpServletRequest request, Map<String, Object> merged, String paramName) {
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		addLongIdsFromParameterValues(request, paramName, out);
		addLongIdsFromMergedValue(merged.get(paramName), out, paramName);
		return new ArrayList<>(out);
	}

	private static void addLongIdsFromParameterValues(HttpServletRequest request, String paramName,
			LinkedHashSet<Long> out) {
		for (String s : collectTrimmedParameterValues(request, paramName)) {
			out.add(parseLongId(s, paramName));
		}
	}

	private static void addLongIdsFromMergedValue(Object v, LinkedHashSet<Long> out, String paramName) {
		if (v == null) {
			return;
		}
		if (v instanceof List<?> list) {
			for (Object el : list) {
				if (el == null) {
					continue;
				}
				if (el instanceof Number n) {
					out.add(n.longValue());
				} else {
					String s = String.valueOf(el).trim();
					if (StringUtils.hasText(s)) {
						out.add(parseLongId(s, paramName));
					}
				}
			}
			return;
		}
		if (v instanceof Number n) {
			out.add(n.longValue());
			return;
		}
		String s = String.valueOf(v).trim();
		if (StringUtils.hasText(s)) {
			out.add(parseLongId(s, paramName));
		}
	}

	private static long parseLongId(String s, String paramName) {
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(paramName + " 格式错误", 422);
		}
	}

	private static String trimToOptional(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	/**
	 * Parses {@code reply_id} as a whole-string decimal integer (complaint primary key). Non-numeric values or values
	 * outside {@link Integer} range are rejected with the same user-visible error as when no matching row exists.
	 */
	private static int parseComplaintReplyIdOrThrow(String replyIdTrim) {
		if (!replyIdTrim.matches("^-?\\d+$")) {
			throw new ResourceException("未查询到更新数据");
		}
		try {
			return new BigInteger(replyIdTrim).intValueExact();
		} catch (ArithmeticException e) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	private static String firstNonBlank(String[] arr) {
		if (arr == null) {
			return null;
		}
		for (String s : arr) {
			if (s != null && StringUtils.hasText(s.trim())) {
				return s.trim();
			}
		}
		return null;
	}

	private static String firstOrNull(String[] arr) {
		if (arr == null || arr.length == 0) {
			return null;
		}
		return arr[0];
	}

	/** Present for filtering when the parameter exists and trim is non-empty; value passed through untrimmed. */
	private static String filterContainsRawIfPresent(HttpServletRequest request, String name) {
		String raw = request.getParameter(name);
		if (raw == null) {
			return null;
		}
		if (raw.trim().isEmpty()) {
			return null;
		}
		return raw;
	}

	@DataPass
	@Activated(routeAlias = "shop.salesperson.lists")
	@GetMapping(value = "/shops/salesperson", name = "获取所有门店人员列表")
	public ResponseEntity<Map<String, Object>> lists(HttpServletRequest request) {
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

		Map<String, Object> merged = mergeInputForCreateSalesperson(request, null);
		List<Long> shopIdsFromRequest = mergeIdList(request, merged, "shop_id");
		List<Long> distributorIdsFromRequest = mergeIdList(request, merged, "distributor_id");
		String mobile = trimToOptional(merged.get("mobile"));
		String salespersonType = trimToOptional(merged.get("salesperson_type"));
		String pageRaw = trimToOptional(merged.get("page"));
		String pageSizeRaw = trimToOptional(merged.get("pageSize"));
		int datapassBlock = resolveDatapassBlock(request);

		Map<String, Object> body = salespersonListService.lists(companyId, userData, shopIdsFromRequest,
				distributorIdsFromRequest, StringUtils.hasText(mobile) ? mobile : null,
				StringUtils.hasText(salespersonType) ? salespersonType : null,
				StringUtils.hasText(pageRaw) ? pageRaw : null, StringUtils.hasText(pageSizeRaw) ? pageSizeRaw : null,
				datapassBlock);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@Activated(routeAlias = "shop.salesperson.del")
	@DeleteMapping(value = "/shops/salesperson/{salespersonId}", name = "删除门店人员")
	public ResponseEntity<Map<String, Object>> deleteSalesperson(HttpServletRequest request,
			@PathVariable("salespersonId") String salespersonId) {
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

		String sid = salespersonId == null ? "" : salespersonId.trim();
		if (!StringUtils.hasText(sid)) {
			throw new BadRequestException("请填写必填信息");
		}
		long id;
		try {
			id = Long.parseLong(sid);
		} catch (NumberFormatException e) {
			return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
		}
		if (id <= 0) {
			throw new BadRequestException("请填写必填信息");
		}

		adminSalespersonDeleteService.deleteSalesperson(companyId, id);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@Activated(routeAlias = "shop.salesperson.update")
	@PutMapping(value = "/shops/salesperson/{salespersonId}", name = "更新门店人员")
	public ResponseEntity<Map<String, Object>> updateSalesperson(HttpServletRequest request,
			@PathVariable("salespersonId") String salespersonId,
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

		Map<String, Object> merged = mergeInputForCreateSalesperson(request, body);
		String sid = salespersonId == null ? "" : salespersonId.trim();
		if (!StringUtils.hasText(sid)) {
			throw new BadRequestException("请填写必填信息");
		}
		long id = parseLongId(sid, "salespersonId");
		if (id <= 0) {
			throw new BadRequestException("请填写必填信息");
		}

		String name = trimToOptional(merged.get("name"));
		String mobile = trimToOptional(merged.get("mobile"));
		String salespersonType = trimToOptional(merged.get("salesperson_type"));
		if (!StringUtils.hasText(salespersonType)) {
			salespersonType = "admin";
		}
		List<Long> shopIds = mergeIdList(request, merged, "shop_id");
		List<Long> distributorIds = mergeIdList(request, merged, "distributor_id");

		adminSalespersonUpdateService.updateSalesperson(companyId, userData, id, name, mobile, salespersonType,
				shopIds, distributorIds);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@Activated(routeAlias = "shop.salesperson.update")
	@GetMapping(value = "/shops/saleperson/shoplist", name = "管理员管理的门店数据")
	public ResponseEntity<Map<String, Object>> getRelShopList(HttpServletRequest request,
			@RequestParam(value = "salesperson_id", required = false) String salespersonId,
			@RequestParam(value = "store_type", defaultValue = "shop") String storeType,
			@RequestParam(value = "store_name", required = false) String storeName,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "500") int pageSize) {
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

		Map<String, Object> body = salespersonRelShopListService.getRelShopList(companyId, salespersonId, storeType,
				storeName, page, pageSize);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@DataPass
	@Activated(routeAlias = "shop.salesperson.getinfo")
	@GetMapping(value = "/shops/saleperson/getinfo", name = "门店人员详细信息")
	public ResponseEntity<Map<String, Object>> getSalespersonInfo(HttpServletRequest request,
			@RequestParam(value = "salesperson_id", required = false) String salespersonId) {
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
		int datapassBlock = resolveDatapassBlock(request);
		Map<String, Object> data = salespersonGetInfoService.getSalespersonInfo(companyId, salespersonId,
				datapassBlock);
		return ResponseEntity.ok(Map.of("data", data));
	}

	/**
	 * Mirrors {@code DatapassBlockResolver} truthy rules for {@code x-datapass-block}, returning {@code 0} or {@code 1}.
	 */
	private static int resolveDatapassBlock(HttpServletRequest request) {
		if (truthyDatapassToken(request.getHeader("X-Datapass-Block"))) {
			return 1;
		}
		Object attr = request.getAttribute("x-datapass-block");
		if (attr instanceof Boolean b && b) {
			return 1;
		}
		if (attr != null && truthyDatapassToken(attr.toString())) {
			return 1;
		}
		return truthyDatapassToken(request.getParameter("x-datapass-block")) ? 1 : 0;
	}

	private static boolean truthyDatapassToken(String v) {
		if (v == null) {
			return false;
		}
		String t = v.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return !"false".equalsIgnoreCase(t);
	}

	@Activated(routeAlias = "shop.salesperson.signlogs")
	@GetMapping(value = "/shops/saleperson/signlogs", name = "签到记录")
	public ResponseEntity<Map<String, Object>> getSignlogs(HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(value = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize) {
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
		if (companyId <= 0L) {
			throw new UnauthorizedException("未登录");
		}

		Map<String, Object> body = salespersonSignLogListService.getSignlogs(companyId, distributorId, name, mobile,
				timeStartBegin, timeStartEnd, page, pageSize);
		return ResponseEntity.ok(Map.of("data", body));
	}
}
