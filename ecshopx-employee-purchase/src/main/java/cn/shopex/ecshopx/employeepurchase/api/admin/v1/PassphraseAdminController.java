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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.PassphraseGenerateService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("employeepurchasePassphraseAdminV1")
@RequestMapping("/api/v1")
public class PassphraseAdminController {

	private final PassphraseGenerateService passphraseGenerateService;

	public PassphraseAdminController(PassphraseGenerateService passphraseGenerateService) {
		this.passphraseGenerateService = passphraseGenerateService;
	}

	@Activated(routeAlias = "employeepurchase.passphrase.generate")
	@PostMapping(value = "/employeepurchase/passphrase-codes/generate", name = "生成企业购口令")
	public ResponseEntity<ApiResult<Map<String, Object>>> generate(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		Map<String, Object> merged = mergeInput(request, body);
		long companyId = readLong(operatorJwt.get("company_id"), 0L);
		long distributorId = readLong(operatorJwt.get("distributor_id"), 0L);
		List<Long> enterpriseIds = PassphraseGenerateService.parseEnterpriseIds(merged.get("enterprise_ids"));
		int count = readInt(merged.get("count"), 1);
		Long activityId = readNullableLong(merged.get("activity_id"));
		Map<String, Object> data =
				passphraseGenerateService.generate(companyId, distributorId, enterpriseIds, count, activityId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "employeepurchase.passphrase.generate.by_activity")
	@PostMapping(
			value = "/employeepurchase/activity/{activityId}/passphrase-codes/generate",
			name = "按活动生成企业购口令")
	public ResponseEntity<ApiResult<Map<String, Object>>> generateByActivity(
			HttpServletRequest request,
			@PathVariable("activityId") String activityIdRaw,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> operatorJwt = requireOperatorJwt(request);
		Map<String, Object> merged = mergeInput(request, body);
		long companyId = readLong(operatorJwt.get("company_id"), 0L);
		long distributorId = readLong(operatorJwt.get("distributor_id"), 0L);
		long activityId = Long.parseLong(activityIdRaw.trim());
		List<Long> enterpriseIds = PassphraseGenerateService.parseEnterpriseIds(merged.get("enterprise_ids"));
		int count = readInt(merged.get("count"), 1);
		Map<String, Object> data =
				passphraseGenerateService.generate(companyId, distributorId, enterpriseIds, count, activityId);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	@SuppressWarnings("unchecked")
	private static Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		if (body != null) {
			merged.putAll(body);
		}
		Map<String, String[]> paramMap = request.getParameterMap();
		for (Map.Entry<String, String[]> e : paramMap.entrySet()) {
			String[] vals = e.getValue();
			if (vals != null && vals.length > 0) {
				merged.putIfAbsent(e.getKey(), vals[0]);
			}
		}
		return merged;
	}

	private static long readLong(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static Long readNullableLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			return x <= 0L ? null : x;
		}
		try {
			long x = Long.parseLong(v.toString().trim());
			return x <= 0L ? null : x;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int readInt(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
