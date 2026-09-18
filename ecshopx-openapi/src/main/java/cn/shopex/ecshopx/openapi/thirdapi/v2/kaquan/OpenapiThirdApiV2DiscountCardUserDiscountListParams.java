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

public final class OpenapiThirdApiV2DiscountCardUserDiscountListParams {

	private static final String MSG_PLAT_ACCOUNT_PARAM = "商派会员Id参数错误";

	private OpenapiThirdApiV2DiscountCardUserDiscountListParams() {}

	public record QueryParams(String platAccount, String code, int page, int pageSize) {}

	public static QueryParams validate(
			String platAccountParam,
			String codeParam,
			String pageParam,
			String pageSizeParam,
			Map<String, Object> body) {
		String platAccount = OpenapiRequestParams.mergeString(platAccountParam, body, "plat_account");
		if (!StringUtils.hasText(platAccount != null ? platAccount.trim() : null)) {
			throw new OpenapiKaquanV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_PLAT_ACCOUNT_PARAM);
		}

		String codeRaw = OpenapiRequestParams.mergeString(codeParam, body, "code");
		String code = isPhpTruthyCode(codeRaw) ? codeRaw.trim() : null;

		OpenapiThirdApiV2DiscountCardListParams.PageSpec pageSpec =
				OpenapiThirdApiV2DiscountCardListParams.resolve(pageParam, pageSizeParam, body);

		return new QueryParams(platAccount.trim(), code, pageSpec.page(), pageSpec.pageSize());
	}

	private static boolean isPhpTruthyCode(String raw) {
		if (raw == null) {
			return false;
		}
		String trimmed = raw.trim();
		return !trimmed.isEmpty() && !"0".equals(trimmed);
	}
}
