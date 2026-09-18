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

package cn.shopex.ecshopx.promotions.service.register;

import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import java.util.Collection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RegisterPromotionMembercardCouponsApplyService {

	private static final Logger log = LoggerFactory.getLogger(RegisterPromotionMembercardCouponsApplyService.class);

	private final UserDiscountReceiveCardService userDiscountReceiveCardService;

	public RegisterPromotionMembercardCouponsApplyService(UserDiscountReceiveCardService userDiscountReceiveCardService) {
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
	}

	public void applyCoupons(long companyId, long userId, String mobilePlain, Object couponsNode) {
		applyCoupons(companyId, userId, mobilePlain, couponsNode, "注册送优惠券");
	}

	public void applyCoupons(
			long companyId, long userId, String mobilePlain, Object couponsNode, String sourceFrom) {
		if (!(couponsNode instanceof Collection<?> coll) || coll.isEmpty()) {
			return;
		}
		String source = sourceFrom == null || sourceFrom.isBlank() ? "注册送优惠券" : sourceFrom.trim();
		for (Object el : coll) {
			if (!(el instanceof Map<?, ?> raw)) {
				log.debug("register promotion coupon skip: non-map element {}", el);
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> row = (Map<String, Object>) raw;
			Long cardId = firstLong(row, "card_id", "cardId");
			if (cardId == null || cardId <= 0L) {
				log.debug("register promotion coupon skip: missing card_id {}", el);
				continue;
			}
			// Only strictly positive integer counts trigger receiveCard; otherwise zero iterations.
			int count = parseCount(row.get("count"));
			for (int i = 0; i < count; i++) {
				try {
					userDiscountReceiveCardService.receiveCard(
							companyId, userId, mobilePlain, cardId, 0L, "", source);
				} catch (Exception ex) {
					log.debug("register promotion coupon error: {}", ex.getMessage(), ex);
				}
			}
		}
	}

	private static int parseCount(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			if (n instanceof Double d && (d.isNaN() || d.isInfinite())) {
				return 0;
			}
			if (n instanceof Float f && (f.isNaN() || f.isInfinite())) {
				return 0;
			}
			long l = n.longValue();
			if (l <= 0L || l > Integer.MAX_VALUE) {
				return 0;
			}
			if (n.doubleValue() != (double) l) {
				return 0;
			}
			return (int) l;
		}
		try {
			int c = Integer.parseInt(String.valueOf(v).trim());
			return c > 0 ? c : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Long firstLong(Map<String, Object> m, String k1, String k2) {
		Object v = m.get(k1);
		if (v == null) {
			v = m.get(k2);
		}
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
