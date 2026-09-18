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

package cn.shopex.ecshopx.popularize.dispatch;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.popularize.service.export.PromoterExportJobContext;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PopularizePromoterExportFileJobPayloadSupport {

	private PopularizePromoterExportFileJobPayloadSupport() {}

	public static Map<String, Object> toPayload(PromoterExportJobContext ctx) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", PopularizePromoterExportFileJobTypes.TYPE_POPULARIZE_PROMOTER_EXPORT);
		payload.put("company_id", ctx.companyId());
		payload.put("operator_id", ctx.operatorId());
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", ctx.companyId());
		if (ctx.mobile() != null && !ctx.mobile().trim().isEmpty()) {
			params.put("mobile", ctx.mobile().trim());
		}
		if (ctx.username() != null && !ctx.username().trim().isEmpty()) {
			params.put("username", ctx.username().trim());
		}
		if (ctx.datapassBlock()) {
			params.put("datapass_block", 1);
		}
		payload.put("params", params);
		return payload;
	}

	public static PromoterExportJobContext contextFromPayload(Map<String, Object> payload) {
		long companyId = asLong(payload.get("company_id"));
		long operatorId = asLong(payload.get("operator_id"));
		Object rawParams = payload.get("params");
		if (!(rawParams instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("payload.params must be a map");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) rawMap;
		long paramsCompanyId = asLong(params.get("company_id"));
		if (paramsCompanyId != companyId) {
			throw new BadRequestException("payload.params.company_id must match payload.company_id");
		}
		String mobile = optionalNonEmptyString(params.get("mobile"));
		String username = optionalNonEmptyString(params.get("username"));
		boolean datapassBlock = asInt(params.get("datapass_block")) == 1;
		return new PromoterExportJobContext(companyId, operatorId, mobile, username, datapassBlock);
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static int asInt(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}

	private static String optionalNonEmptyString(Object v) {
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		return s.isEmpty() ? null : s;
	}
}
