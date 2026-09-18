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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDeliverPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV2Delivery")
@RequestMapping("/api/openapi/internal/v2")
public class DeliveryController extends OpenapiBaseController {

	private final OpenapiOrderDeliverPort orderDeliverPort;

	public DeliveryController(OpenapiOrderDeliverPort orderDeliverPort) {
		this.orderDeliverPort = orderDeliverPort;
	}

	@GetMapping(value = "/ecx.order.deliver.get", name = "开放接口订单发货单列表")
	public Map<String, Object> getOrderDeliveryList(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String orderIdRaw = OpenapiRequestParams.mergeString(orderIdParam, body, "order_id");
		return orderDeliverPort.getOrderDeliveryListV2(companyId, orderIdRaw);
	}

	@PostMapping(value = "/ecx.order.deliver", name = "开放接口订单发货")
	public Map<String, Object> createDelivery(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "item_info", required = false) String itemInfoParam,
			@RequestParam(name = "lc_code", required = false) String lcCodeParam,
			@RequestParam(name = "l_code", required = false) String lCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String orderIdRaw = OpenapiRequestParams.mergeString(orderIdParam, body, "order_id");
		String itemInfoRaw = OpenapiRequestParams.mergeString(itemInfoParam, body, "item_info");
		String lcCode = OpenapiRequestParams.mergeString(lcCodeParam, body, "lc_code");
		String lCode = OpenapiRequestParams.mergeString(lCodeParam, body, "l_code");
		return orderDeliverPort.createDeliveryV2(companyId, orderIdRaw, itemInfoRaw, lcCode, lCode);
	}
}
