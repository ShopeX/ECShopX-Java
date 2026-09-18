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

package cn.shopex.ecshopx.community.service.admin;

import cn.shopex.ecshopx.community.dto.CommunityActivityAdminListQuery;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 管理端活动列表与团购订单导出共用的 query 解析（与 {@code GET /api/v1/community/list} 参数语义一致）。
 */
public final class CommunityActivityAdminListQuerySupport {

	private static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private CommunityActivityAdminListQuerySupport() {}

	public static CommunityActivityAdminListQuery buildQuery(Map<String, Object> jwtUser, HttpServletRequest request) {
		String operatorType = stringVal(jwtUser.get("operator_type"));
		boolean distributorOperator = "distributor".equals(operatorType);
		Integer distributorIdFilter =
				distributorOperator
						? parseShopBranchDistributorIdOrNull(request.getParameter("distributor_id"))
						: Integer.valueOf(0);

		CommunityActivityAdminListQuery query = new CommunityActivityAdminListQuery();
		query.setDistributorOperator(distributorOperator);
		query.setDistributorIdFilter(distributorIdFilter);
		applyCreatedAtFiltersFromRequest(request, query);
		String asParam = request.getParameter("activity_status");
		query.setActivityStatus(StringUtils.hasText(asParam) ? asParam.trim() : null);
		query.setSuccess(parseIsSuccessQuery(request));
		String an = request.getParameter("activity_name");
		query.setActivityNameContains(StringUtils.hasText(an) ? an.trim() : null);
		return query;
	}

	/**
	 * {@code activity_id} 存在且可解析为大于 0 的整数时返回其 long 值，否则 {@code null}（与「有值则写入 filter」语义一致）。
	 */
	public static Long parseOptionalActivityId(HttpServletRequest request) {
		if (!request.getParameterMap().containsKey("activity_id")) {
			return null;
		}
		String raw = request.getParameter("activity_id");
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static Integer parseShopBranchDistributorIdOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			String t = v.toString().trim();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static void applyCreatedAtFiltersFromRequest(HttpServletRequest request, CommunityActivityAdminListQuery query) {
		String timeStart = request.getParameter("time_start_begin");
		if (!StringUtils.hasText(timeStart)) {
			return;
		}
		timeStart = timeStart.trim();
		String timeEndParam = request.getParameter("time_start_end");
		ZoneId z = ZoneId.systemDefault();
		if (timeStart.contains("-")) {
			try {
				LocalDate start = LocalDate.parse(timeStart, ISO_LOCAL_DATE);
				query.setCreatedAtGte(start.atStartOfDay(z).toEpochSecond());
				if (StringUtils.hasText(timeEndParam)) {
					LocalDate end = LocalDate.parse(timeEndParam.trim(), ISO_LOCAL_DATE);
					query.setCreatedAtLte(end.atTime(23, 59, 59).atZone(z).toEpochSecond());
				} else {
					LocalDate today = LocalDate.now(z);
					query.setCreatedAtLte(today.atTime(23, 59, 59).atZone(z).toEpochSecond());
				}
				query.setFilterCreatedAtLteWithNull(false);
			} catch (Exception ignored) {
				// invalid date input: do not apply created_at range
			}
		} else {
			Long gte = parseLongDigitsOnlyOrNull(timeStart);
			if (gte != null) {
				query.setCreatedAtGte(gte);
			}
			if (StringUtils.hasText(timeEndParam)) {
				query.setCreatedAtLte(parseLongDigitsOnlyOrNull(timeEndParam.trim()));
				query.setFilterCreatedAtLteWithNull(false);
			} else {
				query.setCreatedAtLte(null);
				query.setFilterCreatedAtLteWithNull(true);
			}
		}
	}

	public static Long parseLongDigitsOnlyOrNull(String s) {
		if (!StringUtils.hasText(s)) {
			return null;
		}
		String t = s.trim();
		if (!t.matches("-?\\d+")) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static boolean parseIsSuccessQuery(HttpServletRequest request) {
		if (!request.getParameterMap().containsKey("is_success")) {
			return false;
		}
		String v = request.getParameter("is_success");
		if (v == null || v.isBlank()) {
			return false;
		}
		String s = v.trim().toLowerCase(Locale.ROOT);
		return !("0".equals(s) || "false".equals(s) || "no".equals(s) || "off".equals(s));
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}
}
