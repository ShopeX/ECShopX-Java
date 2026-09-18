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

package cn.shopex.ecshopx.promotions.domain.turntable;

/**
 * 奖品类型（PRD §13.1）。
 */
public final class TurntablePrizeType {

	public static final String THANKS = "thanks";
	public static final String POINTS = "points";
	public static final String COUPON = "coupon";
	public static final String COUPONS = "coupons";

	private TurntablePrizeType() {}
}
