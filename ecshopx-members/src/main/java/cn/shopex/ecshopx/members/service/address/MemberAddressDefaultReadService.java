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
import cn.shopex.ecshopx.members.mapper.MembersAddressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberAddressDefaultReadService {

	private static final Logger log = LoggerFactory.getLogger(MemberAddressDefaultReadService.class);

	private final MembersAddressMapper membersAddressMapper;

	public MemberAddressDefaultReadService(MembersAddressMapper membersAddressMapper) {
		this.membersAddressMapper = membersAddressMapper;
	}

	public Map<String, Object> getDefaultAddress(long membersAddressCompanyId, long membersAddressUserIdColumnValue) {
		MembersAddress row =
				membersAddressMapper.selectOne(
						new LambdaQueryWrapper<MembersAddress>()
								.eq(MembersAddress::getCompanyId, membersAddressCompanyId)
								.eq(MembersAddress::getUserId, membersAddressUserIdColumnValue)
								.eq(MembersAddress::getIsDef, Boolean.TRUE)
								.last("LIMIT 1"));
		if (row == null) {
			return null;
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("lat", row.getLat());
		m.put("lng", row.getLng());
		m.put("city", row.getCity());
		m.put("adrdetail", row.getAdrdetail());
		m.put("address_id", row.getAddressId());
		if (!isNumericCoordinate(row.getLat()) || !isNumericCoordinate(row.getLng())) {
			log.warn(
					"member_default_address_lat_lng_non_numeric companyId={} membersAddressUserIdColumnValue={} addressId={} lat={} lng={}",
					membersAddressCompanyId,
					membersAddressUserIdColumnValue,
					row.getAddressId(),
					row.getLat(),
					row.getLng());
		}
		return m;
	}

	private static boolean isNumericCoordinate(String raw) {
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		try {
			double v = Double.parseDouble(raw.trim());
			return Double.isFinite(v);
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
