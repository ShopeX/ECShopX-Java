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

package cn.shopex.ecshopx.promotions.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tier-discount member price math aligned with legacy PHP {@code bcmul}/{@code bcdiv} (scale 0 discount, then subtract).
 */
public final class MemberTierDiscountPrice {

	private MemberTierDiscountPrice() {
	}

	/** Per-unit discount fen: {@code int(price * discount / 100)} after 2-decimal intermediate (PHP {@code bcmul}). */
	public static int discountPerUnitFen(int unitPriceFen, int discount) {
		BigDecimal price = BigDecimal.valueOf(unitPriceFen);
		BigDecimal off =
				price.multiply(BigDecimal.valueOf(discount)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		return off.intValue();
	}

	/** Member unit price fen: {@code price - discountPerUnitFen(price, discount)} (PHP cart/goods items). */
	public static int memberPriceFromTierDiscount(int priceFen, int discount) {
		return priceFen - discountPerUnitFen(priceFen, discount);
	}

	public static int memberPriceFromTierDiscount(long priceFen, int discount) {
		return memberPriceFromTierDiscount((int) priceFen, discount);
	}
}
