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

package cn.shopex.ecshopx.wechat.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.wechat.service.PcLoginQrcodeService;
import cn.shopex.ecshopx.wechat.service.PcQrcodeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("wechatFrontV1Qrcode")
@RequestMapping("/api/v1/h5app/wxapp")
public class QrcodeController {

	private final PcLoginQrcodeService pcLoginQrcodeService;
	private final PcQrcodeService pcQrcodeService;

	public QrcodeController(PcLoginQrcodeService pcLoginQrcodeService, PcQrcodeService pcQrcodeService) {
		this.pcLoginQrcodeService = pcLoginQrcodeService;
		this.pcQrcodeService = pcQrcodeService;
	}

	@GetMapping(value = "/pcqrcode", name = "PC二维码")
	public ResponseEntity<Map<String, Object>> getPcQrcode(
			HttpServletRequest request,
			@org.springframework.web.bind.annotation.RequestParam(name = "id", required = false) String id,
			@org.springframework.web.bind.annotation.RequestParam(name = "uid", required = false) String uid,
			@org.springframework.web.bind.annotation.RequestParam(name = "dtid", required = false) String dtid,
			@org.springframework.web.bind.annotation.RequestParam(name = "pages", required = false) String pages) {
		long companyId = requireCompanyId(request);
		LinkedHashMap<String, Object> data = pcQrcodeService.getPcQrcode(companyId, id, uid, dtid, pages);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@GetMapping(value = "/pcloginqrcode", name = "PC登录二维码")
	public ResponseEntity<Map<String, Object>> getPcLoginQrcode(HttpServletRequest request) {
		long companyId = requireCompanyId(request);
		Map<String, Object> data = pcLoginQrcodeService.getPcLoginQrcode(companyId);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long requireCompanyId(HttpServletRequest request) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
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
