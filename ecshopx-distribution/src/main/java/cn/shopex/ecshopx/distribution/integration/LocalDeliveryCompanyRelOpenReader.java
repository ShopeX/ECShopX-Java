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

package cn.shopex.ecshopx.distribution.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LocalDeliveryCompanyRelOpenReader {

	private final JdbcTemplate jdbcTemplate;

	@Value("${common.local-delivery-dirver:dada}")
	private String localDeliveryDriver;

	public LocalDeliveryCompanyRelOpenReader(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean readIsOpen(long companyId) {
		String driver = localDeliveryDriver != null ? localDeliveryDriver.trim() : "dada";
		if (!"dada".equals(driver) && !"shansong".equals(driver)) {
			throw new ResourceException("同城配仅支持达达和闪送");
		}
		try {
			if ("shansong".equals(driver)) {
				return Boolean.TRUE.equals(
						jdbcTemplate.query(
								"SELECT is_open FROM company_rel_shansong WHERE company_id = ? LIMIT 1",
								rs -> rs.next() ? rs.getBoolean("is_open") : Boolean.FALSE,
								companyId));
			}
			return Boolean.TRUE.equals(
					jdbcTemplate.query(
							"SELECT is_open FROM company_rel_dada WHERE company_id = ? LIMIT 1",
							rs -> rs.next() ? rs.getBoolean("is_open") : Boolean.FALSE,
							companyId));
		} catch (Exception e) {
			return false;
		}
	}
}
