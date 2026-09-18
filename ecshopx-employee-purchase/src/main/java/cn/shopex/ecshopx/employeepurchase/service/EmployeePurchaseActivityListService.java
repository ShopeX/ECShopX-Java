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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseUserActivitiesQueryMapper;
import cn.shopex.ecshopx.employeepurchase.support.ActivityListDisplayStatusQuery;
import cn.shopex.ecshopx.employeepurchase.service.passphrase.PassphraseVerifiedRedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseActivityListService {

	private final EmployeePurchaseUserActivitiesQueryMapper userActivitiesQueryMapper;
	private final EmployeePurchaseActivityDataService employeePurchaseActivityDataService;
	private final ObjectMapper objectMapper;
	private final PassphraseVerifiedRedisService passphraseVerifiedRedisService;

	public EmployeePurchaseActivityListService(
			EmployeePurchaseUserActivitiesQueryMapper userActivitiesQueryMapper,
			EmployeePurchaseActivityDataService employeePurchaseActivityDataService,
			ObjectMapper objectMapper,
			PassphraseVerifiedRedisService passphraseVerifiedRedisService) {
		this.userActivitiesQueryMapper = userActivitiesQueryMapper;
		this.employeePurchaseActivityDataService = employeePurchaseActivityDataService;
		this.objectMapper = objectMapper;
		this.passphraseVerifiedRedisService = passphraseVerifiedRedisService;
	}

	public Map<String, Object> getActivityList(
			long companyId,
			long userId,
			String activityName,
			long enterpriseIdParam,
			String needAggregateRaw,
			Long activityId,
			String statusRaw,
			int page,
			int pageSize) {
		long enterpriseId = enterpriseIdParam;
		if (enterpriseId <= 0L) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", "0");
			empty.put("list", List.of());
			return empty;
		}
		if (companyId <= 0L || userId <= 0L) {
			throw new BadRequestException("参数错误");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		page = Math.max(1, page);
		pageSize = pageSize <= 0 ? 20 : pageSize;

		String nameFilter =
				StringUtils.hasText(activityName) ? activityName.trim() : null;
		List<String> statusFilter = ActivityListDisplayStatusQuery.statusSlugsForFilterOrNull(statusRaw);
		boolean hasStatusFilter = statusFilter != null;

		long total =
				userActivitiesQueryMapper.countUserActivities(
						companyId, userId, now, enterpriseId, nameFilter, activityId);

		List<LinkedHashMap<String, Object>> list;
		if (total == 0L) {
			list = List.of();
		} else {
		long offset = hasStatusFilter ? 0L : (long) (page - 1) * pageSize;
		int fetchSize = hasStatusFilter ? safeIntSize(Math.max(total, pageSize)) : pageSize;
			list =
					userActivitiesQueryMapper.selectUserActivities(
							companyId,
							userId,
							now,
							enterpriseId,
							nameFilter,
							activityId,
							offset,
							fetchSize);
		}

		List<LinkedHashMap<String, Object>> processed = postProcessRows(list, now, companyId, userId, enterpriseId);

		if (hasStatusFilter) {
			Set<String> allowed = new LinkedHashSet<>(statusFilter);
			processed =
					processed.stream()
							.filter(row -> {
								Object st = row.get("status");
								return st != null && allowed.contains(st.toString());
							})
							.toList();
			total = processed.size();
			int fromIndex = (page - 1) * pageSize;
			if (fromIndex >= processed.size()) {
				processed = List.of();
			} else {
				int toIndex = Math.min(fromIndex + pageSize, processed.size());
				processed = processed.subList(fromIndex, toIndex);
			}
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", processed);

		if (StringUtils.hasText(needAggregateRaw)) {
			for (LinkedHashMap<String, Object> row : processed) {
				long aid = parseRowId(row.get("id"));
				Map<String, Object> fee =
						employeePurchaseActivityDataService.getAggregateFeeForInvitee(
								companyId, enterpriseId, aid, userId);
				row.put("fee", fee);
			}
		}

		return result;
	}

	private List<LinkedHashMap<String, Object>> postProcessRows(
			List<LinkedHashMap<String, Object>> rows, int now, long companyId, long userId, long enterpriseId) {
		if (rows == null || rows.isEmpty()) {
			return rows == null ? List.of() : rows;
		}
		List<LinkedHashMap<String, Object>> out = new ArrayList<>(rows.size());
		for (LinkedHashMap<String, Object> row : rows) {
			LinkedHashMap<String, Object> copy = snakeCaseKeys(row);
			copy.remove("share_pic");

			String snapStatus = copy.get("status") == null ? "" : copy.get("status").toString();
			ActivityListDisplayStatusQuery.applyDisplayStatusRewrite(
					copy,
					snapStatus,
					parseRowTime(copy.get("display_time")),
					parseRowTime(copy.get("employee_begin_time")),
					parseRowTime(copy.get("employee_end_time")),
					parseRowTimeNullable(copy.get("relative_begin_time")),
					parseRowTimeNullable(copy.get("relative_end_time")),
					now);

			copy.put("price_display_config", parsePriceDisplayConfig(copy.get("price_display_config")));
			copy.put(
					"is_discount_description_enabled",
					discountEnabledToString(copy.get("is_discount_description_enabled")));

			copy.put("relative_begin_time", relativeTimeForResponse(copy.get("relative_begin_time")));
			copy.put("relative_end_time", relativeTimeForResponse(copy.get("relative_end_time")));

			Object authType = copy.get("auth_type");
			copy.put(
					"auth_type",
					authType != null && StringUtils.hasText(authType.toString()) ? authType.toString() : "");

			appendPassphraseListFields(copy, companyId, userId, enterpriseId);

			out.add(copy);
		}
		return out;
	}

	private void appendPassphraseListFields(
			LinkedHashMap<String, Object> row, long companyId, long userId, long enterpriseId) {
		long activityId = parseRowId(row.get("id"));
		if (activityId <= 0L) {
			row.put("is_passphrase_enabled", 0);
			row.put("passphrase_user_verified", 0);
			return;
		}
		boolean enabled = parsePassphraseEnabled(row.remove("is_passphrase_enabled"));
		row.put("is_passphrase_enabled", enabled ? 1 : 0);
		int verified =
				passphraseVerifiedRedisService.isVerified(companyId, activityId, enterpriseId, userId) ? 1 : 0;
		row.put("passphrase_user_verified", verified);
	}

	private static boolean parsePassphraseEnabled(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		if (raw == null) {
			return false;
		}
		String s = raw.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parsePriceDisplayConfig(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.toString();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return objectMapper.readValue(s, Map.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private static String discountEnabledToString(Object raw) {
		int v = 0;
		if (raw instanceof Boolean b) {
			v = b ? 1 : 0;
		} else if (raw instanceof Number n) {
			v = n.intValue();
		} else if (raw != null && StringUtils.hasText(raw.toString())) {
			try {
				v = Integer.parseInt(raw.toString().trim());
			} catch (NumberFormatException ignored) {
				v = 0;
			}
		}
		return v == 1 ? "true" : "false";
	}

	private static Object relativeTimeForResponse(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int safeIntSize(long size) {
		if (size <= 0L) {
			return 0;
		}
		return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
	}

	private static LinkedHashMap<String, Object> snakeCaseKeys(Map<String, Object> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (src == null || src.isEmpty()) {
			return out;
		}
		for (Map.Entry<String, Object> e : src.entrySet()) {
			String k = e.getKey();
			if (k == null) {
				continue;
			}
			out.put(camelToSnakeKey(k), e.getValue());
		}
		return out;
	}

	private static String camelToSnakeKey(String name) {
		boolean hasUpper = false;
		for (int i = 0; i < name.length(); i++) {
			if (Character.isUpperCase(name.charAt(i))) {
				hasUpper = true;
				break;
			}
		}
		if (!hasUpper) {
			return name;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				if (i > 0) {
					sb.append('_');
				}
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static Long parseRowTimeNullable(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseRowTime(Object raw) {
		Long v = parseRowTimeNullable(raw);
		return v == null ? 0L : v.longValue();
	}

	private static long parseRowId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
