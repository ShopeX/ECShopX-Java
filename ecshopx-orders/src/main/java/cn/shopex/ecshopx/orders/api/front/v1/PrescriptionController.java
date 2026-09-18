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

package cn.shopex.ecshopx.orders.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.service.front.wxapp.PrescriptionCreateDiagnosisService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("ordersFrontV1Prescription")
@RequestMapping("/api/v1/h5app/wxapp/prescription")
public class PrescriptionController {

	private final PrescriptionCreateDiagnosisService prescriptionCreateDiagnosisService;

	public PrescriptionController(PrescriptionCreateDiagnosisService prescriptionCreateDiagnosisService) {
		this.prescriptionCreateDiagnosisService = prescriptionCreateDiagnosisService;
	}

	@PostMapping(
			value = "/diagnosis",
			name = "新增问诊单",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createDiagnosis(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.put("company_id", auth.get("company_id"));
		merged.put("user_id", auth.get("user_id"));
		Map<String, Object> result = prescriptionCreateDiagnosisService.createDiagnosis(request, merged, auth);
		return ResponseEntity.ok(ApiResult.ok(result));
	}
}
