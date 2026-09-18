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
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDeliverPort;
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
@RestController("openapiV1Delivery")
@RequestMapping("/api/openapi/internal/v1")
public class DeliveryController extends OpenapiBaseController {

	private final OpenapiOrderDeliverPort orderDeliverPort;

	public DeliveryController(OpenapiOrderDeliverPort orderDeliverPort) {
		this.orderDeliverPort = orderDeliverPort;
	}

	@PostMapping(value = "/ecx.order.deliver", name = "开放接口订单发货")
	public OpenapiEnvelope createDelivery(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "item_info", required = false) String itemInfoParam,
			@RequestParam(name = "lc_code", required = false) String lcCodeParam,
			@RequestParam(name = "l_code", required = false) String lCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String orderId =
				OpenapiRequestParams.mergeRequiredString(orderIdParam, body, "order_id", "订单号必填");
		String itemInfoRaw =
				OpenapiRequestParams.mergeRequiredString(
						itemInfoParam, body, "item_info", "发货商品信息必填");
		String lcCode =
				OpenapiRequestParams.mergeRequiredString(lcCodeParam, body, "lc_code", "快递公司编码必填");
		String lCode =
				OpenapiRequestParams.mergeRequiredString(lCodeParam, body, "l_code", "快递单编码必填");
		Map<String, Object> data =
				orderDeliverPort.createDelivery(companyId, orderId, itemInfoRaw, lcCode, lCode);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}
}
