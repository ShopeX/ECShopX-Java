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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * C2 等级变更落库（Jdbc，避免 shuyun↔members 循环依赖）。
 * 简化对齐：body.id → members.user_id；grade 数字优先匹配 promotion_condition.total_consumption，再回退 external_id。
 */
@Service
public class LoyaltyGradeCallbackService {

	private static final Logger log = LoggerFactory.getLogger(LoyaltyGradeCallbackService.class);

	private final JdbcTemplate jdbcTemplate;

	public LoyaltyGradeCallbackService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void applyGradeChange(long companyId, Map<String, Object> body) {
		Map<String, Object> p = body == null ? Map.of() : body;
		String userIdRaw = stringVal(p.get("id"));
		if (!StringUtils.hasText(userIdRaw) || !userIdRaw.chars().allMatch(Character::isDigit)) {
			throw new IllegalArgumentException("id (user_id) required");
		}
		long userId = Long.parseLong(userIdRaw);
		String gradeRaw = extractGradeRaw(p);
		if (!StringUtils.hasText(gradeRaw)) {
			throw new IllegalArgumentException("grade required");
		}

		Long targetGradeId = resolveLocalGradeId(companyId, gradeRaw);
		if (targetGradeId == null) {
			throw new IllegalArgumentException("GRADE_NOT_MAPPED");
		}

		Integer current =
				jdbcTemplate.query(
						"SELECT grade_id FROM members WHERE company_id=? AND user_id=? LIMIT 1",
						rs -> rs.next() ? rs.getObject(1, Integer.class) : null,
						companyId,
						userId);
		if (current == null && !memberExists(companyId, userId)) {
			throw new IllegalArgumentException("MEMBER_NOT_FOUND");
		}
		int oldGradeId = current == null ? 0 : current;
		if (oldGradeId == targetGradeId.intValue()) {
			log.info(
					"Shuyun loyalty grade callback skipped unchanged companyId={} userId={} gradeId={}",
					companyId,
					userId,
					oldGradeId);
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		int updated =
				jdbcTemplate.update(
						"UPDATE members SET grade_id=?, updated=? WHERE company_id=? AND user_id=?",
						targetGradeId,
						now,
						companyId,
						userId);
		if (updated <= 0) {
			throw new IllegalArgumentException("MEMBER_NOT_FOUND");
		}
		log.info(
				"Shuyun loyalty grade callback persisted companyId={} userId={} old={} new={}",
				companyId,
				userId,
				oldGradeId,
				targetGradeId);
	}

	private boolean memberExists(long companyId, long userId) {
		Integer c =
				jdbcTemplate.query(
						"SELECT 1 FROM members WHERE company_id=? AND user_id=? LIMIT 1",
						rs -> rs.next() ? 1 : null,
						companyId,
						userId);
		return c != null;
	}

	private Long resolveLocalGradeId(long companyId, String gradeRaw) {
		if (gradeRaw.chars().allMatch(Character::isDigit)) {
			Long byLevel =
					jdbcTemplate.query(
							"""
							SELECT grade_id FROM membercard_grade
							WHERE company_id=? AND JSON_UNQUOTE(JSON_EXTRACT(promotion_condition, '$.total_consumption'))=?
							LIMIT 1
							""",
							rs -> rs.next() ? rs.getLong(1) : null,
							companyId,
							gradeRaw);
			if (byLevel != null) {
				return byLevel;
			}
			return jdbcTemplate.query(
					"SELECT grade_id FROM membercard_grade WHERE company_id=? AND external_id=? LIMIT 1",
					rs -> rs.next() ? rs.getLong(1) : null,
					companyId,
					gradeRaw);
		}
		return jdbcTemplate.query(
				"SELECT grade_id FROM membercard_grade WHERE company_id=? AND external_id=? LIMIT 1",
				rs -> rs.next() ? rs.getLong(1) : null,
				companyId,
				gradeRaw);
	}

	private static String extractGradeRaw(Map<String, Object> p) {
		Object g = p.get("grade");
		if (g == null) {
			g = p.get("gradeId");
		}
		return stringVal(g);
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
