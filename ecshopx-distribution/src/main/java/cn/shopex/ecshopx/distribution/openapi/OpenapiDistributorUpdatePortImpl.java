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

package cn.shopex.ecshopx.distribution.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiDistributorUpdatePort;
import cn.shopex.ecshopx.distribution.openapi.thirdapi.v2.OpenapiThirdApiV2DistributorUpdateService;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OpenapiDistributorUpdatePortImpl implements OpenapiDistributorUpdatePort {

	private final OpenapiThirdApiV2DistributorUpdateService updateService;

	public OpenapiDistributorUpdatePortImpl(OpenapiThirdApiV2DistributorUpdateService updateService) {
		this.updateService = updateService;
	}

	@Override
	public Map<String, Object> update(
			long companyId,
			String shopCodeRaw,
			Optional<String> distributorNamePresent,
			Optional<String> contactUsernamePresent,
			Optional<String> contactMobilePresent,
			Optional<String> hourPresent,
			Optional<String> isZitiPresent,
			Optional<String> isDeliveryPresent,
			Optional<String> isAutoSyncGoodsPresent,
			Optional<String> isDadaPresent,
			Optional<String> isDefaultPresent,
			Optional<String> logoPresent,
			Optional<String> statusPresent,
			Optional<String> provincePresent,
			Optional<String> cityPresent,
			Optional<String> areaPresent,
			Optional<String> addressPresent,
			Optional<String> lngPresent,
			Optional<String> latPresent) {
		return updateService.executeOpenapiUpdate(
				companyId,
				shopCodeRaw,
				distributorNamePresent,
				contactUsernamePresent,
				contactMobilePresent,
				hourPresent,
				isZitiPresent,
				isDeliveryPresent,
				isAutoSyncGoodsPresent,
				isDadaPresent,
				isDefaultPresent,
				logoPresent,
				statusPresent,
				provincePresent,
				cityPresent,
				areaPresent,
				addressPresent,
				lngPresent,
				latPresent);
	}
}
