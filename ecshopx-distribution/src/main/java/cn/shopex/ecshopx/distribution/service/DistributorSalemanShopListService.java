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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorSalemanShopListMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorSalemanShopListService {

	private final DistributorSalemanShopListMapper distributorSalemanShopListMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final ObjectMapper objectMapper;

	public DistributorSalemanShopListService(
			DistributorSalemanShopListMapper distributorSalemanShopListMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorListRowFormatService distributorListRowFormatService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			ObjectMapper objectMapper) {
		this.distributorSalemanShopListMapper = distributorSalemanShopListMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> lists(long companyId, List<Long> distributorIds, String nameOpt, String mobileOpt) {
		String nameLike = null;
		if (ValuePresence.hasEffectiveValue(nameOpt)) {
			nameLike = escapeLikeContains(nameOpt.trim());
		}
		String mobileLikeEnc = null;
		if (ValuePresence.hasEffectiveValue(mobileOpt)) {
			mobileLikeEnc = sensitiveFieldEncryptor.encrypt(mobileOpt.trim());
		}
		long totalCount = distributorSalemanShopListMapper.countForSalemanShopList(distributorIds, nameLike,
				mobileLikeEnc);
		List<Distributor> rows = distributorSalemanShopListMapper.selectListForSalemanShopList(distributorIds,
				nameLike, mobileLikeEnc);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Distributor d : rows) {
			if (d.getMobile() != null) {
				d.setMobile(sensitiveFieldEncryptor.decrypt(d.getMobile()));
			}
			if (d.getContact() != null) {
				d.setContact(sensitiveFieldEncryptor.decrypt(d.getContact()));
			}
			long distributorId = d.getDistributorId() != null ? d.getDistributorId() : 0L;
			int distributorSelf = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
			Map<String, Object> selfDelivery =
					selfDeliverySettingReadService.getSetting(companyId, distributorId, distributorSelf);
			Map<String, Object> row =
					distributorListRowFormatService.formatStoreRow(d, selfDelivery, objectMapper);
			list.add(row);
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", totalCount);
		body.put("list", list);
		return body;
	}

	private static String escapeLikeContains(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
