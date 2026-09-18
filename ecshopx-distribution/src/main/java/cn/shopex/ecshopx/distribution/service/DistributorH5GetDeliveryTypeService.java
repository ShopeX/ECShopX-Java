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
import cn.shopex.ecshopx.distribution.domain.Distributor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DistributorH5GetDeliveryTypeService {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public DistributorH5GetDeliveryTypeService(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> getDeliveryType(long companyId, long distributorIdForResolve) {
		Optional<DistributorRepositoryGetInfoSimpleService.ResolvedDistributorForShop> resolvedOpt =
				distributorRepositoryGetInfoSimpleService.resolveForCompanyShop(companyId, distributorIdForResolve);
		if (resolvedOpt.isEmpty()) {
			return new ArrayList<>();
		}
		DistributorRepositoryGetInfoSimpleService.ResolvedDistributorForShop resolved = resolvedOpt.get();
		Distributor d = resolved.distributor();
		if (d.getMobile() != null) {
			d.setMobile(sensitiveFieldEncryptor.decrypt(d.getMobile()));
		}
		if (d.getContact() != null) {
			d.setContact(sensitiveFieldEncryptor.decrypt(d.getContact()));
		}
		int distributorSelf = d.getDistributorSelf() == null ? 0 : d.getDistributorSelf();
		long rowDistributorId = d.getDistributorId() == null ? 0L : d.getDistributorId();
		Map<String, Object> selfRule =
				selfDeliverySettingReadService.getSetting(companyId, rowDistributorId, distributorSelf);
		Map<String, Object> storedata = distributorListRowFormatService.formatStoreRow(d, selfRule, objectMapper);
		if (resolved.useVirtualMainDistributorIdInClientMap()) {
			storedata.put("distributor_id", 0L);
		}
		List<Map<String, Object>> out = new ArrayList<>();
		if (Boolean.TRUE.equals(storedata.get("is_ziti"))) {
			Map<String, Object> ziti = new LinkedHashMap<>();
			ziti.put("delivery_name", "自提");
			ziti.put("delivery_type", "ziti");
			ziti.put(
					"address",
					storedata.get("address") == null ? "" : String.valueOf(storedata.get("address")));
			out.add(ziti);
		}
		if (Boolean.TRUE.equals(storedata.get("is_delivery"))) {
			Map<String, Object> del = new LinkedHashMap<>();
			del.put("delivery_name", "快递配送");
			del.put("delivery_type", "delivery");
			out.add(del);
		}
		return out;
	}
}
