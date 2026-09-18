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

package cn.shopex.ecshopx.espier.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.espier.service.subdistrict.SubdistrictFrontApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("espierFrontV1Subdistrict")
@RequestMapping("/api/v1/h5app/wxapp/espier")
public class SubdistrictController {

	private final SubdistrictFrontApplicationService subdistrictFrontApplicationService;

	public SubdistrictController(SubdistrictFrontApplicationService subdistrictFrontApplicationService) {
		this.subdistrictFrontApplicationService = subdistrictFrontApplicationService;
	}

	@GetMapping(value = "/subdistrict", name = "获取街道社区列表")
	public ResponseEntity<Map<String, Object>> get(
			HttpServletRequest request,
			@RequestParam(value = "receiver_state", required = false) String receiverState,
			@RequestParam(value = "receiver_city", required = false) String receiverCity,
			@RequestParam(value = "receiver_district", required = false) String receiverDistrict) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyIdForFrontNoAuth(request, claims);
		List<String> normalizedDistributorIds = resolveDistributorIdParams(request);
		List<Map<String, Object>> body =
				subdistrictFrontApplicationService.get(
						companyId, normalizedDistributorIds, receiverState, receiverCity, receiverDistrict);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	/**
	 * Resolves distributor id filter arguments from the query string.
	 * <p>
	 * Bracket-style name {@code distributor_id[]} carries an explicit multi-value payload: every
	 * non-null entry is returned, and the application service uses {@code List} size {@code >= 2} to
	 * apply disjunctive (OR-style) matching across those values.
	 * <p>
	 * For the bare name {@code distributor_id}, {@link HttpServletRequest#getParameterValues(String)}
	 * may return more than one string when the same key is repeated. That servlet-level aggregation is
	 * not treated as the same contract as bracket-style multi-value input: repeated bare keys are
	 * normalized to the last value and wrapped as a single-element list so the service stays on its
	 * scalar branch. If neither form is present, returns {@code null}.
	 */
	private static List<String> resolveDistributorIdParams(HttpServletRequest request) {
		String[] bracketed = request.getParameterValues("distributor_id[]");
		if (bracketed != null && bracketed.length > 0) {
			List<String> list = new ArrayList<>(bracketed.length);
			for (String s : bracketed) {
				if (s != null) {
					list.add(s);
				}
			}
			return list.isEmpty() ? null : list;
		}
		String[] scalar = request.getParameterValues("distributor_id");
		if (scalar == null || scalar.length == 0) {
			return null;
		}
		if (scalar.length == 1) {
			return List.of(scalar[0]);
		}
		return List.of(scalar[scalar.length - 1]);
	}

	private static long resolveCompanyIdForFrontNoAuth(HttpServletRequest request, Map<String, Object> claims) {
		long fromAttr = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		long fromQuery = parsePositiveLongOrZero(FlexibleHttpServletParameterMap.toObjectMap(request).get("company_id"));
		if (fromQuery > 0L) {
			return fromQuery;
		}
		throw new UnauthorizedException("无权访问该API,非法访问！");
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
