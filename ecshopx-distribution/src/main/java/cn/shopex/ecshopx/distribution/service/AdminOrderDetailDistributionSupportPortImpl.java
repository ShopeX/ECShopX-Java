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

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidCoreQueryService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminOrderDetailDistributionSupportPortImpl implements AdminOrderDetailDistributionSupportPort {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final OrderValidityPlatformSettingReadService orderValidityPlatformSettingReadService;
	private final DistributorIsValidCoreQueryService distributorIsValidCoreQueryService;

	public AdminOrderDetailDistributionSupportPortImpl(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			DistributorSelfMetaService distributorSelfMetaService,
			OrderValidityPlatformSettingReadService orderValidityPlatformSettingReadService,
			DistributorIsValidCoreQueryService distributorIsValidCoreQueryService) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.orderValidityPlatformSettingReadService = orderValidityPlatformSettingReadService;
		this.distributorIsValidCoreQueryService = distributorIsValidCoreQueryService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getDistributorInfoSimple(long companyId, String distributorIdStr) {
		Object raw = distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId, distributorIdStr);
		if (raw instanceof Map<?, ?> m) {
			return new LinkedHashMap<String, Object>((Map<String, Object>) m);
		}
		return Collections.emptyMap();
	}

	@Override
	public Map<String, Object> getDistributorSelfSimpleInfo(long companyId) {
		return new LinkedHashMap<>(distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId));
	}

	@Override
	public Map<String, Object> getDistributorInfoFormatted(long companyId, long distributorId) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("distributor_id", distributorId);
		Map<String, Object> row = distributorIsValidCoreQueryService.getInfo(filter);
		return row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row);
	}

	@Override
	public Map<String, Object> readOrderValidityPlatformSetting(long companyId) {
		return orderValidityPlatformSettingReadService.readOrderValidityPlatformSetting(companyId);
	}

	@Override
	public Map<String, Object> resolveWxappListDistributorFilter(long companyId, String mobileFromAuth) {
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("company_id", companyId);
		if (StringUtils.hasText(mobileFromAuth)) {
			q.put("mobile", mobileFromAuth.trim());
		}
		Map<String, Object> row = distributorIsValidCoreQueryService.getInfo(q);
		return row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row);
	}
}
