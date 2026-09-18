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

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiKaquanV2FailException;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OpenapiThirdApiV2DiscountCardSendParams {

	private OpenapiThirdApiV2DiscountCardSendParams() {}

	public record SendParams(String platAccount, String cardId, String sourceType, String activityName) {}

	public static SendParams validate(
			String platAccountParam,
			String cardIdParam,
			String sourceTypeParam,
			String activityNameParam,
			Map<String, Object> body) {
		String platAccount = OpenapiRequestParams.mergeString(platAccountParam, body, "plat_account");
		String cardId = OpenapiRequestParams.mergeString(cardIdParam, body, "card_id");
		String sourceType = OpenapiRequestParams.mergeString(sourceTypeParam, body, "source_type");
		String activityName = OpenapiRequestParams.mergeString(activityNameParam, body, "activity_name");

		if (!StringUtils.hasText(trimToNull(platAccount))) {
			throw fail("商派会员Id参数错误");
		}
		if (!StringUtils.hasText(trimToNull(cardId))) {
			throw fail("优惠券ID参数错误");
		}
		if (!StringUtils.hasText(trimToNull(sourceType))) {
			throw fail("卡券来源参数错误");
		}

		String activity = activityName == null ? "" : activityName.trim();
		return new SendParams(
				platAccount.trim(), cardId.trim(), sourceType.trim(), activity);
	}

	private static String trimToNull(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static OpenapiKaquanV2FailException fail(String message) {
		return new OpenapiKaquanV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}
}
