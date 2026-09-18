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

package cn.shopex.ecshopx.distribution.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.service.SelfDeliveryStaffDistributorService;
import cn.shopex.ecshopx.distribution.service.SelfDeliveryStaffListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
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
@FrontAuth
@RestController("distributionFrontV1SelfDeliveryStaff")
@RequestMapping("/api/v1/h5app")
public class SelfDeliveryStaffController {

	private final SelfDeliveryStaffDistributorService selfDeliveryStaffDistributorService;
	private final SelfDeliveryStaffListService selfDeliveryStaffListService;
	private final LangueProperties langueProperties;

	public SelfDeliveryStaffController(
			SelfDeliveryStaffDistributorService selfDeliveryStaffDistributorService,
			SelfDeliveryStaffListService selfDeliveryStaffListService,
			LangueProperties langueProperties) {
		this.selfDeliveryStaffDistributorService = selfDeliveryStaffDistributorService;
		this.selfDeliveryStaffListService = selfDeliveryStaffListService;
		this.langueProperties = langueProperties;
	}

	@GetMapping(value = "/wxapp/selfdelivery/getDistributorList", name = "自配送员店铺列表")
	public ResponseEntity<ApiResult<Object>> getSelfDeliveryStaffDistributor(
			HttpServletRequest request,
			@RequestParam(value = "self_delivery_operator_id", required = false) List<String> selfDeliveryOperatorIdRaw) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String requestLangTag = RequestLangTag.current(langueProperties);
		List<Long> parsed = parseSelfDeliveryOperatorIds(selfDeliveryOperatorIdRaw);
		if (CollectionUtils.isEmpty(parsed)) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		Map<String, Object> body =
				selfDeliveryStaffDistributorService.getSelfDeliveryStaffDistributor(companyId, parsed, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@GetMapping(value = "/wxapp/selfdelivery/list", name = "自配送列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSelfDeliveryList(
			HttpServletRequest request,
			@RequestParam(value = "self_delivery_operator_id", required = false) List<String> selfDeliveryOperatorIdRaw) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveCompanyId(request, claims);
		String requestLangTag = RequestLangTag.current(langueProperties);
		List<Long> parsed = parseSelfDeliveryOperatorIds(selfDeliveryOperatorIdRaw);
		Map<String, Object> body =
				selfDeliveryStaffListService.getSelfDeliveryList(companyId, parsed, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private static List<Long> parseSelfDeliveryOperatorIds(List<String> raw) {
		if (CollectionUtils.isEmpty(raw)) {
			return List.of();
		}
		LinkedHashSet<Long> set = new LinkedHashSet<>();
		for (String s : raw) {
			if (s == null) {
				continue;
			}
			String t = s.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				long v = Long.parseLong(t);
				if (v > 0L) {
					set.add(v);
				}
			} catch (NumberFormatException ignored) {
				// skip invalid entries
			}
		}
		return new ArrayList<>(set);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object attr = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (attr instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long resolveCompanyId(HttpServletRequest request, Map<String, Object> claims) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long fromAttr = parsePositiveLongOrZero(companyAttr);
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		throw new UnauthorizedException("Unable to authenticate user.");
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
