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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailShippingFeeLineSupport {

	public Map<String, Object> buildLine(Object freightFeeFromOrderInfo) {
		int fee = intVal(freightFeeFromOrderInfo);
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("item_bn", "shippingFeeLine888");
		line.put("item_fee", fee);
		line.put("item_name", "运费");
		line.put("order_item_type", "shipping_fee");
		line.put("price", fee);
		line.put("total_fee", fee);
		line.put("refundable_amount", fee);
		line.put("market_price", fee);
		line.put("num", 1);
		return line;
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
