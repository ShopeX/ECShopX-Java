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

package cn.shopex.ecshopx.employeepurchase.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.employeepurchase.service.StoreHomePageService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("employeepurchaseStoreHomePageAdminV1")
@RequestMapping("/api/v1")
public class StoreHomePageAdminController {

	private static final String[] CREATE_FIELD_KEYS = {
		"template_name",
		"page_name",
		"page_description",
		"page_share_title",
		"page_share_desc",
		"page_share_imageUrl",
		"is_open"
	};

	private static final String[] UPDATE_FIELD_KEYS = {
		"template_name",
		"page_name",
		"page_description",
		"page_share_title",
		"page_share_desc",
		"page_share_imageUrl",
		"is_open"
	};

	private final StoreHomePageService storeHomePageService;

	public StoreHomePageAdminController(StoreHomePageService storeHomePageService) {
		this.storeHomePageService = storeHomePageService;
	}

	@Activated(routeAlias = "employeepurchase.store_home_page.list")
	@GetMapping(value = "/employeepurchase/store-home-page", name = "内购模版列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getList(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
			@RequestParam(value = "distributor_id", required = false) String distributorIdParam) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		long companyId = readCompanyId(operatorJwt);
		int authDistributorId = resolveAuthDistributorId(operatorJwt, request);
		Integer filterDistributorId = resolveFilterDistributorId(operatorJwt, distributorIdParam);
		page = Math.max(1, page);
		pageSize = Math.max(1, Math.min(100, pageSize));

		Map<String, Object> data = storeHomePageService.getList(companyId, authDistributorId, page, pageSize, filterDistributorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.store_home_page.create")
	@PostMapping(value = "/employeepurchase/store-home-page", name = "创建内购模版")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		long companyId = readCompanyId(operatorJwt);
		int authDistributorId = resolveAuthDistributorId(operatorJwt, request);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> params = pickFields(merged, CREATE_FIELD_KEYS);
		Map<String, Object> data = storeHomePageService.createRow(companyId, authDistributorId, params);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.store_home_page.info")
	@GetMapping(value = "/employeepurchase/store-home-page/{id}", name = "内购模版详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInfo(
			HttpServletRequest request, @PathVariable("id") String id) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		long companyId = readCompanyId(operatorJwt);
		int authDistributorId = resolveAuthDistributorId(operatorJwt, request);
		long parsedId = parseRequiredId(id);
		Map<String, Object> data = storeHomePageService.getById(companyId, authDistributorId, parsedId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.store_home_page.update")
	@PutMapping(value = "/employeepurchase/store-home-page/{id}", name = "更新内购模版")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		long companyId = readCompanyId(operatorJwt);
		int authDistributorId = resolveAuthDistributorId(operatorJwt, request);
		long parsedId = parseRequiredId(id);
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> params = pickFields(merged, UPDATE_FIELD_KEYS);
		Map<String, Object> data = storeHomePageService.updateRow(companyId, authDistributorId, parsedId, params);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.store_home_page.delete")
	@DeleteMapping(value = "/employeepurchase/store-home-page/{id}", name = "删除内购模版")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(HttpServletRequest request, @PathVariable("id") String id) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		long companyId = readCompanyId(operatorJwt);
		int authDistributorId = resolveAuthDistributorId(operatorJwt, request);
		long parsedId = parseRequiredId(id);
		Map<String, Object> data = storeHomePageService.deleteRow(companyId, authDistributorId, parsedId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, Object> pickFields(Map<String, Object> merged, String[] keys) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (String k : keys) {
			if (merged.containsKey(k)) {
				out.put(k, merged.get(k));
			}
		}
		return out;
	}

	private static int resolveAuthDistributorId(Map<String, Object> operatorJwt, HttpServletRequest request) {
		if ("distributor".equals(stringOrEmpty(operatorJwt.get("operator_type")))) {
			Object fromJwt = operatorJwt.get("distributor_id");
			if (fromJwt != null) {
				return (int) readLong(fromJwt);
			}
			String fromReq = request.getParameter("distributor_id");
			if (StringUtils.hasText(fromReq)) {
				return (int) readLong(fromReq);
			}
		}
		return 0;
	}

	private static Integer resolveFilterDistributorId(Map<String, Object> operatorJwt, String distributorIdParam) {
		if ("distributor".equals(stringOrEmpty(operatorJwt.get("operator_type")))) {
			return null;
		}
		if (!StringUtils.hasText(distributorIdParam)) {
			return null;
		}
		return (int) readLong(distributorIdParam);
	}

	private static long parseRequiredId(String id) {
		if (!StringUtils.hasText(id)) {
			throw new BadRequestException("id 必传");
		}
		try {
			return Long.parseLong(id.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("id 无效");
		}
	}

	private static Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		if (body != null) {
			merged.putAll(body);
		}
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0) {
				merged.put(k, v[0]);
			}
		});
		return merged;
	}

	private static Map<String, Object> requireOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		return operatorJwt;
	}

	private static long readCompanyId(Map<String, Object> operatorJwt) {
		Object co = operatorJwt.get("company_id");
		if (co instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new BadRequestException("未激活");
			}
			return v;
		}
		try {
			long v = Long.parseLong(co.toString().trim());
			if (v <= 0L) {
				throw new BadRequestException("未激活");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("未激活");
		}
	}

	private static long readLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 无效");
		}
	}

	private static String stringOrEmpty(Object raw) {
		return raw == null ? "" : raw.toString().trim();
	}
}
