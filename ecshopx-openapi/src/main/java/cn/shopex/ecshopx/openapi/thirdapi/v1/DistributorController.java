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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorDetailPort;
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
@RestController("openapiV1Distributor")
@RequestMapping("/api/openapi/internal/v1")
public class DistributorController extends OpenapiBaseController {

	private final OpenapiDistributorDetailPort distributorDetailPort;

	public DistributorController(OpenapiDistributorDetailPort distributorDetailPort) {
		this.distributorDetailPort = distributorDetailPort;
	}

	@PostMapping(value = "/ecx.distributor.detail", name = "开放接口获取店铺详情")
	public OpenapiEnvelope detail(
			HttpServletRequest request,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		String shopCodeRaw = OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code");
		if (shopCodeRaw == null || shopCodeRaw.isEmpty()) {
			throw new ResourceException("店铺号必填");
		}

		Map<String, Object> data =
				distributorDetailPort.getDistributorDetail(companyId, shopCodeRaw);
		return new OpenapiEnvelope("success", "E0000", "成功", data);
	}
}
