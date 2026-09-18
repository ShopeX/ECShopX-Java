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

package cn.shopex.ecshopx.goods.service.ome;

import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OmeItemsFromOmeCategoryBrandSpecResolver {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ObjectMapper objectMapper;

	public OmeItemsFromOmeCategoryBrandSpecResolver(
			ItemsCategoryRepository itemsCategoryRepository,
			ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ObjectMapper objectMapper) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.objectMapper = objectMapper;
	}

	public long getItemCategoryId(long companyId, Map<String, Object> row) {
		Object rawCode = row.get("cat_code");
		String code = rawCode != null ? String.valueOf(rawCode).trim() : "";
		Optional<ItemsCategory> opt = itemsCategoryRepository.getByCategoryCode(code, companyId);
		if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getIsMainCategory()) || !Integer.valueOf(3).equals(opt.get().getCategoryLevel())) {
			throw new cn.shopex.ecshopx.common.exception.ResourceException("商品分类[" + code + "]不存在");
		}
		return opt.get().getCategoryId();
	}

	public long getBrandId(long companyId, Map<String, Object> row) {
		Object raw = row.get("goods_brand");
		String brandName = raw != null ? String.valueOf(raw) : "";
		if (brandName.isEmpty()) {
			return 0L;
		}
		Optional<Long> id = itemsAttributesRepository.findOmsBrandAttributeIdByCompanyAndName(companyId, brandName);
		if (id.isEmpty()) {
			throw new cn.shopex.ecshopx.common.exception.ResourceException("品牌名称[" + brandName + "]不存在");
		}
		return id.get();
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getItemSpec(long companyId, Map<String, Object> row) {
		Object specRaw = row.get("spec_info");
		Map<String, Object> specMap = coerceSpecMap(specRaw);
		if (specMap.isEmpty()) {
			return List.of();
		}
		List<String> attributeCodes = new ArrayList<>();
		List<String> attributeValues = new ArrayList<>();
		for (Map.Entry<String, Object> e : specMap.entrySet()) {
			attributeCodes.add(e.getKey());
			attributeValues.add(e.getValue() != null ? String.valueOf(e.getValue()).trim() : "");
		}
		List<ItemsAttributes> attrList = itemsAttributesRepository.listByCompanyAndItemSpecAttributeCodesIn(companyId, attributeCodes);
		if (attrList.size() != attributeCodes.size()) {
			String joined = String.join("、", attributeCodes);
			throw new cn.shopex.ecshopx.common.exception.ResourceException("存在无效的商品规格[" + joined + "]");
		}
		List<Long> attributeIds = attrList.stream().map(ItemsAttributes::getAttributeId).collect(Collectors.toList());
		List<ItemsAttributeValues> valueRows = itemsAttributeValuesRepository.listByCompanyAttributeIdsAndAttributeValuesIn(companyId, attributeIds, attributeValues);
		if (valueRows.size() != attributeValues.size()) {
			String joined = String.join("、", attributeValues);
			throw new cn.shopex.ecshopx.common.exception.ResourceException("存在无效的商品规格值[" + joined + "]");
		}
		List<Map<String, Object>> data = new ArrayList<>();
		for (ItemsAttributeValues vr : valueRows) {
			Map<String, Object> one = new LinkedHashMap<>();
			one.put("spec_id", vr.getAttributeId());
			one.put("spec_value_id", vr.getAttributeValueId());
			data.add(one);
		}
		data.sort(Comparator.comparingLong(m -> ((Number) m.get("spec_id")).longValue()));
		return data;
	}

	private Map<String, Object> coerceSpecMap(Object raw) {
		if (raw == null) {
			return Map.of();
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return Map.of();
			}
			try {
				return objectMapper.readValue(s, new TypeReference<LinkedHashMap<String, Object>>() {
				});
			} catch (Exception e) {
				return Map.of();
			}
		}
		if (!(raw instanceof Map<?, ?>)) {
			return Map.of();
		}
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}
}
