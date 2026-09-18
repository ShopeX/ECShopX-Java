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

package cn.shopex.ecshopx.chinaumspay.service.divisiondetail;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DivisionDetailExportFilterBuilder {

	public DivisionDetailExportFilter build(Map<String, Object> jwtUser, String divisionIdStr, String orderIdStr,
			String timeStartBegin, String timeStartEnd) {
		Object companyObj = jwtUser.get("company_id");
		long companyId = toLong(companyObj);

		Long orderId = null;
		if (StringUtils.hasText(orderIdStr)) {
			try {
				orderId = Long.parseLong(orderIdStr.trim());
			} catch (NumberFormatException ignored) {
				// skip invalid
			}
		}
		Long divisionId = null;
		if (StringUtils.hasText(divisionIdStr)) {
			try {
				divisionId = Long.parseLong(divisionIdStr.trim());
			} catch (NumberFormatException ignored) {
				// skip invalid
			}
		}

		String createBegin = null;
		String createEnd = null;
		if (StringUtils.hasText(timeStartBegin) && StringUtils.hasText(timeStartEnd)) {
			createBegin = timeStartBegin.trim();
			createEnd = timeStartEnd.trim();
		}

		Long distributorId = null;
		Object operatorType = jwtUser.get("operator_type");
		if (operatorType != null && "distributor".equals(String.valueOf(operatorType))) {
			distributorId = toLong(jwtUser.get("distributor_id"));
		}

		return new DivisionDetailExportFilter(companyId, orderId, divisionId, distributorId, createBegin, createEnd);
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}
}
