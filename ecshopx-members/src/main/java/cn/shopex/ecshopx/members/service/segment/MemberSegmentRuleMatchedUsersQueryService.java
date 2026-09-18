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

package cn.shopex.ecshopx.members.service.segment;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberSegmentRuleMatchedUsersQueryService {

	private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd", Locale.ROOT);

	private final JdbcTemplate jdbcTemplate;

	public MemberSegmentRuleMatchedUsersQueryService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Long> queryMatchedUserIds(JsonNode ruleConfig, long companyId, long distributorId) {
		if (ruleConfig == null || !ruleConfig.isArray() || ruleConfig.isEmpty()) {
			return Collections.emptyList();
		}

		List<List<Long>> topTypeResults = new ArrayList<>();
		for (JsonNode topTypeConfig : ruleConfig) {
			String type = textOrEmpty(topTypeConfig.path("type"));
			JsonNode subConditions = topTypeConfig.path("sub");
			if (!StringUtils.hasText(type) || !subConditions.isArray() || subConditions.isEmpty()) {
				continue;
			}
			JsonNode totalCondition = topTypeConfig.path("total_condition");
			List<Long> userIds;
			switch (type) {
				case "member" -> userIds = queryMemberType(subConditions, companyId, distributorId);
				case "order" -> userIds = queryOrderType(subConditions, totalCondition, companyId, distributorId);
				default -> throw new ResourceException("不支持的规则类型: " + type);
			}
			topTypeResults.add(userIds);
		}

		if (topTypeResults.isEmpty()) {
			return Collections.emptyList();
		}

		Set<Long> result = new HashSet<>(topTypeResults.get(0));
		for (int i = 1; i < topTypeResults.size(); i++) {
			result.retainAll(new HashSet<>(topTypeResults.get(i)));
		}
		List<Long> out = new ArrayList<>(result);
		Collections.sort(out);
		return out;
	}

	private List<Long> queryMemberType(JsonNode subConditions, long companyId, long distributorId) {
		List<Long> allUserIds = null;
		for (JsonNode subCondition : subConditions) {
			String subType = textOrEmpty(subCondition.path("type"));
			JsonNode params = subCondition.path("params");
			List<Long> userIds = switch (subType) {
				case "birthday" -> queryBirthday(params, companyId, distributorId);
				case "grade" -> queryGrade(params, companyId, distributorId);
				case "point" -> queryPoint(params, companyId, distributorId);
				default -> null;
			};
			if (userIds == null) {
				continue;
			}
			if (allUserIds == null) {
				allUserIds = new ArrayList<>(userIds);
			} else {
				if (userIds.isEmpty()) {
					allUserIds = new ArrayList<>();
				} else {
					allUserIds.retainAll(new HashSet<>(userIds));
				}
			}
		}
		if (allUserIds == null) {
			return Collections.emptyList();
		}
		return dedupeSorted(allUserIds);
	}

	private List<Long> queryOrderType(
			JsonNode subConditions, JsonNode totalCondition, long companyId, long distributorId) {
		List<Long> timeRange = extractTimeRangeParams(totalCondition);
		if (timeRange.size() < 2) {
			throw new ResourceException("订单类型必须设置时间范围总条件");
		}
		long startTime = timeRange.get(0);
		long endTime = timeRange.get(1);

		List<Long> allUserIds = null;
		for (JsonNode subCondition : subConditions) {
			String subType = textOrEmpty(subCondition.path("type"));
			JsonNode params = subCondition.path("params");
			List<Long> userIds = switch (subType) {
				case "perOrder" -> queryPerOrder(params, startTime, endTime, companyId, distributorId);
				case "sumaryOrder" -> querySumaryOrder(params, startTime, endTime, companyId, distributorId);
				case "orderItem" -> queryOrderItem(params, startTime, endTime, companyId, distributorId);
				case "hasOrder" -> queryHasOrder(params, startTime, endTime, companyId, distributorId);
				default -> null;
			};
			if (userIds == null) {
				continue;
			}
			if (allUserIds == null) {
				allUserIds = new ArrayList<>(userIds);
			} else {
				if (userIds.isEmpty()) {
					allUserIds = new ArrayList<>();
				} else {
					allUserIds.retainAll(new HashSet<>(userIds));
				}
			}
		}
		if (allUserIds == null) {
			return Collections.emptyList();
		}
		return dedupeSorted(allUserIds);
	}

	private static List<Long> extractTimeRangeParams(JsonNode totalCondition) {
		if (totalCondition == null || !totalCondition.isArray() || totalCondition.isEmpty()) {
			return Collections.emptyList();
		}
		JsonNode first = totalCondition.get(0);
		if (first == null || !first.isObject()) {
			return Collections.emptyList();
		}
		if (!"timeRange".equals(textOrEmpty(first.path("condition_type")))) {
			return Collections.emptyList();
		}
		JsonNode params = first.path("params");
		if (!params.isArray()) {
			return Collections.emptyList();
		}
		List<Long> out = new ArrayList<>();
		for (JsonNode n : params) {
			if (n.isNumber()) {
				out.add(n.longValue());
			} else if (n.isTextual()) {
				try {
					out.add(Long.parseLong(n.asText().trim()));
				} catch (NumberFormatException e) {
					out.add(0L);
				}
			}
		}
		return out;
	}

	private List<Long> queryBirthday(JsonNode params, long companyId, long distributorId) {
		if (params == null || !params.isArray() || params.size() < 2) {
			return Collections.emptyList();
		}
		long startTimestamp = params.get(0).asLong(0L);
		long endTimestamp = params.get(1).asLong(0L);
		ZoneId zone = ZoneId.systemDefault();
		String startDate = Instant.ofEpochSecond(startTimestamp).atZone(zone).format(MD);
		String endDate = Instant.ofEpochSecond(endTimestamp).atZone(zone).format(MD);

		StringBuilder sql = new StringBuilder(
				"""
				SELECT DISTINCT mi.user_id
				FROM members_info mi
				INNER JOIN members m ON mi.user_id = m.user_id
				WHERE mi.company_id = ?
				  AND m.company_id = ?
				""");
		List<Object> bind = new ArrayList<>();
		bind.add(companyId);
		bind.add(companyId);
		if (distributorId > 0) {
			sql.append(
					"""
					 AND EXISTS (
					    SELECT 1 FROM distribution_distributor_user ddu
					    WHERE ddu.user_id = m.user_id AND ddu.distributor_id = ?
					)
					""");
			bind.add(distributorId);
		}
		if (startDate.compareTo(endDate) <= 0) {
			sql.append(
					"""
					 AND (
					    CASE
					        WHEN mi.birthday LIKE '%-%-%' THEN DATE_FORMAT(STR_TO_DATE(mi.birthday, '%Y-%m-%d'), '%m-%d')
					        ELSE mi.birthday
					    END BETWEEN ? AND ?
					)
					""");
			bind.add(startDate);
			bind.add(endDate);
		} else {
			sql.append(
					"""
					 AND (
					    CASE
					        WHEN mi.birthday LIKE '%-%-%' THEN DATE_FORMAT(STR_TO_DATE(mi.birthday, '%Y-%m-%d'), '%m-%d')
					        ELSE mi.birthday
					    END >= ?
					    OR CASE
					        WHEN mi.birthday LIKE '%-%-%' THEN DATE_FORMAT(STR_TO_DATE(mi.birthday, '%Y-%m-%d'), '%m-%d')
					        ELSE mi.birthday
					    END <= ?
					)
					""");
			bind.add(startDate);
			bind.add(endDate);
		}
		return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("user_id"), bind.toArray());
	}

	private List<Long> queryGrade(JsonNode params, long companyId, long distributorId) {
		if (params == null || !params.isArray() || params.isEmpty()) {
			return Collections.emptyList();
		}
		List<Long> gradeIds = new ArrayList<>();
		for (JsonNode n : params) {
			gradeIds.add(n.asLong(0L));
		}
		StringBuilder sql = new StringBuilder(
				"""
				SELECT DISTINCT m.user_id
				FROM members m
				WHERE m.company_id = ?
				""");
		List<Object> bind = new ArrayList<>();
		bind.add(companyId);
		if (distributorId > 0) {
			sql.append(
					"""
					 AND EXISTS (
					    SELECT 1 FROM distribution_distributor_user ddu
					    WHERE ddu.user_id = m.user_id AND ddu.distributor_id = ?
					)
					""");
			bind.add(distributorId);
		}
		sql.append(" AND m.grade_id IN (");
		for (int i = 0; i < gradeIds.size(); i++) {
			if (i > 0) {
				sql.append(", ");
			}
			sql.append("?");
			bind.add(gradeIds.get(i));
		}
		sql.append(")");
		return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("user_id"), bind.toArray());
	}

	private List<Long> queryPoint(JsonNode params, long companyId, long distributorId) {
		if (params == null || !params.isArray() || params.size() < 2) {
			return Collections.emptyList();
		}
		long minPoint = params.get(0).asLong(0L);
		long maxPoint = params.get(1).asLong(0L);
		StringBuilder sql = new StringBuilder(
				"""
				SELECT DISTINCT pm.user_id
				FROM point_member pm
				INNER JOIN members m ON pm.user_id = m.user_id AND pm.company_id = m.company_id
				WHERE pm.company_id = ?
				  AND pm.point >= ?
				  AND pm.point <= ?
				""");
		List<Object> bind = new ArrayList<>();
		bind.add(companyId);
		bind.add(minPoint);
		bind.add(maxPoint);
		if (distributorId > 0) {
			sql.append(
					"""
					 AND EXISTS (
					    SELECT 1 FROM distribution_distributor_user ddu
					    WHERE ddu.user_id = m.user_id AND ddu.distributor_id = ?
					)
					""");
			bind.add(distributorId);
		}
		return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("user_id"), bind.toArray());
	}

	private List<Long> queryPerOrder(
			JsonNode params, long startTime, long endTime, long companyId, long distributorId) {
		if (params == null || !params.isArray() || params.size() < 2) {
			return Collections.emptyList();
		}
		long minAmount = params.get(0).asLong(0L) * 100L;
		long maxAmount = params.get(1).asLong(0L) * 100L;
		StringBuilder sql = new StringBuilder(
				"""
				SELECT DISTINCT o.user_id
				FROM orders_normal_orders o
				WHERE o.company_id = ?
				  AND o.create_time >= ?
				  AND o.create_time <= ?
				  AND o.total_fee >= ?
				  AND o.total_fee <= ?
				  AND o.pay_status = 'PAYED'
				""");
		List<Object> bind = new ArrayList<>();
		bind.add(companyId);
		bind.add(startTime);
		bind.add(endTime);
		bind.add(minAmount);
		bind.add(maxAmount);
		if (distributorId > 0) {
			sql.append(" AND o.distributor_id = ?");
			bind.add(distributorId);
		}
		return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("user_id"), bind.toArray());
	}

	private List<Long> querySumaryOrder(
			JsonNode params, long startTime, long endTime, long companyId, long distributorId) {
		if (params == null || !params.isArray() || params.size() < 2) {
			return Collections.emptyList();
		}
		long minAmount = params.get(0).asLong(0L) * 100L;
		long maxAmount = params.get(1).asLong(0L) * 100L;
		StringBuilder sql = new StringBuilder(
				"""
				SELECT o.user_id, SUM(CAST(o.total_fee AS UNSIGNED)) as total_amount
				FROM orders_normal_orders o
				WHERE o.company_id = ?
				  AND o.create_time >= ?
				  AND o.create_time <= ?
				  AND o.pay_status = 'PAYED'
				""");
		List<Object> bind = new ArrayList<>();
		bind.add(companyId);
		bind.add(startTime);
		bind.add(endTime);
		if (distributorId > 0) {
			sql.append(" AND o.distributor_id = ?");
			bind.add(distributorId);
		}
		sql.append(" GROUP BY o.user_id HAVING total_amount >= ? AND total_amount <= ?");
		bind.add(minAmount);
		bind.add(maxAmount);
		return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("user_id"), bind.toArray());
	}

	private List<Long> queryOrderItem(
			JsonNode params, long startTime, long endTime, long companyId, long distributorId) {
		if (params == null || !params.isArray() || params.isEmpty()) {
			return Collections.emptyList();
		}
		List<Long> itemIds = new ArrayList<>();
		for (JsonNode n : params) {
			itemIds.add(n.asLong(0L));
		}
		StringBuilder sql = new StringBuilder(
				"""
				SELECT DISTINCT o.user_id
				FROM orders_normal_orders o
				INNER JOIN orders_normal_orders_items oi ON o.order_id = oi.order_id
				WHERE o.company_id = ?
				  AND o.create_time >= ?
				  AND o.create_time <= ?
				  AND o.pay_status = 'PAYED'
				""");
		List<Object> bind = new ArrayList<>();
		bind.add(companyId);
		bind.add(startTime);
		bind.add(endTime);
		if (distributorId > 0) {
			sql.append(" AND o.distributor_id = ?");
			bind.add(distributorId);
		}
		sql.append(" AND oi.item_id IN (");
		for (int i = 0; i < itemIds.size(); i++) {
			if (i > 0) {
				sql.append(", ");
			}
			sql.append("?");
			bind.add(itemIds.get(i));
		}
		sql.append(")");
		return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("user_id"), bind.toArray());
	}

	private List<Long> queryHasOrder(
			JsonNode params, long startTime, long endTime, long companyId, long distributorId) {
		if (params == null || !params.isArray() || params.isEmpty()) {
			return Collections.emptyList();
		}
		long hasOrder = params.get(0).asLong(0L);
		if (hasOrder == 1L) {
			StringBuilder sql = new StringBuilder(
					"""
					SELECT DISTINCT o.user_id
					FROM orders_normal_orders o
					WHERE o.company_id = ?
					  AND o.create_time >= ?
					  AND o.create_time <= ?
					  AND o.pay_status = 'PAYED'
					""");
			List<Object> bind = new ArrayList<>();
			bind.add(companyId);
			bind.add(startTime);
			bind.add(endTime);
			if (distributorId > 0) {
				sql.append(" AND o.distributor_id = ?");
				bind.add(distributorId);
			}
			return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("user_id"), bind.toArray());
		}
		StringBuilder sql = new StringBuilder(
				"""
				SELECT DISTINCT m.user_id
				FROM members m
				WHERE m.company_id = ?
				  AND NOT EXISTS (
				      SELECT 1 FROM orders_normal_orders o
				      WHERE o.user_id = m.user_id
				        AND o.company_id = ?
				        AND o.create_time >= ?
				        AND o.create_time <= ?
				        AND o.pay_status = 'PAYED'
				""");
		List<Object> bind = new ArrayList<>();
		bind.add(companyId);
		bind.add(companyId);
		bind.add(startTime);
		bind.add(endTime);
		if (distributorId > 0) {
			sql.append(" AND o.distributor_id = ?");
			bind.add(distributorId);
		}
		sql.append(")");
		if (distributorId > 0) {
			sql.append(
					"""
					 AND EXISTS (
					    SELECT 1 FROM distribution_distributor_user ddu
					    WHERE ddu.user_id = m.user_id AND ddu.distributor_id = ?
					)
					""");
			bind.add(distributorId);
		}
		return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong("user_id"), bind.toArray());
	}

	private static String textOrEmpty(JsonNode n) {
		if (n == null || n.isMissingNode() || n.isNull()) {
			return "";
		}
		if (n.isTextual()) {
			return n.asText("");
		}
		return n.asText("");
	}

	private static List<Long> dedupeSorted(List<Long> ids) {
		Set<Long> set = new HashSet<>(ids);
		List<Long> out = new ArrayList<>(set);
		Collections.sort(out);
		return out;
	}
}
