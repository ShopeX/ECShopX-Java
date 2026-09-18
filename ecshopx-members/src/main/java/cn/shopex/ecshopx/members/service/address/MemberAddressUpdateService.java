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

import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MembersAddress;
import cn.shopex.ecshopx.members.mapper.MembersAddressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class MemberAddressUpdateService {

	private final MembersAddressMapper membersAddressMapper;
	private final CompanyMapGeocodePort companyMapGeocodePort;
	private final TransactionTemplate transactionTemplate;

	public MemberAddressUpdateService(
			MembersAddressMapper membersAddressMapper,
			CompanyMapGeocodePort companyMapGeocodePort,
			TransactionTemplate transactionTemplate) {
		this.membersAddressMapper = membersAddressMapper;
		this.companyMapGeocodePort = companyMapGeocodePort;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> updateAddress(
			long companyId,
			long userId,
			long addressId,
			String username,
			String telephone,
			String province,
			String city,
			String county,
			String adrdetail,
			boolean postalCodeKeyPresent,
			String postalCodeOrNull,
			boolean isDefKeyPresent,
			boolean isDefNormalizedBoolean) {
		String lat = "";
		String lng = "";
		if (StringUtils.hasText(province) && StringUtils.hasText(city) && StringUtils.hasText(adrdetail)) {
			CompanyMapGeocodePort.GeocodeLatLng g =
					companyMapGeocodePort.geocodeAllowEmpty(companyId, city, adrdetail);
			if (g != null) {
				lat = nullToEmpty(g.lat());
				lng = nullToEmpty(g.lng());
			}
		}

		final String latFinal = lat;
		final String lngFinal = lng;

		return transactionTemplate.execute(
				status -> {
					if (isDefKeyPresent && isDefNormalizedBoolean) {
						long cnt =
								membersAddressMapper.selectCount(
										new LambdaQueryWrapper<MembersAddress>()
												.eq(MembersAddress::getCompanyId, companyId)
												.eq(MembersAddress::getUserId, userId));
						if (cnt == 0L) {
							throw new ResourceException("未查询到更新数据");
						}
						MembersAddress patchClear = new MembersAddress();
						patchClear.setIsDef(false);
						int cleared =
								membersAddressMapper.update(
										patchClear,
										new LambdaUpdateWrapper<MembersAddress>()
												.eq(MembersAddress::getCompanyId, companyId)
												.eq(MembersAddress::getUserId, userId));
						if (cleared == 0) {
							throw new ResourceException("未查询到更新数据");
						}
					}

					MembersAddress existing =
							membersAddressMapper.selectOne(
									new LambdaQueryWrapper<MembersAddress>()
											.eq(MembersAddress::getAddressId, addressId)
											.eq(MembersAddress::getCompanyId, companyId)
											.eq(MembersAddress::getUserId, userId)
											.last("LIMIT 1"));
					if (existing == null) {
						throw new ResourceException("未查询到更新数据");
					}

					long nowSec = java.time.Instant.now().getEpochSecond();
					LambdaUpdateWrapper<MembersAddress> uw =
							new LambdaUpdateWrapper<MembersAddress>()
									.eq(MembersAddress::getAddressId, addressId)
									.eq(MembersAddress::getCompanyId, companyId)
									.eq(MembersAddress::getUserId, userId)
									.set(MembersAddress::getUsername, username)
									.set(MembersAddress::getTelephone, telephone)
									.set(MembersAddress::getProvince, province)
									.set(MembersAddress::getCity, city)
									.set(MembersAddress::getCounty, county)
									.set(MembersAddress::getAdrdetail, adrdetail)
									.set(MembersAddress::getLat, latFinal)
									.set(MembersAddress::getLng, lngFinal)
									.set(MembersAddress::getUpdated, nowSec);
					if (postalCodeKeyPresent && StringUtils.hasText(postalCodeOrNull)) {
						uw.set(MembersAddress::getPostalCode, postalCodeOrNull);
					}
					if (isDefKeyPresent) {
						uw.set(MembersAddress::getIsDef, isDefNormalizedBoolean);
					}

					int n = membersAddressMapper.update(null, uw);
					if (n == 0) {
						throw new ResourceException("未查询到更新数据");
					}

					MembersAddress refreshed =
							membersAddressMapper.selectOne(
									new LambdaQueryWrapper<MembersAddress>()
											.eq(MembersAddress::getAddressId, addressId)
											.eq(MembersAddress::getCompanyId, companyId)
											.eq(MembersAddress::getUserId, userId)
											.last("LIMIT 1"));
					if (refreshed == null) {
						throw new ResourceException("未查询到更新数据");
					}
					return toAddressRowMap(refreshed);
				});
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static Map<String, Object> toAddressRowMap(MembersAddress e) {
		return MemberAddressResponseMaps.toRow(e);
	}
}
