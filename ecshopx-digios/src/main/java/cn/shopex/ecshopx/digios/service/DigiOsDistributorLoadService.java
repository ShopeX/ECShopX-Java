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
public class DigiOsDistributorLoadService {

	private final JdbcTemplate jdbcTemplate;

	public DigiOsDistributorLoadService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Map<String, Object> loadDistributor(long companyId, long distributorId) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"""
				SELECT distributor_id, company_id, shop_code, name, address, house_number,
				       contact, mobile, hour, is_valid, regions_id, province, city, area
				FROM distribution_distributor
				WHERE company_id = ? AND distributor_id = ?
				LIMIT 1
				""",
				companyId,
				distributorId);
		if (rows.isEmpty()) {
			return Map.of();
		}
		return new HashMap<>(rows.get(0));
	}
}
