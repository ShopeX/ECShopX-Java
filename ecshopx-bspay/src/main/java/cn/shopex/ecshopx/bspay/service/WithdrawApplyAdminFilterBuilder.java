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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WithdrawApplyAdminFilterBuilder {

	private static final ZoneId WITHDRAW_FILTER_ZONE = ZoneId.systemDefault();

	public LinkedHashMap<String, Object> buildForLists(
			long companyId, long jwtOperatorId, Map<String, Object> jwtMap, HttpServletRequest request) {
		return build(companyId, jwtOperatorId, jwtMap, request, AdminWithdrawFilterMode.LISTS);
	}

	public LinkedHashMap<String, Object> buildForExport(
			long companyId, long jwtOperatorId, Map<String, Object> jwtMap, HttpServletRequest request) {
		return build(companyId, jwtOperatorId, jwtMap, request, AdminWithdrawFilterMode.EXPORT);
	}

	private LinkedHashMap<String, Object> build(
			long companyId,
			long jwtOperatorId,
			Map<String, Object> jwtMap,
			HttpServletRequest request,
			AdminWithdrawFilterMode mode) {
		String operatorTypeRaw = str(jwtMap.get("operator_type")).trim();

		String statusRaw = request.getParameter("status");
		Integer statusVal = null;
		if (statusRaw != null) {
			String st = statusRaw.trim();
			if (st.isEmpty()) {
				throw new BadRequestException("状态必须为整数");
			}
			try {
				statusVal = Integer.parseInt(st);
			} catch (NumberFormatException e) {
				throw new BadRequestException("状态必须为整数");
			}
		}

		String typeParam = request.getParameter("type");
		String type = (typeParam == null || typeParam.trim().isEmpty()) ? "list" : typeParam.trim();
		if (!"list".equals(type) && !"audit".equals(type)) {
			if (mode == AdminWithdrawFilterMode.LISTS) {
				throw new BadRequestException("列表类型只能是list或audit");
			}
			throw new ResourceException("列表类型只能是list或audit");
		}

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", Long.valueOf(companyId));

		if ("list".equals(type)) {
			filter.put("operator_type", operatorTypeRaw);
			String opLower = operatorTypeRaw.toLowerCase(Locale.ROOT);
			if ("admin".equals(opLower) || "staff".equals(opLower)) {
				filter.put("operator_id", Long.valueOf(jwtOperatorId));
			} else if ("distributor".equals(opLower)) {
				long did = parseLong(jwtMap.get("distributor_id"));
				if (did <= 0) {
					throw new ResourceException("分销商身份信息异常，请重新登录");
				}
				filter.put("distributor_id", Long.valueOf(did));
			} else if ("merchant".equals(opLower)) {
				long mid = parseLong(jwtMap.get("merchant_id"));
				if (mid <= 0) {
					throw new ResourceException("商户身份信息异常，请重新登录");
				}
				filter.put("merchant_id", Long.valueOf(mid));
			} else {
				throw new ResourceException("无效的操作者类型");
			}
		} else {
			String otLower = operatorTypeRaw.toLowerCase(Locale.ROOT);
			if (!"admin".equals(otLower) && !"staff".equals(otLower)) {
				throw new ForbiddenException("只有超级管理员和员工可以审核提现申请");
			}
		}

		if (statusVal != null) {
			filter.put("status", Integer.valueOf(statusVal));
		}

		putCreatedRangeFromRequest(filter, request, mode);
		return filter;
	}

	private static void putCreatedRangeFromRequest(
			Map<String, Object> filter, HttpServletRequest request, AdminWithdrawFilterMode mode) {
		String ts = request.getParameter("time_start");
		if (ts != null) {
			int gte =
					mode == AdminWithdrawFilterMode.LISTS
							? parseCreatedBoundStrictForLists(ts)
							: parseCreatedBoundSilentForExport(ts);
			filter.put("created|gte", Integer.valueOf(gte));
		}
		String te = request.getParameter("time_end");
		if (te != null) {
			int lte =
					mode == AdminWithdrawFilterMode.LISTS
							? parseCreatedBoundStrictForLists(te)
							: parseCreatedBoundSilentForExport(te);
			filter.put("created|lte", Integer.valueOf(lte));
		}
	}

	private static int parseCreatedBoundSilentForExport(String raw) {
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0;
		}
		if (s.startsWith("@")) {
			String tail = s.substring(1).trim();
			if (!tail.matches("^\\d+$")) {
				return 0;
			}
			try {
				long sec = Long.parseLong(tail);
				if (sec < 0) {
					return 0;
				}
				return Math.toIntExact(sec);
			} catch (NumberFormatException | ArithmeticException e) {
				return 0;
			}
		}
		if (s.matches("^-?\\d+$")) {
			return 0;
		}
		Long sec = DateExpressionParser.parseToEpochSecond(s, WITHDRAW_FILTER_ZONE);
		if (sec == null || sec < 0 || sec > Integer.MAX_VALUE) {
			return 0;
		}
		return sec.intValue();
	}

	private static int parseCreatedBoundStrictForLists(String raw) {
		String s = raw.trim();
		if (s.isEmpty()) {
			throw new BadRequestException("时间参数格式错误");
		}
		if (s.startsWith("@")) {
			String tail = s.substring(1).trim();
			if (!tail.matches("^\\d+$")) {
				throw new BadRequestException("时间参数格式错误");
			}
			try {
				long sec = Long.parseLong(tail);
				if (sec < 0) {
					throw new BadRequestException("时间参数格式错误");
				}
				return Math.toIntExact(sec);
			} catch (NumberFormatException | ArithmeticException e) {
				throw new BadRequestException("时间参数格式错误");
			}
		}
		if (s.matches("^-?\\d+$")) {
			throw new BadRequestException("时间参数格式错误");
		}
		Long sec = DateExpressionParser.parseToEpochSecond(s, WITHDRAW_FILTER_ZONE);
		if (sec == null || sec < 0 || sec > Integer.MAX_VALUE) {
			throw new BadRequestException("时间参数格式错误");
		}
		return sec.intValue();
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long parseLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
