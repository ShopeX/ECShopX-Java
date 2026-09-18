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
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 批量解析 {@code items_rel_attributes} 与属性主档/属性值，产出详情页规格与参数结构。
 */
@Service
public class ItemsRelAttrValuesQueryService {

	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;

	public ItemsRelAttrValuesQueryService(ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository) {
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
	}

	public ItemDetailAttrData assemble(long companyId, List<ItemRelAttributes> relRows) {
		ItemDetailAttrData out = new ItemDetailAttrData();
		if (relRows == null || relRows.isEmpty()) {
			return out;
		}

		Set<Long> attributeValueIds = new LinkedHashSet<>();
		Set<Long> attributeIds = new LinkedHashSet<>();
		Map<String, String> customByItemValueKey = new HashMap<>();
		Map<String, List<String>> imageByItemValueKey = new HashMap<>();

		for (ItemRelAttributes row : relRows) {
			Long av = row.getAttributeValueId();
			if (av != null && av > 0) {
				attributeValueIds.add(av);
				String key = row.getItemId() + "_" + av;
				List<String> parsedImages = parseRelImageUrls(row.getImageUrl());
				if (!imageByItemValueKey.containsKey(key) || imageByItemValueKey.get(key).isEmpty()) {
					imageByItemValueKey.put(key, parsedImages);
				}
				customByItemValueKey.put(key, row.getCustomAttributeValue());
				out.attrValuesCustom.put(av, row.getCustomAttributeValue());
			}
			if (row.getAttributeId() != null && row.getAttributeId() > 0) {
				attributeIds.add(row.getAttributeId());
			}
		}

		Map<Long, ItemsAttributes> attrById = itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, attributeIds).stream()
				.collect(Collectors.toMap(ItemsAttributes::getAttributeId, a -> a, (a, b) -> a, LinkedHashMap::new));

		Map<Long, ItemsAttributeValues> valById = new HashMap<>();
		if (!attributeValueIds.isEmpty()) {
			for (ItemsAttributeValues v : itemsAttributeValuesRepository.listByCompanyAndAttributeValueIdsIn(companyId, attributeValueIds)) {
				valById.put(v.getAttributeValueId(), v);
			}
		}

		Map<Long, ItemSpecDescBuilder> specDescBuilders = new LinkedHashMap<>();

		for (ItemRelAttributes row : relRows) {
			String at = row.getAttributeType();
			long aid = row.getAttributeId() != null ? row.getAttributeId() : 0L;
			ItemsAttributes attrDef = attrById.get(aid);
			if ("brand".equals(at)) {
				out.brand.put("brand_id", aid);
				out.brand.put("goods_brand", attrDef != null && attrDef.getAttributeName() != null ? attrDef.getAttributeName() : "");
				out.brand.put("brand_logo", row.getImageUrl() != null ? row.getImageUrl() : "");
			} else if (aid > 0 && !out.attributeIds.contains(aid)) {
				out.attributeIds.add(aid);
			}

			long avId = row.getAttributeValueId() != null ? row.getAttributeValueId() : 0L;
			String ivKey = row.getItemId() + "_" + avId;
			ItemsAttributeValues valRow = avId > 0 ? valById.get(avId) : null;
			String customName = customByItemValueKey.get(ivKey);
			String baseValName = valRow != null && valRow.getAttributeValue() != null ? valRow.getAttributeValue() : "";
			String displayValName = StringUtils.hasText(customName) ? customName : baseValName;
			List<String> itemImgList = imageByItemValueKey.getOrDefault(ivKey, List.of());
			String specImg = valRow != null && valRow.getImageUrl() != null ? valRow.getImageUrl() : "";
			if (!itemImgList.isEmpty()) {
				specImg = itemImgList.get(0);
			}

			if ("item_params".equals(at) && avId > 0) {
				Map<String, Object> pm = new LinkedHashMap<>();
				pm.put("attribute_id", aid);
				pm.put("attribute_name", attrDef != null && attrDef.getAttributeName() != null ? attrDef.getAttributeName() : "");
				pm.put("attribute_value_id", avId);
				pm.put("attribute_value_name", displayValName);
				out.itemParams.add(pm);
			}

			if ("item_spec".equals(at)) {
				ItemsAttributes specAttrEff = attrDef;
				if (specAttrEff == null) {
					specAttrEff = new ItemsAttributes();
					specAttrEff.setAttributeName("-");
					specAttrEff.setIsImage("false");
				}
				final ItemsAttributes specAttr = specAttrEff;
				ItemSpecDescBuilder sb = specDescBuilders.computeIfAbsent(aid, k -> new ItemSpecDescBuilder(aid, specAttr));
				Map<String, Object> sv = new LinkedHashMap<>();
				sv.put("spec_value_id", avId);
				sv.put("spec_custom_value_name", StringUtils.hasText(customName) ? customName : null);
				sv.put("spec_value_name", displayValName);
				sv.put("item_image_url", itemImgList);
				sv.put("spec_image_url", specImg);
				sb.specValues.put(avId, sv);

				long iid = row.getItemId() != null ? row.getItemId() : 0L;
				Map<String, Object> ispec = new LinkedHashMap<>();
				ispec.put("item_id", iid);
				ispec.put("spec_id", aid);
				ispec.put("spec_value_id", avId);
				ispec.put("spec_name", specAttr.getAttributeName() != null ? specAttr.getAttributeName() : "");
				ispec.put("spec_custom_value_name", StringUtils.hasText(customName) ? customName : null);
				ispec.put("spec_value_name", displayValName);
				ispec.put("item_image_url", itemImgList);
				ispec.put("spec_image_url", specImg);
				out.itemSpecNested.computeIfAbsent(iid, k -> new LinkedHashMap<>()).put(aid, ispec);
			}
		}

		for (ItemSpecDescBuilder sb : specDescBuilders.values()) {
			Map<String, Object> desc = new LinkedHashMap<>();
			desc.put("spec_id", sb.specId);
			desc.put("spec_name", sb.attr.getAttributeName() != null ? sb.attr.getAttributeName() : "");
			boolean isImage = "true".equalsIgnoreCase(String.valueOf(sb.attr.getIsImage()));
			desc.put("is_image", isImage);
			List<Map<String, Object>> vals = new ArrayList<>(sb.specValues.values());
			vals.sort(Comparator.comparing(m -> Objects.toString(m.get("spec_value_id"), "")));
			desc.put("spec_values", vals);
			out.itemSpecDesc.add(desc);
			if (isImage) {
				out.specImages = vals;
			}
		}

		return out;
	}

	private static List<String> parseRelImageUrls(String imageUrl) {
		Object resolved = GoodsItemsListRowMapper.resolvePicsForListRow(imageUrl);
		if (resolved instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o != null) {
					String s = o.toString();
					if (StringUtils.hasText(s)) {
						out.add(s);
					}
				}
			}
			return out;
		}
		if (resolved instanceof String s && StringUtils.hasText(s)) {
			return List.of(s);
		}
		return List.of();
	}

	private static final class ItemSpecDescBuilder {
		final long specId;
		final ItemsAttributes attr;
		final Map<Long, Map<String, Object>> specValues = new LinkedHashMap<>();

		ItemSpecDescBuilder(long specId, ItemsAttributes attr) {
			this.specId = specId;
			this.attr = attr;
		}
	}

	public static final class ItemDetailAttrData {
		public final List<Long> attributeIds = new ArrayList<>();
		public final Map<Long, String> attrValuesCustom = new LinkedHashMap<>();
		public final Map<String, Object> brand = new LinkedHashMap<>();
		public final List<Map<String, Object>> itemParams = new ArrayList<>();
		public final List<Map<String, Object>> itemSpecDesc = new ArrayList<>();
		public List<Map<String, Object>> specImages = List.of();
		/** itemId -> (specAttributeId -> spec row map) */
		public final Map<Long, Map<Long, Map<String, Object>>> itemSpecNested = new LinkedHashMap<>();
	}
}
