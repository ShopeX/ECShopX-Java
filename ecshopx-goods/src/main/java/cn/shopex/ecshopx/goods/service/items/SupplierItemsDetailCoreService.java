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
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.service.items.ItemsRelAttrValuesQueryService.ItemDetailAttrData;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsAttr;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsAttrListRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierItemsDetailCoreService {

	private final SupplierItemsRepository supplierItemsRepository;
	private final SupplierItemsAttrListRepository supplierItemsAttrListRepository;
	private final ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService;
	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final ItemsCategoryPathByItemService itemsCategoryPathByItemService;
	private final ItemsDetailIntroArticleService itemsDetailIntroArticleService;
	private final ItemsDetailVideoPicService itemsDetailVideoPicService;
	private final ItemDetailScalarHelper itemDetailScalarHelper;
	private final ObjectMapper objectMapper;
	private final ItemsMedicineService itemsMedicineService;

	public SupplierItemsDetailCoreService(SupplierItemsRepository supplierItemsRepository,
			SupplierItemsAttrListRepository supplierItemsAttrListRepository,
			ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService, PlatformItemsDetailCoreService platformItemsDetailCoreService,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository, ItemsCategoryPathByItemService itemsCategoryPathByItemService,
			ItemsDetailIntroArticleService itemsDetailIntroArticleService, ItemsDetailVideoPicService itemsDetailVideoPicService,
			ItemDetailScalarHelper itemDetailScalarHelper, ObjectMapper objectMapper, ItemsMedicineService itemsMedicineService) {
		this.supplierItemsRepository = supplierItemsRepository;
		this.supplierItemsAttrListRepository = supplierItemsAttrListRepository;
		this.itemsRelAttrValuesQueryService = itemsRelAttrValuesQueryService;
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.itemsCategoryPathByItemService = itemsCategoryPathByItemService;
		this.itemsDetailIntroArticleService = itemsDetailIntroArticleService;
		this.itemsDetailVideoPicService = itemsDetailVideoPicService;
		this.itemDetailScalarHelper = itemDetailScalarHelper;
		this.objectMapper = objectMapper;
		this.itemsMedicineService = itemsMedicineService;
	}

	public Map<String, Object> build(long companyId, long itemId, String authorizerAppId) {
		return build(companyId, itemId, authorizerAppId, List.of());
	}

	public Map<String, Object> build(long companyId, long itemId, String authorizerAppId, List<Long> limitItemIds) {
		SupplierItems si = supplierItemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (si == null) {
			return Map.of();
		}
		Map<String, Object> detail = SupplierItemsDetailRowMapper.toDetailMap(si);
		detail.put("data_source", "supplier_goods");
		detail.put("approve_status", "");
		itemDetailScalarHelper.normalizeDetailScalars(detail);

		boolean multiSpec = itemDetailScalarHelper.isMultiSpec(detail.get("nospec"));
		List<SupplierItems> skuRows = List.of();
		List<Long> skuIds = List.of(itemId);
		if (multiSpec) {
			long def = si.getDefaultItemId() != null ? si.getDefaultItemId() : itemId;
			skuRows = supplierItemsRepository.listByDefaultItemIdAndCompany(def, companyId);
			skuRows = filterSupplierSkuRowsByLimitItemIds(skuRows, limitItemIds);
			skuIds = skuRows.stream().map(SupplierItems::getItemId).collect(Collectors.toList());
		}

		long defaultItemId = si.getDefaultItemId() != null ? si.getDefaultItemId() : itemId;
		List<ItemRelAttributes> relAll = new ArrayList<>();
		relAll.addAll(expandSupplierAttrs(companyId, supplierItemsAttrListRepository.listByCompanyAndItemIdAllTypes(companyId, defaultItemId)));
		if (multiSpec) {
			relAll.addAll(expandSupplierAttrs(companyId,
					supplierItemsAttrListRepository.listByCompanyItemIdsAndAttributeType(companyId, skuIds, "item_spec")));
		}

		detail.put("spec_pics", List.of());
		for (ItemRelAttributes ra : relAll) {
			if (!"item_spec".equals(ra.getAttributeType()) || ra.getItemId() == null || !ra.getItemId().equals(itemId)) {
				continue;
			}
			Object pics = GoodsItemsListRowMapper.resolvePicsForListRow(ra.getImageUrl());
			if (pics instanceof List<?> list && !list.isEmpty()) {
				detail.put("spec_pics", pics);
			} else if (pics instanceof String s && StringUtils.hasText(s)) {
				detail.put("spec_pics", s);
			}
		}

		ItemDetailAttrData ad = itemsRelAttrValuesQueryService.assemble(companyId, relAll);
		if (!ad.brand.isEmpty()) {
			detail.put("brand_id", ad.brand.get("brand_id"));
			detail.put("goods_brand", ad.brand.get("goods_brand"));
			detail.put("brand_logo", ad.brand.get("brand_logo"));
		}
		detail.put("attribute_ids", ad.attributeIds);
		if (ad.attrValuesCustom == null || ad.attrValuesCustom.isEmpty()) {
			detail.put("attr_values_custom", new ArrayList<>());
		} else {
			detail.put("attr_values_custom", new LinkedHashMap<>(ad.attrValuesCustom));
		}

		if (ad.itemParams.isEmpty()) {
			itemDetailScalarHelper.applyFallbackItemParams(detail);
		} else {
			detail.put("item_params", ad.itemParams);
		}
		detail.put("item_spec_desc", ad.itemSpecDesc);
		detail.put("spec_images", ad.specImages);

		List<Items> skuAsItems = skuRows.stream().map(SupplierItemsDetailRowMapper::toPseudoPlatformItem).collect(Collectors.toList());
		Items mainPseudo = SupplierItemsDetailRowMapper.toPseudoPlatformItem(si);
		platformItemsDetailCoreService.applyItemSpecBlockForSupplier(detail, ad, skuAsItems, companyId, mainPseudo, multiSpec);

		detail.put("item_category", loadSupplierCategoryIds(companyId, itemId));
		long mainCat = parseMainCategoryId(detail.get("item_main_cat_id"));
		if (mainCat > 0) {
			List<Map<String, Object>> mainPath = itemsCategoryPathByItemService.getCategoryPathById(companyId, mainCat, true);
			detail.put("item_category_main", mainPath);
			if (mainPath.isEmpty()) {
				detail.put("item_main_cat_id", "");
			}
		} else {
			detail.put("item_category_main", List.of());
		}
		long firstSaleCat = firstCategoryId(detail.get("item_category"));
		if (firstSaleCat > 0) {
			detail.put("item_category_info", itemsCategoryPathByItemService.getCategoryPathById(companyId, firstSaleCat, false));
		} else {
			detail.put("item_category_info", List.of());
		}

		detail.put("intro", itemsDetailIntroArticleService.pro(detail.get("intro"), authorizerAppId, 0L, "supplier_goods", companyId));
		String vt = detail.get("video_type") != null ? detail.get("video_type").toString() : "";
		if (StringUtils.hasText(str(detail.get("videos"))) && StringUtils.hasText(authorizerAppId) && !"tencent".equals(vt)) {
			itemsDetailVideoPicService.applyVideoUrl(detail, authorizerAppId);
		} else {
			detail.put("videos_url", "");
		}
		if ("tencent".equals(vt)) {
			detail.put("tencent_vid", detail.get("videos"));
		}

		detail.put("distributor_sale_status", true);
		detail.put("item_total_store", detail.getOrDefault("item_total_store", si.getStore()));
		detail.put("item_total_sales", detail.getOrDefault("item_total_sales", si.getSales()));
		detail.put("distributor_info", List.of());

		detail.put("tax_rate", 0);
		detail.put("cross_border_tax", 0);

		detail.put("templates_name", "");
		Integer tid = si.getTemplatesId();
		if (tid != null && tid > 0) {
			shippingTemplatesQueryRepository.findTemplateName(tid.longValue(), companyId).ifPresent(name -> detail.put("templates_name", name));
		}

		detail.put("brand_id", String.valueOf(detail.getOrDefault("brand_id", 0)));
		ensurePicsCreateQrcode(detail);
		SupplierItemsDetailRowMapper.applySupplierDetailWireScalars(detail);
		itemsMedicineService.applyMedicineDataToRows(companyId, List.of(detail));
		return detail;
	}

	private static long parseMainCategoryId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw != null && StringUtils.hasText(raw.toString())) {
			try {
				return Long.parseLong(raw.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return 0L;
	}

	private static long firstCategoryId(Object raw) {
		if (raw instanceof List<?> list && !list.isEmpty()) {
			return parseMainCategoryId(list.get(0));
		}
		return 0L;
	}

	private void ensurePicsCreateQrcode(Map<String, Object> detail) {
		Object pics = detail.get("pics");
		if (!(pics instanceof List<?> picList) || picList.isEmpty()) {
			return;
		}
		Object existing = detail.get("pics_create_qrcode");
		if (existing instanceof List<?> el && !el.isEmpty()) {
			return;
		}
		List<Boolean> flags = new ArrayList<>();
		for (int i = 0; i < picList.size(); i++) {
			flags.add(Boolean.FALSE);
		}
		detail.put("pics_create_qrcode", flags);
	}

	private List<String> loadSupplierCategoryIds(long companyId, long itemId) {
		List<SupplierItemsAttr> rows = supplierItemsAttrListRepository.listByCompanyItemIdsAndAttributeType(companyId, List.of(itemId), "category");
		for (SupplierItemsAttr a : rows) {
			if (!StringUtils.hasText(a.getAttrData())) {
				continue;
			}
			try {
				Map<String, Object> json = objectMapper.readValue(a.getAttrData(), new TypeReference<Map<String, Object>>() {});
				Object cat = json.get("category");
				if (cat instanceof List<?> list) {
					List<String> out = new ArrayList<>();
					for (Object o : list) {
						if (o != null) {
							out.add(o.toString());
						}
					}
					return out;
				}
			} catch (Exception ignored) {
			}
		}
		return List.of();
	}

	private List<ItemRelAttributes> expandSupplierAttrs(long companyId, List<SupplierItemsAttr> rows) {
		List<ItemRelAttributes> out = new ArrayList<>();
		for (SupplierItemsAttr row : rows) {
			if (!StringUtils.hasText(row.getAttrData())) {
				continue;
			}
			try {
				Map<String, Object> root = objectMapper.readValue(row.getAttrData(), new TypeReference<Map<String, Object>>() {});
				String at = row.getAttributeType();
				Object inner = root.get(at);
				Long rowAttributeId = row.getAttributeId();
				if ("item_params".equals(at) && inner instanceof Map<?, ?> pm && pm.get("params") instanceof List<?> list) {
					for (Object o : list) {
						if (o instanceof Map<?, ?> m) {
							out.add(syntheticRel(companyId, row.getItemId(), "item_params", (Map<String, Object>) m, rowAttributeId));
						}
					}
					continue;
				}
				if (inner instanceof List<?> list) {
					for (Object o : list) {
						if (o instanceof Map<?, ?> m) {
							out.add(syntheticRel(companyId, row.getItemId(), at, (Map<String, Object>) m, rowAttributeId));
						}
					}
				} else if (inner instanceof Map<?, ?> m) {
					out.add(syntheticRel(companyId, row.getItemId(), at, (Map<String, Object>) m, rowAttributeId));
				} else if ("brand".equals(at) && inner != null) {
					ItemRelAttributes rel = new ItemRelAttributes();
					rel.setCompanyId(companyId);
					rel.setItemId(row.getItemId());
					rel.setAttributeType("brand");
					rel.setAttributeId(inner instanceof Number ? ((Number) inner).longValue() : Long.parseLong(inner.toString()));
					rel.setAttributeValueId(0L);
					out.add(rel);
				}
			} catch (Exception ignored) {
			}
		}
		return out;
	}

	private ItemRelAttributes syntheticRel(long companyId, long itemId, String attributeType, Map<String, Object> m,
			Long rowAttributeId) {
		ItemRelAttributes rel = new ItemRelAttributes();
		rel.setCompanyId(companyId);
		rel.setItemId(itemId);
		rel.setAttributeType(attributeType);
		long attributeId = longVal(m.get("attribute_id"), 0L);
		if (attributeId <= 0L && "item_spec".equals(attributeType)) {
			attributeId = longVal(m.get("spec_id"), 0L);
		}
		if (attributeId <= 0L && rowAttributeId != null && rowAttributeId > 0L) {
			attributeId = rowAttributeId;
		}
		rel.setAttributeId(attributeId);
		long attributeValueId = longVal(m.get("attribute_value_id"), 0L);
		if (attributeValueId <= 0L && "item_spec".equals(attributeType)) {
			attributeValueId = longVal(m.get("spec_value_id"), 0L);
		}
		rel.setAttributeValueId(attributeValueId);
		Object c = m.get("custom_attribute_value");
		rel.setCustomAttributeValue(c != null ? c.toString() : null);
		rel.setImageUrl(encodeRelImageUrl(m.get("image_url")));
		Object sort = m.get("attribute_sort");
		rel.setAttributeSort(sort instanceof Number ? ((Number) sort).intValue() : 0);
		return rel;
	}

	private String encodeRelImageUrl(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return null;
			}
			try {
				return objectMapper.writeValueAsString(list);
			} catch (JsonProcessingException e) {
				throw new ResourceException("规格图片数据解析失败");
			}
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "[]".equals(t)) {
				return null;
			}
			if (t.startsWith("[")) {
				return t;
			}
			try {
				return objectMapper.writeValueAsString(List.of(t));
			} catch (JsonProcessingException e) {
				throw new ResourceException("规格图片数据解析失败");
			}
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			throw new ResourceException("规格图片数据解析失败");
		}
	}

	private static long longVal(Object o, long def) {
		if (o == null) {
			return def;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static List<SupplierItems> filterSupplierSkuRowsByLimitItemIds(List<SupplierItems> skuRows, List<Long> limitItemIds) {
		if (limitItemIds == null || limitItemIds.isEmpty() || skuRows == null || skuRows.isEmpty()) {
			return skuRows == null ? List.of() : skuRows;
		}
		Set<Long> allowed = new LinkedHashSet<>(limitItemIds);
		Map<Long, SupplierItems> byId =
				skuRows.stream().filter(s -> s.getItemId() != null).collect(Collectors.toMap(SupplierItems::getItemId, s -> s, (a, b) -> a, LinkedHashMap::new));
		List<SupplierItems> ordered = new ArrayList<>();
		for (Long id : limitItemIds) {
			SupplierItems it = byId.get(id);
			if (it != null) {
				ordered.add(it);
			}
		}
		if (ordered.isEmpty()) {
			return skuRows.stream().filter(s -> s.getItemId() != null && allowed.contains(s.getItemId())).collect(Collectors.toList());
		}
		return ordered;
	}
}
