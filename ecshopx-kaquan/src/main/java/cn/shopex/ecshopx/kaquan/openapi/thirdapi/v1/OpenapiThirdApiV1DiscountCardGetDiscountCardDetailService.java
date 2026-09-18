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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.discount.DiscountCardKaquanDetailLoadService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1DiscountCardGetDiscountCardDetailService {

	private static final String CARD_DETAIL_ERROR_MSG = "获取卡券的详细信息出错.";
	private static final Pattern INTEGER_STRING = Pattern.compile("^-?\\d+$");

	private final DiscountCardKaquanDetailLoadService discountCardKaquanDetailLoadService;
	private final OpenapiDiscountCardDetailPresentationService presentationService;

	public OpenapiThirdApiV1DiscountCardGetDiscountCardDetailService(
			DiscountCardKaquanDetailLoadService discountCardKaquanDetailLoadService,
			OpenapiDiscountCardDetailPresentationService presentationService) {
		this.discountCardKaquanDetailLoadService = discountCardKaquanDetailLoadService;
		this.presentationService = presentationService;
	}

	public Map<String, Object> execute(long companyId, String cardIdRaw) {
		validateCardIdForOpenapi(cardIdRaw);
		String cardIdTrimmed = cardIdRaw.trim();

		Map<String, Object> result;
		try {
			result = discountCardKaquanDetailLoadService.loadDetailForAdmin(companyId, cardIdTrimmed, "");
		} catch (ResourceException ex) {
			if (KaquanDiscountCardMessages.COUPON_INVALID.equals(ex.getMessage())) {
				throw new OpenapiLegacyZeroCodeFailException(KaquanDiscountCardMessages.COUPON_INVALID);
			}
			throw ex;
		}

		if (result == null || result.isEmpty()) {
			throw new OpenapiLegacyZeroCodeFailException("获取失败.");
		}

		result.remove("user_tag_ids");
		presentationService.apply(result);
		return result;
	}

	private static void validateCardIdForOpenapi(String cardIdRaw) {
		if (cardIdRaw == null || cardIdRaw.isBlank()) {
			throw new OpenapiLegacyZeroCodeFailException(CARD_DETAIL_ERROR_MSG);
		}
		String trimmed = cardIdRaw.trim();
		if (!INTEGER_STRING.matcher(trimmed).matches()) {
			throw new OpenapiLegacyZeroCodeFailException(CARD_DETAIL_ERROR_MSG);
		}
	}
}
