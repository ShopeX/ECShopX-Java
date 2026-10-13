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

package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.promotions.service.export.MemberTaskCompletionExportJobContext;
import cn.shopex.ecshopx.promotions.service.export.MemberTaskOverviewExportJobContext;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskGrantStatusDisplay;
import java.util.Map;

public final class MemberTaskExportFileJobPayloadSupport {

	private MemberTaskExportFileJobPayloadSupport() {}

	public static MemberTaskCompletionExportJobContext contextFromPayload(Map<String, Object> payload) {
		long companyId = longFrom(payload.get("company_id"));
		long operatorId = longFrom(payload.get("operator_id"));
		long merchantId = longFrom(payload.get("merchant_id"));
		long supplierId = longFrom(payload.get("supplier_id"));
		Object rawFilter = payload.get("filter");
		Map<?, ?> fm = rawFilter instanceof Map<?, ?> m ? m : Map.of();
		long taskId = longFrom(fm.get("task_id"));
		String datapassBlock = stringOrNull(fm.get("datapass_block"));
		String grantStatus =
				MemberTaskGrantStatusDisplay.normalizeGrantStatusFilter(
						stringOrNull(fm.get("grant_status")));
		return new MemberTaskCompletionExportJobContext(
				companyId, operatorId, merchantId, supplierId, taskId, datapassBlock, grantStatus);
	}

	public static MemberTaskOverviewExportJobContext overviewContextFromPayload(
			Map<String, Object> payload) {
		long companyId = longFrom(payload.get("company_id"));
		long operatorId = longFrom(payload.get("operator_id"));
		long merchantId = longFrom(payload.get("merchant_id"));
		long supplierId = longFrom(payload.get("supplier_id"));
		Object rawFilter = payload.get("filter");
		Map<?, ?> fm = rawFilter instanceof Map<?, ?> m ? m : Map.of();
		return new MemberTaskOverviewExportJobContext(
				companyId,
				operatorId,
				merchantId,
				supplierId,
				stringOrNull(fm.get("task_id")),
				stringOrNull(fm.get("name")),
				stringOrNull(fm.get("type_detail")),
				stringOrNull(fm.get("display_status")),
				intOrNull(fm.get("filter_begin")),
				intOrNull(fm.get("filter_end")),
				stringOrNull(fm.get("datapass_block")));
	}

	private static Integer intOrNull(Object o) {
		if (o == null) {
			return null;
		}
		long v = longFrom(o);
		if (v <= 0L || v > Integer.MAX_VALUE) {
			return null;
		}
		return (int) v;
	}

	private static String stringOrNull(Object o) {
		return o == null ? null : o.toString();
	}

	private static long longFrom(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
