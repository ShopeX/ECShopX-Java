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

package cn.shopex.ecshopx.digios.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DigiOsOperatorLoadService {

	private final JdbcTemplate jdbcTemplate;

	public DigiOsOperatorLoadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Map<String, Object> loadOperator(long companyId, long operatorId) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"""
				SELECT operator_id, company_id, login_name, username, mobile, operator_type
				FROM operators
				WHERE company_id = ? AND operator_id = ?
				LIMIT 1
				""",
				companyId,
				operatorId);
		if (rows.isEmpty()) {
			return Map.of();
		}
		return new HashMap<>(rows.get(0));
	}
}
