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

package cn.shopex.ecshopx.thirdparty.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.thirdparty.service.saascert.SaasCertBindRelationReadService;
import cn.shopex.ecshopx.thirdparty.service.saascert.SaasCertCertificateReadService;
import cn.shopex.ecshopx.thirdparty.service.saascert.SaasCertErpBindReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("thirdPartySaasCertAdminV1")
@RequestMapping("/api/v1")
public class SaasCertAdminV1Controller {

	private final SaasCertErpBindReadService saasCertErpBindReadService;
	private final SaasCertCertificateReadService saasCertCertificateReadService;
	private final SaasCertBindRelationReadService saasCertBindRelationReadService;

	public SaasCertAdminV1Controller(
			SaasCertErpBindReadService saasCertErpBindReadService,
			SaasCertCertificateReadService saasCertCertificateReadService,
			SaasCertBindRelationReadService saasCertBindRelationReadService) {
		this.saasCertErpBindReadService = saasCertErpBindReadService;
		this.saasCertCertificateReadService = saasCertCertificateReadService;
		this.saasCertBindRelationReadService = saasCertBindRelationReadService;
	}

	@GetMapping(value = "/third/saascert/certificate", name = "证书节点")
	public ResponseEntity<Map<String, Object>> certificate(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		Map<String, Object> data = saasCertCertificateReadService.buildCertificatePayload(companyId);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@GetMapping(value = "/third/saascert/apply/bindrelation", name = "申请绑定节点")
	public ResponseEntity<Map<String, Object>> applyBindRelation(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		String url = saasCertBindRelationReadService.buildApplyBindRelationUrl(companyId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("url", url);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@GetMapping(value = "/third/saascert/accept/bindrelation", name = "查看绑定节点")
	public ResponseEntity<Map<String, Object>> acceptBindRelation(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		String url = saasCertBindRelationReadService.buildAcceptBindRelationUrl(companyId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("url", url);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	@GetMapping(value = "/third/saascert/isbind", name = "查看是否绑定了erp")
	public ResponseEntity<Map<String, Object>> isBind(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = readRequiredLong(ud, "company_id");
		boolean bound = saasCertErpBindReadService.isErpBound(companyId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("result", bound);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("data", data);
		return ResponseEntity.ok(body);
	}

	private static long readRequiredLong(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new UnauthorizedException("未登录");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
