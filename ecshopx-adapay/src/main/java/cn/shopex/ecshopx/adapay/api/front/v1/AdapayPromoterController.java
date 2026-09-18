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

package cn.shopex.ecshopx.adapay.api.front.v1;

import cn.shopex.ecshopx.adapay.service.AdapayPromoterCertService;
import cn.shopex.ecshopx.adapay.web.AdapayPromoterFrontRequestMerge;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("adapayPromoterFrontV1")
@RequestMapping("/api/v1/h5app/wxapp/adapay")
public class AdapayPromoterController {

	private final AdapayPromoterCertService adapayPromoterCertService;

	public AdapayPromoterController(AdapayPromoterCertService adapayPromoterCertService) {
		this.adapayPromoterCertService = adapayPromoterCertService;
	}

	@GetMapping(value = "/popularize/cert", name = "获取分销员认证信息")
	public ResponseEntity<Map<String, Object>> getCertInfo(
			HttpServletRequest request,
			@RequestParam(name = "is_data_masking", required = false) String isDataMaskingRaw) {
		long companyId = parseCompanyIdFromRequest(request);
		long promoterUserId = parsePromoterUserIdFromRequest(request);
		boolean dataMasking;
		if (isDataMaskingRaw == null) {
			dataMasking = true;
		} else {
			String s = isDataMaskingRaw.trim();
			dataMasking = !(s.isEmpty() || "0".equals(s));
		}
		Map<String, Object> body =
				adapayPromoterCertService.getCertInfo(companyId, promoterUserId, dataMasking);
		return ResponseEntity.ok(Map.of("data", body));
	}

	@PostMapping(value = "/popularize/create_cert", name = "新建分销员认证信息")
	public ResponseEntity<Map<String, Object>> createCert(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = AdapayPromoterFrontRequestMerge.mergeRequestAll(request, body);
		long companyId = parseCompanyIdFromRequest(request);
		long promoterUserId = parsePromoterUserIdFromRequest(request);
		adapayPromoterCertService.createCert(companyId, promoterUserId, merged);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@PostMapping(value = "/popularize/update_cert", name = "编辑分销员认证信息")
	public ResponseEntity<Map<String, Object>> updateCert(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = AdapayPromoterFrontRequestMerge.mergeRequestAll(request, body);
		long companyId = parseCompanyIdFromRequest(request);
		adapayPromoterCertService.updateCert(companyId, merged);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@SuppressWarnings("unchecked")
	private static long parsePromoterUserIdFromRequest(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (!(raw instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("用户身份无效");
		}
		Map<String, Object> claims = (Map<String, Object>) rawMap;
		Object uid = claims.get("user_id");
		if (uid == null) {
			throw new BadRequestException("用户身份无效");
		}
		if (uid instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L || v > (long) Integer.MAX_VALUE) {
				throw new BadRequestException("用户身份无效");
			}
			return v;
		}
		String s = uid.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new BadRequestException("用户身份无效");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L || v > (long) Integer.MAX_VALUE) {
				throw new BadRequestException("用户身份无效");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("用户身份无效");
		}
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}
}
