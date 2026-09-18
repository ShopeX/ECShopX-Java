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

package cn.shopex.ecshopx.goods.service.promotion;

import cn.shopex.ecshopx.goods.service.items.PlatformItemsDetailCoreService;
import cn.shopex.ecshopx.promotions.port.PackagePromotionFrontGoodsItemsDetailPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PackagePromotionFrontGoodsItemsDetailPortImpl implements PackagePromotionFrontGoodsItemsDetailPort {

	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final PackagePromotionFrontItemsDetailWireService packagePromotionFrontItemsDetailWireService;

	public PackagePromotionFrontGoodsItemsDetailPortImpl(
			PlatformItemsDetailCoreService platformItemsDetailCoreService,
			PackagePromotionFrontItemsDetailWireService packagePromotionFrontItemsDetailWireService) {
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.packagePromotionFrontItemsDetailWireService = packagePromotionFrontItemsDetailWireService;
	}

	@Override
	public Map<String, Object> loadFrontItemsDetail(
			long companyId,
			long itemId,
			String authorizerAppId,
			List<Long> limitSkuItemIdsOrNull) {
		Map<String, Object> detail;
		if (limitSkuItemIdsOrNull == null || limitSkuItemIdsOrNull.isEmpty()) {
			detail = platformItemsDetailCoreService.build(companyId, itemId, authorizerAppId);
		} else {
			detail = platformItemsDetailCoreService.build(companyId, itemId, authorizerAppId, limitSkuItemIdsOrNull);
		}
		if (detail == null || detail.isEmpty()) {
			return Map.of();
		}
		LinkedHashMap<String, Object> wired = new LinkedHashMap<>(detail);
		packagePromotionFrontItemsDetailWireService.apply(wired);
		return wired;
	}
}
