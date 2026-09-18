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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.companys.service.deliverystaff.AdminDeliveryStaffDataExportFilter;
import cn.shopex.ecshopx.datacube.service.DatacubeShopRoutePermissionService;
import cn.shopex.ecshopx.datacube.service.deliverystaff.AdminDeliveryStaffDataExportParamBuilder;
import cn.shopex.ecshopx.datacube.service.deliverystaff.AdminDeliveryStaffDataListService;
import cn.shopex.ecshopx.datacube.service.deliverystaff.DeliveryStaffDataExportFacadeService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@AdminAuth
@ShopLog
@DingoResponse(resource = DingoResponse.ResourceStyle.DINGO)
@RestController("datacubeAdminV1DeliveryStaffData")
@RequestMapping("/api/v1/datacube")
public class DeliveryStaffDataController {

	private static final Pattern INTEGER_STRING = Pattern.compile("^-?\\d+$");

	private final CompanysActivationService companysActivationService;

	private final DatacubeShopRoutePermissionService datacubeShopRoutePermissionService;

	private final AdminDeliveryStaffDataExportParamBuilder adminDeliveryStaffDataExportParamBuilder;

	private final DeliveryStaffDataExportFacadeService deliveryStaffDataExportFacadeService;

	private final AdminDeliveryStaffDataListService adminDeliveryStaffDataListService;

	public DeliveryStaffDataController(
			CompanysActivationService companysActivationService,
			DatacubeShopRoutePermissionService datacubeShopRoutePermissionService,
			AdminDeliveryStaffDataExportParamBuilder adminDeliveryStaffDataExportParamBuilder,
			DeliveryStaffDataExportFacadeService deliveryStaffDataExportFacadeService,
			AdminDeliveryStaffDataListService adminDeliveryStaffDataListService) {
		this.companysActivationService = companysActivationService;
		this.datacubeShopRoutePermissionService = datacubeShopRoutePermissionService;
		this.adminDeliveryStaffDataExportParamBuilder = adminDeliveryStaffDataExportParamBuilder;
		this.deliveryStaffDataExportFacadeService = deliveryStaffDataExportFacadeService;
		this.adminDeliveryStaffDataListService = adminDeliveryStaffDataListService;
	}

	@Activated(routeAlias = "datacube.deliverystaff.data")
	@GetMapping(value = "/deliverystaffdata", name = "获取配送员统计列表")
	public ResponseEntity<Map<String, Object>> getDeliveryStaffData(
			HttpServletRequest request,
			@RequestParam(name = "delivery_staff_name", required = false) String deliveryStaffName,
			@RequestParam(name = "delivery_staff_mobile", required = false) String deliveryStaffMobile,
			@RequestParam(name = "merchant_id", required = false) String merchantIdQuery,
			@RequestParam(name = "distributor_id", required = false) String distributorIdQuery,
			@RequestParam(name = "start", required = false) String startRaw,
			@RequestParam(name = "end", required = false) String endRaw,
			@RequestParam(name = "year", required = false) Integer year,
			@RequestParam(name = "month", required = false) String month,
			@RequestParam(name = "day", required = false) String day,
			@RequestParam(name = "page", required = false) String pageRaw,
			@RequestParam(name = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(name = "is_sort", required = false) String isSortRaw) {
		Map<String, Object> user = requireOperatorUser(request);
		long companyId = requireActiveCompanyId(user);

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertDeliveryStaffData(user);

		AdminDeliveryStaffDataExportFilter filter = adminDeliveryStaffDataExportParamBuilder.build(
				user,
				deliveryStaffName,
				deliveryStaffMobile,
				merchantIdQuery,
				distributorIdQuery,
				startRaw,
				endRaw,
				year,
				month,
				day);

		if (filter.getStartEpoch() > filter.getEndEpoch()) {
			throw new ResourceException("结束日期要大于等于开始日期");
		}

		int pageVal = resolvePage(pageRaw);
		int pageSizeVal = resolvePageSize(pageSizeRaw);
		boolean sort = shouldSortDeliveryStaffList(isSortRaw);

		Map<String, Object> result =
				adminDeliveryStaffDataListService.getDeliveryStaffDataList(filter, pageVal, pageSizeVal);
		if (sort) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("list");
			if (list != null && !list.isEmpty()) {
				List<Map<String, Object>> sorted = new ArrayList<>(list);
				sorted.sort(Comparator.comparing(DeliveryStaffDataController::selfDeliveryFeeCountForSort).reversed());
				result = new LinkedHashMap<>(result);
				result.put("list", sorted);
			}
		}

		return ResponseEntity.ok(Map.of("data", result));
	}

	@Activated(routeAlias = "datacube.deliverystaff.data.export")
	@GetMapping(value = "/Deliverystaffdata/export", name = "获取配送员统计导出")
	public ResponseEntity<Map<String, Object>> exportDeliverystaffdata(
			HttpServletRequest request,
			@RequestParam(name = "delivery_staff_name", required = false) String deliveryStaffName,
			@RequestParam(name = "delivery_staff_mobile", required = false) String deliveryStaffMobile,
			@RequestParam(name = "merchant_id", required = false) String merchantIdQuery,
			@RequestParam(name = "distributor_id", required = false) String distributorIdQuery,
			@RequestParam(name = "start", required = false) String startRaw,
			@RequestParam(name = "end", required = false) String endRaw) {
		Map<String, Object> user = requireOperatorUser(request);
		long companyId = requireActiveCompanyId(user);

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		datacubeShopRoutePermissionService.assertDeliveryStaffDataExport(user);

		AdminDeliveryStaffDataExportFilter filter = adminDeliveryStaffDataExportParamBuilder.build(
				user,
				deliveryStaffName,
				deliveryStaffMobile,
				merchantIdQuery,
				distributorIdQuery,
				startRaw,
				endRaw,
				null,
				null,
				null);

		if (filter.getStartEpoch() > filter.getEndEpoch()) {
			throw new ResourceException("结束日期要大于等于开始日期");
		}

		deliveryStaffDataExportFacadeService.submitExport(filter);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", true);
		return ResponseEntity.ok(Map.of("data", data));
	}

	private static Map<String, Object> requireOperatorUser(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}
		return user;
	}

	private static long requireActiveCompanyId(Map<String, Object> user) {
		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return companyId;
	}

	/** 缺省页码 1；无法解析为整数的 query 视为 0 再参与 offset（仓库侧对 offset 做下限钳制）。 */
	private static int resolvePage(String pageRaw) {
		if (!StringUtils.hasText(pageRaw)) {
			return 1;
		}
		String s = pageRaw.trim();
		if (INTEGER_STRING.matcher(s).matches()) {
			try {
				return Math.toIntExact(Long.parseLong(s));
			} catch (ArithmeticException | NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private static int resolvePageSize(String pageSizeRaw) {
		if (!StringUtils.hasText(pageSizeRaw)) {
			return 20;
		}
		String s = pageSizeRaw.trim();
		if (!INTEGER_STRING.matcher(s).matches()) {
			return 20;
		}
		int v;
		try {
			v = Math.toIntExact(Long.parseLong(s));
		} catch (ArithmeticException | NumberFormatException e) {
			return 20;
		}
		if (v < 0) {
			throw new BadRequestException("pageSize 非法");
		}
		if (v == 0) {
			return 20;
		}
		return Math.min(v, 50);
	}

	private static boolean shouldSortDeliveryStaffList(String isSortRaw) {
		if (isSortRaw == null) {
			return false;
		}
		String s = isSortRaw.trim();
		if (s.isEmpty()) {
			return false;
		}
		String lower = s.toLowerCase(Locale.ROOT);
		if ("true".equals(lower)
				|| "1".equals(lower)
				|| "yes".equals(lower)
				|| "on".equals(lower)
				|| "y".equals(lower)) {
			return true;
		}
		if (INTEGER_STRING.matcher(s).matches()) {
			try {
				return Long.parseLong(s) != 0L;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	private static long selfDeliveryFeeCountForSort(Map<String, Object> row) {
		Object v = row.get("self_delivery_fee_count");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
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
}
