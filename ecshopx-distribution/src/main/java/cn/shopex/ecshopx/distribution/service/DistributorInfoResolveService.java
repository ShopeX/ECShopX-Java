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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.repository.DistributorInfoReadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DistributorInfoResolveService {

	private final DistributorInfoReadRepository distributorInfoReadRepository;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final ObjectMapper objectMapper;

	public DistributorInfoResolveService(
			DistributorInfoReadRepository distributorInfoReadRepository,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			ObjectMapper objectMapper) {
		this.distributorInfoReadRepository = distributorInfoReadRepository;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.objectMapper = objectMapper;
	}

	public Optional<Map<String, Object>> resolveStoreDetail(
			long companyId, long requestedDistributorId, String requestLang) {
		if (requestedDistributorId > 0) {
			Optional<Distributor> direct =
					distributorInfoReadRepository.findByCompanyAndDistributorId(
							companyId, requestedDistributorId, requestLang);
			if (direct.isPresent()) {
				return Optional.of(formatFromEntity(direct.get()));
			}
		}
		Optional<Distributor> def =
				distributorInfoReadRepository.loadDefaultDistributorForCompany(companyId, requestLang);
		if (def.isEmpty()) {
			return distributorInfoReadRepository
					.loadSelfDistributorForCompany(companyId, requestLang)
					.map(this::formatMainDistributorRowFromEntity);
		}
		if (shouldReplaceDefaultWithMainEntity(def.get())) {
			return distributorInfoReadRepository
					.loadSelfDistributorForCompany(companyId, requestLang)
					.map(this::formatMainDistributorRowFromEntity);
		}
		return Optional.of(formatFromEntity(def.get()));
	}

	private boolean shouldReplaceDefaultWithMainEntity(Distributor chosen) {
		Object isValid = chosen.getIsValid();
		if (Boolean.FALSE.equals(isValid)) {
			return true;
		}
		if (isValid != null && "false".equalsIgnoreCase(isValid.toString().trim())) {
			return true;
		}
		Integer self = chosen.getDistributorSelf();
		if (self == null) {
			return false;
		}
		return self == 0;
	}

	private Map<String, Object> formatMainDistributorRowFromEntity(Distributor d) {
		Map<String, Object> formatted = formatFromEntity(d);
		formatted.put("distributor_id", 0L);
		return formatted;
	}

	private Map<String, Object> formatFromEntity(Distributor d) {
		int distributorSelf = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
		long effectiveDistributorId = d.getDistributorId() != null ? d.getDistributorId() : 0L;
		Map<String, Object> selfDelivery =
				selfDeliverySettingReadService.getSetting(
						d.getCompanyId() != null ? d.getCompanyId() : 0L,
						effectiveDistributorId,
						distributorSelf);
		return distributorListRowFormatService.formatStoreRow(d, selfDelivery, objectMapper);
	}
}
