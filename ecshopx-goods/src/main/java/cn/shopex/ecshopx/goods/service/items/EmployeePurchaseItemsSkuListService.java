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

import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsListSkuSpecApplier;
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

@Service
public class EmployeePurchaseItemsSkuListService {

	private static final Set<String> MARKETING_GIFT_SKU_STRIP_KEYS = Set.of(
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

	private final ItemsMapper itemsMapper;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier;
	private final ItemsSkuListAssembler itemsSkuListAssembler;
	private final ItemsMedicineService itemsMedicineService;
	private final ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;

	public EmployeePurchaseItemsSkuListService(
			ItemsMapper itemsMapper,
			ItemRelAttributesRepository itemRelAttributesRepository,
			DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier,
			ItemsSkuListAssembler itemsSkuListAssembler,
			ItemsMedicineService itemsMedicineService,
			ItemLogisticsStoreEnricher itemLogisticsStoreEnricher) {
		this.itemsMapper = itemsMapper;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.distributorItemsListSkuSpecApplier = distributorItemsListSkuSpecApplier;
		this.itemsSkuListAssembler = itemsSkuListAssembler;
		this.itemsMedicineService = itemsMedicineService;
		this.itemLogisticsStoreEnricher = itemLogisticsStoreEnricher;
	}

	/**
	 * Maps a persisted {@link Items} row to the initial list map shape before enrichment (multi-spec pics, SKU spec,
	 * labels, medicine).
	 */
	public static Map<String, Object> itemEntityToInitialSkuMap(Items item) {
		return itemsRowToMap(item);
	}

	/**
	 * Applies the same post-query enrichment chain as {@link #loadSkuItemsList} after rows are loaded elsewhere.
	 */
	public void applyPostQuerySkuEnrichment(long companyId, List<Map<String, Object>> list) {
		applyPostQuerySkuEnrichment(companyId, list, true);
	}

	/**
	 * @param applyListThumbPics when true, collapse pics to list-thumb form; when false, decode pics JSON arrays
	 */
	public void applyPostQuerySkuEnrichment(long companyId, List<Map<String, Object>> list, boolean applyListThumbPics) {
		if (list == null || list.isEmpty()) {
			return;
		}
		if (applyListThumbPics) {
			applyMultiSpecPics(companyId, list);
		} else {
			decodePicsJsonArrays(list);
		}
		distributorItemsListSkuSpecApplier.apply(companyId, list);
		for (Map<String, Object> row : list) {
			String dbItemType = row.get("item_type") != null ? row.get("item_type").toString() : "";
			String effectiveItemType = dbItemType.isEmpty() ? "services" : dbItemType;
			row.put("item_type", effectiveItemType);
		}
		itemsSkuListAssembler.applyTypeLabels(list);
		itemsMedicineService.applyMedicineDataToRows(companyId, list);
		for (Map<String, Object> row : list) {
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
			row.put("medicine_data", medicineData);
		}
	}

	public Map<String, Object> loadSkuItemsList(long companyId, List<Long> itemIds) {
		return loadSkuItemsListInternal(companyId, itemIds, true);
	}

	public Map<String, Object> loadSkuItemsListForMarketingGift(long companyId, List<Long> itemIds) {
		return loadSkuItemsListInternal(companyId, itemIds, false);
	}

	private Map<String, Object> loadSkuItemsListInternal(long companyId, List<Long> itemIds, boolean applyListThumbPics) {
		Map<String, Object> out = new LinkedHashMap<>();
		if (itemIds == null || itemIds.isEmpty()) {
			out.put("total_count", 0);
			out.put("list", List.of());
			return out;
		}
		List<Long> distinctOrdered = itemIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
		if (distinctOrdered.isEmpty()) {
			out.put("total_count", 0);
			out.put("list", List.of());
			return out;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, distinctOrdered);
		List<Items> dbRows = itemsMapper.selectList(w);
		Map<Long, Items> byId = dbRows.stream().collect(Collectors.toMap(Items::getItemId, it -> it, (a, b) -> a, LinkedHashMap::new));
		List<Map<String, Object>> list = new ArrayList<>();
		for (Long id : distinctOrdered) {
			Items item = byId.get(id);
			if (item != null) {
				if (applyListThumbPics) {
					list.add(itemsRowToMap(item));
				} else {
					list.add(stripMarketingGiftSkuExtras(GoodsItemsListRowMapper.toRow(item)));
				}
			}
		}
		if (list.isEmpty()) {
			out.put("total_count", 0);
			out.put("list", List.of());
			return out;
		}
		if (applyListThumbPics) {
			applyMultiSpecPics(companyId, list);
		} else {
			decodePicsJsonArrays(list);
		}
		distributorItemsListSkuSpecApplier.apply(companyId, list);
		for (Map<String, Object> row : list) {
			String dbItemType = row.get("item_type") != null ? row.get("item_type").toString() : "";
			String effectiveItemType = dbItemType.isEmpty() ? "services" : dbItemType;
			row.put("item_type", effectiveItemType);
		}
		itemsSkuListAssembler.applyTypeLabels(list);
		if (applyListThumbPics) {
			itemsMedicineService.applyMedicineDataToRows(companyId, list);
			for (Map<String, Object> row : list) {
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
				row.put("medicine_data", medicineData);
			}
		} else {
			List<Map<String, Object>> medicineRows = new ArrayList<>();
			for (Map<String, Object> row : list) {
				if (isMedicineItemRow(row)) {
					medicineRows.add(row);
				}
			}
			if (!medicineRows.isEmpty()) {
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
					row.put("medicine_data", medicineData);
				}
			}
			for (Map<String, Object> row : list) {
				Object spec = row.get("item_spec");
				if (spec instanceof List<?> l && l.isEmpty()) {
					row.remove("item_spec");
				}
			}
			// 满赠赠品需带 logistics_store，结算软校验才能与普通供应商商品同源
			itemLogisticsStoreEnricher.enrichListRowsWithLogisticsStore(companyId, list);
			applyWxappSkuListAliases(list);
		}
		out.put("total_count", list.size());
		out.put("list", list);
		return out;
	}

	private void applyMultiSpecPics(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			long id = toLong(row.get("item_id"));
			if (id > 0L) {
				itemIds.add(id);
			}
		}
		if (itemIds.isEmpty()) {
			return;
		}
		List<ItemRelAttributes> specRows =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, itemIds, "item_spec");
		Map<Long, List<ItemRelAttributes>> byItem =
				specRows.stream().collect(Collectors.groupingBy(ItemRelAttributes::getItemId));
		for (Map<String, Object> row : rows) {
			long itemId = toLong(row.get("item_id"));
			Object nospec = row.get("nospec");
			boolean multiSpec = "false".equalsIgnoreCase(String.valueOf(nospec)) || "0".equals(String.valueOf(nospec));
			if (!multiSpec) {
				row.put("pics", firstPicString(row.get("pics")));
				continue;
			}
			List<ItemRelAttributes> rels = byItem.getOrDefault(itemId, List.of());
			String specImage = null;
			for (ItemRelAttributes r : rels) {
				if (r.getItemId() != null
						&& r.getItemId().equals(itemId)
						&& StringUtils.hasText(r.getImageUrl())) {
					specImage = r.getImageUrl();
					break;
				}
			}
			if (StringUtils.hasText(specImage)) {
				row.put("spec_image_url", specImage);
			} else {
				row.put("pics", firstPicString(row.get("pics")));
			}
		}
	}

	private static Map<String, Object> stripMarketingGiftSkuExtras(Map<String, Object> row) {
		Map<String, Object> copy = new LinkedHashMap<>(row);
		for (String key : MARKETING_GIFT_SKU_STRIP_KEYS) {
			copy.remove(key);
		}
		return copy;
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

	private static Map<String, Object> itemsRowToMap(Items item) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (item.getItemId() != null) {
			m.put("item_id", item.getItemId());
		}
		if (item.getCompanyId() != null) {
			m.put("company_id", item.getCompanyId());
		}
		if (item.getItemName() != null) {
			m.put("item_name", item.getItemName());
		}
		if (item.getApproveStatus() != null) {
			m.put("approve_status", item.getApproveStatus());
		}
		if (item.getStore() != null) {
			m.put("store", item.getStore());
		}
		if (item.getPrice() != null) {
			m.put("price", item.getPrice());
		}
		if (item.getPics() != null) {
			m.put("pics", item.getPics());
		}
		if (item.getMarketPrice() != null) {
			m.put("market_price", item.getMarketPrice());
		}
		if (item.getBrief() != null) {
			m.put("brief", item.getBrief());
		}
		if (item.getItemType() != null) {
			m.put("item_type", item.getItemType());
		}
		if (item.getNospec() != null) {
			m.put("nospec", item.getNospec());
		}
		if (item.getGoodsId() != null) {
			m.put("goods_id", item.getGoodsId());
		}
		if (item.getItemCategory() != null) {
			m.put("item_category", item.getItemCategory());
		}
		if (item.getType() != null) {
			m.put("type", item.getType());
		}
		if (item.getCrossborderTaxRate() != null) {
			m.put("crossborder_tax_rate", item.getCrossborderTaxRate());
		}
		if (item.getTaxstrategyId() != null) {
			m.put("taxstrategy_id", item.getTaxstrategyId());
		}
		if (item.getTaxationNum() != null) {
			m.put("taxation_num", item.getTaxationNum());
		}
		if (item.getOrigincountryId() != null) {
			m.put("origincountry_id", item.getOrigincountryId());
		}
		if (item.getIsMedicine() != null) {
			m.put("is_medicine", item.getIsMedicine());
		}
		if (item.getIsPrescription() != null) {
			m.put("is_prescription", item.getIsPrescription());
		}
		if (item.getSpecialType() != null) {
			m.put("special_type", item.getSpecialType());
		}
		m.put("brand_logo", item.getBrandLogo() != null ? item.getBrandLogo() : "");
		m.put("distributor_id", item.getDistributorId() != null ? item.getDistributorId() : 0);
		if (item.getIsGift() != null) {
			m.put("is_gift", item.getIsGift());
		}
		if (item.getStartNum() != null) {
			m.put("start_num", item.getStartNum());
		}
		if (item.getDefaultItemId() != null) {
			m.put("default_item_id", item.getDefaultItemId());
		}
		m.put("logistics_store", 0);
		return m;
	}

	private static void applyWxappSkuListAliases(List<Map<String, Object>> rows) {
		for (Map<String, Object> row : rows) {
			putAliasIfAbsent(row, "item_id", "itemId");
			putAliasIfAbsent(row, "item_name", "itemName");
			putAliasIfAbsent(row, "item_bn", "itemBn");
			putAliasIfAbsent(row, "company_id", "companyId");
			putAliasIfAbsent(row, "consume_type", "consumeType");
		}
	}

	private static void putAliasIfAbsent(Map<String, Object> row, String snakeKey, String camelKey) {
		if (!row.containsKey(camelKey) && row.get(snakeKey) != null) {
			row.put(camelKey, row.get(snakeKey));
		}
	}

	private static void decodePicsJsonArrays(List<Map<String, Object>> rows) {
		for (Map<String, Object> row : rows) {
			Object pics = row.get("pics");
			if (!(pics instanceof String s) || !StringUtils.hasText(s)) {
				continue;
			}
			String trimmed = s.trim();
			if (!trimmed.startsWith("[")) {
				continue;
			}
			try {
				Object parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(trimmed, List.class);
				if (parsed instanceof List<?> l) {
					row.put("pics", new ArrayList<>(l));
				}
			} catch (Exception ignored) {
			}
		}
	}

	private static String firstPicString(Object picsRaw) {
		if (picsRaw == null) {
			return "";
		}
		if (picsRaw instanceof String s) {
			s = s.trim();
			if (s.startsWith("[") && s.endsWith("]")) {
				String inner = s.substring(1, s.length() - 1).trim();
				if (inner.isEmpty()) {
					return "";
				}
				String[] parts = inner.split(",");
				if (parts.length > 0) {
					String p0 = parts[0].trim();
					if ((p0.startsWith("\"") && p0.endsWith("\"")) || (p0.startsWith("'") && p0.endsWith("'"))) {
						return p0.substring(1, p0.length() - 1);
					}
					return p0;
				}
			}
			return s;
		}
		return picsRaw.toString();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
