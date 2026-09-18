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

import cn.shopex.ecshopx.promotions.service.TurntableConfigService;
import cn.shopex.ecshopx.promotions.service.export.LuckyDrawLogExportJobContext;
import java.util.Map;

public final class LuckyDrawLogExportFileJobPayloadSupport {

	private LuckyDrawLogExportFileJobPayloadSupport() {}

	public static LuckyDrawLogExportJobContext contextFromPayload(
			Map<String, Object> payload, TurntableConfigService turntableConfigService) {
		long companyId = longFrom(payload.get("company_id"));
		long operatorId = longFrom(payload.get("operator_id"));
		long merchantId = longFrom(payload.get("merchant_id"));
		long supplierId = longFrom(payload.get("supplier_id"));
		Object rawFilter = payload.get("filter");
		Map<?, ?> fm = rawFilter instanceof Map<?, ?> m ? m : Map.of();
		String activityIdRaw = stringOrNull(fm.get("activity_id"));
		long actId = turntableConfigService.parseRequiredActivityId(activityIdRaw);
		String datapassBlock = stringOrNull(fm.get("datapass_block"));
		return new LuckyDrawLogExportJobContext(companyId, operatorId, merchantId, supplierId, actId, datapassBlock);
	}

	private static String stringOrNull(Object o) {
		if (o == null) {
			return null;
		}
		return o.toString();
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
