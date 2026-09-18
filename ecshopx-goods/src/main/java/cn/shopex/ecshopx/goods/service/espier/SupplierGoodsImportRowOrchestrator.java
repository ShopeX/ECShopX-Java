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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.items.SupplierItemsAddService;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsAttr;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import cn.shopex.ecshopx.supplier.service.SupplierItemsAttrPersistenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierGoodsImportRowOrchestrator {

	private static final Map<String, Integer> IS_MARKET_LABEL = Map.of("可售", 1, "不可售", 0);
	private static final ConcurrentHashMap<Long, Session> SESSION = new ConcurrentHashMap<>();

	private final SupplierItemsRepository supplierItemsRepository;
	private final SupplierItemsAddService supplierItemsAddService;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final SupplierSaleCategoryPathIdsResolver supplierSaleCategoryPathIdsResolver;
	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final SupplierItemsAttrPersistenceService supplierItemsAttrPersistenceService;
	private final ObjectMapper objectMapper;

	public SupplierGoodsImportRowOrchestrator(
			SupplierItemsRepository supplierItemsRepository,
			SupplierItemsAddService supplierItemsAddService,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			ItemsCategoryRepository itemsCategoryRepository,
			SupplierSaleCategoryPathIdsResolver supplierSaleCategoryPathIdsResolver,
			ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			SupplierItemsAttrPersistenceService supplierItemsAttrPersistenceService,
			ObjectMapper objectMapper) {
		this.supplierItemsRepository = supplierItemsRepository;
		this.supplierItemsAddService = supplierItemsAddService;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.supplierSaleCategoryPathIdsResolver = supplierSaleCategoryPathIdsResolver;
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.supplierItemsAttrPersistenceService = supplierItemsAttrPersistenceService;
		this.objectMapper = objectMapper;
	}

	public void acceptRow(long companyId, long supplierIdCtx, long distributorIdCtx, Map<String, Object> row) {
		Map<String, String> r = trimRow(row);
		long supplierId = supplierIdCtx > 0L ? supplierIdCtx : parseLong(r.get("supplier_id"));
		long distributorId = parseLong(r.get("distributor_id"));
		if (distributorId <= 0L) {
			distributorId = distributorIdCtx > 0L ? distributorIdCtx : 0L;
		}
		boolean supplierMode = supplierId > 0L;
		String itemBn = r.get("item_bn");

		if (StringUtils.hasText(itemBn)) {
			SupplierItems old = supplierItemsRepository.findByItemBnAndCompany(itemBn, companyId);
			if (old != null) {
				if (supplierMode && old.getSupplierId() != null && old.getSupplierId().intValue() != supplierId) {
					throw new ResourceException("商品编码已存在其他供应商，不能更新");
				}
				Map<String, Object> merged = buildUpdateParams(companyId, supplierId, distributorId, supplierMode, r, old);
				supplierItemsAddService.addItemsTransactional(merged);
				return;
			}
		}

		validateCreateRequired(r, supplierMode);

		ItemsCategory mainCategory = resolveMainCategoryLeaf(companyId, r.get("item_main_category"));
		long templateId = resolveTemplateId(companyId, r.get("templates_id"), supplierId, distributorId);
		List<Long> saleCatIds = resolveSaleCategories(companyId, distributorId, r.get("item_category"), supplierMode);

		boolean nospec = !StringUtils.hasText(r.get("item_spec"));
		Session session = sessionOf(row);
		boolean isCreateRelData = true;
		Long defaultItemId = null;
		if (!nospec && session != null) {
			String nm = r.get("item_name").trim();
			if (session.itemName != null && nm.equals(session.itemName)) {
				isCreateRelData = false;
				defaultItemId = session.defaultItemId;
			}
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("supplier_id", supplierId);
		merged.put("operator_type", supplierMode ? "supplier" : "");
		merged.put("item_type", "normal");
		merged.put("audit_status", "submitting");
		merged.put("distributor_id", distributorId);
		merged.put("item_main_cat_id", mainCategory.getCategoryId());
		merged.put("item_name", r.get("item_name"));
		merged.put("item_bn", r.get("item_bn"));
		merged.put("goods_bn", r.get("goods_bn"));
		merged.put("brief", r.get("brief"));
		merged.put("price", parseYuan(r.get("price")));
		merged.put("cost_price", parseYuan(r.get("cost_price")));
		merged.put("market_price", parseYuan(r.get("market_price")));
		merged.put("store", parseStore(r.get("store")));
		merged.put("pics", splitCommaList(r.get("pics")));
		merged.put("intro", buildIntroHtml(r.get("intro")));
		merged.put("videos", r.get("videos"));
		merged.put("item_category", saleCatIds);
		if (templateId > Integer.MAX_VALUE) {
			throw new BadRequestException("运费模板ID超出范围");
		}
		merged.put("templates_id", (int) templateId);
		merged.put("weight", parseWeight(r.get("weight")));
		merged.put("barcode", r.get("barcode"));
		merged.put("item_unit", r.get("item_unit"));
		merged.put("nospec", nospec ? "true" : "false");
		merged.put("is_default", isCreateRelData);
		merged.put("isCreateRelData", isCreateRelData);
		merged.put("approve_status", "onsale");
		merged.put("start_num", parseStartNum(r.get("start_num")));
		if (defaultItemId != null) {
			merged.put("default_item_id", defaultItemId);
		}
		if (session != null && session.goodsId != null && session.goodsId > 0L && !isCreateRelData) {
			merged.put("goods_id", session.goodsId);
		}
		if (supplierMode) {
			merged.put("is_market", parseIsMarketRequired(r.get("is_market")));
		} else if (StringUtils.hasText(r.get("is_market"))) {
			merged.put("is_market", parseIsMarketRequired(r.get("is_market")));
		}
		String brand = r.get("goods_brand");
		if (StringUtils.hasText(brand)) {
			int bid = itemsAttributesRepository
					.findFirstBrandByCompanyAndName(companyId, brand)
					.map(a -> a.getAttributeId() != null ? a.getAttributeId().intValue() : 0)
					.orElse(0);
			if (bid <= 0) {
				throw new BadRequestException(brand + " 品牌名称不存在");
			}
			merged.put("brand_id", bid);
		}

		if (!nospec) {
			r.put("default_item_id", defaultItemId != null ? String.valueOf(defaultItemId) : "");
			List<Map<String, Object>> itemSpec = getItemSpec(companyId, r, mainCategory);
			Map<String, Object> specItem = new LinkedHashMap<>();
			specItem.put("item_bn", r.get("item_bn"));
			specItem.put("weight", parseWeight(r.get("weight")));
			specItem.put("barcode", r.get("barcode"));
			specItem.put("price", parseYuan(r.get("price")));
			specItem.put("cost_price", parseYuan(r.get("cost_price")));
			specItem.put("market_price", parseYuan(r.get("market_price")));
			specItem.put("item_unit", r.get("item_unit"));
			specItem.put("store", parseStore(r.get("store")));
			specItem.put("is_default", isCreateRelData);
			if (defaultItemId != null) {
				specItem.put("default_item_id", defaultItemId);
			}
			specItem.put("item_spec", itemSpec);
			specItem.put("approve_status", "onsale");
			merged.put("spec_items", List.of(specItem));
			List<Map<String, Object>> specImages = getItemSpecImages(companyId, r, mainCategory);
			if (!specImages.isEmpty()) {
				merged.put("spec_images", specImages);
			}
		}

		supplierItemsAddService.addItemsTransactional(merged);

		if (!nospec && session != null && isCreateRelData && StringUtils.hasText(itemBn)) {
			SupplierItems created = supplierItemsRepository.findByItemBnAndCompany(itemBn.trim(), companyId);
			if (created != null && created.getItemId() != null) {
				session.defaultItemId = created.getItemId();
				session.itemName = r.get("item_name").trim();
				session.goodsId = created.getGoodsId() != null && created.getGoodsId() > 0L
						? created.getGoodsId()
						: created.getItemId();
			}
		}
	}

	private Map<String, Object> buildUpdateParams(
			long companyId,
			long supplierId,
			long distributorId,
			boolean supplierMode,
			Map<String, String> r,
			SupplierItems old) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("supplier_id", supplierId);
		merged.put("operator_type", supplierMode ? "supplier" : "");
		merged.put("item_type", nz(old.getItemType(), "normal"));
		merged.put("audit_status", "submitting");
		merged.put("distributor_id", old.getDistributorId() != null ? old.getDistributorId().longValue() : distributorId);
		merged.put("item_id", old.getItemId());
		merged.put("goods_id", old.getGoodsId());
		merged.put("default_item_id", old.getDefaultItemId());
		merged.put("item_name", nz(old.getItemName(), ""));
		merged.put("item_bn", nz(old.getItemBn(), ""));
		merged.put("brief", nz(old.getBrief(), ""));
		merged.put("price", fenToYuan(old.getPrice()));
		merged.put("cost_price", fenToYuan(old.getCostPrice()));
		merged.put("market_price", fenToYuan(old.getMarketPrice()));
		merged.put("store", old.getStore() != null ? old.getStore() : 0);
		merged.put("templates_id", old.getTemplatesId() != null ? old.getTemplatesId() : 0);
		merged.put("weight", old.getWeight() != null ? old.getWeight() : 0.0);
		merged.put("barcode", nz(old.getBarcode(), ""));
		merged.put("item_unit", nz(old.getItemUnit(), ""));
		merged.put("nospec", nz(old.getNospec(), "true"));
		merged.put("pics", old.getPics());
		merged.put("intro", nz(old.getIntro(), ""));
		merged.put("videos", nz(old.getVideos(), ""));
		merged.put("approve_status", nz(old.getApproveStatus(), "onsale"));
		if (old.getBrandId() != null && old.getBrandId() > 0) {
			merged.put("brand_id", old.getBrandId());
		}
		merged.put("is_market", old.getIsMarket() != null && old.getIsMarket() > 0);
		merged.put("start_num", old.getStartNum() != null ? old.getStartNum() : 0);

		ItemsCategory mainCategory = null;
		if (StringUtils.hasText(r.get("item_main_category"))) {
			mainCategory = resolveMainCategoryLeaf(companyId, r.get("item_main_category"));
			merged.put("item_main_cat_id", mainCategory.getCategoryId());
		}
		if (StringUtils.hasText(r.get("item_category"))) {
			merged.put("item_category", supplierSaleCategoryPathIdsResolver.resolve(companyId, distributorId, r.get("item_category"), supplierMode));
		}
		if (StringUtils.hasText(r.get("templates_id"))) {
			long tid = resolveTemplateId(companyId, r.get("templates_id"), supplierId, distributorId);
			if (tid > Integer.MAX_VALUE) {
				throw new BadRequestException("运费模板ID超出范围");
			}
			merged.put("templates_id", (int) tid);
		}
		if (StringUtils.hasText(r.get("pics"))) {
			merged.put("pics", splitCommaList(r.get("pics")));
		}
		if (StringUtils.hasText(r.get("goods_brand"))) {
			int bid = itemsAttributesRepository
					.findFirstBrandByCompanyAndName(companyId, r.get("goods_brand"))
					.map(a -> a.getAttributeId() != null ? a.getAttributeId().intValue() : 0)
					.orElse(0);
			if (bid <= 0) {
				throw new BadRequestException(r.get("goods_brand") + " 品牌名称不存在");
			}
			merged.put("brand_id", bid);
		}
		if (StringUtils.hasText(r.get("intro"))) {
			merged.put("intro", buildIntroHtml(r.get("intro")));
		}
		if (StringUtils.hasText(r.get("is_market"))) {
			merged.put("is_market", parseIsMarketRequired(r.get("is_market")));
		}
		if (StringUtils.hasText(r.get("price"))) {
			merged.put("price", parseYuan(r.get("price")));
		}
		if (r.containsKey("cost_price") && StringUtils.hasText(r.get("cost_price"))) {
			merged.put("cost_price", parseYuan(r.get("cost_price")));
		}
		if (r.containsKey("market_price") && StringUtils.hasText(r.get("market_price"))) {
			merged.put("market_price", parseYuan(r.get("market_price")));
		}
		if (StringUtils.hasText(r.get("store"))) {
			merged.put("store", parseStore(r.get("store")));
		}
		if (StringUtils.hasText(r.get("weight"))) {
			merged.put("weight", parseWeight(r.get("weight")));
		}
		if (StringUtils.hasText(r.get("barcode"))) {
			merged.put("barcode", r.get("barcode"));
		}
		if (StringUtils.hasText(r.get("item_unit"))) {
			merged.put("item_unit", r.get("item_unit"));
		}
		if (StringUtils.hasText(r.get("videos"))) {
			merged.put("videos", r.get("videos"));
		}
		if (StringUtils.hasText(r.get("start_num"))) {
			merged.put("start_num", parseStartNum(r.get("start_num")));
		}

		if (StringUtils.hasText(r.get("item_spec")) && mainCategory != null) {
			long defId = old.getDefaultItemId() != null && old.getDefaultItemId() > 0L
					? old.getDefaultItemId()
					: old.getItemId();
			r.put("default_item_id", String.valueOf(defId));
			List<Map<String, Object>> itemSpec = getItemSpec(companyId, r, mainCategory);
			merged.put("nospec", "false");
			List<SupplierItems> siblings = supplierItemsRepository.listByDefaultItemIdAndCompany(defId, companyId);
			if (siblings.isEmpty()) {
				siblings = List.of(old);
			}
			List<Map<String, Object>> specItems = new ArrayList<>();
			for (SupplierItems sib : siblings) {
				Map<String, Object> si = new LinkedHashMap<>();
				si.put("item_id", sib.getItemId());
				si.put("item_bn", nz(sib.getItemBn(), ""));
				si.put("weight", sib.getWeight() != null ? sib.getWeight() : 0.0);
				si.put("barcode", nz(sib.getBarcode(), ""));
				si.put("price", fenToYuan(sib.getPrice()));
				si.put("cost_price", fenToYuan(sib.getCostPrice()));
				si.put("market_price", fenToYuan(sib.getMarketPrice()));
				si.put("item_unit", nz(sib.getItemUnit(), ""));
				si.put("store", sib.getStore() != null ? sib.getStore() : 0);
				si.put("is_default", Boolean.TRUE.equals(sib.getIsDefault()));
				si.put("default_item_id", defId);
				si.put("approve_status", nz(sib.getApproveStatus(), "onsale"));
				if (Objects.equals(sib.getItemId(), old.getItemId())) {
					if (StringUtils.hasText(r.get("price"))) {
						si.put("price", parseYuan(r.get("price")));
					}
					if (StringUtils.hasText(r.get("cost_price"))) {
						si.put("cost_price", parseYuan(r.get("cost_price")));
					}
					if (StringUtils.hasText(r.get("market_price"))) {
						si.put("market_price", parseYuan(r.get("market_price")));
					}
					if (StringUtils.hasText(r.get("store"))) {
						si.put("store", parseStore(r.get("store")));
					}
					if (StringUtils.hasText(r.get("weight"))) {
						si.put("weight", parseWeight(r.get("weight")));
					}
					if (StringUtils.hasText(r.get("barcode"))) {
						si.put("barcode", r.get("barcode"));
					}
					if (StringUtils.hasText(r.get("item_unit"))) {
						si.put("item_unit", r.get("item_unit"));
					}
					si.put("item_spec", itemSpec);
				}
				specItems.add(si);
			}
			merged.put("spec_items", specItems);
			List<Map<String, Object>> specImages = getItemSpecImages(companyId, r, mainCategory);
			if (!specImages.isEmpty()) {
				merged.put("spec_images", specImages);
			}
		}

		return merged;
	}

	List<Map<String, Object>> getItemSpec(long companyId, Map<String, String> item, ItemsCategory mainCategory) {
		return getItemSpec(companyId, item, mainCategory, true);
	}

	List<Map<String, Object>> getItemSpec(
			long companyId, Map<String, String> item, ItemsCategory mainCategory, boolean checkSupplierDuplicate) {
		List<Map<String, Object>> specInfo = new ArrayList<>();
		if (!StringUtils.hasText(item.get("item_spec"))) {
			return specInfo;
		}
		List<Long> goodsSpecIds = parseGoodsSpecIds(mainCategory.getGoodsSpec());
		List<String> attributeNames = new ArrayList<>();
		List<String> attributeValues = new ArrayList<>();
		for (String part : item.get("item_spec").split("\\|")) {
			String[] pair = part.split(":", 2);
			if (pair.length < 1 || !StringUtils.hasText(pair[0])) {
				throw new BadRequestException("商品规格解析错误");
			}
			if (pair.length < 2 || !StringUtils.hasText(pair[1])) {
				throw new BadRequestException("商品规格值解析错误");
			}
			attributeNames.add(pair[0].trim());
			attributeValues.add(pair[1].trim());
		}
		if (goodsSpecIds.isEmpty()) {
			throw new BadRequestException("商品规格[" + String.join(",", attributeNames) + "]存在无效值");
		}
		List<ItemsAttributes> attrList =
				itemsAttributesRepository.listItemSpecByCompanyNamesAndIdsOrdered(companyId, attributeNames, goodsSpecIds);
		if (attrList.size() != attributeNames.size()) {
			throw new BadRequestException("商品规格[" + String.join(",", attributeNames) + "]存在无效值");
		}
		List<Long> attributeIds = attrList.stream().map(ItemsAttributes::getAttributeId).toList();
		List<ItemsAttributeValues> attrValuesList =
				itemsAttributeValuesRepository.listByCompanyAttributeIdsAndAttributeValuesIn(
						companyId, attributeIds, attributeValues);
		if (attrValuesList.size() != attributeValues.size()) {
			throw new BadRequestException("商品规格值[" + String.join(",", attributeValues) + "]无效");
		}
		Map<Long, Map<String, Object>> byAttrId = new LinkedHashMap<>();
		for (ItemsAttributeValues row : attrValuesList) {
			Map<String, Object> one = new LinkedHashMap<>();
			one.put("spec_id", row.getAttributeId());
			one.put("spec_value_id", row.getAttributeValueId());
			byAttrId.put(row.getAttributeId(), one);
		}
		for (Long specId : attributeIds) {
			if (byAttrId.containsKey(specId)) {
				specInfo.add(byAttrId.get(specId));
			}
		}

		long defaultItemId = parseLong(item.get("default_item_id"));
		if (checkSupplierDuplicate && defaultItemId > 0L) {
			assertNoDuplicateSpec(companyId, defaultItemId, item.get("item_bn"), specInfo);
		}
		return specInfo;
	}

	private void assertNoDuplicateSpec(
			long companyId, long defaultItemId, String itemBn, List<Map<String, Object>> specInfo) {
		List<SupplierItems> rs = supplierItemsRepository.listByDefaultItemIdAndCompany(defaultItemId, companyId);
		if (rs.isEmpty()) {
			return;
		}
		Map<Long, Map<Long, Long>> goodsAttrs = new LinkedHashMap<>();
		for (SupplierItems v : rs) {
			if (v.getItemId() == null) {
				continue;
			}
			List<SupplierItemsAttr> attrs =
					supplierItemsAttrPersistenceService.listByCompanyIdAndItemIdAndAttributeType(
							companyId, v.getItemId(), "item_spec");
			for (SupplierItemsAttr a : attrs) {
				Long valueId = readSpecValueId(a.getAttrData());
				if (valueId == null || a.getAttributeId() == null) {
					continue;
				}
				goodsAttrs.computeIfAbsent(v.getItemId(), k -> new LinkedHashMap<>()).put(a.getAttributeId(), valueId);
			}
		}
		String bn = itemBn != null ? itemBn.trim() : "";
		for (SupplierItems v : rs) {
			if (v.getItemId() == null || !goodsAttrs.containsKey(v.getItemId())) {
				continue;
			}
			if (bn.equals(nz(v.getItemBn(), "").trim())) {
				continue;
			}
			boolean isRepeat = true;
			Map<Long, Long> existing = goodsAttrs.get(v.getItemId());
			for (Map.Entry<Long, Long> e : existing.entrySet()) {
				boolean matched = false;
				for (Map<String, Object> s : specInfo) {
					if (Objects.equals(toLongObj(s.get("spec_id")), e.getKey())
							&& Objects.equals(toLongObj(s.get("spec_value_id")), e.getValue())) {
						matched = true;
						break;
					}
				}
				if (!matched) {
					isRepeat = false;
					break;
				}
			}
			if (isRepeat) {
				throw new BadRequestException("相同规格值的商品已存在");
			}
		}
	}

	List<Map<String, Object>> getItemSpecImages(
			long companyId, Map<String, String> item, ItemsCategory mainCategory) {
		if (!StringUtils.hasText(item.get("item_spec")) || !StringUtils.hasText(item.get("item_spec_pics"))) {
			return List.of();
		}
		List<Long> goodsSpecIds = parseGoodsSpecIds(mainCategory.getGoodsSpec());
		List<String> attributeNames = new ArrayList<>();
		List<String> attributeValues = new ArrayList<>();
		for (String part : item.get("item_spec").split("\\|")) {
			String[] pair = part.split(":", 2);
			if (pair.length < 1 || !StringUtils.hasText(pair[0])) {
				throw new BadRequestException("商品规格解析错误");
			}
			if (pair.length < 2 || !StringUtils.hasText(pair[1])) {
				throw new BadRequestException("商品规格值解析错误");
			}
			attributeNames.add(pair[0].trim());
			attributeValues.add(pair[1].trim());
		}
		ItemsAttributes attr =
				itemsAttributesRepository.findFirstItemSpecImageByCompanyNamesAndIds(companyId, attributeNames, goodsSpecIds);
		if (attr == null || attr.getAttributeId() == null) {
			return List.of();
		}
		List<ItemsAttributeValues> values =
				itemsAttributeValuesRepository.listByCompanyAttributeIdsAndAttributeValuesIn(
						companyId, List.of(attr.getAttributeId()), attributeValues);
		if (values.isEmpty() || values.get(0).getAttributeValueId() == null) {
			return List.of();
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("spec_value_id", values.get(0).getAttributeValueId());
		row.put("item_image_url", splitCommaList(item.get("item_spec_pics")));
		return List.of(row);
	}

	private Long readSpecValueId(String attrData) {
		if (!StringUtils.hasText(attrData)) {
			return null;
		}
		try {
			Map<String, Object> root = objectMapper.readValue(attrData, new TypeReference<Map<String, Object>>() {});
			Object inner = root.get("item_spec");
			if (!(inner instanceof Map<?, ?> m)) {
				return null;
			}
			Object v = m.get("attribute_value_id");
			return v == null ? null : toLongObj(v);
		} catch (Exception e) {
			return null;
		}
	}

	private List<Long> parseGoodsSpecIds(String goodsSpec) {
		if (!StringUtils.hasText(goodsSpec)) {
			return List.of();
		}
		try {
			List<Object> raw = objectMapper.readValue(goodsSpec.trim(), new TypeReference<List<Object>>() {});
			List<Long> out = new ArrayList<>();
			for (Object o : raw) {
				if (o == null) {
					continue;
				}
				try {
					out.add(Long.parseLong(o.toString().trim()));
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
			return out;
		} catch (Exception e) {
			return List.of();
		}
	}

	private static Session sessionOf(Map<String, Object> row) {
		Object v = row.get("__upload_file_id__");
		if (v == null) {
			return null;
		}
		try {
			long id = Long.parseLong(String.valueOf(v).trim());
			if (id <= 0L) {
				return null;
			}
			return SESSION.computeIfAbsent(id, k -> new Session());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void validateCreateRequired(Map<String, String> r, boolean supplierMode) {
		List<String> errs = new ArrayList<>();
		if (!StringUtils.hasText(r.get("item_name"))) {
			errs.add("请填写商品名称");
		}
		if (!StringUtils.hasText(r.get("price"))) {
			errs.add("请填写价格");
		}
		if (!StringUtils.hasText(r.get("templates_id"))) {
			errs.add("请填写运费模板");
		}
		if (!StringUtils.hasText(r.get("item_main_category"))) {
			errs.add("请上传管理分类");
		}
		if (!StringUtils.hasText(r.get("store"))) {
			errs.add("请填写库存");
		}
		if (supplierMode) {
			if (!StringUtils.hasText(r.get("cost_price"))) {
				errs.add("请填写成本价");
			}
			if (!StringUtils.hasText(r.get("is_market"))) {
				errs.add("请填写供应状态");
			}
		} else {
			if (!StringUtils.hasText(r.get("item_category"))) {
				errs.add("请上传销售分类");
			}
		}
		if (!errs.isEmpty()) {
			throw new BadRequestException(String.join(", ", errs));
		}
		int st = parseStore(r.get("store"));
		if (st < 0 || st > 999999999) {
			throw new BadRequestException("库存为0-999999999的整数");
		}
	}

	List<Long> resolveSaleCategories(long companyId, long distributorId, String cell, boolean supplierMode) {
		if (supplierMode && !StringUtils.hasText(cell)) {
			return List.of();
		}
		return supplierSaleCategoryPathIdsResolver.resolve(companyId, distributorId, cell, supplierMode);
	}

	long resolveMainCategoryLeafId(long companyId, String threeLevelPath) {
		return resolveMainCategoryLeaf(companyId, threeLevelPath).getCategoryId();
	}

	ItemsCategory resolveMainCategoryLeaf(long companyId, String threeLevelPath) {
		if (!StringUtils.hasText(threeLevelPath)) {
			throw new BadRequestException("请上传管理分类");
		}
		String[] parts = threeLevelPath.split("->");
		if (parts.length != 3) {
			throw new BadRequestException("上传管理分类必须是三层级," + threeLevelPath.trim());
		}
		long parentId = 0L;
		ItemsCategory cur = null;
		for (String raw : parts) {
			String nm = raw.trim();
			if (!StringUtils.hasText(nm)) {
				throw new BadRequestException("无法识别的管理分类," + threeLevelPath.trim());
			}
			cur = itemsCategoryRepository.findMainCategoryChild(companyId, parentId, nm, 0L);
			if (cur == null || cur.getCategoryId() == null) {
				throw new BadRequestException("无法识别的管理分类," + threeLevelPath.trim());
			}
			parentId = cur.getCategoryId();
		}
		return cur;
	}

	long resolveTemplateId(long companyId, String name, long supplierId, long distributorId) {
		if (!StringUtils.hasText(name)) {
			throw new BadRequestException("请填写商品运费模版");
		}
		Optional<Long> id = shippingTemplatesQueryRepository.findTemplateIdByNameCompanySupplierAndDistributor(
				companyId, name, supplierId, distributorId);
		if (id.isEmpty()) {
			throw new BadRequestException("填写的运费模版不存在");
		}
		return id.get();
	}

	private static int parseIsMarketRequired(String raw) {
		String t = raw.trim();
		Integer v = IS_MARKET_LABEL.get(t);
		if (v != null) {
			return v;
		}
		throw new BadRequestException("供应状态错误");
	}

	private static int parseStartNum(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		try {
			int n = new BigDecimal(raw.trim()).intValue();
			return Math.max(n, 0);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int parseStore(String raw) {
		return (int) parseLong(raw);
	}

	private static double parseWeight(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0.0;
		}
		try {
			return new BigDecimal(raw.trim()).doubleValue();
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static long parseLong(String s) {
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return new BigDecimal(s.trim()).longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long toLongObj(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static BigDecimal parseYuan(Object yuan) {
		if (yuan == null) {
			return BigDecimal.ZERO;
		}
		String s = yuan.toString().trim();
		if (s.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return new BigDecimal(s);
	}

	private static BigDecimal fenToYuan(Integer fen) {
		if (fen == null) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(fen.longValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	/**
	 * 拆分图片列。PHP 导出会把多图写成 {@code "url1,url2"}（内容自带引号），
	 * 直接 {@code split(",")} 会得到 {@code ["\"url1", "url2\""]} 脏数据。
	 */
	private static List<String> splitCommaList(String pics) {
		if (!StringUtils.hasText(pics)) {
			return List.of();
		}
		String s = stripOuterQuotes(pics.trim());
		return Arrays.stream(s.split(",")).map(String::trim).map(SupplierGoodsImportRowOrchestrator::stripOuterQuotes)
				.filter(StringUtils::hasText).toList();
	}

	private static String stripOuterQuotes(String s) {
		if (!StringUtils.hasText(s)) {
			return "";
		}
		String t = s.trim();
		while (!t.isEmpty() && (t.charAt(0) == '\\' || t.charAt(0) == '"')) {
			t = t.substring(1).trim();
		}
		while (!t.isEmpty()) {
			char c = t.charAt(t.length() - 1);
			if (c != '\\' && c != '"') {
				break;
			}
			t = t.substring(0, t.length() - 1).trim();
		}
		return t;
	}

	private static String buildIntroHtml(String introComma) {
		if (!StringUtils.hasText(introComma)) {
			return "";
		}
		StringBuilder intro = new StringBuilder();
		for (String u : introComma.split(",")) {
			String x = u.trim();
			if (!StringUtils.hasText(x)) {
				continue;
			}
			intro.append("<img src=\"").append(x).append("\" style=\"display: block;\">");
		}
		return intro.toString();
	}

	private static Map<String, String> trimRow(Map<String, Object> row) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			Object v = e.getValue();
			out.put(e.getKey(), v == null ? "" : String.valueOf(v).trim());
		}
		return out;
	}

	private static String nz(String s, String d) {
		return s != null ? s : d;
	}

	private static final class Session {
		volatile String itemName;
		volatile Long defaultItemId;
		volatile Long goodsId;
	}
}
