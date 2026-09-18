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

package cn.shopex.ecshopx.distribution.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.DistributeCompanyCountService;
import cn.shopex.ecshopx.distribution.service.DistributeLogsListCoreService;
import cn.shopex.ecshopx.distribution.service.DistributeLogsListRequestGate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1DistributeLogs")
@RequestMapping("/api/v1/distribution")
public class DistributeLogsController {

	private static final String DISTRIBUTE_LOGS_PAGE_ERROR = "分页参数错误";
	private static final String DISTRIBUTE_LOGS_PAGESIZE_ERROR = "每页最多查询50条数据";

	private final DistributeLogsListRequestGate distributeLogsListRequestGate;
	private final DistributeLogsListCoreService distributeLogsListCoreService;
	private final DistributeCompanyCountService distributeCompanyCountService;

	public DistributeLogsController(
			DistributeLogsListRequestGate distributeLogsListRequestGate,
			DistributeLogsListCoreService distributeLogsListCoreService,
			DistributeCompanyCountService distributeCompanyCountService) {
		this.distributeLogsListRequestGate = distributeLogsListRequestGate;
		this.distributeLogsListCoreService = distributeLogsListCoreService;
		this.distributeCompanyCountService = distributeCompanyCountService;
	}

	@Activated(routeAlias = "front.wxapp.distribution.log")
	@GetMapping(value = "/logs", name = "获取佣金记录")
	public ResponseEntity<?> getDistributeLogs(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0") String distributorId,
			@RequestParam(value = "is_close", required = false) String isCloseRaw,
			Locale locale) {
		Map<String, Object> merged = new LinkedHashMap<>();
		if (page != null) {
			merged.put("page", page);
		}
		if (pageSize != null) {
			merged.put("pageSize", pageSize);
		}
		merged.put("distributor_id", distributorId);
		merged.put("is_close_raw", isCloseRaw);

		try {
			Map<String, Object> user = distributeLogsListRequestGate.validateBeforeList(request, merged);
			Map<String, Object> data = distributeLogsListCoreService.buildList(user, merged, locale);
			return ResponseEntity.ok(ApiResult.ok(data));
		} catch (ResourceException ex) {
			String msg = ex.getMessage();
			if (DISTRIBUTE_LOGS_PAGE_ERROR.equals(msg) || DISTRIBUTE_LOGS_PAGESIZE_ERROR.equals(msg)) {
				LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
				payload.put("message", msg);
				payload.put("status_code", 422);
				LinkedHashMap<String, Object> root = new LinkedHashMap<>();
				root.put("data", payload);
				return ResponseEntity.ok(root);
			}
			throw ex;
		}
	}

	@Activated(routeAlias = "front.wxapp.distribution.count")
	@GetMapping(value = "/count", name = "获取分销统计")
	public ResponseEntity<?> getCompanyCount(HttpServletRequest request) {
		Map<String, Object> user = distributeLogsListRequestGate.validateBeforeCompanyCount(request);
		long companyId = toLong(user.get("company_id"));
		Map<String, Object> data = distributeCompanyCountService.getCount(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
