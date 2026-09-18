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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeePurchaseItemsSkuDetailService {

	private final ItemsMapper itemsMapper;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsSkuListAssembler itemsSkuListAssembler;
	private final ItemsMedicineService itemsMedicineService;

	public EmployeePurchaseItemsSkuDetailService(
			ItemsMapper itemsMapper,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsSkuListAssembler itemsSkuListAssembler,
			ItemsMedicineService itemsMedicineService) {
		this.itemsMapper = itemsMapper;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsSkuListAssembler = itemsSkuListAssembler;
		this.itemsMedicineService = itemsMedicineService;
	}

	public Map<String, Object> loadSkuDetailMap(long companyId, long itemId) {
		if (companyId <= 0L) {
			throw new BadRequestException("公司ID必填");
		}
		if (itemId <= 0L) {
			throw new BadRequestException("商品ID必填");
		}
		Items item = itemsMapper.selectById(itemId);
		if (item == null) {
			throw new ResourceException("商品不存在");
		}

		Map<String, Object> rowMap = itemsRowToMap(item);

		String nospec = item.getNospec();
		boolean multiSpec =
				"false".equalsIgnoreCase(nospec) || "0".equals(nospec);
		if (multiSpec) {
			List<ItemRelAttributes> specRows =
					itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(
							companyId, List.of(itemId), "item_spec");
			for (ItemRelAttributes row : specRows) {
				if (row.getItemId() != null
						&& row.getItemId().equals(itemId)
						&& StringUtils.hasText(row.getImageUrl())) {
					rowMap.put("pics", row.getImageUrl());
				}
			}
		}

		String dbItemType = item.getItemType();
		String effectiveItemType =
				(dbItemType == null || dbItemType.isEmpty()) ? "services" : dbItemType;
		rowMap.put("item_type", effectiveItemType);

		rowMap.put("type_labels", new ArrayList<Map<String, Object>>());
		if ("services".equals(effectiveItemType)) {
			itemsSkuListAssembler.applyTypeLabels(List.of(rowMap));
		}

		itemsMedicineService.applyMedicineDataToRows(companyId, List.of(rowMap));
		Object maxNum = rowMap.get("max_num");
		Map<String, Object> medicineData = new LinkedHashMap<>();
		if (maxNum != null) {
			medicineData.put("max_num", maxNum);
		}
		rowMap.put("medicine_data", medicineData);

		return rowMap;
	}

	private static Map<String, Object> itemsRowToMap(Items item) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (item.getItemId() != null) {
			m.put("item_id", item.getItemId());
		}
		if (item.getCompanyId() != null) {
			m.put("company_id", item.getCompanyId());
		}
		if (item.getApproveStatus() != null) {
			m.put("approve_status", item.getApproveStatus());
		}
		if (item.getStore() != null) {
			m.put("store", item.getStore());
		}
		if (item.getItemType() != null) {
			m.put("item_type", item.getItemType());
		}
		if (item.getNospec() != null) {
			m.put("nospec", item.getNospec());
		}
		if (item.getIsMedicine() != null) {
			m.put("is_medicine", item.getIsMedicine());
		}
		return m;
	}
}
