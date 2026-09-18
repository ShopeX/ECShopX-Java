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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DistributorH5GetDistributorInfoService {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final ObjectMapper objectMapper;

	public DistributorH5GetDistributorInfoService(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			ObjectMapper objectMapper) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getDistributorInfo(long companyId, long distributorId, String requestLangTag) {
		if (distributorId > 0L) {
			Optional<Distributor> explicit =
					distributorRepositoryGetInfoSimpleService.findDistributorEntityByCompanyAndDistributorId(
							companyId, distributorId);
			if (explicit.isPresent()) {
				return buildRowAfterFormat(companyId, explicit.get(), false, requestLangTag);
			}
		}

		Optional<DistributorRepositoryGetInfoSimpleService.ResolvedDistributorForShop> resolved =
				distributorRepositoryGetInfoSimpleService.resolveForCompanyShop(companyId, distributorId);
		if (resolved.isEmpty()) {
			return Collections.emptyMap();
		}
		DistributorRepositoryGetInfoSimpleService.ResolvedDistributorForShop r = resolved.get();
		return buildRowAfterFormat(companyId, r.distributor(), r.useVirtualMainDistributorIdInClientMap(), requestLangTag);
	}

	private Map<String, Object> buildRowAfterFormat(
			long companyId, Distributor d, boolean useVirtualMainDistributorIdInClientMap, String requestLangTag) {
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
		Map<String, Object> row = distributorListRowFormatService.formatStoreRow(d, selfRule, objectMapper);
		if (useVirtualMainDistributorIdInClientMap) {
			row.put("distributor_id", 0L);
		}
		distributorListOutsideLangReadService.applyLangMaps(companyId, requestLangTag, List.of(row));
		return row;
	}
}
