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

package cn.shopex.ecshopx.distribution.integration;

import cn.shopex.ecshopx.common.port.distribution.DistributorAftersalesAddressDetailReadPort;
import cn.shopex.ecshopx.distribution.service.DistributorAftersalesAddressReadService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DistributorAftersalesAddressDetailReadPortImpl implements DistributorAftersalesAddressDetailReadPort {

	private final DistributorAftersalesAddressReadService distributorAftersalesAddressReadService;

	public DistributorAftersalesAddressDetailReadPortImpl(
			DistributorAftersalesAddressReadService distributorAftersalesAddressReadService) {
		this.distributorAftersalesAddressReadService = distributorAftersalesAddressReadService;
	}

	@Override
	public Map<String, Object> getDetail(long companyId, long addressId, String requestLangTag) {
		return distributorAftersalesAddressReadService.getDistributorAfterSalesAddressDetail(
				companyId, addressId, requestLangTag == null ? "" : requestLangTag);
	}
}
