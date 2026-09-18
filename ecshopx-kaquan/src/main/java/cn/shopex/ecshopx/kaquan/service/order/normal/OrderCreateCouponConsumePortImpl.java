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

package cn.shopex.ecshopx.kaquan.service.order.normal;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateCouponConsumePort;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountConsumCardService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCreateCouponConsumePortImpl implements OrderCreateCouponConsumePort {

	private final UserDiscountConsumCardService userDiscountConsumCardService;

	public OrderCreateCouponConsumePortImpl(UserDiscountConsumCardService userDiscountConsumCardService) {
		this.userDiscountConsumCardService = userDiscountConsumCardService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void consumeIfNeeded(NormalOrderCreateParams p) {
		Map<String, Object> res = p.getOrdersInsertResult();
		Object discountInfoRaw = res.get("discount_info");
		Iterable<?> rows = null;
		if (discountInfoRaw instanceof List<?> list) {
			rows = list;
		} else if (discountInfoRaw instanceof Map<?, ?> map) {
			rows = map.values();
		}
		if (rows == null) {
			return;
		}
		long companyId = longVal(res.get("company_id"), longVal(p.getParams().get("company_id"), 0L));
		long userId = longVal(res.get("user_id"), longVal(p.getParams().get("user_id"), 0L));
		String transId = String.valueOf(res.getOrDefault("order_id", ""));
		String fee = String.valueOf(res.getOrDefault("total_fee", ""));
		for (Object o : rows) {
			if (!(o instanceof Map<?, ?> row)) {
				continue;
			}
			Object codeObj = row.get("coupon_code");
			if (codeObj == null || !StringUtils.hasText(codeObj.toString())) {
				continue;
			}
			userDiscountConsumCardService.consumeCouponForShopadminOrderCreate(
					companyId, userId, codeObj.toString().trim(), "商城下单使用优惠券", transId, fee);
		}
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
