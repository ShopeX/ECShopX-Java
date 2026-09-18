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
import cn.shopex.ecshopx.distribution.domain.BasicConfig;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.repository.BasicConfigWriteRepository;
import cn.shopex.ecshopx.distribution.support.BasicConfigColumnNamesDataMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DistributorH5GetDistributorService {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final BasicConfigWriteRepository basicConfigWriteRepository;
	private final ObjectMapper objectMapper;

	public DistributorH5GetDistributorService(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			BasicConfigWriteRepository basicConfigWriteRepository,
			ObjectMapper objectMapper) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.basicConfigWriteRepository = basicConfigWriteRepository;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getDistributor(long companyId) {
		Optional<DistributorRepositoryGetInfoSimpleService.ResolvedDistributorForShop> resolved =
				distributorRepositoryGetInfoSimpleService.resolveForCompanyShop(companyId, 0L);

		Map<String, Object> row;
		if (resolved.isPresent()) {
			Distributor d = resolved.get().distributor();
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
			row = distributorListRowFormatService.formatStoreRow(d, selfRule, objectMapper);
			if (resolved.get().useVirtualMainDistributorIdInClientMap()) {
				row.put("distributor_id", 0L);
			}
		} else {
			row = new LinkedHashMap<>();
		}

		BasicConfig cfg = basicConfigWriteRepository.getInfoByCompanyId(companyId);
		if (cfg != null) {
			row.put("config", BasicConfigColumnNamesDataMapper.toColumnNamesData(cfg));
		} else {
			row.put("config", Collections.emptyList());
		}
		return row;
	}
}
