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
import cn.shopex.ecshopx.common.auth.DualJwtSupport;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.web.CompanysAdminRequestMerge;
import cn.shopex.ecshopx.shuyun.service.openplatform.OperatorStoreMemberReadinessService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 店务移动收银：开单前会员 OPEN 线下登记。对齐 PHP {@code OperatorStoreMemberController}。 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@RestController("companysAdminV1OperatorStoreMember")
@RequestMapping("/api/v1/operator")
public class OperatorStoreMemberController {

	private final OperatorStoreMemberReadinessService operatorStoreMemberReadinessService;

	public OperatorStoreMemberController(OperatorStoreMemberReadinessService operatorStoreMemberReadinessService) {
		this.operatorStoreMemberReadinessService = operatorStoreMemberReadinessService;
	}

	@Activated(routeAlias = "companys.operator.member.ready")
	@PostMapping(value = "/member/ready", name = "门店会员就绪（开单前）")
	public ResponseEntity<ApiResult<Object>> postReady(HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> input = CompanysAdminRequestMerge.mergeInputLikeFlexibleResolver(request, body);
		long companyId = DualJwtSupport.requireCompanyId(request);
		long userId = longVal(input.get("user_id"), 0L);
		long distributorId = longVal(input.get("distributor_id"), 0L);
		Map<String, Object> result =
				operatorStoreMemberReadinessService.ensureOfflineMemberAtStore(
						companyId, distributorId, userId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("ok", true);
		data.putAll(result);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long longVal(Object raw, long defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return defaultValue;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
