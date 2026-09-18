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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.statistics.CompanysDashboardStatisticsService;
import cn.shopex.ecshopx.companys.service.statistics.CompanysNoticeStatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController("companysAdminV1Statistics")
@RequestMapping("/api/v1")
public class StatisticsController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final CompanysNoticeStatisticsService companysNoticeStatisticsService;
	private final CompanysDashboardStatisticsService companysDashboardStatisticsService;

	public StatisticsController(
			CompanysNoticeStatisticsService companysNoticeStatisticsService,
			CompanysDashboardStatisticsService companysDashboardStatisticsService) {
		this.companysNoticeStatisticsService = companysNoticeStatisticsService;
		this.companysDashboardStatisticsService = companysDashboardStatisticsService;
	}

	@Activated(routeAlias = "company.real.statistics")
	@GetMapping(value = "/getStatistics", name = "获取商城订单统计信息")
	public ApiResult<LinkedHashMap<String, Object>> getDataList(
			HttpServletRequest request,
			@RequestParam(name = "is_app", required = false) String isAppRaw,
			@RequestParam(name = "shop_id", required = false) String shopIdRaw) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		String operatorType = readOperatorType(jwt);
		long merchantId = readLongJwtField(jwt, "merchant_id");
		long operatorId = readLongJwtField(jwt, "operator_id");
		boolean isApp = isAppQueryTruthy(isAppRaw);
		return ApiResult.ok(
				companysDashboardStatisticsService.getDataList(
						companyId, isApp, shopIdRaw, operatorType, merchantId, operatorId));
	}

	@Activated(routeAlias = "company.notice.statistics")
	@GetMapping(value = "/getNoticeStatistics", name = "获取商城总量统计")
	public ApiResult<LinkedHashMap<String, Object>> getOrderStatusCount(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		return ApiResult.ok(companysNoticeStatisticsService.getOrderStatusCount(companyId));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		return (Map<String, Object>) (Map<?, ?>) ud;
	}

	private static String readOperatorType(Map<String, Object> jwt) {
		Object v = jwt.get("operator_type");
		return v == null ? "" : v.toString().trim();
	}

	private static long readLongJwtField(Map<String, Object> jwt, String key) {
		Object v = jwt.get(key);
		if (v == null) {
			return 0L;
		}
		try {
			return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean isAppQueryTruthy(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		try {
			if (Long.parseLong(t) == 0L) {
				return false;
			}
		} catch (NumberFormatException ignored) {
			return true;
		}
		return true;
	}

	private static long readCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}
}
