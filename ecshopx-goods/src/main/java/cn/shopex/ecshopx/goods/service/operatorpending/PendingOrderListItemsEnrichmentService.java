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

package cn.shopex.ecshopx.goods.service.operatorpending;

import cn.shopex.ecshopx.common.companys.operatorpending.OperatorPendingListItemsEnrichmentPort;
import cn.shopex.ecshopx.common.companys.operatorpending.PendingListItemsBundle;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.goods.service.items.ItemsRelAttrValuesQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PendingOrderListItemsEnrichmentService implements OperatorPendingListItemsEnrichmentPort {

	private final ItemsRepository itemsRepository;
	private final ItemsMedicineService itemsMedicineService;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService;
	private final ObjectMapper objectMapper;

	public PendingOrderListItemsEnrichmentService(
			ItemsRepository itemsRepository,
			ItemsMedicineService itemsMedicineService,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.itemsMedicineService = itemsMedicineService;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsRelAttrValuesQueryService = itemsRelAttrValuesQueryService;
		this.objectMapper = objectMapper;
	}

	@Override
	public PendingListItemsBundle loadForPendingList(long companyId, List<Long> distinctItemIdsOrdered) {
		if (distinctItemIdsOrdered == null || distinctItemIdsOrdered.isEmpty()) {
			return new PendingListItemsBundle(Collections.emptyMap(), Collections.emptyMap());
		}
		List<Items> items = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, distinctItemIdsOrdered);
		List<Map<String, Object>> rowMapsList = new ArrayList<>(items.size());
		LinkedHashMap<Long, Map<String, Object>> itemByItemId = new LinkedHashMap<>();
		for (Items it : items) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			Long iid = it.getItemId();
			row.put("item_id", iid == null ? Long.valueOf(0L) : iid);
			row.put("item_name", it.getItemName() == null ? "" : it.getItemName());
			row.put("item_bn", it.getItemBn() == null ? "" : it.getItemBn());
			row.put("price", it.getPrice() == null ? Integer.valueOf(0) : it.getPrice());
			row.put("pics", resolvePics(it.getPics()));
			row.put("is_medicine", it.getIsMedicine() == null ? Integer.valueOf(0) : it.getIsMedicine());
			row.put("is_prescription", it.getIsPrescription() == null ? Integer.valueOf(0) : it.getIsPrescription());
			row.put("itemId", row.get("item_id"));
			row.put("consumeType", "");
			row.put("itemName", row.get("item_name"));
			row.put("itemBn", row.get("item_bn"));
			row.put("companyId", "");
			row.put("item_main_cat_id", "");
			row.put("nospec", Boolean.FALSE);
			rowMapsList.add(row);
			long key = iid == null ? 0L : iid.longValue();
			itemByItemId.put(key, row);
		}
		itemsMedicineService.enrichFlatItemsWithMedicineData(companyId, rowMapsList);

		List<ItemRelAttributes> rels =
				itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, distinctItemIdsOrdered, "item_spec");
		ItemsRelAttrValuesQueryService.ItemDetailAttrData attrData = itemsRelAttrValuesQueryService.assemble(companyId, rels);
		LinkedHashMap<Long, List<Map<String, Object>>> specLinesByItemId = new LinkedHashMap<>();
		for (ItemRelAttributes rel : rels) {
			long iid = rel.getItemId() == null ? 0L : rel.getItemId().longValue();
			long aid = rel.getAttributeId() == null ? 0L : rel.getAttributeId().longValue();
			Map<Long, Map<String, Object>> nested = attrData.itemSpecNested.get(iid);
			Map<String, Object> ispec = nested == null ? null : nested.get(aid);
			String specName = "";
			String specValueName = "";
			if (ispec != null) {
				Object sn = ispec.get("spec_name");
				Object svn = ispec.get("spec_value_name");
				specName = sn == null ? "" : sn.toString();
				specValueName = svn == null ? "" : svn.toString();
			}
			LinkedHashMap<String, Object> line = new LinkedHashMap<>();
			line.put("spec_name", specName);
			line.put("spec_value_name", specValueName);
			specLinesByItemId.computeIfAbsent(iid, k -> new ArrayList<>()).add(line);
		}
		return new PendingListItemsBundle(itemByItemId, specLinesByItemId);
	}

	private Object resolvePics(String picsRaw) {
		if (!StringUtils.hasText(picsRaw)) {
			return "";
		}
		try {
			return objectMapper.readTree(picsRaw);
		} catch (JsonProcessingException e) {
			return "";
		}
	}
}
