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
public class DigiOsPosAccountLoadService {

	private final JdbcTemplate jdbcTemplate;

	public DigiOsPosAccountLoadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Map<String, Object> loadPosAccount(long companyId, long posStoreAccountId) {
		List<Map<String, Object>> rows =
				jdbcTemplate.queryForList(
						"""
						SELECT id, company_id, employee_no, name, mobile, email
						FROM pos_store_account
						WHERE company_id = ? AND id = ?
						LIMIT 1
						""",
						companyId,
						posStoreAccountId);
		if (rows.isEmpty()) {
			return Map.of();
		}
		return new HashMap<>(rows.get(0));
	}
}