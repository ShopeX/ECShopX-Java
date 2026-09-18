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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DistributorBatchApiRowQueryService {

	private final DistributorMapper distributorMapper;
	private final ObjectMapper objectMapper;

	public DistributorBatchApiRowQueryService(DistributorMapper distributorMapper, ObjectMapper objectMapper) {
		this.distributorMapper = distributorMapper;
		this.objectMapper = objectMapper;
	}

	public Map<Long, Map<String, Object>> loadByCompanyAndDistributorIds(long companyId, Collection<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<Long> ids = normalizePositiveDistributorIds(distributorIds);
		if (ids.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Distributor> rows = distributorMapper.selectList(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.in(Distributor::getDistributorId, ids));
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Distributor d : rows) {
			out.put(d.getDistributorId(), DistributorRowMaps.toApiRow(d, objectMapper));
		}
		return out;
	}

	/**
	 * Loads a narrow projection of distributor rows for theme decoration (contentpart / shop widgets).
	 */
	public Map<Long, Map<String, Object>> loadForPagesTemplateDecoration(long companyId, Collection<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<Long> ids = normalizePositiveDistributorIds(distributorIds);
		if (ids.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Distributor> rows =
				distributorMapper.selectList(
						new LambdaQueryWrapper<Distributor>()
								.select(
										Distributor::getDistributorId,
										Distributor::getName,
										Distributor::getLogo,
										Distributor::getFirstLetter,
										Distributor::getIsSelfDelivery)
								.eq(Distributor::getCompanyId, companyId)
								.in(Distributor::getDistributorId, ids));
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Distributor d : rows) {
			if (d.getDistributorId() == null) {
				continue;
			}
			out.put(d.getDistributorId(), toDecorationDistributorRow(d));
		}
		return out;
	}

	private static Set<Long> normalizePositiveDistributorIds(Collection<Long> distributorIds) {
		Set<Long> ids = new LinkedHashSet<>();
		for (Long id : distributorIds) {
			if (id != null && id > 0L) {
				ids.add(id);
			}
		}
		return ids;
	}

	private static Map<String, Object> toDecorationDistributorRow(Distributor d) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("distributor_id", d.getDistributorId());
		m.put("name", d.getName());
		m.put("logo", d.getLogo());
		m.put("first_letter", d.getFirstLetter());
		m.put("is_self_delivery", d.getIsSelfDelivery());
		return m;
	}
}
