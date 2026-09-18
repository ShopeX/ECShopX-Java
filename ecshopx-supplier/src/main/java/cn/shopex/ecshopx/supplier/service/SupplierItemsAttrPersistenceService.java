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

package cn.shopex.ecshopx.supplier.service;

import cn.shopex.ecshopx.supplier.domain.SupplierItemsAttr;
import cn.shopex.ecshopx.supplier.mapper.SupplierItemsAttrMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SupplierItemsAttrPersistenceService {

	private final SupplierItemsAttrMapper mapper;
	private final ObjectMapper objectMapper;

	public SupplierItemsAttrPersistenceService(SupplierItemsAttrMapper mapper, ObjectMapper objectMapper) {
		this.mapper = mapper;
		this.objectMapper = objectMapper;
	}

	public void markDelByFilter(long companyId, long itemId, long attributeId, String attributeType) {
		LambdaQueryWrapper<SupplierItemsAttr> w = baseFilter(companyId, itemId, attributeId, attributeType);
		SupplierItemsAttr one = mapper.selectOne(w);
		if (one != null) {
			LambdaUpdateWrapper<SupplierItemsAttr> u = new LambdaUpdateWrapper<>();
			u.eq(SupplierItemsAttr::getId, one.getId()).set(SupplierItemsAttr::getIsDel, 1L);
			mapper.update(null, u);
		}
	}

	public void execDelData(long companyId, long itemId, long attributeId, String attributeType) {
		LambdaQueryWrapper<SupplierItemsAttr> w = baseFilter(companyId, itemId, attributeId, attributeType);
		w.eq(SupplierItemsAttr::getIsDel, 1L);
		mapper.delete(w);
	}

	public void saveAttrData(long companyId, long itemId, long attributeId, String attributeType, Map<String, Object> attrData) {
		String json;
		try {
			json = objectMapper.writeValueAsString(attrData);
		} catch (Exception e) {
			json = "{}";
		}
		LambdaQueryWrapper<SupplierItemsAttr> w = baseFilter(companyId, itemId, attributeId, attributeType);
		SupplierItemsAttr existing = mapper.selectOne(w);
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (existing != null) {
			LambdaUpdateWrapper<SupplierItemsAttr> u = new LambdaUpdateWrapper<>();
			u.eq(SupplierItemsAttr::getId, existing.getId()).set(SupplierItemsAttr::getAttrData, json).set(SupplierItemsAttr::getIsDel, 0L).set(SupplierItemsAttr::getUpdated, now);
			mapper.update(null, u);
		} else {
			SupplierItemsAttr row = new SupplierItemsAttr();
			row.setCompanyId(companyId);
			row.setItemId(itemId);
			row.setAttributeId(attributeId);
			row.setAttributeType(attributeType);
			row.setAttrData(json);
			row.setIsDel(0L);
			row.setCreated(now);
			row.setUpdated(now);
			mapper.insert(row);
		}
	}

	public void saveCategoryAttr(long companyId, long defaultItemId, java.util.List<Long> categoryIds) {
		List<String> catIds = new ArrayList<>();
		for (Long id : categoryIds) {
			if (id != null && id > 0L) {
				catIds.add(String.valueOf(id));
			}
		}
		saveAttrData(companyId, defaultItemId, 0L, "category", Map.of("category", catIds));
	}

	public void saveBrandAttr(long companyId, long defaultItemId, long brandId) {
		saveAttrData(companyId, defaultItemId, brandId, "brand", Map.of("brand", String.valueOf(brandId)));
	}

	public void saveItemSpecAttr(long companyId, long itemId, List<Map<String, Object>> specRows, Map<Long, Object> specImages) {
		if (specRows == null || specRows.isEmpty()) {
			return;
		}
		markDelByCompanyItemAndAttributeType(companyId, itemId, "item_spec");
		int sort = 0;
		for (Map<String, Object> row : specRows) {
			long specId = toLong(row.get("spec_id"));
			long specValueId = toLong(row.get("spec_value_id"));
			Object imageUrl = specImages != null ? specImages.get(specValueId) : "";
			if (imageUrl == null) {
				imageUrl = "";
			}
			int tempSort = row.get("attribute_sort") != null ? (int) toLong(row.get("attribute_sort")) : 0;
			Map<String, Object> itemSpec = new LinkedHashMap<>();
			itemSpec.put("attribute_sort", tempSort + sort);
			itemSpec.put("image_url", imageUrl);
			itemSpec.put("attribute_value_id", specValueId);
			itemSpec.put("custom_attribute_value", row.get("spec_custom_value_name"));
			saveAttrData(companyId, itemId, specId, "item_spec", Map.of("item_spec", itemSpec));
			sort++;
		}
		execDelByCompanyItemAndAttributeType(companyId, itemId, "item_spec");
	}

	public void saveItemParamsAttr(long companyId, long defaultItemId, List<Map<String, Object>> itemParams) {
		if (itemParams == null || itemParams.isEmpty()) {
			return;
		}
		markDelByCompanyItemAndAttributeType(companyId, defaultItemId, "item_params");
		for (Map<String, Object> row : itemParams) {
			long attributeId = 0L;
			Object aid = row.get("attribute_id");
			if (aid != null) {
				attributeId = aid instanceof Number n ? n.longValue() : Long.parseLong(aid.toString().trim());
			}
			Map<String, Object> attrData = new LinkedHashMap<>();
			attrData.put("item_params", row);
			saveAttrData(companyId, defaultItemId, attributeId, "item_params", attrData);
		}
		execDelByCompanyItemAndAttributeType(companyId, defaultItemId, "item_params");
	}

	public void markDelByCompanyItemAndAttributeType(long companyId, long itemId, String attributeType) {
		LambdaUpdateWrapper<SupplierItemsAttr> u = new LambdaUpdateWrapper<>();
		u.eq(SupplierItemsAttr::getCompanyId, companyId).eq(SupplierItemsAttr::getItemId, itemId)
				.eq(SupplierItemsAttr::getAttributeType, attributeType).eq(SupplierItemsAttr::getIsDel, 0L).set(SupplierItemsAttr::getIsDel, 1L);
		mapper.update(null, u);
	}

	public void execDelByCompanyItemAndAttributeType(long companyId, long itemId, String attributeType) {
		LambdaQueryWrapper<SupplierItemsAttr> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItemsAttr::getCompanyId, companyId).eq(SupplierItemsAttr::getItemId, itemId)
				.eq(SupplierItemsAttr::getAttributeType, attributeType).eq(SupplierItemsAttr::getIsDel, 1L);
		mapper.delete(w);
	}

	/**
	 * Lists non-deleted supplier attribute rows for one SKU and type (e.g. {@code item_spec}, {@code category},
	 * {@code brand}, {@code item_params}).
	 */
	public List<SupplierItemsAttr> listByCompanyIdAndItemIdAndAttributeType(long companyId, long itemId, String attributeType) {
		LambdaQueryWrapper<SupplierItemsAttr> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItemsAttr::getCompanyId, companyId).eq(SupplierItemsAttr::getItemId, itemId).eq(SupplierItemsAttr::getAttributeType, attributeType)
				.eq(SupplierItemsAttr::getIsDel, 0L);
		return mapper.selectList(w);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static LambdaQueryWrapper<SupplierItemsAttr> baseFilter(long companyId, long itemId, long attributeId, String attributeType) {
		LambdaQueryWrapper<SupplierItemsAttr> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItemsAttr::getCompanyId, companyId).eq(SupplierItemsAttr::getItemId, itemId).eq(SupplierItemsAttr::getAttributeId, attributeId)
				.eq(SupplierItemsAttr::getAttributeType, attributeType);
		return w;
	}
}
