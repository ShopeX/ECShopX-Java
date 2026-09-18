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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WxappH5CartListTotalAndPromotionAggregator {

	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;

	public WxappH5CartListTotalAndPromotionAggregator(
			GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService) {
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
	}

	public void apply(long companyId, Map<String, Object> listResult) {
		if (listResult == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> validCart = (List<Map<String, Object>>) listResult.get("valid_cart");
		if (validCart == null) {
			return;
		}
		List<Map<String, Object>> flatSkuRows = new ArrayList<>();
		for (Map<String, Object> shopBlock : validCart) {
			if (shopBlock == null) {
				continue;
			}
			Object listObj = shopBlock.get("list");
			if (listObj instanceof List<?> lines) {
				for (Object line : lines) {
					if (line instanceof Map<?, ?> m) {
						Map<String, Object> row = new LinkedHashMap<>();
						for (Map.Entry<?, ?> e : m.entrySet()) {
							row.put(String.valueOf(e.getKey()), e.getValue());
						}
						flatSkuRows.add(row);
					}
				}
			}
		}
		if (!flatSkuRows.isEmpty()) {
			goodsItemsListPromotionEnrichmentService.enrich(flatSkuRows);
		}
		BigDecimal goodsFee = BigDecimal.ZERO;
		for (Map<String, Object> row : flatSkuRows) {
			int unit = intFen(row.get("price"));
			int num = intQty(row.get("num"));
			goodsFee = goodsFee.add(BigDecimal.valueOf((long) unit * num));
		}
		BigDecimal yuan = goodsFee.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		listResult.put("cart_total_goods_fee", yuan.toPlainString());
		listResult.put("total_fee", yuan.toPlainString());
		listResult.put("discount_fee", "0.00");
	}

	private static int intFen(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int intQty(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
