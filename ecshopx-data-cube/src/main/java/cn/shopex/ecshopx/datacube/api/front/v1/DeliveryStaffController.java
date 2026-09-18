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

package cn.shopex.ecshopx.datacube.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.datacube.service.FrontDeliveryStaffDataService;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@FrontAuth
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		unauthorized = true)
@RestController("datacubeFrontV1DeliveryStaff")
@RequestMapping("/api/v1/h5app")
public class DeliveryStaffController {

	private final FrontDeliveryStaffDataService frontDeliveryStaffDataService;

	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;

	public DeliveryStaffController(
			FrontDeliveryStaffDataService frontDeliveryStaffDataService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService) {
		this.frontDeliveryStaffDataService = frontDeliveryStaffDataService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
	}

	@GetMapping(value = "/wxapp/datacube/Deliverystaffdata", name = "配送员统计")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDeliveryStaffData(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) Long distributorId,
			@RequestParam(required = false) String datetype,
			@RequestParam(required = false) String date,
			@RequestParam(name = "self_delivery_operator_id", required = false) List<String> selfDeliveryOperatorId) {
		h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("Unable to authenticate user."));
		long companyId = resolveCompanyId(request);
		if (companyId <= 0) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		List<Long> operatorIds = parseOperatorIds(selfDeliveryOperatorId);
		Long distributorParam = (distributorId != null && distributorId > 0) ? distributorId : null;
		return ResponseEntity.ok(
				frontDeliveryStaffDataService.getDeliveryStaffData(companyId, operatorIds, datetype, date, distributorParam));
	}

	private static long resolveCompanyId(HttpServletRequest request) {
		Object cidAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (cidAttr instanceof Number n) {
			return n.longValue();
		}
		if (cidAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException ignored) {
				return 0L;
			}
		}
		return 0L;
	}

	private static List<Long> parseOperatorIds(List<String> raw) {
		if (raw == null || raw.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String s : raw) {
			if (s == null) {
				continue;
			}
			String t = s.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip tokens that do not parse as Long
			}
		}
		return out;
	}

	@GetMapping(value = "/wxapp/datacube/DeliverystaffdataDetail", name = "配送员统计详情")
	public ResponseEntity<Void> getDeliverystaffdataDetail() {
		return ResponseEntity.ok().build();
	}
}
