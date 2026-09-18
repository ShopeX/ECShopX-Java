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

package cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardV2ListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardV2SendPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardV2UserDiscountListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenapiResponse
@RestController("openapiV2DiscountCard")
@RequestMapping("/api/openapi/internal/v2")
public class DiscountCardController extends OpenapiBaseController {

	private final OpenapiDiscountCardV2ListPort discountCardV2ListPort;
	private final OpenapiDiscountCardV2SendPort discountCardV2SendPort;
	private final OpenapiDiscountCardV2UserDiscountListPort userDiscountListPort;

	public DiscountCardController(
			OpenapiDiscountCardV2ListPort discountCardV2ListPort,
			OpenapiDiscountCardV2SendPort discountCardV2SendPort,
			OpenapiDiscountCardV2UserDiscountListPort userDiscountListPort) {
		this.discountCardV2ListPort = discountCardV2ListPort;
		this.discountCardV2SendPort = discountCardV2SendPort;
		this.userDiscountListPort = userDiscountListPort;
	}

	@PostMapping(value = "/ecx.discountcard.list", name = "开放接口获取优惠券列表")
	public OpenapiEnvelope getDiscountCardList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2DiscountCardListParams.PageSpec pageSpec =
				OpenapiThirdApiV2DiscountCardListParams.resolve(pageParam, pageSizeParam, body);
		Map<String, Object> data = discountCardV2ListPort.getDiscountCardList(
				companyId, pageSpec.page(), pageSpec.pageSize());
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.discountcard.send", name = "开放接口单张优惠券发放")
	public Map<String, Object> userGetCard(
			HttpServletRequest request,
			@RequestParam(name = "plat_account", required = false) String platAccountParam,
			@RequestParam(name = "card_id", required = false) String cardIdParam,
			@RequestParam(name = "source_type", required = false) String sourceTypeParam,
			@RequestParam(name = "activity_name", required = false) String activityNameParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2DiscountCardSendParams.SendParams params =
				OpenapiThirdApiV2DiscountCardSendParams.validate(
						platAccountParam, cardIdParam, sourceTypeParam, activityNameParam, body);
		return discountCardV2SendPort.userSendDiscountCard(
				companyId,
				params.platAccount(),
				params.cardId(),
				params.sourceType(),
				params.activityName());
	}

	@PostMapping(value = "/ecx.userdiscount.list", name = "开放接口会员优惠券列表")
	public Map<String, Object> getUserDiscountList(
			HttpServletRequest request,
			@RequestParam(name = "plat_account", required = false) String platAccountParam,
			@RequestParam(name = "code", required = false) String codeParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2DiscountCardUserDiscountListParams.QueryParams params =
				OpenapiThirdApiV2DiscountCardUserDiscountListParams.validate(
						platAccountParam, codeParam, pageParam, pageSizeParam, body);
		return userDiscountListPort.getUserDiscountList(
				companyId, params.platAccount(), params.code(), params.page(), params.pageSize());
	}
}
