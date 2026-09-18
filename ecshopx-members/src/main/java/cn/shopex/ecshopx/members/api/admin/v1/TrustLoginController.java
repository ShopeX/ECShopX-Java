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
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.members.service.admin.AdminTrustLoginListService;
import cn.shopex.ecshopx.members.service.trustlogin.TrustLoginConfigSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("membersAdminV1TrustLogin")
@RequestMapping("/api/v1/members/trustlogin")
public class TrustLoginController {

	private final AdminTrustLoginListService adminTrustLoginListService;

	public TrustLoginController(AdminTrustLoginListService adminTrustLoginListService) {
		this.adminTrustLoginListService = adminTrustLoginListService;
	}

	@Activated(routeAlias = "member.trustlogin.list")
	@PostMapping(value = "/list", name = "信任登录列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTrustLoginList(HttpServletRequest request) {
		long companyId = requireOperatorCompanyId(request);
		Map<String, Object> data = adminTrustLoginListService.getTrustLoginList(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "member.trustlogin.setting")
	@RequestMapping(
			value = "/setting",
			method = {RequestMethod.PUT, RequestMethod.POST},
			name = "保存信任登录状态",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Boolean>> saveStatusSetting(
			HttpServletRequest request,
			@RequestParam(name = "type", required = false) String type,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "app_id", required = false) String appId,
			@RequestParam(name = "secret", required = false) String secret,
			@RequestParam(name = "loginversion", required = false) String loginVersion,
			@RequestParam(name = "status", required = false) String statusQuery,
			@FlexibleBody(required = false) Map<String, Object> trustLoginPutBody) {
		long companyId = requireOperatorCompanyId(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		if (type != null) {
			merged.put("type", type);
		}
		if (name != null) {
			merged.put("name", name);
		}
		if (appId != null) {
			merged.put("app_id", appId);
		}
		if (secret != null) {
			merged.put("secret", secret);
		}
		if (loginVersion != null) {
			merged.put("loginversion", loginVersion);
		}
		if (statusQuery != null) {
			merged.put("status", statusQuery);
		}
		if (trustLoginPutBody != null) {
			for (Map.Entry<String, Object> e : trustLoginPutBody.entrySet()) {
				merged.put(e.getKey(), e.getValue());
			}
		}
		if (!merged.containsKey("app_id")
				&& trustLoginPutBody != null
				&& trustLoginPutBody.containsKey("appId")) {
			merged.put("app_id", trustLoginPutBody.get("appId"));
		}
		if (!merged.containsKey("loginversion")
				&& trustLoginPutBody != null
				&& trustLoginPutBody.containsKey("loginVersion")) {
			merged.put("loginversion", trustLoginPutBody.get("loginVersion"));
		}
		Object rawStatus = merged.get("status");
		boolean statusNormalized = TrustLoginConfigSupport.normalizeStatus(rawStatus);
		merged.put("status", statusNormalized);
		boolean ok = adminTrustLoginListService.saveStatusSetting(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(ok));
	}

	private static long requireOperatorCompanyId(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> map = (Map<?, ?>) attr;
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
		if (companyId <= 0) {
			throw new UnauthorizedException("未登录");
		}
		return companyId;
	}
}
