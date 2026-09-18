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

package cn.shopex.ecshopx.promotions.service.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 使用显式 {@code members} + {@code members_info} 条件，与 PHP {@code MembersRepository::getList} 活动筛选语义一致。
 */
@Service
public class PromotionScheduleMemberListPortImpl implements PromotionScheduleMemberListPort {

	private final JdbcTemplate jdbcTemplate;

	public PromotionScheduleMemberListPortImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public long countMembers(long companyId, MemberFilterSpec spec) {
		Clause c = buildClause(companyId, spec);
		String sql = "SELECT COUNT(DISTINCT m.user_id) " + c.fromWhere;
		return jdbcTemplate.queryForObject(sql, Long.class, c.argsArray());
	}

	@Override
	public List<Map<String, Object>> getMembers(
			long companyId, MemberFilterSpec spec, int pageSize, int page) {
		Clause c = buildClause(companyId, spec);
		int offset = (page - 1) * pageSize;
		String sql =
				"SELECT m.user_id, m.company_id, m.grade_id, m.mobile, m.user_card_code, "
						+ "i.username, i.name, i.sex, i.birthday "
						+ c.fromWhere
						+ " ORDER BY m.user_id DESC "
						+ " LIMIT ? OFFSET ?";
		List<Object> args = new ArrayList<>(c.argsList());
		args.add(pageSize);
		args.add(offset);
		return jdbcTemplate.queryForList(sql, args.toArray());
	}

	private static Clause buildClause(long companyId, MemberFilterSpec spec) {
		StringBuilder w =
				new StringBuilder(
						"FROM members m LEFT JOIN members_info i ON m.company_id = i.company_id AND m.user_id = i.user_id "
								+ "WHERE m.company_id = ? AND m.mobile IS NOT NULL AND m.user_card_code IS NOT NULL");
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		switch (spec.kind) {
			case ALL -> { }
			case BIRTHDAY_MONTH -> {
				w.append(" AND i.month = ?");
				args.add(spec.month);
			}
			case BIRTHDAY_WEEK -> {
				w.append(" AND i.month = ? AND i.day >= ? AND i.day <= ?");
				args.add(spec.month);
				args.add(spec.dayOrFrom);
				args.add(spec.dayTo);
			}
			case BIRTHDAY_DAY -> {
				w.append(" AND i.month = ? AND i.day = ?");
				args.add(spec.month);
				args.add(spec.dayOrFrom);
			}
			case ANNIVERSARY_MONTH -> {
				w.append(" AND m.created_month = ?");
				args.add(spec.month);
			}
			case ANNIVERSARY_WEEK -> {
				w.append(" AND m.created_month = ? AND m.created_day >= ? AND m.created_day <= ?");
				args.add(spec.month);
				args.add(spec.dayOrFrom);
				args.add(spec.dayTo);
			}
			case ANNIVERSARY_DAY -> {
				w.append(" AND m.created_month = ? AND m.created_day = ?");
				args.add(spec.month);
				args.add(spec.dayOrFrom);
			}
			default -> throw new IllegalStateException("unsupported kind: " + spec.kind);
		}
		return new Clause(w.toString(), args);
	}

	private static final class Clause {
		final String fromWhere;
		private final List<Object> args;

		Clause(String fromWhere, List<Object> args) {
			this.fromWhere = fromWhere;
			this.args = args;
		}

		Object[] argsArray() {
			return args.toArray();
		}

		List<Object> argsList() {
			return args;
		}
	}
}
