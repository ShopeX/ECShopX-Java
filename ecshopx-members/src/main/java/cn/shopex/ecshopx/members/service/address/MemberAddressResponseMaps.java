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

package cn.shopex.ecshopx.members.service.address;

import cn.shopex.ecshopx.members.domain.MembersAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MemberAddressResponseMaps {

	private MemberAddressResponseMaps() {
	}

	public static Map<String, Object> toRow(MembersAddress e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("address_id", e.getAddressId());
		m.put("company_id", e.getCompanyId());
		m.put("user_id", e.getUserId());
		m.put("username", e.getUsername());
		m.put("telephone", e.getTelephone());
		m.put("area", e.getArea());
		m.put("province", e.getProvince());
		m.put("city", e.getCity());
		m.put("county", e.getCounty());
		m.put("adrdetail", e.getAdrdetail());
		m.put("postalCode", e.getPostalCode());
		m.put("is_def", e.getIsDef());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("third_data", e.getThirdData());
		m.put("lng", e.getLng());
		m.put("lat", e.getLat());
		return m;
	}
}
