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

package cn.shopex.ecshopx.goods.service.pointsmall;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.crossborder.domain.OriginCountry;
import cn.shopex.ecshopx.crossborder.mapper.OriginCountryMapper;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryPathByItemService;
import cn.shopex.ecshopx.goods.service.items.ItemsDetailIntroArticleService;
import cn.shopex.ecshopx.goods.service.items.ItemsRelAttrValuesQueryService;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemRelAttributes;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemsRelCats;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemRelAttributesMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsRelCatsMapper;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsAdminDetailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallItemsAdminDetailServiceImpl implements PointsmallItemsAdminDetailService {

	private static final Pattern HTTP = Pattern.compile("(https?://)", Pattern.CASE_INSENSITIVE);

	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper;
	private final PointsmallItemsRelCatsMapper pointsmallItemsRelCatsMapper;
	private final ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService;
	private final ItemsCategoryPathByItemService itemsCategoryPathByItemService;
	private final ItemsDetailIntroArticleService itemsDetailIntroArticleService;
	private final OriginCountryMapper originCountryMapper;
	private final ObjectMapper objectMapper;

	public PointsmallItemsAdminDetailServiceImpl(PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper,
			PointsmallItemsRelCatsMapper pointsmallItemsRelCatsMapper,
			ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService,
			ItemsCategoryPathByItemService itemsCategoryPathByItemService,
			ItemsDetailIntroArticleService itemsDetailIntroArticleService,
			OriginCountryMapper originCountryMapper,
			ObjectMapper objectMapper) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.pointsmallItemRelAttributesMapper = pointsmallItemRelAttributesMapper;
		this.pointsmallItemsRelCatsMapper = pointsmallItemsRelCatsMapper;
		this.itemsRelAttrValuesQueryService = itemsRelAttrValuesQueryService;
		this.itemsCategoryPathByItemService = itemsCategoryPathByItemService;
		this.itemsDetailIntroArticleService = itemsDetailIntroArticleService;
		this.originCountryMapper = originCountryMapper;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> getDetail(long itemId, long companyId, String authorizerAppid) {
		PointsmallItems entity = pointsmallItemsMapper.selectById(itemId);
		if (entity == null || entity.getCompanyId() == null || entity.getCompanyId() != companyId) {
			return new LinkedHashMap<>();
		}

		LinkedHashMap<String, Object> m = entityToDetailMap(entity);

		if (StringUtils.hasText(entity.getRegionsId())) {
			List<String> regParts = new ArrayList<>();
			for (String p : entity.getRegionsId().split(",")) {
				if (StringUtils.hasText(p)) {
					regParts.add(p.trim());
				}
			}
			m.put("regions_id", regParts);
		}

		String effectiveType = resolveEffectiveItemType(entity.getItemType());
		m.put("item_type", effectiveType);

		if ("services".equals(effectiveType)) {
			m.put("type_labels", new ArrayList<>());
		} else {
			applyNormalItemSpecAndAttrs(m, entity, companyId);
		}

		long mainCat = parseMainCatId(m.get("item_main_cat_id"));
		if (mainCat > 0) {
			List<Map<String, Object>> mainPath = itemsCategoryPathByItemService.getCategoryPathById(companyId, mainCat, true);
			m.put("item_category_main", mainPath);
			if (mainPath.isEmpty()) {
				m.put("item_main_cat_id", "");
			}
		} else {
			m.put("item_category_main", List.of());
		}

		if (m.containsKey("intro")) {
			m.put("intro", itemsDetailIntroArticleService.pro(m.get("intro"), authorizerAppid, 0L, "", companyId));
		}

		applyVideoFields(m, authorizerAppid);

		m.put("distributor_sale_status", true);
		String ap = str(m.get("approve_status"));
		if ("instock".equals(ap) || "offline_sale".equals(ap) || "only_show".equals(ap)) {
			m.put("distributor_sale_status", false);
		}

		Object its = m.get("item_total_store");
		int storeFallback = entity.getStore() != null ? entity.getStore() : 0;
		if (its == null) {
			m.put("item_total_store", storeFallback);
		}

		long ocId = toLong(m.get("origincountry_id"));
		if (ocId <= 0) {
			m.put("origincountry_name", "");
			m.put("origincountry_img_url", "");
		} else {
			OriginCountry oc = originCountryMapper.selectById(ocId);
			if (oc != null) {
				m.put("origincountry_name", nz(oc.getOrigincountryName()));
				m.put("origincountry_img_url", nz(oc.getOrigincountryImgUrl()));
			} else {
				m.put("origincountry_name", "");
				m.put("origincountry_img_url", "");
			}
		}

		m.put("tax_rate", 0);
		m.put("cross_border_tax", 0);

		return m;
	}

	private void applyNormalItemSpecAndAttrs(LinkedHashMap<String, Object> m, PointsmallItems entity, long companyId) {
		m.put("type_labels", new ArrayList<>());
		long mainItemId = entity.getItemId() != null ? entity.getItemId() : 0L;
		long defaultItemId = entity.getDefaultItemId() != null && entity.getDefaultItemId() > 0 ? entity.getDefaultItemId() : mainItemId;

		List<Long> skuItemIds;
		List<PointsmallItems> skuRows;
		if (isMultiSpec(entity.getNospec())) {
			skuRows = pointsmallItemsMapper.listSkusByCompanyAndDefaultItemId(companyId, defaultItemId);
			skuItemIds = new ArrayList<>();
			for (PointsmallItems row : skuRows) {
				if (row.getItemId() != null) {
					skuItemIds.add(row.getItemId());
				}
			}
			if (skuItemIds.isEmpty()) {
				skuItemIds = List.of(mainItemId);
				skuRows = List.of();
			}
		} else {
			skuItemIds = List.of(mainItemId);
			skuRows = List.of();
		}

		m.put("item_params", new ArrayList<>());
		m.put("item_spec_desc", new ArrayList<>());
		m.put("spec_images", new ArrayList<>());
		m.put("spec_items", new ArrayList<>());
		m.put("spec_pics", List.of());

		List<PointsmallItemRelAttributes> defaultAttrs = loadRelAttributes(companyId, defaultItemId, null);
		List<PointsmallItemRelAttributes> specAttrs = loadRelAttributes(companyId, skuItemIds, "item_spec");
		List<PointsmallItemRelAttributes> merged = new ArrayList<>(defaultAttrs);
		merged.addAll(specAttrs);

		applySpecPicsFromRel(m, mainItemId, specAttrs);

		if (merged.isEmpty()) {
			loadItemCategoryIds(m, mainItemId, companyId);
			applyItemParamsFallbackFromGoodsFields(m);
			return;
		}

		List<ItemRelAttributes> relGoods = new ArrayList<>();
		for (PointsmallItemRelAttributes p : merged) {
			relGoods.add(toGoodsRel(p));
		}

		ItemsRelAttrValuesQueryService.ItemDetailAttrData ad = itemsRelAttrValuesQueryService.assemble(companyId, relGoods);

		if (!ad.brand.isEmpty()) {
			if (ad.brand.get("brand_id") != null) {
				m.put("brand_id", ad.brand.get("brand_id"));
			}
			if (ad.brand.get("goods_brand") != null) {
				m.put("goods_brand", ad.brand.get("goods_brand"));
			}
			if (ad.brand.get("brand_logo") != null) {
				m.put("brand_logo", normalizeBrandLogo(ad.brand.get("brand_logo").toString()));
			}
		}

		m.put("item_params", new ArrayList<>(ad.itemParams));
		m.put("attribute_ids", new ArrayList<>(ad.attributeIds));
		m.put("attr_values_custom", new LinkedHashMap<>(ad.attrValuesCustom));
		applyItemParamsFallbackFromGoodsFields(m);

		if (ad.itemSpecNested.isEmpty()) {
			loadItemCategoryIds(m, mainItemId, companyId);
			return;
		}

		m.put("item_spec_desc", new ArrayList<>(ad.itemSpecDesc));
		m.put("spec_images", ad.specImages != null ? ad.specImages : List.of());

		List<Map<String, Object>> specItems = new ArrayList<>();
		List<String> approveStatuses = new ArrayList<>();
		int totalStore = 0;
		for (PointsmallItems sku : skuRows) {
			Long sid = sku.getItemId();
			if (sid == null) {
				continue;
			}
			Map<Long, Map<String, Object>> bySpec = ad.itemSpecNested.get(sid);
			List<Map<String, Object>> tempItemSpec = new ArrayList<>();
			if (bySpec != null) {
				tempItemSpec.addAll(bySpec.values());
			}
			approveStatuses.add(nz(sku.getApproveStatus()));
			Integer st = sku.getStore();
			totalStore += st != null ? st : 0;

			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("item_id", sid);
			row.put("price", sku.getPrice());
			row.put("store", sku.getStore());
			row.put("cost_price", sku.getCostPrice());
			row.put("item_bn", nz(sku.getItemBn()));
			row.put("barcode", nz(sku.getBarcode()));
			row.put("market_price", sku.getMarketPrice());
			row.put("point", sku.getPoint());
			row.put("pay_class", nz(sku.getPayClass()));
			row.put("item_unit", nz(sku.getItemUnit()));
			row.put("volume", sku.getVolume());
			row.put("approve_status", nz(sku.getApproveStatus()));
			row.put("is_default", Boolean.TRUE.equals(sku.getIsDefault()));
			row.put("weight", sku.getWeight());
			row.put("item_spec", tempItemSpec);
			specItems.add(row);
		}

		m.put("spec_items", specItems);
		m.put("approve_status", aggregateApproveStatus(approveStatuses));
		m.put("item_total_store", totalStore);

		loadItemCategoryIds(m, mainItemId, companyId);
	}

	private static void applyItemParamsFallbackFromGoodsFields(Map<String, Object> m) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> itemParams = (List<Map<String, Object>>) m.get("item_params");
		if (itemParams == null || !itemParams.isEmpty()) {
			return;
		}
		String brand = str(m.get("goods_brand"));
		if (StringUtils.hasText(brand) && m.get("brand_id") == null) {
			itemParams.add(Map.of("attribute_name", "品牌", "attribute_value_name", brand));
		}
		String color = str(m.get("goods_color"));
		if (StringUtils.hasText(color)) {
			itemParams.add(Map.of("attribute_name", "颜色", "attribute_value_name", color));
		}
		String fn = str(m.get("goods_function"));
		if (StringUtils.hasText(fn)) {
			itemParams.add(Map.of("attribute_name", "功能", "attribute_value_name", fn));
		}
		String series = str(m.get("goods_series"));
		if (StringUtils.hasText(series)) {
			itemParams.add(Map.of("attribute_name", "系列", "attribute_value_name", series));
		}
	}

	private static String aggregateApproveStatus(List<String> statuses) {
		if (statuses.isEmpty()) {
			return "instock";
		}
		if (statuses.stream().anyMatch("onsale"::equals)) {
			return "onsale";
		}
		if (statuses.stream().anyMatch("only_show"::equals)) {
			return "only_show";
		}
		if (statuses.stream().anyMatch("offline_sale"::equals)) {
			return "offline_sale";
		}
		return "instock";
	}

	private void loadItemCategoryIds(Map<String, Object> m, long itemId, long companyId) {
		LambdaQueryWrapper<PointsmallItemsRelCats> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItemsRelCats::getCompanyId, companyId).eq(PointsmallItemsRelCats::getItemId, itemId);
		List<Long> catIds = new ArrayList<>();
		for (PointsmallItemsRelCats r : pointsmallItemsRelCatsMapper.selectList(w)) {
			if (r.getCategoryId() != null) {
				catIds.add(r.getCategoryId());
			}
		}
		m.put("item_category", catIds);
	}

	private List<PointsmallItemRelAttributes> loadRelAttributes(long companyId, long singleItemId, String attributeTypeOrNull) {
		LambdaQueryWrapper<PointsmallItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItemRelAttributes::getCompanyId, companyId).eq(PointsmallItemRelAttributes::getItemId, singleItemId)
				.orderByAsc(PointsmallItemRelAttributes::getAttributeSort);
		if (attributeTypeOrNull != null) {
			w.eq(PointsmallItemRelAttributes::getAttributeType, attributeTypeOrNull);
		}
		return pointsmallItemRelAttributesMapper.selectList(w);
	}

	private List<PointsmallItemRelAttributes> loadRelAttributes(long companyId, List<Long> itemIds, String attributeType) {
		if (itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<PointsmallItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItemRelAttributes::getCompanyId, companyId).in(PointsmallItemRelAttributes::getItemId, itemIds)
				.eq(PointsmallItemRelAttributes::getAttributeType, attributeType).orderByAsc(PointsmallItemRelAttributes::getAttributeSort);
		return pointsmallItemRelAttributesMapper.selectList(w);
	}

	private static void applySpecPicsFromRel(Map<String, Object> m, long mainItemId, List<PointsmallItemRelAttributes> specAttrs) {
		for (PointsmallItemRelAttributes row : specAttrs) {
			if (row.getItemId() != null && row.getItemId() == mainItemId && StringUtils.hasText(row.getImageUrl())) {
				m.put("spec_pics", row.getImageUrl());
				return;
			}
		}
	}

	private String resolveEffectiveItemType(String raw) {
		if (raw == null || raw.isBlank()) {
			return "normal";
		}
		String t = raw.trim();
		if ("normal".equals(t)) {
			return "normal";
		}
		if ("services".equals(t)) {
			return "services";
		}
		throw new ResourceException("获取商品信息有误，请确认商品ID.");
	}

	private static boolean isMultiSpec(Object nospec) {
		if (nospec instanceof Boolean b) {
			return !b;
		}
		String s = nospec == null ? "" : nospec.toString().trim();
		return "false".equalsIgnoreCase(s) || "0".equals(s);
	}

	private void applyVideoFields(Map<String, Object> m, String authorizerAppid) {
		String vt = str(m.get("video_type"));
		Object videosObj = m.get("videos");
		String videosStr = videosObj != null ? videosObj.toString() : "";
		boolean hasVideos = StringUtils.hasText(videosStr);
		boolean hasAuth = StringUtils.hasText(authorizerAppid);
		if (hasVideos && hasAuth && !"tencent".equals(vt)) {
			if (HTTP.matcher(videosStr).find()) {
				m.put("videos_url", videosStr);
			} else {
				m.put("videos_url", "");
			}
		} else {
			m.put("videos_url", "");
		}
		if ("tencent".equals(vt)) {
			m.put("tencent_vid", videosObj);
		}
	}

	private LinkedHashMap<String, Object> entityToDetailMap(PointsmallItems e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		putNum(m, "item_id", e.getItemId());
		m.put("item_type", nz(e.getItemType()));
		m.put("consume_type", nz(e.getConsumeType()));
		putBool(m, "is_show_specimg", e.getIsShowSpecimg());
		putNum(m, "store", e.getStore());
		m.put("barcode", nz(e.getBarcode()));
		putNullableInt(m, "sales", e.getSales());
		m.put("approve_status", nz(e.getApproveStatus()));
		putNum(m, "cost_price", e.getCostPrice());
		putNum(m, "point", e.getPoint());
		putNum(m, "goods_id", e.getGoodsId());
		putNum(m, "brand_id", e.getBrandId());
		m.put("item_name", nz(e.getItemName()));
		m.put("item_unit", nz(e.getItemUnit()));
		m.put("item_bn", nz(e.getItemBn()));
		m.put("brief", nz(e.getBrief()));
		m.put("intro", decodeIntro(e.getIntro()));
		putNum(m, "price", e.getPrice());
		putNum(m, "market_price", e.getMarketPrice());
		m.put("special_type", nz(e.getSpecialType()));
		m.put("goods_function", nz(e.getGoodsFunction()));
		m.put("goods_series", nz(e.getGoodsSeries()));
		putNum(m, "volume", e.getVolume());
		m.put("goods_color", nz(e.getGoodsColor()));
		m.put("goods_brand", nz(e.getGoodsBrand()));
		m.put("item_address_province", nz(e.getItemAddressProvince()));
		m.put("item_address_city", nz(e.getItemAddressCity()));
		m.put("regions_id", nz(e.getRegionsId()));
		m.put("brand_logo", normalizeBrandLogo(e.getBrandLogo()));
		putNum(m, "sort", e.getSort());
		putNullableInt(m, "templates_id", e.getTemplatesId());
		putBool(m, "is_default", e.getIsDefault());
		m.put("nospec", nospecToBool(e.getNospec()));
		putNum(m, "default_item_id", e.getDefaultItemId());
		m.put("pics", decodePics(e.getPics()));
		putNum(m, "company_id", e.getCompanyId());
		putBool(m, "enable_agreement", e.getEnableAgreement());
		m.put("purchase_agreement", nz(e.getPurchaseAgreement()));
		m.put("date_type", nz(e.getDateType()));
		m.put("item_category", nz(e.getItemCategory()));
		putNum(m, "weight", e.getWeight());
		putNum(m, "begin_date", e.getBeginDate());
		putNum(m, "end_date", e.getEndDate());
		putNum(m, "fixed_term", e.getFixedTerm());
		putNum(m, "tax_rate", e.getTaxRate());
		putNum(m, "created", e.getCreated());
		putNum(m, "updated", e.getUpdated());
		m.put("video_type", nz(e.getVideoType()));
		m.put("videos", nz(e.getVideos()));
		m.put("video_pic_url", nz(e.getVideoPicUrl()));
		m.put("audit_status", nz(e.getAuditStatus()));
		m.put("audit_reason", nz(e.getAuditReason()));
		m.put("crossborder_tax_rate", nz(e.getCrossborderTaxRate()));
		putNum(m, "origincountry_id", e.getOrigincountryId());
		putNum(m, "type", e.getType());
		m.put("pay_class", nz(e.getPayClass()));

		m.put("itemId", m.get("item_id"));
		m.put("consumeType", m.get("consume_type"));
		m.put("itemName", m.get("item_name"));
		m.put("itemBn", m.get("item_bn"));
		m.put("companyId", m.get("company_id"));
		m.put("item_main_cat_id", m.get("item_category"));
		return m;
	}

	/** 详情页 {@code brand_logo}：空为数组，否则为图片 URL 列表（含单 URL）。 */
	private List<Object> normalizeBrandLogo(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new ArrayList<>();
		}
		String s = raw.trim();
		if (s.startsWith("[")) {
			try {
				JsonNode n = objectMapper.readTree(s);
				if (n != null && n.isArray()) {
					List<Object> out = new ArrayList<>();
					for (JsonNode el : n) {
						if (!el.isNull()) {
							out.add(el.isTextual() ? el.asText() : el.toString());
						}
					}
					return out;
				}
			} catch (Exception ignored) {
			}
		}
		return new ArrayList<>(List.of(s));
	}

	private Object decodePics(String raw) {
		if (raw == null) {
			return "";
		}
		String s = raw.trim();
		if (!StringUtils.hasText(s)) {
			return "";
		}
		if (s.length() >= 2 && s.charAt(0) == '"') {
			try {
				JsonNode outer = objectMapper.readTree(s);
				if (outer != null && outer.isTextual()) {
					String inner = outer.asText().trim();
					if (StringUtils.hasText(inner)) {
						char ic = inner.charAt(0);
						if (ic == '[' || ic == '{') {
							try {
								objectMapper.readTree(inner);
								return inner;
							} catch (Exception ignored) {
								return inner;
							}
						}
					}
				}
			} catch (Exception ignored) {
			}
		}
		for (int depth = 0; depth < 8; depth++) {
			char c = s.charAt(0);
			if (c != '[' && c != '{' && c != '"') {
				return s;
			}
			try {
				JsonNode n = objectMapper.readTree(s);
				if (n.isArray() || n.isObject()) {
					return objectMapper.convertValue(n, Object.class);
				}
				if (n.isTextual()) {
					String inner = n.asText();
					if (inner.equals(s)) {
						return inner;
					}
					s = inner.trim();
					if (!StringUtils.hasText(s)) {
						return "";
					}
					continue;
				}
				return s;
			} catch (Exception ignored) {
				return s;
			}
		}
		return s;
	}

	private Object decodeIntro(String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		String t = raw.trim();
		if (!t.startsWith("[") && !t.startsWith("{")) {
			return raw;
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			if (n == null || n.isNull()) {
				return raw;
			}
			if (n.isArray() || n.isObject()) {
				return objectMapper.convertValue(n, Object.class);
			}
		} catch (Exception ignored) {
		}
		return raw;
	}

	private static ItemRelAttributes toGoodsRel(PointsmallItemRelAttributes p) {
		ItemRelAttributes r = new ItemRelAttributes();
		r.setId(p.getId());
		r.setCompanyId(p.getCompanyId());
		r.setItemId(p.getItemId());
		r.setAttributeSort(p.getAttributeSort());
		r.setAttributeId(p.getAttributeId());
		r.setAttributeType(p.getAttributeType());
		r.setAttributeValueId(p.getAttributeValueId());
		r.setCustomAttributeValue(p.getCustomAttributeValue());
		r.setImageUrl(p.getImageUrl());
		return r;
	}

	private static void putNum(Map<String, Object> m, String k, Object v) {
		m.put(k, v);
	}

	private static void putNullableInt(Map<String, Object> m, String k, Integer v) {
		m.put(k, v);
	}

	private static void putBool(Map<String, Object> m, String k, Boolean v) {
		m.put(k, v != null ? v : false);
	}

	private static String nz(String s) {
		return s != null ? s : "";
	}

	private static Object nospecToBool(Object nospec) {
		if (nospec instanceof Boolean b) {
			return b;
		}
		String s = nospec == null ? "" : nospec.toString();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static long parseMainCatId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o != null ? o.toString() : "";
	}
}
