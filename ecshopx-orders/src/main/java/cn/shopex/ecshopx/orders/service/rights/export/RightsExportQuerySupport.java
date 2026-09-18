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

package cn.shopex.ecshopx.orders.service.rights.export;

import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.orders.domain.Rights;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class RightsExportQuerySupport {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private RightsExportQuerySupport() {}

	public static LambdaQueryWrapper<Rights> toCountWrapper(long companyId, Map<String, Object> filter) {
		return baseWrapper(companyId, filter);
	}

	public static LambdaQueryWrapper<Rights> toPageWrapper(long companyId, Map<String, Object> filter) {
		LambdaQueryWrapper<Rights> w = baseWrapper(companyId, filter);
		w.orderByAsc(Rights::getRightsId);
		return w;
	}

	public static LambdaQueryWrapper<Rights> toRightsListPageWrapper(long companyId, Map<String, Object> filter) {
		LambdaQueryWrapper<Rights> w = baseWrapper(companyId, filter);
		w.orderByDesc(Rights::getCreated);
		return w;
	}

	private static LambdaQueryWrapper<Rights> baseWrapper(long companyId, Map<String, Object> filter) {
		LambdaQueryWrapper<Rights> w = Wrappers.lambdaQuery();
		w.eq(Rights::getCompanyId, companyId);

		Object valid = filter.get("valid");
		if (valid instanceof Number vn) {
			int vi = vn.intValue();
			if (vi == 1) {
				w.eq(Rights::getStatus, "valid");
			} else if (vi == 0) {
				w.ne(Rights::getStatus, "valid");
			}
		}

		Set<String> consumed = Set.of("company_id", "valid");

		for (Map.Entry<String, Object> e : filter.entrySet()) {
			String field = e.getKey();
			if (consumed.contains(field) || "datapass_block".equals(field)) {
				continue;
			}
			Object value = e.getValue();
			if ("datetime".equals(field)) {
				applyDatetime(w, value);
				continue;
			}
			if ("user_id".equals(field)) {
				applyUserId(w, value);
				continue;
			}
			if ("mobile".equals(field)) {
				if (!isEffectiveFilterValue(value)) {
					continue;
				}
				String enc = LegacyFixedMobileEncrypt.fixedEncryptMobile(String.valueOf(value));
				w.eq(Rights::getMobile, enc);
				continue;
			}
			if ("rights_from".equals(field)) {
				if (!isEffectiveFilterValue(value)) {
					continue;
				}
				w.eq(Rights::getRightsFrom, String.valueOf(value).trim());
				continue;
			}
			if ("order_id".equals(field)) {
				if (!isEffectiveFilterValue(value)) {
					continue;
				}
				Long oid = parseLongFlexible(value);
				if (oid != null) {
					w.eq(Rights::getOrderId, oid);
				}
				continue;
			}
			if ("company_id".equals(field)) {
				continue;
			}
		}
		return w;
	}

	private static void applyDatetime(LambdaQueryWrapper<Rights> w, Object raw) {
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return;
		}
		Object a = list.get(0);
		Object b = list.size() > 1 ? list.get(1) : null;
		Long begin = DateExpressionParser.parseToEpochSecond(a, SHANGHAI);
		if (begin == null) {
			return;
		}
		int bi = begin.intValue();
		w.gt(Rights::getEndTime, bi);
		Long end = DateExpressionParser.parseToEpochSecond(b, SHANGHAI);
		if (end != null) {
			w.lt(Rights::getEndTime, end.intValue());
		}
	}

	private static void applyUserId(LambdaQueryWrapper<Rights> w, Object value) {
		if (value instanceof Collection<?> c) {
			List<Long> ids = new ArrayList<>();
			LinkedHashSet<Long> seen = new LinkedHashSet<>();
			for (Object o : c) {
				Long id = parseLongFlexible(o);
				if (id != null && id > 0L && seen.add(id)) {
					ids.add(id);
				}
			}
			if (!ids.isEmpty()) {
				w.in(Rights::getUserId, ids);
			}
			return;
		}
		Long id = parseLongFlexible(value);
		if (id != null && isEffectiveFilterValue(value)) {
			w.eq(Rights::getUserId, id);
		}
	}

	private static Long parseLongFlexible(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Whether a filter entry value should apply a scalar equality condition: ignore null, false, numeric
	 * zero, blank or {@code "0"} strings, and empty collections; other values count as present.
	 */
	private static boolean isEffectiveFilterValue(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Boolean b) {
			return b;
		}
		if (value instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		if (value instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			return true;
		}
		if (value instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		return true;
	}
}
