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

package cn.shopex.ecshopx.employeepurchase.support;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * 内购活动列表「展示态」status 查询解析与改写。
 *
 * <p>筛选解析与 PHP {@code ActivityListDisplayStatusQuery} 一致；展示态改写与 PHP {@code
 * ActivitiesRepository::buildActivityListStatusCondition} 及后台列表一致：未开通亲友购时 {@code
 * relative_begin_time} 为 null/0，只要员工购买尚未开始，仍判为预热中（不能因 0 &lt; now 误判进行中）。
 */
public final class ActivityListDisplayStatusQuery {

	public static final List<String> ALLOWED_STATUSES =
			List.of("not_started", "warm_up", "ongoing", "pending", "cancel", "over");

	private ActivityListDisplayStatusQuery() {}

	public static List<String> statusSlugsForFilterOrNull(String rawStatus) {
		List<String> statuses = collectSlugs(rawStatus);
		if (statuses.isEmpty()) {
			return null;
		}
		for (String s : statuses) {
			if (!ALLOWED_STATUSES.contains(s)) {
				throw new ResourceException("status 参数不合法");
			}
		}
		return statuses;
	}

	private static List<String> collectSlugs(String rawStatus) {
		if (!StringUtils.hasText(rawStatus)) {
			return List.of();
		}
		Set<String> unique = new LinkedHashSet<>();
		for (String part : rawStatus.split(",")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				unique.add(trimmed);
			}
		}
		return new ArrayList<>(unique);
	}

	/**
	 * 按 PHP 后台列表 / SQL 展示态条件改写 {@code status}、{@code status_desc}。
	 *
	 * @param relativeBegin 亲友开始时间，null 与 0 同义（未开通亲友购）
	 * @param relativeEnd 亲友结束时间，null 与 0 同义
	 */
	public static void applyDisplayStatusRewrite(
			Map<String, Object> row,
			String dbStatus,
			long displayTime,
			long employeeBegin,
			long employeeEnd,
			Long relativeBegin,
			Long relativeEnd,
			int now) {
		long rb = relativeBegin == null ? 0L : relativeBegin.longValue();
		long re = relativeEnd == null ? 0L : relativeEnd.longValue();
		boolean relativeNotStarted = rb == 0L || rb > now;
		boolean relativeOngoing = rb > 0L && rb < now;

		if (displayTime > now && "active".equals(dbStatus)) {
			row.put("status", "not_started");
			row.put("status_desc", "未开始");
		}
		if (displayTime < now
				&& employeeBegin > now
				&& relativeNotStarted
				&& "active".equals(dbStatus)) {
			row.put("status", "warm_up");
			row.put("status_desc", "预热中");
		}
		if ((employeeBegin < now || relativeOngoing)
				&& (employeeEnd > now || re > now)
				&& "active".equals(dbStatus)) {
			row.put("status", "ongoing");
			row.put("status_desc", "进行中");
		}
		if ((employeeBegin < now || relativeOngoing)
				&& (employeeEnd > now || re > now)
				&& "pending".equals(dbStatus)) {
			row.put("status", "pending");
			row.put("status_desc", "已暂停");
		}
		if ("cancel".equals(dbStatus)) {
			row.put("status", "cancel");
			row.put("status_desc", "已取消");
		}
		if ((employeeEnd < now && re < now) || "over".equals(dbStatus)) {
			row.put("status", "over");
			row.put("status_desc", "已结束");
		}
	}
}
