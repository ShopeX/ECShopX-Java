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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiCompanyInfoPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV1Company")
@RequestMapping("/api/openapi/internal/v1")
public class CompanyController extends OpenapiBaseController {

	private final OpenapiCompanyInfoPort companyInfoPort;

	public CompanyController(OpenapiCompanyInfoPort companyInfoPort) {
		this.companyInfoPort = companyInfoPort;
	}

	@PostMapping(value = "/ecx.company.info", name = "开放接口获取云店基本信息")
	public OpenapiEnvelope getInfo(
			HttpServletRequest request,
			@RequestParam(name = "country_code", required = false) String countryCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String countryCode = OpenapiPaginationParams.mergeCountryCode(countryCodeParam, body);
		if (countryCode == null) {
			countryCode = "zh-CN";
		}
		Map<String, Object> data = companyInfoPort.getCompanyInfo(companyId, countryCode);
		return new OpenapiEnvelope("success", "0", "success", data);
	}
}
