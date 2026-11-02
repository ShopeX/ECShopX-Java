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
import cn.shopex.ecshopx.common.openapi.OpenapiDigiOsProductStockPushPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 对齐 PHP {@code OpenapiBundle/Http/ThirdApi/V1/Action/DigiOsStock}。 */
@OpenapiResponse
@RestController("openapiV1DigiOsStock")
@RequestMapping("/api/openapi/internal/v1")
public class DigiOsStockController extends OpenapiBaseController {

	private final OpenapiDigiOsProductStockPushPort stockPushPort;

	public DigiOsStockController(OpenapiDigiOsProductStockPushPort stockPushPort) {
		this.stockPushPort = stockPushPort;
	}

	@PostMapping(value = "/ecx.digios.product.stock_push", name = "DigiOS门店库存推送")
	public OpenapiEnvelope stockPush(
			HttpServletRequest request,
			@RequestParam(name = "sku_list", required = false) String skuListParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Object skuListRaw = resolveSkuList(skuListParam, body);
		Map<String, Object> data = stockPushPort.pushStoreStock(companyId, skuListRaw);
		return new OpenapiEnvelope("success", "E0000", "更新成功", data);
	}

	private static Object resolveSkuList(String skuListParam, Map<String, Object> body) {
		if (body != null && body.containsKey("sku_list")) {
			return body.get("sku_list");
		}
		return OpenapiRequestParams.mergeRequiredString(skuListParam, body, "sku_list", "sku_list 必填且格式正确");
	}
}
