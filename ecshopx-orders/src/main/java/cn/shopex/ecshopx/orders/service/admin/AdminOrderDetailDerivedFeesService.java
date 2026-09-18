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

package cn.shopex.ecshopx.orders.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AdminOrderDetailDerivedFeesService {

	private static final Set<String> PROMOTION_TYPES =
			Set.of("full_minus", "full_discount", "member_tag_targeted_promotion");

	public void apply(Map<String, Object> orderInfo) {
		int pointFeeOrder = intVal(orderInfo.get("point_fee"));
		int freightFee = intVal(orderInfo.get("freight_fee"));

		int pointFreightFee = 0;
		if (pointFeeOrder > 0 && freightFee > 0) {
			int itemsPointSum = sumItemsPointFee(orderInfo.get("items"));
			pointFreightFee = pointFeeOrder - itemsPointSum;
		}
		orderInfo.put("point_freight_fee", pointFreightFee);

		int totalFee = intVal(orderInfo.get("total_fee"));
		int itemTotalFee = totalFee - (freightFee - pointFreightFee);
		orderInfo.put("item_total_fee", itemTotalFee);

		int promotionDiscount = 0;
		Object di = orderInfo.get("discount_info");
		if (di instanceof List<?> list) {
			for (Object o : list) {
				if (!(o instanceof Map<?, ?> dm)) {
					continue;
				}
				Object typeObj = dm.get("type");
				String type = typeObj == null ? "" : String.valueOf(typeObj);
				if (PROMOTION_TYPES.contains(type)) {
					promotionDiscount += intVal(dm.get("discount_fee"));
				}
			}
		}
		orderInfo.put("promotion_discount", promotionDiscount);

		int couponDiscount = intVal(orderInfo.get("coupon_discount"));
		int memberDiscount = intVal(orderInfo.get("member_discount"));
		int itemFeeNew =
				totalFee
						- freightFee
						+ pointFeeOrder
						+ couponDiscount
						+ promotionDiscount
						+ memberDiscount;
		orderInfo.put("item_fee_new", itemFeeNew);
	}

	private static int sumItemsPointFee(Object itemsObj) {
		if (!(itemsObj instanceof List<?> list)) {
			return 0;
		}
		int sum = 0;
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				sum += intVal(m.get("point_fee"));
			}
		}
		return sum;
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
