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

package cn.shopex.ecshopx.orders.service.normal.create;

import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.distribution.PickupLocationGetInfoPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelZiti;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelZitiMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NormalOrderSaveOrderRelZitiService {

	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;
	private final PickupLocationGetInfoPort pickupLocationGetInfoPort;
	private final NormalOrdersRelZitiMapper normalOrdersRelZitiMapper;
	private final ObjectMapper objectMapper;

	public NormalOrderSaveOrderRelZitiService(
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort,
			PickupLocationGetInfoPort pickupLocationGetInfoPort,
			NormalOrdersRelZitiMapper normalOrdersRelZitiMapper,
			ObjectMapper objectMapper) {
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
		this.pickupLocationGetInfoPort = pickupLocationGetInfoPort;
		this.normalOrdersRelZitiMapper = normalOrdersRelZitiMapper;
		this.objectMapper = objectMapper;
	}

	public void saveOrderRelZiti(Map<String, Object> params) {
		long companyId = longVal(params.get("company_id"), 0L);
		long orderId = longVal(params.get("order_id"), 0L);
		long pickupLocationId = longVal(params.get("pickup_location"), 0L);

		Map<String, Object> pickupLocation;
		if (pickupLocationId < 0L) {
			long distributorId = Math.abs(pickupLocationId);
			Map<String, Object> distributor =
					distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(
							companyId, distributorId);
			if (distributor == null || distributor.isEmpty()) {
				throw new ResourceException("店铺不存在");
			}
			pickupLocation =
					Map.of(
							"name",
							str(distributor.get("name")),
							"lng",
							str(distributor.get("lng")),
							"lat",
							str(distributor.get("lat")),
							"province",
							str(distributor.get("province")),
							"city",
							str(distributor.get("city")),
							"area",
							str(distributor.get("area")),
							"address",
							str(distributor.get("address")),
							"contract_phone",
							str(distributor.get("mobile")));
		} else {
			pickupLocation = pickupLocationGetInfoPort.getInfo(companyId, pickupLocationId);
			if (pickupLocation == null) {
				pickupLocation = Map.of();
			}
		}

		if (pickupLocation == null || pickupLocation.isEmpty()) {
			throw new ResourceException("自提点不存在");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		NormalOrdersRelZiti row = new NormalOrdersRelZiti();
		row.setCompanyId(companyId);
		row.setOrderId(orderId);
		row.setName(str(pickupLocation.get("name")));
		row.setLng(str(pickupLocation.get("lng")));
		row.setLat(str(pickupLocation.get("lat")));
		row.setProvince(str(pickupLocation.get("province")));
		row.setCity(str(pickupLocation.get("city")));
		row.setArea(str(pickupLocation.get("area")));
		row.setAddress(str(pickupLocation.get("address")));
		row.setContractPhone(str(pickupLocation.get("contract_phone")));
		row.setPickupDate(str(params.get("pickup_date")));
		row.setPickupTime(encodePickupTime(params.get("pickup_time")));
		row.setCreateTime(now);
		row.setUpdateTime(now);
		normalOrdersRelZitiMapper.insert(row);
	}

	private String encodePickupTime(Object pickupTime) {
		try {
			return objectMapper.writeValueAsString(pickupTime);
		} catch (JsonProcessingException e) {
			return pickupTime == null ? "null" : String.valueOf(pickupTime);
		}
	}

	private static String str(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
