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

package cn.shopex.ecshopx.goods.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.service.ome.OmeBrandSyncFacade;
import cn.shopex.ecshopx.goods.service.ome.OmeItemCategorySyncFacade;
import cn.shopex.ecshopx.goods.service.ome.OmeItemSpecSyncFacade;
import cn.shopex.ecshopx.goods.service.ome.OmeItemsSyncFacade;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true
)
@AdminAuth
@ShopLog
@RestController("goodsAdminV1SyncFromOme")
@RequestMapping("/api/v1/goods/sync")
public class SyncFromOmeController {

	private final OmeBrandSyncFacade omeBrandSyncFacade;
	private final OmeItemCategorySyncFacade omeItemCategorySyncFacade;
	private final OmeItemSpecSyncFacade omeItemSpecSyncFacade;
	private final OmeItemsSyncFacade omeItemsSyncFacade;

	public SyncFromOmeController(
			OmeBrandSyncFacade omeBrandSyncFacade,
			OmeItemCategorySyncFacade omeItemCategorySyncFacade,
			OmeItemSpecSyncFacade omeItemSpecSyncFacade,
			OmeItemsSyncFacade omeItemsSyncFacade) {
		this.omeBrandSyncFacade = omeBrandSyncFacade;
		this.omeItemCategorySyncFacade = omeItemCategorySyncFacade;
		this.omeItemSpecSyncFacade = omeItemSpecSyncFacade;
		this.omeItemsSyncFacade = omeItemsSyncFacade;
	}

	@Activated(routeAlias = "goods.sync.items")
	@PostMapping(value = "/items", name = "同步商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> syncItems(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = (Map<String, Object>) raw;
		long companyId = AdminGoodsUploadRequestMerge.readRequiredLong(ud, "company_id");
		omeItemsSyncFacade.enqueueInitialSync(companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.sync.itemCategory")
	@PostMapping(value = "/itemCategory", name = "同步分类")
	public ResponseEntity<ApiResult<Map<String, Object>>> syncItemCategory(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = (Map<String, Object>) raw;
		long companyId = AdminGoodsUploadRequestMerge.readRequiredLong(ud, "company_id");
		omeItemCategorySyncFacade.enqueueSync(companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.sync.itemSpec")
	@PostMapping(value = "/itemSpec", name = "同步规格")
	public ResponseEntity<ApiResult<Map<String, Object>>> syncItemSpec(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = (Map<String, Object>) raw;
		long companyId = AdminGoodsUploadRequestMerge.readRequiredLong(ud, "company_id");
		omeItemSpecSyncFacade.enqueueInitialSync(companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "goods.sync.brand")
	@PostMapping(value = "/brand", name = "同步品牌")
	public ResponseEntity<ApiResult<Map<String, Object>>> syncBrand(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		@SuppressWarnings("unchecked")
		Map<String, Object> ud = (Map<String, Object>) raw;
		long companyId = AdminGoodsUploadRequestMerge.readRequiredLong(ud, "company_id");
		omeBrandSyncFacade.enqueueInitialSync(companyId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
