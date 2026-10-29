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

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;

/** Aligns with PHP {@code DistributorService} platform-store-buy helpers. */
public final class DistributorPlatformStoreBuySupport {

	private DistributorPlatformStoreBuySupport() {
	}

	public static boolean isEnabledRaw(Map<String, Object> distributorRow) {
		if (distributorRow == null || distributorRow.isEmpty()) {
			return false;
		}
		Object v = distributorRow.get("is_platform_store_buy");
		return v == Boolean.TRUE
				|| (v instanceof Number n && n.intValue() == 1)
				|| "1".equals(String.valueOf(v))
				|| "true".equalsIgnoreCase(String.valueOf(v));
	}

	/** Aligns with PHP {@code ItemsService::isSkuHeadquartersShipment}. */
	public static boolean isSkuHeadquartersShipment(Map<String, Object> itemInfo) {
		if (itemInfo == null) {
			return false;
		}
		Object isTotal = itemInfo.get("is_total_store");
		return isTotal == Boolean.TRUE
				|| (isTotal instanceof Number n && n.intValue() == 1)
				|| "1".equals(String.valueOf(isTotal))
				|| "true".equalsIgnoreCase(String.valueOf(isTotal));
	}

	/**
	 * Pure gate: headquarters shipment skips the cloud-buy switch; store shipment requires it on.
	 *
	 * @throws ResourceException when store shipment and switch is off
	 */
	public static void assertShopadminFastBuyAllowedForItem(
			Map<String, Object> itemInfo, boolean platformStoreBuyEnabled) {
		if (isSkuHeadquartersShipment(itemInfo)) {
			return;
		}
		if (!platformStoreBuyEnabled) {
			throw new ResourceException("该店铺未开启云仓可购买");
		}
	}
}
