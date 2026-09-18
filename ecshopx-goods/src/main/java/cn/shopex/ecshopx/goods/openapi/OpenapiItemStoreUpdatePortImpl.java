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

import cn.shopex.ecshopx.common.openapi.OpenapiItemStoreUpdatePort;
import cn.shopex.ecshopx.goods.openapi.thirdapi.v2.OpenapiThirdApiV2ItemStoreUpdateService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiItemStoreUpdatePortImpl implements OpenapiItemStoreUpdatePort {

	private final OpenapiThirdApiV2ItemStoreUpdateService itemStoreUpdateService;

	public OpenapiItemStoreUpdatePortImpl(OpenapiThirdApiV2ItemStoreUpdateService itemStoreUpdateService) {
		this.itemStoreUpdateService = itemStoreUpdateService;
	}

	@Override
	public void updateStore(long companyId, Map<String, Object> mergedParams) {
		itemStoreUpdateService.executeUpdateStore(companyId, mergedParams);
	}
}
