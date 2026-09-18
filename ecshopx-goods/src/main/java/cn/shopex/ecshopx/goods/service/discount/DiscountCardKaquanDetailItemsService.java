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

package cn.shopex.ecshopx.goods.service.discount;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsListSkuSpecApplier;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.goods.service.items.ItemsSkuListAssembler;
import cn.shopex.ecshopx.goods.service.promotion.MultiSpecItemsTreeFormatter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 卡券明细中的商品 SKU 树：按 getSkuItemsList 全量行加载后合并为主行 + {@code spec_items}。
 */
@Service
public class DiscountCardKaquanDetailItemsService {

	private static final Set<String> ADMIN_LIST_ONLY_KEYS = Set.of(
			"promotion_activity",
			"activity_price",
			"tagList",
			"operator_name",
			"distributor_name",
			"item_holder",
			"supplier_name",
			"gross_profit_rate",
			"commission_ratio",
			"itemMainCatName",
			"itemCatName",
			"data_source",
			"item_cat_id",
			"intro",
			"purchase_agreement");

	private static final Set<String> PDO_STRING_KEYS = Set.of(
			"item_id",
			"goods_id",
			"store",
			"sales",
			"rebate",
			"cost_price",
			"is_point",
			"point",
			"brand_id",
			"is_market",
			"price",
			"market_price",
			"volume",
			"supplier_id",
			"supplier_item_id",
			"sort",
			"templates_id",
			"is_default",
			"default_item_id",
			"distributor_id",
			"company_id",
			"enable_agreement",
			"weight",
			"tax_rate",
			"created",
			"updated",
			"is_gift",
			"is_package",
			"profit_type",
			"profit_fee",
			"is_profit",
			"origincountry_id",
			"taxstrategy_id",
			"taxation_num",
			"type",
			"is_epidemic",
			"is_medicine",
			"is_prescription",
			"start_num",
			"delivery_time",
			"is_taobao",
			"is_show_specimg",
			"item_main_cat_id");

	private final ItemsMapper itemsMapper;
	private final DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier;
	private final ItemsSkuListAssembler itemsSkuListAssembler;
	private final ItemsMedicineService itemsMedicineService;

	public DiscountCardKaquanDetailItemsService(
			ItemsMapper itemsMapper,
			DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier,
			ItemsSkuListAssembler itemsSkuListAssembler,
			ItemsMedicineService itemsMedicineService) {
		this.itemsMapper = itemsMapper;
		this.distributorItemsListSkuSpecApplier = distributorItemsListSkuSpecApplier;
		this.itemsSkuListAssembler = itemsSkuListAssembler;
		this.itemsMedicineService = itemsMedicineService;
	}

	public List<Map<String, Object>> buildItemTreeLists(
			long companyId, List<Long> itemIdsOrdered, Map<Long, Integer> itemIdToUseLimit) {
		if (itemIdsOrdered == null || itemIdsOrdered.isEmpty()) {
			return List.of();
		}
		List<Long> distinctOrdered = itemIdsOrdered.stream().filter(Objects::nonNull).distinct().toList();
		if (distinctOrdered.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, distinctOrdered);
		List<Items> dbRows = itemsMapper.selectList(w);
		Map<Long, Items> byId =
				dbRows.stream().collect(Collectors.toMap(Items::getItemId, it -> it, (a, b) -> a, LinkedHashMap::new));
		List<Map<String, Object>> list = new ArrayList<>();
		for (Long id : distinctOrdered) {
			Items item = byId.get(id);
			if (item == null) {
				continue;
			}
			Map<String, Object> row = stripAdminListOnlyKeys(GoodsItemsListRowMapper.toRow(item));
			int ul = 0;
			if (itemIdToUseLimit != null && item.getItemId() != null) {
				Integer u = itemIdToUseLimit.get(item.getItemId());
				ul = u != null ? u : 0;
			}
			row.put("use_limit", ul);
			list.add(row);
		}
		if (list.isEmpty()) {
			return List.of();
		}
		distributorItemsListSkuSpecApplier.apply(companyId, list);
		for (Map<String, Object> row : list) {
			Object itype = row.get("item_type");
			String dbItemType = itype != null ? itype.toString() : "";
			row.put("item_type", dbItemType.isEmpty() ? "services" : dbItemType);
		}
		itemsSkuListAssembler.applyTypeLabels(list);
		applyMedicineDataOnlyForMedicineItems(companyId, list);
		for (Map<String, Object> row : list) {
			Object spec = row.get("item_spec");
			if (spec instanceof List<?> l && l.isEmpty()) {
				row.remove("item_spec");
			}
			Object specDesc = row.get("item_spec_desc");
			if (specDesc == null || !StringUtils.hasText(specDesc.toString())) {
				row.remove("item_spec_desc");
			}
		}
		List<Map<String, Object>> tree = MultiSpecItemsTreeFormatter.formatItemsList(list);
		for (Map<String, Object> row : tree) {
			applyPhpListWireScalars(row);
			Object specs = row.get("spec_items");
			if (specs instanceof List<?> specList) {
				for (Object el : specList) {
					if (el instanceof Map<?, ?> raw) {
						@SuppressWarnings("unchecked")
						Map<String, Object> specRow = (Map<String, Object>) raw;
						applyPhpListWireScalars(specRow);
					}
				}
			}
		}
		return tree;
	}

	private void applyMedicineDataOnlyForMedicineItems(long companyId, List<Map<String, Object>> list) {
		List<Map<String, Object>> medicineRows = new ArrayList<>();
		for (Map<String, Object> row : list) {
			if (isMedicineItemRow(row)) {
				medicineRows.add(row);
			}
		}
		if (medicineRows.isEmpty()) {
			return;
		}
		itemsMedicineService.applyMedicineDataToRows(companyId, medicineRows);
		for (Map<String, Object> row : medicineRows) {
			Object maxNum = row.get("max_num");
			Map<String, Object> medicineData = new LinkedHashMap<>();
			if (maxNum != null) {
				medicineData.put("max_num", maxNum);
			}
			Object isPrescription = row.get("medicine_is_prescription");
			if (isPrescription == null) {
				isPrescription = row.get("is_prescription");
			}
			if (isPrescription != null) {
				medicineData.put("is_prescription", isPrescription);
			}
			if (!medicineData.isEmpty()) {
				row.put("medicine_data", medicineData);
			}
		}
	}

	private static Map<String, Object> stripAdminListOnlyKeys(Map<String, Object> row) {
		Map<String, Object> copy = new LinkedHashMap<>(row);
		for (String key : ADMIN_LIST_ONLY_KEYS) {
			copy.remove(key);
		}
		return copy;
	}

	/**
	 * Aligns list-row scalars with PHP {@code ItemsRepository::list} PDO string / bool / array shapes.
	 */
	private static void applyPhpListWireScalars(Map<String, Object> row) {
		Object itemCategory = row.get("item_category");
		if (itemCategory != null) {
			row.put("item_main_cat_id", String.valueOf(itemCategory));
		} else if (row.get("item_main_cat_id") != null) {
			row.put("item_main_cat_id", String.valueOf(row.get("item_main_cat_id")));
		}

		for (String key : PDO_STRING_KEYS) {
			if (!row.containsKey(key)) {
				continue;
			}
			Object v = row.get(key);
			if (v == null) {
				continue;
			}
			row.put(key, toPhpPdoString(v));
		}

		row.put("itemId", row.get("item_id"));
		row.put("consumeType", row.get("consume_type") != null ? row.get("consume_type") : "");
		row.put("itemName", row.get("item_name") != null ? row.get("item_name") : "");
		row.put("itemBn", row.get("item_bn") != null ? row.get("item_bn") : "");
		row.put("companyId", row.get("company_id") != null ? row.get("company_id") : "");
	}

	private static String toPhpPdoString(Object v) {
		if (v instanceof Boolean b) {
			return b ? "1" : "0";
		}
		if (v instanceof Number n) {
			if (v instanceof Double || v instanceof Float) {
				return stripTrailingZeros(n.doubleValue());
			}
			return Long.toString(n.longValue());
		}
		String s = v.toString().trim();
		if ("true".equalsIgnoreCase(s)) {
			return "1";
		}
		if ("false".equalsIgnoreCase(s)) {
			return "0";
		}
		return v.toString();
	}

	private static String stripTrailingZeros(double d) {
		String s = java.math.BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
		return s;
	}

	private static boolean isMedicineItemRow(Map<String, Object> row) {
		Object v = row.get("is_medicine");
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		if (v == null) {
			return false;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}
}
