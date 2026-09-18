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
import cn.shopex.ecshopx.popularize.service.export.PopularizeOrderExportJobContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PopularizeOrderExportFileJobPayloadSupport {

	private PopularizeOrderExportFileJobPayloadSupport() {}

	public static Map<String, Object> toPayload(PopularizeOrderExportJobContext ctx) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", PopularizePromoterExportFileJobTypes.TYPE_POPULARIZE_ORDER_EXPORT);
		payload.put("company_id", ctx.companyId());
		payload.put("operator_id", ctx.operatorId());
		payload.put("merchant_id", ctx.merchantId());
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", ctx.companyId());
		if (ctx.mobile() != null && !ctx.mobile().trim().isEmpty()) {
			params.put("mobile", ctx.mobile().trim());
		}
		if (ctx.username() != null && !ctx.username().trim().isEmpty()) {
			params.put("username", ctx.username().trim());
		}
		if (ctx.distributorIdRaw() != null && !ctx.distributorIdRaw().trim().isEmpty()) {
			params.put("distributor_id", ctx.distributorIdRaw());
		}
		if (ctx.dIds() != null && !ctx.dIds().isEmpty()) {
			params.put("dIds", ctx.dIds());
		}
		if (ctx.datapassBlock()) {
			params.put("datapass_block", 1);
		}
		params.put("date_start", ctx.dateStart());
		params.put("date_end", ctx.dateEnd());
		payload.put("params", params);
		return payload;
	}

	public static PopularizeOrderExportJobContext contextFromPayload(Map<String, Object> payload) {
		long companyId = asLong(payload.get("company_id"));
		long operatorId = asLong(payload.get("operator_id"));
		long merchantId = asLong(payload.get("merchant_id"));
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
		String dateStart = requiredNonEmptyString(params.get("date_start"), "date_start");
		String dateEnd = requiredNonEmptyString(params.get("date_end"), "date_end");
		String mobile = optionalNonEmptyString(params.get("mobile"));
		String username = optionalNonEmptyString(params.get("username"));
		String distributorIdRaw = optionalNonEmptyString(params.get("distributor_id"));
		List<Long> dIds = asLongList(params.get("dIds"));
		boolean datapassBlock = asInt(params.get("datapass_block")) == 1;
		return new PopularizeOrderExportJobContext(
				companyId, operatorId, merchantId, "", mobile, username, distributorIdRaw, dIds, dateStart, dateEnd, datapassBlock);
	}

	private static String requiredNonEmptyString(Object v, String field) {
		String s = optionalNonEmptyString(v);
		if (s == null) {
			throw new BadRequestException("payload.params." + field + " is required");
		}
		return s;
	}

	private static List<Long> asLongList(Object v) {
		if (v == null) {
			return null;
		}
		if (!(v instanceof List<?> rawList)) {
			throw new BadRequestException("payload.params.dIds must be a list when present");
		}
		List<Long> out = new ArrayList<>(rawList.size());
		for (Object o : rawList) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else {
				out.add(Long.parseLong(String.valueOf(o).trim()));
			}
		}
		return out.isEmpty() ? null : out;
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
