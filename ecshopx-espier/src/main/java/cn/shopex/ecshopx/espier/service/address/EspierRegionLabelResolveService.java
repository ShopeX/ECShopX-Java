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

package cn.shopex.ecshopx.espier.service.address;

import cn.shopex.ecshopx.espier.domain.Address;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Resolves province / city / district labels to {@code espier_address} ids (aligned with PHP getLocalRegionV2). */
@Service
public class EspierRegionLabelResolveService {

	private static final String DIRECT_COUNTY_LABEL = "省直辖县级行政区划";
	private static final String REGION_NOT_FOUND = "地区不存在";

	private final AddressMapper addressMapper;

	public EspierRegionLabelResolveService(AddressMapper addressMapper) {
		this.addressMapper = addressMapper;
	}

	/** @return length-3 array: province id, city id, district id */
	public int[] resolveIds(String provinceLabel, String cityLabel, String districtLabel) {
		if (!StringUtils.hasText(provinceLabel)
				|| !StringUtils.hasText(cityLabel)
				|| !StringUtils.hasText(districtLabel)) {
			throw new IllegalArgumentException(REGION_NOT_FOUND);
		}

		Address provinceRow = selectByParentAndLabel(0L, provinceLabel);
		if (provinceRow == null || provinceRow.getId() == null) {
			throw new IllegalArgumentException(REGION_NOT_FOUND);
		}
		long provinceId = provinceRow.getId();

		Address cityRow = selectByParentAndLabel(provinceId, cityLabel);
		if (cityRow == null) {
			cityRow = selectByParentAndLabel(provinceId, cityLabel.replace("市", ""));
		}
		if (cityRow == null && cityLabel.equals(districtLabel)) {
			cityRow = selectByParentAndLabel(provinceId, DIRECT_COUNTY_LABEL);
		}
		if (cityRow == null || cityRow.getId() == null) {
			throw new IllegalArgumentException(REGION_NOT_FOUND);
		}
		long cityId = cityRow.getId();

		Address distRow = selectByParentAndLabel(cityId, districtLabel);
		if (distRow == null) {
			distRow = selectByParentAndLabel(cityId, stripDistrictSuffix(districtLabel));
		}
		if (distRow == null || distRow.getId() == null) {
			throw new IllegalArgumentException(REGION_NOT_FOUND);
		}

		return new int[] {
			provinceRow.getId().intValue(), cityRow.getId().intValue(), distRow.getId().intValue()
		};
	}

	private static String stripDistrictSuffix(String label) {
		return label.replace("区", "").replace("市", "").replace("县", "");
	}

	private Address selectByParentAndLabel(Long parentId, String label) {
		if (!StringUtils.hasText(label)) {
			return null;
		}
		LambdaQueryWrapper<Address> w = new LambdaQueryWrapper<>();
		w.eq(Address::getParentId, parentId).eq(Address::getLabel, label).last("LIMIT 1");
		w.getSqlSegment();
		return addressMapper.selectOne(w);
	}
}
