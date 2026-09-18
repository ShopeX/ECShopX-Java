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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelReasonConfig {

	private static final Map<Integer, String> REASONS =
			Map.ofEntries(
					Map.entry(1, "客户现在不想购买"),
					Map.entry(2, "客户商品价格较贵"),
					Map.entry(3, "客户价格波动"),
					Map.entry(4, "客户商品缺货"),
					Map.entry(5, "客户重复下单"),
					Map.entry(6, "客户订单商品选择有误"),
					Map.entry(7, "客户支付方式选择有误"),
					Map.entry(8, "客户收货信息填写有误"),
					Map.entry(9, "客户发票信息填写有误"),
					Map.entry(10, "客户无法支付订单"),
					Map.entry(11, "客户长时间未付款"),
					Map.entry(12, "客户其他原因"));

	public boolean hasReasonKey(int key) {
		return key >= 1 && key <= 12;
	}

	public String reasonTextForKey(int key) {
		String t = REASONS.get(key);
		if (t == null) {
			throw new BadRequestException("取消原因无效");
		}
		return t;
	}

	public Map<String, String> allReasonsAsStringKeyMap() {
		LinkedHashMap<String, String> result = new LinkedHashMap<>(12);
		for (int key = 1; key <= 12; key++) {
			result.put(String.valueOf(key), REASONS.get(key));
		}
		return result;
	}
}
