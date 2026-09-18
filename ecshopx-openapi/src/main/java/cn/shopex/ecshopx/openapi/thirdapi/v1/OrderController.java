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
import cn.shopex.ecshopx.common.openapi.OpenapiOrderListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenapiResponse
@RestController("openapiV1Order")
@RequestMapping("/api/openapi/internal/v1")
public class OrderController extends OpenapiBaseController {

	private final OpenapiOrderListPort orderListPort;

	public OrderController(OpenapiOrderListPort orderListPort) {
		this.orderListPort = orderListPort;
	}

	@PostMapping(value = "/ecx.order.list", name = "开放接口会员订单列表")
	public OpenapiEnvelope list(
			HttpServletRequest request,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		OpenapiOrderListParams.PageSpec pageSpec =
				OpenapiOrderListParams.resolve(pageParam, pageSizeParam, body);
		Map<String, Object> data =
				orderListPort.list(
						companyId,
						mobileParam,
						unionidParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						unionidRaw,
						pageSpec.page(),
						pageSpec.pageSize(),
						pageSpec.pageOverridden());
		String message = resolveListMessage(data);
		return new OpenapiEnvelope("success", "E0000", message, data);
	}

	private static String resolveListMessage(Map<String, Object> data) {
		Object count = data != null ? data.get("count") : null;
		long c = count instanceof Number n ? n.longValue() : 0L;
		return c <= 0L ? "成功" : "操作成功";
	}
}
