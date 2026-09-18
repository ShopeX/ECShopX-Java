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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.thirdparty.service.ome.OmeDistributorShopStructPort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OmeDistributorShopStructPortImpl implements OmeDistributorShopStructPort {

	private final DistributorMapper distributorMapper;

	public OmeDistributorShopStructPortImpl(DistributorMapper distributorMapper) {
		this.distributorMapper = distributorMapper;
	}

	@Override
	public Optional<Map<String, Object>> tryBuildShopUpdate(long companyId, long distributorId) {
		return tryBuildShopAdd(companyId, distributorId);
	}

	@Override
	public Optional<Map<String, Object>> tryBuildShopAdd(long companyId, long distributorId) {
		Distributor d = distributorMapper.selectById(distributorId);
		if (d == null || d.getCompanyId() == null || d.getCompanyId() != companyId) {
			return Optional.empty();
		}
		if (!StringUtils.hasText(d.getName())) {
			return Optional.empty();
		}
		Map<String, Object> m = new LinkedHashMap<>();
		String branchBn =
				StringUtils.hasText(d.getShopCode()) ? d.getShopCode().trim() : "d_" + distributorId;
		m.put("branch_bn", branchBn);
		m.put("name", d.getName());
		m.put("mobile", d.getMobile() != null ? d.getMobile() : "");
		m.put("contact", d.getContact() != null ? d.getContact() : "");
		String addr = joinAddress(d.getAddress(), d.getHouseNumber());
		m.put("addr", addr);
		m.put("address", addr);
		m.put("province", nullToEmpty(d.getProvince()));
		m.put("city", nullToEmpty(d.getCity()));
		m.put("area", nullToEmpty(d.getArea()));
		m.put("lng", nullToEmpty(d.getLng()));
		m.put("lat", nullToEmpty(d.getLat()));
		m.put("distributor_id", distributorId);
		m.put("company_id", companyId);
		return Optional.of(m);
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String joinAddress(String address, String houseNumber) {
		String a = address == null ? "" : address.trim();
		String h = houseNumber == null ? "" : houseNumber.trim();
		if (!StringUtils.hasText(h)) {
			return a;
		}
		if (!StringUtils.hasText(a)) {
			return h;
		}
		return a + " " + h;
	}
}
