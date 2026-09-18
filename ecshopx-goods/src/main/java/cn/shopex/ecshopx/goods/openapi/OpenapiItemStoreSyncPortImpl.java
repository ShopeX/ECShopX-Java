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

import cn.shopex.ecshopx.common.openapi.OpenapiItemStoreSyncPort;
import cn.shopex.ecshopx.goods.openapi.thirdapi.v2.OpenapiThirdApiV2ItemStoreSyncService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiItemStoreSyncPortImpl implements OpenapiItemStoreSyncPort {

	private final OpenapiThirdApiV2ItemStoreSyncService itemStoreSyncService;

	public OpenapiItemStoreSyncPortImpl(OpenapiThirdApiV2ItemStoreSyncService itemStoreSyncService) {
		this.itemStoreSyncService = itemStoreSyncService;
	}

	@Override
	public void syncStore(long companyId, Map<String, Object> mergedParams) {
		itemStoreSyncService.executeSyncStore(companyId, mergedParams);
	}
}
