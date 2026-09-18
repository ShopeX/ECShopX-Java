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

package cn.shopex.ecshopx.kaquan.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.discount.DiscountCardKaquanDetailLoadService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardDetailListService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardWxappGetCardListService;
import cn.shopex.ecshopx.kaquan.service.discount.dto.WxappGetCardListParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@RestController("kaquanDiscountCardFrontV1")
@RequestMapping("/api/v1/h5app")
public class DiscountCardController {

	private static final String MSG_H5_COMPANY_CONTEXT_INVALID = "删除卡券失败，信息有误";

	private final DiscountCardKaquanDetailLoadService discountCardKaquanDetailLoadService;
	private final DiscountCardDetailListService discountCardDetailListService;
	private final DiscountCardWxappGetCardListService discountCardWxappGetCardListService;

	public DiscountCardController(
			DiscountCardKaquanDetailLoadService discountCardKaquanDetailLoadService,
			DiscountCardDetailListService discountCardDetailListService,
			DiscountCardWxappGetCardListService discountCardWxappGetCardListService) {
		this.discountCardKaquanDetailLoadService = discountCardKaquanDetailLoadService;
		this.discountCardDetailListService = discountCardDetailListService;
		this.discountCardWxappGetCardListService = discountCardWxappGetCardListService;
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/getCardList", name = "卡券列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDiscountCardList(
			HttpServletRequest request,
			@RequestParam(value = "page_no", required = false, defaultValue = "1") int pageNo,
			@RequestParam(value = "page_size", required = false, defaultValue = "8") int pageSize,
			@RequestParam(value = "card_type", required = false) String cardType,
			@RequestParam(value = "end_date", required = false) String endDate,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "item_id", required = false) Long itemId,
			@RequestParam(value = "card_id", required = false) String cardIdCsv,
			@RequestParam(value = "work_userid", required = false) String workUserid,
			@RequestParam(value = "userWorkId", required = false) String userWorkId) {
		WxappGetCardListParams params = WxappGetCardListParams.fromRequest(
				request, pageNo, pageSize, cardType, endDate, distributorId, itemId, cardIdCsv, workUserid, userWorkId);
		Map<String, Object> data = discountCardWxappGetCardListService.build(params);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(value = "/wxapp/getCardDetail/{cardId}", name = "卡券详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDiscountCardDetail(
			HttpServletRequest request, @PathVariable("cardId") String cardId) {
		long companyId = parsePositiveCompanyIdFromRequestOrThrow(request);
		discountCardDetailListService.validateCardIdForAdminDetail(cardId);
		String cardIdTrimmed = cardId.trim();
		Map<String, Object> result =
				discountCardKaquanDetailLoadService.loadDetailForAdmin(companyId, cardIdTrimmed, "");
		result.remove("user_tag_ids");
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static long parsePositiveCompanyIdFromRequestOrThrow(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && !s.isBlank()) {
			try {
				companyId = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException(MSG_H5_COMPANY_CONTEXT_INVALID);
			}
		} else {
			throw new ResourceException(MSG_H5_COMPANY_CONTEXT_INVALID);
		}
		if (companyId <= 0L) {
			throw new ResourceException(MSG_H5_COMPANY_CONTEXT_INVALID);
		}
		return companyId;
	}
}
