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

package cn.shopex.ecshopx.distribution.service.distributorvalid;

import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorIsValidNearShopService {

	private final DistributorIsValidCoreQueryService distributorIsValidCoreQueryService;
	private final DistributorMapper distributorMapper;

	public DistributorIsValidNearShopService(
			DistributorIsValidCoreQueryService distributorIsValidCoreQueryService,
			DistributorMapper distributorMapper) {
		this.distributorIsValidCoreQueryService = distributorIsValidCoreQueryService;
		this.distributorMapper = distributorMapper;
	}

	public Map<String, Object> getNearShopData(
			Map<String, Object> filter, double lat, double lng, int isShopDivided) {
		Map<String, Object> q = new LinkedHashMap<>(filter);
		Map<String, Object> nearRow = distributorMapper.selectNearestDistributorHaversineFirst(q, lat, lng);
		if (nearRow != null && !nearRow.isEmpty()) {
			DistributorIsValidRowKeyBridge.ensureSnakeCaseCoreKeys(nearRow);
			distributorIsValidCoreQueryService.applyFormatStoreInfo(nearRow);
			return nearRow;
		}
		if (isShopDivided != 0) {
			Map<String, Object> q2 = new LinkedHashMap<>(filter);
			q2.remove("open_divided");
			Map<String, Object> second = distributorMapper.selectNearestDistributorHaversineFirst(q2, lat, lng);
			if (second != null && !second.isEmpty()) {
				DistributorIsValidRowKeyBridge.ensureSnakeCaseCoreKeys(second);
				distributorIsValidCoreQueryService.applyFormatStoreInfo(second);
				second.put("white_hidden", 1);
				return second;
			}
		}
		Map<String, Object> def = loadDefaultDistributorRowForNearFallback(
				((Number) filter.get("company_id")).longValue());
		if (def == null) {
			def = new LinkedHashMap<>();
		}
		def.put("real_default", 1);
		return def;
	}

	private Map<String, Object> loadDefaultDistributorRowForNearFallback(long companyId) {
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("company_id", companyId);
		q.put("is_default", 1);
		Map<String, Object> row = distributorMapper.selectDistributorRowDynamic(q);
		if (row == null || row.isEmpty()) {
			return null;
		}
		DistributorIsValidRowKeyBridge.ensureSnakeCaseCoreKeys(row);
		distributorIsValidCoreQueryService.applyFormatStoreInfo(row);
		return row;
	}
}
