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

package cn.shopex.ecshopx.members.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.members.service.admin.AdminMemberBatchOperatingService;
import cn.shopex.ecshopx.members.service.export.AdminMemberExportDispatchService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("membersAdminV1ExportData")
@RequestMapping("/api/v1/member")
public class ExportDataController {

	private final AdminMemberBatchOperatingService adminMemberBatchOperatingService;
	private final AdminMemberExportDispatchService adminMemberExportDispatchService;

	public ExportDataController(
			AdminMemberBatchOperatingService adminMemberBatchOperatingService,
			AdminMemberExportDispatchService adminMemberExportDispatchService) {
		this.adminMemberBatchOperatingService = adminMemberBatchOperatingService;
		this.adminMemberExportDispatchService = adminMemberExportDispatchService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@DataPass
	@Activated(routeAlias = "member.export")
	@GetMapping(value = "/export", name = "导出会员", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> exportMemberData(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", defaultValue = "0") String distributorIdRaw) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		long distributorId = 0L;
		if (distributorIdRaw != null) {
			String t = distributorIdRaw.trim();
			if (!t.isEmpty()) {
				try {
					distributorId = Long.parseLong(t);
				} catch (NumberFormatException ignored) {
					distributorId = 0L;
				}
			}
		}
		merged.put("distributor_id", distributorId);

		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwtMap = (Map<?, ?>) attr;
		Object companyIdObj = jwtMap.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("操作员账号有误");
		}

		Map<String, Object> body = adminMemberExportDispatchService.exportMemberData(request, merged, jwtMap);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ApiResult.ok(body));
	}

	/**
	 * Admin batch member operations ({@code POST /batchOperating}).
	 * <p>
	 * Delegates to {@link AdminMemberBatchOperatingService#batchProcessMemberData}. When the matching member count is
	 * at most 100, work runs inline on the current request (synchronous {@code executeChunk} with {@code page=1} and
	 * {@code pageSize=100} inside the service); the {@code BatchActionMembers} chunk job is not enqueued on that path.
	 * When the count exceeds 100, the service enqueues sharded chunks on the {@code slow} dispatch queue instead.
	 * Action-specific behavior (for example SMS group-send when {@code action_type} is {@code send_sms}) is implemented
	 * inside the service layer.
	 */
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "member.batch.operating")
	@PostMapping(value = "/batchOperating", name = "批量操作会员", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> batchProcessMemberData(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwtMap = (Map<?, ?>) attr;
		Object companyIdObj = jwtMap.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("操作员账号有误");
		}
		return ResponseEntity.ok(
				ApiResult.ok(adminMemberBatchOperatingService.batchProcessMemberData(companyId, jwtMap, merged)));
	}
}
