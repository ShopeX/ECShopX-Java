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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.common.dispatch.ProfitExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.salesperson.service.ProfitWithdrawalListService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
@RestController("salespersonAdminV1Profit")
@RequestMapping("/api/v1/profit")
public class ProfitController {

	private final ProfitExportFileJobDispatchPublisher profitExportFileJobDispatchPublisher;
	private final ProfitWithdrawalListService profitWithdrawalListService;

	public ProfitController(
			ProfitExportFileJobDispatchPublisher profitExportFileJobDispatchPublisher,
			ProfitWithdrawalListService profitWithdrawalListService) {
		this.profitExportFileJobDispatchPublisher = profitExportFileJobDispatchPublisher;
		this.profitWithdrawalListService = profitWithdrawalListService;
	}

	@Activated(routeAlias = "profit.statistics.list.get")
	@GetMapping(value = "/statistics", name = "获取分润统计")
	public ResponseEntity<Map<String, Object>> lists(HttpServletRequest request,
			@RequestParam(value = "profitType", required = false) String profitType,
			@RequestParam(value = "distributor", required = false) String distributor,
			@RequestParam(value = "salesperson", required = false) String salesperson,
			@RequestParam(value = "dealer", required = false) String dealer,
			@RequestParam(value = "date", required = false) String date,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) raw;
		requireCompanyId(userData);

		return ResponseEntity.ok(
				Map.of("data", profitWithdrawalListService.lists(profitType, distributor, salesperson, dealer, date,
						page, pageSize)));
	}

	@Activated(routeAlias = "profit.export")
	@GetMapping(value = "/export", name = "导出分润信息")
	public ResponseEntity<Map<String, Object>> exportProfitData(HttpServletRequest request,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "date", required = false) String date,
			@RequestParam(value = "profit_user_type", required = false) String profitUserType,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") long distributorId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> userData = (Map<String, Object>) raw;
		long companyId = requireCompanyId(userData);

		String dateYm = date;
		if (!StringUtils.hasText(dateYm)) {
			dateYm = YearMonth.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyyMM"));
		}

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("date", dateYm);
		filter.put("profit_user_type", profitUserType);
		String normalizedType =
				StringUtils.hasText(type) ? type.trim().toLowerCase(Locale.ROOT) : null;
		profitExportFileJobDispatchPublisher.publish(
				normalizedType, companyId, distributorId, new LinkedHashMap<>(filter));
		return ResponseEntity.ok(Map.of("data", Map.of("status", Boolean.TRUE)));
	}

	private static long requireCompanyId(Map<String, Object> userData) {
		Object cid = userData.get("company_id");
		if (cid == null) {
			throw new UnauthorizedException("未登录");
		}
		if (cid instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(cid.toString().trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
