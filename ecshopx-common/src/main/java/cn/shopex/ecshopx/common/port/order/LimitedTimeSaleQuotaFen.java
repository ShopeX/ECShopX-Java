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

package cn.shopex.ecshopx.common.port.order;

/**
 * 限时特惠额度（分）：与加购校验一致，按活动价而非原价累计 / 回补。
 */
public final class LimitedTimeSaleQuotaFen {

	private LimitedTimeSaleQuotaFen() {}

	/**
	 * 订单行活动单价（分）=（原价 × 数量 − 限时优惠金额）/ 数量。
	 */
	public static int unitFen(int originalPriceFen, int itemNum, int discountFeeFen) {
		int num = itemNum <= 0 ? 1 : itemNum;
		int price = Math.max(0, originalPriceFen);
		int discount = Math.max(0, discountFeeFen);
		long activityTotal = (long) price * (long) num - (long) discount;
		if (activityTotal < 0L) {
			activityTotal = 0L;
		}
		return (int) (activityTotal / num);
	}
}
