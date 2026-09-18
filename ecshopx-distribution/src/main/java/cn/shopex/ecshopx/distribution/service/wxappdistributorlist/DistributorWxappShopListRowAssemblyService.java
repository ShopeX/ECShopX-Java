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

package cn.shopex.ecshopx.distribution.service.wxappdistributorlist;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListOutsideLangReadService;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorWxappShopListRowAssemblyService {

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final ObjectMapper objectMapper;

	public DistributorWxappShopListRowAssemblyService(
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			ObjectMapper objectMapper) {
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> toResponseRows(
			long companyId,
			boolean geoEnabled,
			List<Distributor> entities,
			String requestLangTag,
			Map<Long, Long> outSortWhitelistRowIdByDistributorId) {
		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (Distributor d : entities) {
			if (d.getMobile() != null) {
				d.setMobile(sensitiveFieldEncryptor.decrypt(d.getMobile()));
			}
			if (d.getContact() != null) {
				d.setContact(sensitiveFieldEncryptor.decrypt(d.getContact()));
			}
			int distributorSelf = d.getDistributorSelf() == null ? 0 : d.getDistributorSelf().intValue();
			long rowDistributorId = d.getDistributorId() == null ? 0L : d.getDistributorId();
			Map<String, Object> setting =
					selfDeliverySettingReadService.getSetting(companyId, rowDistributorId, distributorSelf);
			Map<String, Object> row = distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			if (geoEnabled && d.getDistance() != null) {
				BigDecimal km = d.getDistance();
				row.put("distance", km.stripTrailingZeros().toPlainString());
				if (km.compareTo(BigDecimal.ONE) < 0) {
					row.put(
							"distance_show",
							km.multiply(BigDecimal.valueOf(1000))
									.setScale(0, RoundingMode.HALF_UP)
									.toPlainString());
					row.put("distance_unit", "m");
				} else {
					row.put(
							"distance_show",
							km.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
					row.put("distance_unit", "km");
				}
			}
			Long sid = outSortWhitelistRowIdByDistributorId.get(rowDistributorId);
			if (sid != null) {
				row.put("sort_id", sid);
			}
			listMaps.add(row);
		}
		distributorListOutsideLangReadService.applyLangMaps(companyId, requestLangTag, listMaps);
		return listMaps;
	}
}
