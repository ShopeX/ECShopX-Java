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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WxappOrderDetailMarkdownFilterSupport")
class WxappOrderDetailMarkdownFilterSupportTest {

	@Test
	void stripMarkDown_removesOrderLevelMarkDownKeepsCoupon() {
		Map<String, Object> orderInfo = new LinkedHashMap<>();
		List<Map<String, Object>> discountInfo = new ArrayList<>();
		discountInfo.add(entry("coupon_discount", "优惠券", 100));
		discountInfo.add(entry("mark_down", "订单改价", 849999));
		orderInfo.put("discount_info", discountInfo);

		WxappOrderDetailMarkdownFilterSupport.stripMarkDown(orderInfo);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> after = (List<Map<String, Object>>) orderInfo.get("discount_info");
		assertEquals(1, after.size());
		assertEquals("coupon_discount", after.get(0).get("type"));
		assertFalse(containsMarkDown(after));
	}

	@Test
	void stripMarkDown_removesItemLevelMarkDown() {
		Map<String, Object> item = new LinkedHashMap<>();
		List<Map<String, Object>> itemDiscount = new ArrayList<>();
		itemDiscount.add(entry("member_discount", "会员折扣", 50));
		itemDiscount.add(entry("mark_down", "订单改价", 200));
		item.put("discount_info", itemDiscount);

		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("discount_info", List.of(entry("full_minus", "满减", 10)));
		orderInfo.put("items", List.of(item));

		WxappOrderDetailMarkdownFilterSupport.stripMarkDown(orderInfo);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> afterItem =
				(List<Map<String, Object>>) item.get("discount_info");
		assertEquals(1, afterItem.size());
		assertEquals("member_discount", afterItem.get(0).get("type"));
		assertFalse(containsMarkDown(afterItem));
	}

	@Test
	void stripMarkDown_removesMapKeyedMarkDown() {
		Map<String, Object> discountMap = new LinkedHashMap<>();
		discountMap.put("coupon_discount", entry("coupon_discount", "优惠券", 80));
		discountMap.put("mark_down", entry("mark_down", "订单改价", 900));

		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("discount_info", discountMap);

		WxappOrderDetailMarkdownFilterSupport.stripMarkDown(orderInfo);

		@SuppressWarnings("unchecked")
		Map<String, Object> after = (Map<String, Object>) orderInfo.get("discount_info");
		assertEquals(1, after.size());
		assertTrue(after.containsKey("coupon_discount"));
		assertFalse(after.containsKey("mark_down"));
	}

	@Test
	void stripMarkDown_noopWhenMissing() {
		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("items", List.of());
		WxappOrderDetailMarkdownFilterSupport.stripMarkDown(orderInfo);
		assertFalse(orderInfo.containsKey("discount_info"));
	}

	private static Map<String, Object> entry(String type, String info, int fee) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("type", type);
		m.put("info", info);
		m.put("rule", info);
		m.put("discount_fee", fee);
		return m;
	}

	private static boolean containsMarkDown(List<Map<String, Object>> list) {
		for (Map<String, Object> e : list) {
			if ("mark_down".equals(String.valueOf(e.get("type")))) {
				return true;
			}
		}
		return false;
	}
}
