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

/**
 * 店铺/总店前台配送能力，用于供应商共享库存展示合成。
 */
public final class DistributorDeliveryCapability {

	public enum DisplayMode {
		/** 仅快递：只展示供应商共享库存 */
		LOGISTICS_ONLY,
		/** 仅自配/自提：只展示本地库存 */
		LOCAL_ONLY,
		/** 快递且（自配或自提）：本地 + 共享 */
		SUM
	}

	private final boolean delivery;
	private final boolean selfDelivery;
	private final boolean ziti;

	private DistributorDeliveryCapability(boolean delivery, boolean selfDelivery, boolean ziti) {
		this.delivery = delivery;
		this.selfDelivery = selfDelivery;
		this.ziti = ziti;
	}

	public static DistributorDeliveryCapability of(Boolean delivery, Boolean selfDelivery, Boolean ziti) {
		return new DistributorDeliveryCapability(
				Boolean.TRUE.equals(delivery),
				Boolean.TRUE.equals(selfDelivery),
				Boolean.TRUE.equals(ziti));
	}

	/** 配置缺失时回退为加和，避免误隐藏库存。 */
	public static DistributorDeliveryCapability sumFallback() {
		return new DistributorDeliveryCapability(true, true, false);
	}

	/** 无平台总店（{@code distributor_self=1}）时默认仅快递，展示/合成走供应商共享库存。 */
	public static DistributorDeliveryCapability logisticsOnlyFallback() {
		return new DistributorDeliveryCapability(true, false, false);
	}

	public boolean isDelivery() {
		return delivery;
	}

	public boolean isSelfDelivery() {
		return selfDelivery;
	}

	public boolean isZiti() {
		return ziti;
	}

	public boolean supportsLocalFulfillment() {
		return selfDelivery || ziti;
	}

	public DisplayMode displayMode() {
		boolean local = supportsLocalFulfillment();
		if (delivery && !local) {
			return DisplayMode.LOGISTICS_ONLY;
		}
		if (!delivery && local) {
			return DisplayMode.LOCAL_ONLY;
		}
		return DisplayMode.SUM;
	}

	public int combineDisplayStore(int localStore, int logisticsStore) {
		int local = Math.max(localStore, 0);
		int logistics = Math.max(logisticsStore, 0);
		return switch (displayMode()) {
			case LOGISTICS_ONLY -> logistics;
			case LOCAL_ONLY -> local;
			case SUM -> local + logistics;
		};
	}
}
