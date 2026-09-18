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

package cn.shopex.ecshopx.popularize.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MerchantScopeDistributorIdsReadService {

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public MerchantScopeDistributorIdsReadService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public List<Long> listDistributorIdsForMerchantShops(long companyId, long merchantId) {
		String sql =
				"SELECT distributor_id FROM distribution_distributor WHERE company_id = :companyId AND merchant_id = :merchantId ORDER BY distributor_id ASC LIMIT 10000";
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("companyId", companyId);
		p.addValue("merchantId", merchantId);
		List<Long> rows =
				namedParameterJdbcTemplate.query(
						sql, p, (rs, rowNum) -> rs.getLong("distributor_id"));
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		return new ArrayList<>(rows);
	}
}
