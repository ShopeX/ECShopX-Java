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

import cn.shopex.ecshopx.common.openapi.OpenapiDistributorListPort;
import cn.shopex.ecshopx.distribution.openapi.thirdapi.v2.OpenapiThirdApiV2DistributorListService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiDistributorListPortImpl implements OpenapiDistributorListPort {

	private final OpenapiThirdApiV2DistributorListService listService;

	public OpenapiDistributorListPortImpl(OpenapiThirdApiV2DistributorListService listService) {
		this.listService = listService;
	}

	@Override
	public Map<String, Object> list(
			long companyId,
			int page,
			int pageSize,
			String shopCodeRaw,
			String distributorNameRaw,
			String statusRaw,
			String provinceRaw,
			String cityRaw,
			String areaRaw,
			String contactUsernameRaw,
			String contactMobileRaw,
			String distributorIdRaw) {
		return listService.executeOpenapiList(
				companyId,
				page,
				pageSize,
				shopCodeRaw,
				distributorNameRaw,
				statusRaw,
				provinceRaw,
				cityRaw,
				areaRaw,
				contactUsernameRaw,
				contactMobileRaw,
				distributorIdRaw);
	}
}
