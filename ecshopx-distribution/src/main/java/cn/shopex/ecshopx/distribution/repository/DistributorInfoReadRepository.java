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

package cn.shopex.ecshopx.distribution.repository;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorInfoReadMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class DistributorInfoReadRepository {

	private final DistributorInfoReadMapper distributorInfoReadMapper;
	private final ObjectMapper objectMapper;

	public DistributorInfoReadRepository(
			DistributorInfoReadMapper distributorInfoReadMapper, ObjectMapper objectMapper) {
		this.distributorInfoReadMapper = distributorInfoReadMapper;
		this.objectMapper = objectMapper;
	}

	public Optional<Map<String, Object>> selectByCompanyAndDistributorId(
			long companyId, long distributorId, String requestLang) {
		var row =
				distributorInfoReadMapper.selectByCompanyAndDistributorId(
						companyId, distributorId, requestLang);
		return Optional.ofNullable(toRowMap(row));
	}

	/** Same lookup as {@link #selectByCompanyAndDistributorId} but returns the mapped entity (avoids Map round-trip). */
	public Optional<Distributor> findByCompanyAndDistributorId(
			long companyId, long distributorId, String requestLang) {
		return Optional.ofNullable(
				distributorInfoReadMapper.selectByCompanyAndDistributorId(
						companyId, distributorId, requestLang));
	}

	public Optional<Map<String, Object>> selectDefaultByCompany(long companyId, String requestLang) {
		var row = distributorInfoReadMapper.selectDefaultByCompany(companyId, requestLang);
		return Optional.ofNullable(toRowMap(row));
	}

	public Optional<Distributor> loadDefaultDistributorForCompany(long companyId, String requestLang) {
		Distributor row = distributorInfoReadMapper.selectDefaultByCompany(companyId, requestLang);
		return Optional.ofNullable(row);
	}

	public Optional<Distributor> loadSelfDistributorForCompany(long companyId, String requestLang) {
		Distributor row = distributorInfoReadMapper.selectSelfByCompany(companyId, requestLang);
		return Optional.ofNullable(row);
	}

	public Optional<Map<String, Object>> selectSelfByCompany(long companyId, String requestLang) {
		var row = distributorInfoReadMapper.selectSelfByCompany(companyId, requestLang);
		return Optional.ofNullable(toRowMap(row));
	}

	private Map<String, Object> toRowMap(cn.shopex.ecshopx.distribution.domain.Distributor row) {
		if (row == null) {
			return null;
		}
		Map<String, Object> m =
				objectMapper.convertValue(row, new TypeReference<LinkedHashMap<String, Object>>() {});
		return m;
	}
}
