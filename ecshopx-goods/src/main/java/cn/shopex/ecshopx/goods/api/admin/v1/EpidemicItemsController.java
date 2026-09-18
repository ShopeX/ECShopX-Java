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

package cn.shopex.ecshopx.goods.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.EpidemicItemsListService;
import cn.shopex.ecshopx.goods.service.EpidemicRegisterExportFacadeService;
import cn.shopex.ecshopx.goods.service.EpidemicRegisterListService;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterListFilter;
import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterQueryRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("goodsAdminV1EpidemicItems")
@RequestMapping("/api/v1/goods")
public class EpidemicItemsController {

	private final EpidemicItemsListService epidemicItemsListService;
	private final EpidemicRegisterListService epidemicRegisterListService;
	private final OrderEpidemicRegisterQueryRepository orderEpidemicRegisterQueryRepository;
	private final EpidemicRegisterExportFacadeService epidemicRegisterExportFacadeService;

	public EpidemicItemsController(EpidemicItemsListService epidemicItemsListService,
			EpidemicRegisterListService epidemicRegisterListService,
			OrderEpidemicRegisterQueryRepository orderEpidemicRegisterQueryRepository,
			EpidemicRegisterExportFacadeService epidemicRegisterExportFacadeService) {
		this.epidemicItemsListService = epidemicItemsListService;
		this.epidemicRegisterListService = epidemicRegisterListService;
		this.orderEpidemicRegisterQueryRepository = orderEpidemicRegisterQueryRepository;
		this.epidemicRegisterExportFacadeService = epidemicRegisterExportFacadeService;
	}

	@Activated(routeAlias = "goods.epidemicItems.list")
	@GetMapping(value = "/epidemicItems/list", name = "疫情商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> epidemicItemsList(HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "page_size", required = false, defaultValue = "10") String pageSizeRaw,
			@RequestParam(name = "country_code", required = false, defaultValue = "zh-CN") String countryCode) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Integer distributorFilter = parseDistributorIdFilter(distributorIdRaw);
		int page = parsePageInt(pageRaw);
		int pageSize = parsePageSizeInt(pageSizeRaw);
		validatePaging(page, pageSize);

		String country = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";
		Map<String, Object> data = epidemicItemsListService.listEpidemicItems(companyId, distributorFilter, page, pageSize,
				country);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Integer parseDistributorIdFilter(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return -1;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static int parsePageInt(String pageRaw) {
		try {
			return Integer.parseInt(pageRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}
	}

	private static int parsePageSizeInt(String pageSizeRaw) {
		try {
			return Integer.parseInt(pageSizeRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分页参数错误");
		}
	}

	private static void validatePaging(int page, int pageSize) {
		if (page < 1) {
			throw new BadRequestException("分页参数错误");
		}
		if (pageSize < 1) {
			throw new BadRequestException("分页参数错误");
		}
		if (pageSize > 1000) {
			throw new BadRequestException("每页最多查询1000条数据");
		}
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

	/** Export/async log: JWT may omit operator_id; missing, blank, or non-numeric → 0 so count runs first. */
	private static long readOptionalOperatorIdForExport(Map<?, ?> ud) {
		Object v = ud.get("operator_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@DataPass
	@Activated(routeAlias = "goods.epidemicRegister.list")
	@GetMapping(value = "/epidemicRegister/list", name = "疫情登记列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> epidemicRegisterList(HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "page_size", required = false, defaultValue = "10") String pageSizeRaw,
			@RequestParam(name = "order_time_start", required = false) String orderTimeStart,
			@RequestParam(name = "order_time_end", required = false) String orderTimeEnd,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		String operatorType = ud.get("operator_type") != null ? ud.get("operator_type").toString() : "";
		List<Long> jwtDistributorIds = EpidemicRegisterListService.parseDistributorIds(ud.get("distributor_ids"));
		Long distributorIdParam = parseDistributorIdQueryParam(distributorIdRaw);
		int page = parsePageInt(pageRaw);
		int pageSize = parsePageSizeInt(pageSizeRaw);
		validatePaging(page, pageSize);
		boolean datapassBlock = DatapassBlockResolver.isBlocked(request);
		Map<String, Object> data = epidemicRegisterListService.buildAndList(companyId, operatorType, jwtDistributorIds,
				distributorIdParam, orderTimeStart, orderTimeEnd, page, pageSize, datapassBlock);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Long parseDistributorIdQueryParam(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("店铺参数错误");
		}
	}

	@DataPass
	@Activated(routeAlias = "goods.epidemicRegister.export")
	@PostMapping(value = "/epidemicRegister/export", name = "疫情登记导出")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportEpidemicRegisterData(HttpServletRequest request,
			@RequestParam(name = "order_time_start", required = false) String orderTimeStart,
			@RequestParam(name = "order_time_end", required = false) String orderTimeEnd,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		long operatorId = readOptionalOperatorIdForExport(ud);
		String operatorType = ud.get("operator_type") != null ? ud.get("operator_type").toString() : "";
		List<Long> jwtDistributorIds = EpidemicRegisterListService.parseDistributorIds(ud.get("distributor_ids"));
		Long distributorIdParam = parseDistributorIdQueryParam(distributorIdRaw);
		OrderEpidemicRegisterListFilter filter = epidemicRegisterListService.buildListFilter(companyId, operatorType,
				jwtDistributorIds, distributorIdParam, orderTimeStart, orderTimeEnd, false);
		long count = orderEpidemicRegisterQueryRepository.countByFilter(filter);
		if (count <= 0L) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		if (count > 15000L) {
			throw new ResourceException("导出有误，当前导出数据为 " + count + " 条，最高导出 15000 条数据");
		}
		String datapassBlockHeader = request.getHeader("x-datapass-block");
		if (datapassBlockHeader == null) {
			datapassBlockHeader = request.getHeader("X-Datapass-Block");
		}
		epidemicRegisterExportFacadeService.submitExport(filter, operatorId, datapassBlockHeader);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
