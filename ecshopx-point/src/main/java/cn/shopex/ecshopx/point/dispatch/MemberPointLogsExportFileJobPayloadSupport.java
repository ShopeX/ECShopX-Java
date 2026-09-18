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

package cn.shopex.ecshopx.point.dispatch;

import cn.shopex.ecshopx.point.service.export.PointMemberLogExportContext;
import java.util.Map;

public final class MemberPointLogsExportFileJobPayloadSupport {

	private MemberPointLogsExportFileJobPayloadSupport() {}

	public static PointMemberLogExportContext contextFromPayload(Map<String, Object> payload) {
		long companyId = longFrom(payload.get("company_id"));
		long operatorId = longFrom(payload.get("operator_id"));
		long supplierId = longFrom(payload.get("supplier_id"));

		Object rawFilter = payload.get("filter");
		Map<?, ?> fm = rawFilter instanceof Map<?, ?> m ? m : Map.of();

		long companyFromFilter = longFrom(fm.get("company_id"));
		if (companyFromFilter != 0L) {
			companyId = companyFromFilter;
		}

		long userIdParam = longFrom(fm.get("user_id"));
		String mobile = filterString(fm.get("mobile"));
		String username = filterString(fm.get("username"));
		String name = filterString(fm.get("name"));
		Long dateBegin = longObjectToNullableLong(fm.get("date_begin"));
		Long dateEnd = longObjectToNullableLong(fm.get("date_end"));
		boolean datapassBlock = truthyDatapass(fm.get("datapass_block"));

		return new PointMemberLogExportContext(
				companyId, operatorId, supplierId, userIdParam, mobile, username, name, dateBegin, dateEnd, datapassBlock);
	}

	private static boolean truthyDatapass(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = o.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static String filterString(Object o) {
		if (o == null) {
			return null;
		}
		return o.toString();
	}

	private static Long longObjectToNullableLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
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
