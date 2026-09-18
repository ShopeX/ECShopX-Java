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
public class MemberAddressCreateService {

	private final MembersAddressMapper membersAddressMapper;
	private final CompanyMapGeocodePort companyMapGeocodePort;
	private final TransactionTemplate transactionTemplate;

	public MemberAddressCreateService(
			MembersAddressMapper membersAddressMapper,
			CompanyMapGeocodePort companyMapGeocodePort,
			TransactionTemplate transactionTemplate) {
		this.membersAddressMapper = membersAddressMapper;
		this.companyMapGeocodePort = companyMapGeocodePort;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> createAddress(
			long companyId,
			long userId,
			String username,
			String telephone,
			String province,
			String city,
			String county,
			String adrdetail,
			String postalCodeOrNull,
			int isDefNormalized01,
			String thirdDataOrNull) {
		long addrCount =
				membersAddressMapper.selectCount(
						new LambdaQueryWrapper<MembersAddress>()
								.eq(MembersAddress::getCompanyId, companyId)
								.eq(MembersAddress::getUserId, userId));
		if (addrCount >= 20L) {
			throw new ResourceException("最多添加20个地址");
		}

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

		final long addrCountFinal = addrCount;
		final String latFinal = lat;
		final String lngFinal = lng;

		return transactionTemplate.execute(
				status -> {
					int effectiveIsDef = isDefNormalized01;
					if (addrCountFinal == 0L) {
						effectiveIsDef = 1;
					}
					if (addrCountFinal > 0L && effectiveIsDef == 1) {
						long existingRowCount =
								membersAddressMapper.selectCount(
										new LambdaQueryWrapper<MembersAddress>()
												.eq(MembersAddress::getCompanyId, companyId)
												.eq(MembersAddress::getUserId, userId));
						if (existingRowCount == 0L) {
							throw new ResourceException("未查询到更新数据");
						}
						MembersAddress patch = new MembersAddress();
						patch.setIsDef(false);
						int updated =
								membersAddressMapper.update(
										patch,
										new LambdaUpdateWrapper<MembersAddress>()
												.eq(MembersAddress::getCompanyId, companyId)
												.eq(MembersAddress::getUserId, userId));
						if (updated == 0) {
							throw new ResourceException("未查询到更新数据");
						}
					}

					long nowSec = java.time.Instant.now().getEpochSecond();
					MembersAddress row = new MembersAddress();
					row.setCompanyId(companyId);
					row.setUserId(userId);
					row.setUsername(username);
					row.setTelephone(telephone);
					row.setProvince(province);
					row.setCity(city);
					row.setCounty(county);
					row.setAdrdetail(adrdetail);
					if (StringUtils.hasText(postalCodeOrNull)) {
						row.setPostalCode(postalCodeOrNull);
					}
					row.setIsDef(effectiveIsDef == 1);
					row.setThirdData(thirdDataOrNull);
					row.setLat(latFinal);
					row.setLng(lngFinal);
					row.setCreated(nowSec);
					row.setUpdated(nowSec);

					membersAddressMapper.insert(row);
					return toAddressRowMap(row);
				});
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static Map<String, Object> toAddressRowMap(MembersAddress e) {
		return MemberAddressResponseMaps.toRow(e);
	}
}
