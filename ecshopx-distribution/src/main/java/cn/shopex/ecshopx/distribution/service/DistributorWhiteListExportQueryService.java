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
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListExportMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMobileAggRow;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListExportFilter;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListExportRow;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListListRow;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.springframework.stereotype.Service;

@Service
public class DistributorWhiteListExportQueryService {

	private final DistributorWhiteListExportMapper distributorWhiteListExportMapper;
	private final DistributorMapper distributorMapper;

	public DistributorWhiteListExportQueryService(
			DistributorWhiteListExportMapper distributorWhiteListExportMapper,
			DistributorMapper distributorMapper) {
		this.distributorWhiteListExportMapper = distributorWhiteListExportMapper;
		this.distributorMapper = distributorMapper;
	}

	public long countGroupedByMobile(DistributorWhiteListExportFilter filter) {
		if (filter.shopNotFound()) {
			return 0L;
		}
		return distributorWhiteListExportMapper.countGroupedByMobile(filter);
	}

	public List<DistributorWhiteListListRow> listGroupedByMobileWithoutPageSizeLimit(
			DistributorWhiteListExportFilter filter) {
		if (filter.shopNotFound()) {
			return List.of();
		}
		List<DistributorWhiteListMobileAggRow> raw =
				distributorWhiteListExportMapper.selectGroupedByMobileAll(filter);
		if (raw.isEmpty()) {
			return List.of();
		}
		Set<Long> allIds = new LinkedHashSet<>();
		for (DistributorWhiteListMobileAggRow row : raw) {
			for (Long id : parseIdCsv(row.getDistributorIdCsv())) {
				allIds.add(id);
			}
		}
		Map<Long, Distributor> distById = loadDistributorShopFields(filter.companyId(), allIds);
		List<DistributorWhiteListListRow> out = new ArrayList<>(raw.size());
		for (DistributorWhiteListMobileAggRow row : raw) {
			List<Map<String, Object>> distributorInfo = new ArrayList<>();
			for (Long distId : parseIdCsv(row.getDistributorIdCsv())) {
				Distributor d = distById.get(distId);
				if (d == null) {
					continue;
				}
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("name", d.getName() == null ? "" : d.getName());
				m.put("shop_code", d.getShopCode() == null ? "" : d.getShopCode());
				m.put("distributor_id", d.getDistributorId());
				distributorInfo.add(m);
			}
			out.add(new DistributorWhiteListListRow(
					row.getMobile() == null ? "" : row.getMobile(),
					row.getUsername() == null ? "" : row.getUsername(),
					row.getId(),
					row.getCompanyId(),
					row.getCreated(),
					row.getUpdated(),
					distributorInfo));
		}
		return out;
	}

	public List<DistributorWhiteListExportRow> pageGroupedByMobile(
			DistributorWhiteListExportFilter filter, int page, int pageSize) {
		if (filter.shopNotFound()) {
			return List.of();
		}
		int offset = (page - 1) * pageSize;
		List<DistributorWhiteListMobileAggRow> raw =
				distributorWhiteListExportMapper.selectGroupedByMobilePage(filter, offset, pageSize);
		if (raw.isEmpty()) {
			return List.of();
		}
		Set<Long> allIds = new LinkedHashSet<>();
		for (DistributorWhiteListMobileAggRow row : raw) {
			for (Long id : parseIdCsv(row.getDistributorIdCsv())) {
				allIds.add(id);
			}
		}
		Map<Long, String> nameById = loadDistributorNames(filter.companyId(), allIds);
		List<DistributorWhiteListExportRow> out = new ArrayList<>(raw.size());
		for (DistributorWhiteListMobileAggRow row : raw) {
			String shop = buildShopLabel(row.getDistributorIdCsv(), nameById);
			out.add(new DistributorWhiteListExportRow(
					row.getMobile() == null ? "" : row.getMobile(),
					row.getUsername() == null ? "" : row.getUsername(),
					shop));
		}
		return out;
	}

	private static List<Long> parseIdCsv(String csv) {
		if (csv == null || csv.isBlank()) {
			return List.of();
		}
		List<Long> ids = new ArrayList<>();
		for (String p : csv.split(",")) {
			String t = p.trim();
			if (t.isEmpty()) {
				continue;
			}
			ids.add(Long.parseLong(t));
		}
		return ids;
	}

	private Map<Long, Distributor> loadDistributorShopFields(long companyId, Set<Long> distributorIds) {
		if (distributorIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.in(Distributor::getDistributorId, distributorIds)
				.select(Distributor::getDistributorId, Distributor::getName, Distributor::getShopCode);
		List<Distributor> rows = distributorMapper.selectList(w);
		Map<Long, Distributor> m = new LinkedHashMap<>();
		for (Distributor d : rows) {
			if (d.getDistributorId() != null) {
				m.put(d.getDistributorId(), d);
			}
		}
		return m;
	}

	private Map<Long, String> loadDistributorNames(long companyId, Set<Long> distributorIds) {
		if (distributorIds.isEmpty()) {
			return Map.of();
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.in(Distributor::getDistributorId, distributorIds)
				.select(Distributor::getDistributorId, Distributor::getName);
		List<Distributor> rows = distributorMapper.selectList(w);
		Map<Long, String> m = new LinkedHashMap<>();
		for (Distributor d : rows) {
			if (d.getDistributorId() != null) {
				m.put(d.getDistributorId(), d.getName() == null ? "" : d.getName());
			}
		}
		return m;
	}

	private static String buildShopLabel(String distributorIdCsv, Map<Long, String> nameById) {
		if (distributorIdCsv == null || distributorIdCsv.isBlank()) {
			return "";
		}
		StringJoiner joiner = new StringJoiner(",");
		for (String p : distributorIdCsv.split(",")) {
			String t = p.trim();
			if (t.isEmpty()) {
				continue;
			}
			long id = Long.parseLong(t);
			joiner.add(nameById.getOrDefault(id, ""));
		}
		return joiner.toString();
	}
}
