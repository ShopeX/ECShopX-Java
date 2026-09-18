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

import cn.shopex.ecshopx.distribution.integration.MapGeocodeClient;
import cn.shopex.ecshopx.distribution.service.distributorvalid.dto.DistributorIsValidQuery;
import cn.shopex.ecshopx.orders.service.nostores.DistributorNostoresCartDistributorIdsReadService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorIsValidNostoresBranchService {

	private final DistributorNostoresCartDistributorIdsReadService distributorNostoresCartDistributorIdsReadService;
	private final DistributorIsValidNearShopService distributorIsValidNearShopService;
	private final MapGeocodeClient mapGeocodeClient;

	public DistributorIsValidNostoresBranchService(
			DistributorNostoresCartDistributorIdsReadService distributorNostoresCartDistributorIdsReadService,
			DistributorIsValidNearShopService distributorIsValidNearShopService,
			MapGeocodeClient mapGeocodeClient) {
		this.distributorNostoresCartDistributorIdsReadService = distributorNostoresCartDistributorIdsReadService;
		this.distributorIsValidNearShopService = distributorIsValidNearShopService;
		this.mapGeocodeClient = mapGeocodeClient;
	}

	public Map<String, Object> runNostoresBranch(
			long companyId,
			long userId,
			Map<String, Object> filter,
			String lngWork,
			String latWork,
			DistributorIsValidQuery query) {
		Map<String, Object> result = new LinkedHashMap<>();
		String lng = lngWork;
		String lat = latWork;
		if (!coordinatesLookPresent(lng, lat)) {
			MapGeocodeClient.LngLat fixed =
					mapGeocodeClient.getLatAndLng(companyId, "", "北京市东城区东长安街");
			lng = fixed.lng();
			lat = fixed.lat();
		}
		if (!coordinatesLookPresent(lng, lat)) {
			return result;
		}
		filter.put("is_ziti", Boolean.TRUE);
		double latNum = Double.parseDouble(lat.trim());
		double lngNum = Double.parseDouble(lng.trim());
		List<Long> ids =
				distributorNostoresCartDistributorIdsReadService.listDistributorIdsByNostoresCart(
						companyId,
						userId,
						query.cartType(),
						query.orderType(),
						query.seckillId(),
						query.seckillTicket(),
						query.iscrossborder(),
						query.bargainId());
		if (ids.isEmpty()) {
			return result;
		}
		filter.put("distributor_id", ids);
		Map<String, Object> near =
				distributorIsValidNearShopService.getNearShopData(filter, latNum, lngNum, 0);
		Object isDada = near.get("is_dada");
		if (isDada != null && "1".equals(String.valueOf(isDada).trim())) {
			near.put("is_dada", Boolean.TRUE);
		} else if (isDada != null) {
			near.put("is_dada", Boolean.FALSE);
		}
		return near;
	}

	private static boolean coordinatesLookPresent(String lng, String lat) {
		if (lng == null || lat == null) {
			return false;
		}
		String lt = lng.trim();
		String la = lat.trim();
		if (lt.isEmpty() || la.isEmpty()) {
			return false;
		}
		double ln;
		double laNum;
		try {
			ln = Double.parseDouble(lt);
			laNum = Double.parseDouble(la);
		} catch (NumberFormatException e) {
			return false;
		}
		if (!Double.isFinite(ln) || !Double.isFinite(laNum)) {
			return false;
		}
		return ln != 0d && laNum != 0d;
	}
}
