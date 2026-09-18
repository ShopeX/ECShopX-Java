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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiItemStoreGetPort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemStoreSyncPort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemStoreUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV2Store")
@RequestMapping("/api/openapi/internal/v2")
public class StoreController extends OpenapiBaseController {

	private final OpenapiItemStoreSyncPort itemStoreSyncPort;
	private final OpenapiItemStoreUpdatePort itemStoreUpdatePort;
	private final OpenapiItemStoreGetPort itemStoreGetPort;

	public StoreController(
			OpenapiItemStoreSyncPort itemStoreSyncPort,
			OpenapiItemStoreUpdatePort itemStoreUpdatePort,
			OpenapiItemStoreGetPort itemStoreGetPort) {
		this.itemStoreSyncPort = itemStoreSyncPort;
		this.itemStoreUpdatePort = itemStoreUpdatePort;
		this.itemStoreGetPort = itemStoreGetPort;
	}

	@PutMapping("/ecx.item.store.sync")
	public Void sync(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = OpenapiThirdApiV2ItemStoreSyncParams.buildMergedParams(request, body);
		itemStoreSyncPort.syncStore(companyId, merged);
		return null;
	}

	@PutMapping("/ecx.item.store.update")
	public Void update(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = OpenapiThirdApiV2ItemStoreUpdateParams.buildMergedParams(request, body);
		itemStoreUpdatePort.updateStore(companyId, merged);
		return null;
	}

	@GetMapping("/ecx.item.store.get")
	public Map<String, Object> getStoreDetail(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = OpenapiThirdApiV2ItemStoreGetParams.buildMergedParams(request, body);
		return itemStoreGetPort.getStoreDetail(companyId, merged);
	}
}
