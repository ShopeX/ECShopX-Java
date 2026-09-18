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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SupplierItemsDetailRowMapper {

	private static final List<String> LIST_ONLY_KEYS = List.of(
			"type_labels", "promotion_activity", "activity_price", "tagList", "operator_name", "distributor_name",
			"item_holder", "supplier_name", "gross_profit_rate", "commission_ratio", "itemMainCatName", "itemCatName",
			"supplier_item_id", "is_taobao", "main_item_id", "item_cat_id", "delivery_time",
			"itemId", "consumeType", "itemName", "itemBn", "companyId");

	private SupplierItemsDetailRowMapper() {
	}

	static Map<String, Object> toDetailMap(SupplierItems it) {
		Map<String, Object> m = new LinkedHashMap<>(GoodsItemsListRowMapper.toRow(GoodsItemsListRowMapper.toItemsEntity(it)));
		for (String key : LIST_ONLY_KEYS) {
			m.remove(key);
		}
		m.put("supplier_goods_bn", it.getSupplierGoodsBn());
		return m;
	}

	static Items toPseudoPlatformItem(SupplierItems s) {
		Items it = new Items();
		it.setItemId(s.getItemId());
		it.setCompanyId(s.getCompanyId());
		it.setPrice(s.getPrice());
		it.setStore(s.getStore());
		it.setCostPrice(s.getCostPrice());
		it.setItemBn(s.getItemBn());
		it.setBarcode(s.getBarcode());
		it.setMarketPrice(s.getMarketPrice());
		it.setItemUnit(s.getItemUnit());
		it.setVolume(s.getVolume());
		it.setApproveStatus(s.getApproveStatus());
		it.setIsDefault(s.getIsDefault());
		it.setWeight(s.getWeight());
		it.setStartNum(s.getStartNum());
		it.setDeliveryTime(0);
		it.setSales(s.getSales());
		return it;
	}

	static void applySupplierDetailWireScalars(Map<String, Object> detail) {
		stringifyId(detail, "item_id");
		stringifyId(detail, "goods_id");
		stringifyId(detail, "default_item_id");
		stringifyId(detail, "company_id");
		stringifyId(detail, "brand_id");
		stringifyId(detail, "origincountry_id");
		stringifyId(detail, "taxstrategy_id");
		stringifyId(detail, "item_main_cat_id");

		if (detail.get("rebate_conf") == null) {
			detail.put("rebate_conf", new ArrayList<>());
		}

		Object weight = detail.get("weight");
		if (weight instanceof String s && !s.isBlank()) {
			try {
				detail.put("weight", new BigDecimal(s.trim()).doubleValue());
			} catch (NumberFormatException ignored) {
			}
		}

		Object rebate = detail.get("rebate");
		if (rebate == null || (rebate instanceof String rs && rs.isBlank())) {
			detail.put("rebate", null);
		}
	}

	private static void stringifyId(Map<String, Object> detail, String key) {
		Object v = detail.get(key);
		if (v == null) {
			return;
		}
		detail.put(key, v.toString());
	}
}
