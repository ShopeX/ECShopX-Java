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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

	/**
	 * Finds the nearest shop. When {@code whitelistDistributorIds} is non-null, applies public-or-whitelist
	 * mix filtering; when null, keeps the caller's filter (e.g. show_type=self / nostores).
	 */
	public Map<String, Object> getNearShopData(
			Map<String, Object> filter, double lat, double lng, List<Long> whitelistDistributorIds) {
		Map<String, Object> q = new LinkedHashMap<>(filter);
		if (whitelistDistributorIds != null) {
			q.remove("open_divided");
			q.put("open_divided_public_or_whitelist", Boolean.TRUE);
			q.put("open_divided_whitelist_ids", new ArrayList<>(whitelistDistributorIds));
		}
		Map<String, Object> nearRow = distributorMapper.selectNearestDistributorHaversineFirst(q, lat, lng);
		if (nearRow != null && !nearRow.isEmpty()) {
			DistributorIsValidRowKeyBridge.ensureSnakeCaseCoreKeys(nearRow);
			distributorIsValidCoreQueryService.applyFormatStoreInfo(nearRow);
			return nearRow;
		}
		Map<String, Object> def =
				loadDefaultDistributorRowForNearFallback(((Number) filter.get("company_id")).longValue());
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