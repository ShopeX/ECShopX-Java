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
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV1DiscountCard")
@RequestMapping("/api/openapi/internal/v1")
public class DiscountCardController extends OpenapiBaseController {

	private final OpenapiDiscountCardDetailPort discountCardDetailPort;
	private final OpenapiDiscountCardListPort discountCardListPort;

	public DiscountCardController(OpenapiDiscountCardDetailPort discountCardDetailPort,
			OpenapiDiscountCardListPort discountCardListPort) {
		this.discountCardDetailPort = discountCardDetailPort;
		this.discountCardListPort = discountCardListPort;
	}

	@PostMapping(value = "/ecx.discountcard.info", name = "开放接口获取卡券详情")
	public OpenapiEnvelope getDiscountCardDetail(
			HttpServletRequest request,
			@RequestParam(name = "card_id", required = false) String cardIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String cardId = OpenapiRequestParams.mergeString(cardIdParam, body, "card_id");
		Map<String, Object> data = discountCardDetailPort.getDiscountCardDetail(companyId, cardId);
		return new OpenapiEnvelope("success", "0", "success", data);
	}

	@PostMapping(value = "/ecx.discountcard.list", name = "开放接口获取卡券列表")
	public OpenapiEnvelope getDiscountCardList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "pageSize", required = false) String pageSizeParam,
			@RequestParam(name = "title", required = false) String titleParam,
			@RequestParam(name = "card_type", required = false) String cardTypeParam,
			@RequestParam(name = "card_ids", required = false) String cardIdsParam,
			@RequestParam(name = "is_valid", required = false) String isValidParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("page", OpenapiRequestParams.mergeString(pageParam, body, "page"));
		merged.put("pageSize", OpenapiRequestParams.mergeString(pageSizeParam, body, "pageSize"));
		merged.put("title", OpenapiRequestParams.mergeString(titleParam, body, "title"));
		merged.put("card_type", OpenapiRequestParams.mergeString(cardTypeParam, body, "card_type"));
		merged.put("card_ids", OpenapiRequestParams.mergeString(cardIdsParam, body, "card_ids"));
		merged.put("is_valid", OpenapiRequestParams.mergeString(isValidParam, body, "is_valid"));
		merged.put("status", OpenapiRequestParams.mergeString(statusParam, body, "status"));
		Map<String, Object> data = discountCardListPort.getDiscountCardList(companyId, merged);
		return new OpenapiEnvelope("success", "0", "success", data);
	}
}
