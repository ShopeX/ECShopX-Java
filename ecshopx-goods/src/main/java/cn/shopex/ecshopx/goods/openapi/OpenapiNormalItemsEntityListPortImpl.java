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

package cn.shopex.ecshopx.goods.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiNormalItemsEntityListPort;
import cn.shopex.ecshopx.goods.openapi.thirdapi.v2.OpenapiThirdApiV2NormalItemsEntityListService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiNormalItemsEntityListPortImpl implements OpenapiNormalItemsEntityListPort {

	private final OpenapiThirdApiV2NormalItemsEntityListService entityListService;

	public OpenapiNormalItemsEntityListPortImpl(OpenapiThirdApiV2NormalItemsEntityListService entityListService) {
		this.entityListService = entityListService;
	}

	@Override
	public Map<String, Object> getEntityList(
			long companyId,
			int page,
			int pageSize,
			String approveStatusRaw,
			String brandIdRaw,
			String categoryIdRaw,
			String timeBeginRaw,
			String timeEndRaw,
			boolean isSelfPresent,
			String isSelfRaw) {
		return entityListService.executeOpenapiGetEntityList(
				companyId,
				page,
				pageSize,
				approveStatusRaw,
				brandIdRaw,
				categoryIdRaw,
				timeBeginRaw,
				timeEndRaw,
				isSelfPresent,
				isSelfRaw);
	}
}
