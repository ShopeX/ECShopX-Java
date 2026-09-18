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

package cn.shopex.ecshopx.companys.service.employee;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.dto.DistributorIdRef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业员工账号编排共用：JWT 声明读取、分销商 JSON 片段、自配送五字段校验。
 */
public final class EmployeeOrchestrationSupport {

	private EmployeeOrchestrationSupport() {}

	public static long requireLongClaim(Map<String, Object> jwt, String snake, String camel) {
		Long v = longClaimOrNull(jwt, snake, camel);
		if (v == null) {
			throw new BadRequestException("缺少企业或账号信息");
		}
		return v;
	}

	public static Long longClaimOrNull(Map<String, Object> jwt, String snake, String camel) {
		Object v = jwt.get(snake);
		if (v == null) {
			v = jwt.get(camel);
		}
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static String strClaim(Map<String, Object> jwt, String snake, String camel) {
		Object v = jwt.get(snake);
		if (v == null) {
			v = jwt.get(camel);
		}
		return v != null ? v.toString() : "";
	}

	public static String normalizeDistributorIdForJsonLookup(Object did) {
		if (did instanceof Number n) {
			return String.valueOf(n.longValue());
		}
		String s = did.toString().trim();
		try {
			return String.valueOf(Long.parseLong(s));
		} catch (NumberFormatException e) {
			return s;
		}
	}

	public static Long longFromDistributorIdObject(Object did) {
		if (did == null) {
			return null;
		}
		if (did instanceof Number n) {
			long v = n.longValue();
			return v > 0 ? v : null;
		}
		String s = did.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static List<Map<String, Object>> toDistributorMapList(List<DistributorIdRef> refs) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (DistributorIdRef r : refs) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("distributor_id", r.getDistributorId());
			m.put("name", r.getName());
			out.add(m);
		}
		return out;
	}

	public static void validateSelfDeliveryStaffFiveFields(
			String staffType,
			String staffNo,
			String staffAttribute,
			String paymentMethod,
			Object paymentFee) {
		if (isBlank(staffType)) {
			throw new BadRequestException("配送员类型必填");
		}
		if (isBlank(staffNo)) {
			throw new BadRequestException("配送员编号必填");
		}
		if (isBlank(staffAttribute)) {
			throw new BadRequestException("配送员属性必填");
		}
		if (isBlank(paymentMethod)) {
			throw new BadRequestException("结算方式必填");
		}
		if (paymentFee == null) {
			throw new BadRequestException("结算费用必填");
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.isEmpty();
	}
}
