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

package cn.shopex.ecshopx.goods.service.wxapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cn.shopex.ecshopx.goods.search.GoodsSearchFacade;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WxappGoodsItemsListRestoreSalePriceTest {

	@Test
	void whenActivityTagPresent_keepsSalePriceAndActivityPrice() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("price", 1200000L);
		row.put("activity_price", 100L);
		row.put(GoodsSearchFacade.KEY_LIST_SALE_PRICE, 1200000L);
		Map<String, Object> tag = new LinkedHashMap<>();
		tag.put("tag_type", "limited_time_sale");
		tag.put("activity_price", 100L);
		row.put("promotion_activity", List.of(tag));

		WxappGoodsItemsListServiceImpl.restoreSalePriceWhenNoActivityPriceTag(new ArrayList<>(List.of(row)));

		assertNull(row.get(GoodsSearchFacade.KEY_LIST_SALE_PRICE));
		assertEquals(1200000L, ((Number) row.get("price")).longValue());
		assertEquals(100L, ((Number) row.get("activity_price")).longValue());
	}

	@Test
	void whenNoActivityTag_stripsActivityPriceAndRestoresSale() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("price", 100L);
		row.put("activity_price", 100L);
		row.put(GoodsSearchFacade.KEY_LIST_SALE_PRICE, 1200000L);
		row.put("promotion_activity", List.of());

		WxappGoodsItemsListServiceImpl.restoreSalePriceWhenNoActivityPriceTag(new ArrayList<>(List.of(row)));

		assertNull(row.get(GoodsSearchFacade.KEY_LIST_SALE_PRICE));
		assertNull(row.get("activity_price"));
		assertEquals(1200000L, ((Number) row.get("price")).longValue());
	}
}
