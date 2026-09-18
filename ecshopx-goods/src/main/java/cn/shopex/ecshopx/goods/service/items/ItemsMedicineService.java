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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsMedicine;
import cn.shopex.ecshopx.goods.dispatch.MedicineItemsSubmitAuditDispatchPublisher;
import cn.shopex.ecshopx.goods.repository.ItemsMedicineRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ItemsMedicineService {

	private final ItemsMedicineRepository itemsMedicineRepository;
	private final ItemsRepository itemsRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final MedicineItemsSubmitAuditDispatchPublisher medicineItemsSubmitAuditDispatchPublisher;
	private final LangueProperties langueProperties;

	public ItemsMedicineService(
			ItemsMedicineRepository itemsMedicineRepository,
			ItemsRepository itemsRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier,
			MedicineItemsSubmitAuditDispatchPublisher medicineItemsSubmitAuditDispatchPublisher,
			LangueProperties langueProperties) {
		this.itemsMedicineRepository = itemsMedicineRepository;
		this.itemsRepository = itemsRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.medicineItemsSubmitAuditDispatchPublisher = medicineItemsSubmitAuditDispatchPublisher;
		this.langueProperties = langueProperties;
	}

	public void assertMedicineSettingsAndEnrichParams(long companyId, Map<String, Object> params, Map<String, Object> data) {
		Object im = params.get("is_medicine");
		if (im == null || !Integer.valueOf(1).equals(toIntBoxed(im))) {
			return;
		}
		data.put("is_prescription", params.get("is_prescription") != null ? toInt(params.get("is_prescription")) : 0);
	}

	public void enrichFlatItemsWithMedicineData(long companyId, List<Map<String, Object>> itemRowMaps) {
		if (itemRowMaps == null || itemRowMaps.isEmpty()) {
			return;
		}
		List<Long> medIds = new ArrayList<>();
		for (Map<String, Object> r : itemRowMaps) {
			if (Integer.valueOf(1).equals(toIntBoxed(r.get("is_medicine")))) {
				long id = toLong(r.get("item_id"));
				if (id > 0L) {
					medIds.add(id);
				}
			}
		}
		if (medIds.isEmpty()) {
			return;
		}
		Map<Long, ItemsMedicine> byItem = itemsMedicineRepository.mapByItemIds(companyId, medIds);
		for (Map<String, Object> r : itemRowMaps) {
			if (!Integer.valueOf(1).equals(toIntBoxed(r.get("is_medicine")))) {
				continue;
			}
			long itemId = toLong(r.get("item_id"));
			ItemsMedicine medicineRow = byItem.get(itemId);
			if (medicineRow == null) {
				continue;
			}
			r.put("medicine_data", toMedicineDataMap(medicineRow));
			Object nospec = r.get("nospec");
			boolean nospecTrue =
					"true".equalsIgnoreCase(String.valueOf(nospec)) || Integer.valueOf(1).equals(toIntBoxed(nospec));
			if (nospecTrue) {
				r.put("medicine_spec", medicineRow.getSpec() == null ? "" : medicineRow.getSpec());
			}
		}
	}

	public void applyMedicineDataToRows(long companyId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> r : rows) {
			long id = toLong(r.get("item_id"));
			if (id > 0) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return;
		}
		Map<Long, ItemsMedicine> map = itemsMedicineRepository.mapByItemIds(companyId, ids);
		for (Map<String, Object> r : rows) {
			long id = toLong(r.get("item_id"));
			ItemsMedicine m = map.get(id);
			if (m == null) {
				continue;
			}
			r.put("medicine_type", m.getMedicineType());
			r.put("common_name", m.getCommonName());
			r.put("dosage", m.getDosage());
			r.put("spec", m.getSpec());
			r.put("packing_spec", m.getPackingSpec());
			r.put("manufacturer", m.getManufacturer());
			r.put("approval_number", m.getApprovalNumber());
			r.put("unit", m.getUnit());
			r.put("medicine_is_prescription", m.getIsPrescription());
			r.put("special_common_name", m.getSpecialCommonName());
			r.put("special_spec", m.getSpecialSpec());
			r.put("medicine_audit_status", m.getAuditStatus());
			r.put("medicine_audit_reason", m.getAuditReason());
			r.put("medicine_item_type", m.getItemType());
			r.put("use_tip", m.getUseTip());
			r.put("symptom", m.getSymptom());
			r.put("max_num", m.getMaxNum());
		}
	}

	public void updateItemMedicineData(Map<String, Object> skuParams, Map<String, Object> medicineData, Map<String, Object> itemsResult) {
		if (medicineData == null || medicineData.isEmpty()) {
			return;
		}
		long itemId = toLong(itemsResult.get("item_id"));
		long companyId = toLong(itemsResult.get("company_id"));
		ItemsMedicine row = new ItemsMedicine();
		row.setItemId(itemId);
		row.setCompanyId(companyId);
		row.setSpec(str(skuParams.get("spec_name")));
		row.setMaxNum(skuParams.get("max_num") != null ? toInt(skuParams.get("max_num")) : 0);
		itemsMedicineRepository.upsertByItemId(row);

		ItemsMedicine reloaded = itemsMedicineRepository.mapByItemIds(companyId, List.of(itemId)).get(itemId);
		if (reloaded == null) {
			return;
		}
		Integer st = reloaded.getAuditStatus();
		if (st == null || st <= 0) {
			return;
		}
		String itemName = resolveItemNameForSubmitAudit(itemsResult, skuParams);
		String barcode = resolveBarcodeForSubmitAudit(itemsResult, skuParams);
		int price = resolvePriceForSubmitAudit(itemsResult, skuParams);
		medicineItemsSubmitAuditDispatchPublisher.publishMedicineItemSubmitAudit(
				new MedicineSubmitAuditPayload(companyId, itemId, itemName, barcode, price, reloaded));
	}

	private static String resolveItemNameForSubmitAudit(Map<String, Object> itemsResult, Map<String, Object> skuParams) {
		String s = str(itemsResult.get("item_name"));
		if (!s.isEmpty()) {
			return s;
		}
		s = str(itemsResult.get("title"));
		if (!s.isEmpty()) {
			return s;
		}
		s = str(skuParams.get("item_name"));
		if (!s.isEmpty()) {
			return s;
		}
		return str(skuParams.get("title"));
	}

	private static String resolveBarcodeForSubmitAudit(Map<String, Object> itemsResult, Map<String, Object> skuParams) {
		String s = str(itemsResult.get("barcode"));
		if (!s.isEmpty()) {
			return s;
		}
		return str(skuParams.get("barcode"));
	}

	private static int resolvePriceForSubmitAudit(Map<String, Object> itemsResult, Map<String, Object> skuParams) {
		if (itemsResult.get("price") != null) {
			return toInt(itemsResult.get("price"));
		}
		if (skuParams.get("price") != null) {
			return toInt(skuParams.get("price"));
		}
		return 0;
	}

	public void syncMedicine(HttpServletRequest request, long companyId, List<Long> goodsIds) {
		List<Items> items = itemsRepository.listByGoodsIds(goodsIds);
		if (items.isEmpty()) {
			throw new ResourceException("商品不存在");
		}
		List<Map<String, Object>> rows = new ArrayList<>();
		for (Items it : items) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("item_id", it.getItemId());
			row.put("item_name", it.getItemName() != null ? it.getItemName() : "");
			row.put("barcode", it.getBarcode() != null ? it.getBarcode() : "");
			row.put("price", it.getPrice() != null ? it.getPrice() : 0);
			row.put("company_id", it.getCompanyId());
			row.put("is_medicine", it.getIsMedicine());
			row.put("goods_id", it.getGoodsId());
			rows.add(row);
		}
		itemsListMultiLangApplier.applyToRows(companyId, RequestLangTag.current(langueProperties), rows);
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> r : rows) {
			Object i = r.get("item_id");
			if (i instanceof Number n) {
				itemIds.add(n.longValue());
			}
		}
		List<ItemsMedicine> medRows = itemsMedicineRepository.listNormalByItemIds(itemIds);
		Map<Long, ItemsMedicine> byItemId = new LinkedHashMap<>();
		for (ItemsMedicine m : medRows) {
			if (m.getItemId() != null) {
				byItemId.put(m.getItemId(), m);
			}
		}
		for (Map<String, Object> row : rows) {
			String itemName = row.get("item_name") != null ? row.get("item_name").toString() : "";
			if (!Integer.valueOf(1).equals(toIntBoxed(row.get("is_medicine")))) {
				throw new ResourceException("商品【" + itemName + "】非药品");
			}
			Object idObj = row.get("item_id");
			long itemId = idObj instanceof Number n ? n.longValue() : 0L;
			ItemsMedicine medicine = byItemId.get(itemId);
			if (medicine == null) {
				throw new ResourceException("商品【" + itemName + "】药品数据缺失");
			}
			if (medicine.getAuditStatus() != null && medicine.getAuditStatus() == 2) {
				throw new ResourceException("商品【" + itemName + "】药品审核已通过");
			}
			int price = row.get("price") instanceof Number n ? n.intValue() : 0;
			String barcode = row.get("barcode") != null ? row.get("barcode").toString() : "";
			MedicineSubmitAuditPayload payload = new MedicineSubmitAuditPayload(companyId, itemId, itemName, barcode, price, medicine);
			medicineItemsSubmitAuditDispatchPublisher.publishMedicineItemSubmitAudit(payload);
		}
		itemsMedicineRepository.updateAuditStatusForNormalItems(itemIds, 1);
	}

	private static Integer toIntBoxed(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int toInt(Object o) {
		return (int) toLong(o);
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static LinkedHashMap<String, Object> toMedicineDataMap(ItemsMedicine m) {
		LinkedHashMap<String, Object> medicineDataMap = new LinkedHashMap<>();
		medicineDataMap.put("item_id", m.getItemId() == null ? Long.valueOf(0L) : m.getItemId());
		medicineDataMap.put("company_id", m.getCompanyId() == null ? Long.valueOf(0L) : m.getCompanyId());
		medicineDataMap.put("medicine_type", m.getMedicineType() == null ? Integer.valueOf(0) : m.getMedicineType());
		medicineDataMap.put("common_name", m.getCommonName() == null ? "" : m.getCommonName());
		medicineDataMap.put("dosage", m.getDosage() == null ? "" : m.getDosage());
		medicineDataMap.put("spec", m.getSpec() == null ? "" : m.getSpec());
		medicineDataMap.put("packing_spec", m.getPackingSpec() == null ? "" : m.getPackingSpec());
		medicineDataMap.put("manufacturer", m.getManufacturer() == null ? "" : m.getManufacturer());
		medicineDataMap.put("approval_number", m.getApprovalNumber() == null ? "" : m.getApprovalNumber());
		medicineDataMap.put("unit", m.getUnit() == null ? "" : m.getUnit());
		medicineDataMap.put("is_prescription", m.getIsPrescription() == null ? Integer.valueOf(0) : m.getIsPrescription());
		medicineDataMap.put("special_common_name", m.getSpecialCommonName() == null ? "" : m.getSpecialCommonName());
		medicineDataMap.put("special_spec", m.getSpecialSpec() == null ? "" : m.getSpecialSpec());
		medicineDataMap.put("audit_status", m.getAuditStatus() == null ? Integer.valueOf(0) : m.getAuditStatus());
		medicineDataMap.put("audit_reason", m.getAuditReason() == null ? "" : m.getAuditReason());
		medicineDataMap.put("item_type", m.getItemType() == null ? "" : m.getItemType());
		medicineDataMap.put("use_tip", m.getUseTip() == null ? "" : m.getUseTip());
		medicineDataMap.put("symptom", m.getSymptom() == null ? "" : m.getSymptom());
		medicineDataMap.put("max_num", m.getMaxNum() == null ? Integer.valueOf(0) : m.getMaxNum());
		medicineDataMap.put("created", m.getCreated() == null ? Integer.valueOf(0) : m.getCreated());
		medicineDataMap.put("updated", m.getUpdated() == null ? Integer.valueOf(0) : m.getUpdated());
		return medicineDataMap;
	}
}
