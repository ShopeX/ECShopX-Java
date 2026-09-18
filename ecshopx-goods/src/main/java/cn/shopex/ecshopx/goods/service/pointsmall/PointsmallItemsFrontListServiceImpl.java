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

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.goods.service.items.ItemsRelAttrValuesQueryService;
import cn.shopex.ecshopx.goods.service.pointsmall.export.PointsmallItemsExportQuerySupport;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemRelAttributes;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItemsRelCats;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemRelAttributesMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsRelCatsMapper;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsFrontListService;
import cn.shopex.ecshopx.tdkset.service.TdkGivenSaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallItemsFrontListServiceImpl implements PointsmallItemsFrontListService {

	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemsRelCatsMapper pointsmallItemsRelCatsMapper;
	private final PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper;
	private final ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final PointsmallFrontBrandListService pointsmallFrontBrandListService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final TdkGivenSaveService tdkGivenSaveService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public PointsmallItemsFrontListServiceImpl(ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			PointsmallItemsMapper pointsmallItemsMapper,
			PointsmallItemsRelCatsMapper pointsmallItemsRelCatsMapper,
			PointsmallItemRelAttributesMapper pointsmallItemRelAttributesMapper,
			ItemsRelAttrValuesQueryService itemsRelAttrValuesQueryService,
			ItemsListMultiLangApplier itemsListMultiLangApplier,
			PointsmallFrontBrandListService pointsmallFrontBrandListService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			TdkGivenSaveService tdkGivenSaveService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.pointsmallItemsRelCatsMapper = pointsmallItemsRelCatsMapper;
		this.pointsmallItemRelAttributesMapper = pointsmallItemRelAttributesMapper;
		this.itemsRelAttrValuesQueryService = itemsRelAttrValuesQueryService;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.pointsmallFrontBrandListService = pointsmallFrontBrandListService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.tdkGivenSaveService = tdkGivenSaveService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Override
	public Map<String, Object> list(HttpServletRequest request, long companyId) {
		String countryCode = resolveCountryCode(request);
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("audit_status", "approved");

		String approveRaw = request.getParameter("approve_status");
		if (StringUtils.hasText(approveRaw)) {
			String[] parts = approveRaw.split(",", -1);
			List<String> tokens = new ArrayList<>(parts.length);
			for (String p : parts) {
				tokens.add(p.trim());
			}
			boolean anyWhitelist = tokens.stream()
					.anyMatch(t -> "onsale".equals(t) || "only_show".equals(t));
			if (anyWhitelist) {
				params.put("approve_status", tokens);
			} else {
				params.put("approve_status", List.of("onsale", "only_show"));
			}
		} else {
			params.put("approve_status", List.of("onsale", "only_show"));
		}

		List<Long> itemIdsFromQuery = readLongListParameter(request, "item_id");
		if (itemIdsFromQuery != null && !itemIdsFromQuery.isEmpty()) {
			params.put("item_id", itemIdsFromQuery);
		}

		Long resolvedSaleCategoryId = resolveSaleCategoryId(request);
		if (resolvedSaleCategoryId != null && resolvedSaleCategoryId > 0) {
			List<Long> treeItemIds = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, resolvedSaleCategoryId);
			if (treeItemIds.isEmpty()) {
				LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
				empty.put("total_count", 0L);
				empty.put("list", List.of());
				appendCurTdk(request, companyId, empty);
				return empty;
			}
			@SuppressWarnings("unchecked")
			List<Long> existingIds = (List<Long>) params.get("item_id");
			if (existingIds != null && !existingIds.isEmpty()) {
				LinkedHashSet<Long> catSet = new LinkedHashSet<>(treeItemIds);
				List<Long> intersected = new ArrayList<>();
				for (Long id : existingIds) {
					if (id != null && catSet.contains(id)) {
						intersected.add(id);
					}
				}
				if (intersected.isEmpty()) {
					LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
					empty.put("total_count", 0L);
					empty.put("list", List.of());
					appendCurTdk(request, companyId, empty);
					return empty;
				}
				params.put("item_id", intersected);
			} else {
				params.put("item_id", new ArrayList<>(treeItemIds));
			}
		}

		String keywords = request.getParameter("keywords");
		if (ValuePresence.hasEffectiveValue(keywords)) {
			params.put("item_name|contains", keywords.trim());
		}

		String mainCatParam = request.getParameter("main_category");
		if (ValuePresence.hasEffectiveValue(mainCatParam)) {
			long mainCatId = toLong(mainCatParam.trim());
			List<Long> expanded = itemsCategoryItemIdResolver.expandMainCategoryIdsForPointsmallExport(companyId, mainCatId);
			List<String> asStr = new ArrayList<>(expanded.stream().map(String::valueOf).toList());
			asStr.add(String.valueOf(mainCatId));
			params.put("item_category", asStr);
		}

		if (isQueryParamTruthyAfterDefaultZero(request, "start_price")) {
			params.put("point|gte", toPointFilterValue(request.getParameter("start_price")));
		}
		if (isQueryParamTruthyAfterDefaultZero(request, "end_price")) {
			params.put("point|lte", toPointFilterValue(request.getParameter("end_price")));
		}

		if (numericNonZero(request.getParameter("brand_id"))) {
			params.put("brand_id", toInt(request.getParameter("brand_id")));
		}

		String itemType = request.getParameter("item_type");
		params.put("item_type", StringUtils.hasText(itemType) ? itemType.trim() : "services");

		String goodsSortRaw = request.getParameter("goodsSort");

		int page = toInt(request.getParameter("page"));
		int pageSize = toInt(request.getParameter("pageSize"));

		boolean skuBranch = "false".equals(request.getParameter("is_default"));
		if (!skuBranch) {
			params.put("is_default", Boolean.TRUE);
		}

		LinkedHashMap<String, Object> result;
		if (skuBranch) {
			@SuppressWarnings("unchecked")
			List<Long> skuFilterItemIds = (List<Long>) params.get("item_id");
			if (skuFilterItemIds != null && !skuFilterItemIds.isEmpty()) {
				params.put("default_item_id", new ArrayList<>(skuFilterItemIds));
				params.remove("item_id");
			}
			// Front SKU list: always no page limit (-1) before spec rows are expanded; Admin list may paginate in this path.
			Map<String, Object> skuCore = getSkuItemsList(companyId, params, page, -1);
			result = new LinkedHashMap<>(skuCore);
		} else {
			if (pageSize <= 0) {
				pageSize = 10;
			}
			Map<String, Object> listCore = getItemsListPaged(companyId, params, page, pageSize, goodsSortRaw);
			result = new LinkedHashMap<>(listCore);

			List<Long> itemCatLongs = new ArrayList<>();
			@SuppressWarnings("unchecked")
			Collection<?> ic = (Collection<?>) params.get("item_category");
			if (ic != null) {
				for (Object o : ic) {
					if (o == null) {
						continue;
					}
					try {
						itemCatLongs.add(Long.parseLong(o.toString().trim()));
					} catch (NumberFormatException ignored) {
					}
				}
			}
			Long distributorId = parseOptionalLong(request.getParameter("distributor_id"));
			String brandKw = ValuePresence.hasEffectiveValue(keywords) ? keywords.trim() : null;
			PointsmallFrontBrandFilter brandFilter = new PointsmallFrontBrandFilter(companyId, itemCatLongs, resolvedSaleCategoryId, brandKw,
					distributorId);
			Map<String, Object> brandEnvelope = pointsmallFrontBrandListService.getBrandList(brandFilter);
			Object bl = brandEnvelope.get("brand_list");
			result.put("brand_list", bl != null ? bl : List.of());
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("list");
		if (rows != null && !rows.isEmpty()) {
			itemsListMultiLangApplier.applyToRows(companyId, countryCode, rows);
			for (Map<String, Object> row : rows) {
				Object in = row.get("item_name");
				row.put("itemName", in != null ? in : "");
			}
		}

		appendCurTdk(request, companyId, result);
		return result;
	}

	@Override
	public Map<String, Object> lovelyList(HttpServletRequest request, long companyId, long excludeItemId) {
		String countryCode = resolveCountryCode(request);
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("item_id|neq", excludeItemId);
		params.put("approve_status", List.of("onsale", "only_show"));
		params.put("audit_status", "approved");
		params.put("item_type", "normal");
		params.put("is_default", Boolean.TRUE);

		Map<String, Object> paged = getItemsListPaged(companyId, params, 1, 10, "1");
		LinkedHashMap<String, Object> result = new LinkedHashMap<>(paged);

		PointsmallFrontBrandFilter brandFilter = new PointsmallFrontBrandFilter(companyId, List.of(), null, null, null);
		Map<String, Object> brandEnvelope = pointsmallFrontBrandListService.getBrandList(brandFilter);
		Object bl = brandEnvelope.get("brand_list");
		result.put("brand_list", bl != null ? bl : List.of());

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("list");
		if (rows != null && !rows.isEmpty()) {
			itemsListMultiLangApplier.applyToRows(companyId, countryCode, rows);
			for (Map<String, Object> row : rows) {
				Object in = row.get("item_name");
				row.put("itemName", in != null ? in : "");
			}
		}

		putDefaultCur(result, companyId);
		return result;
	}

	private void putDefaultCur(Map<String, Object> result, long companyId) {
		CurrencyExchangeRate row = companyDefaultCurrencyService.getCur(companyId);
		LinkedHashMap<String, Object> curMap = new LinkedHashMap<>();
		if (row.getId() != null) {
			curMap.put("id", String.valueOf(row.getId()));
		}
		curMap.put("company_id", String.valueOf(companyId));
		curMap.put("currency", row.getCurrency());
		curMap.put("title", row.getTitle());
		curMap.put("symbol", row.getSymbol());
		curMap.put("rate", row.getRate());
		curMap.put("is_default", Boolean.TRUE.equals(row.getIsDefault()));
		if (row.getUsePlatform() != null) {
			curMap.put("use_platform", row.getUsePlatform());
		}
		result.put("cur", curMap);
	}

	private void appendCurTdk(HttpServletRequest request, long companyId, LinkedHashMap<String, Object> result) {
		putDefaultCur(result, companyId);

		String isTdk = request.getParameter("is_tdk");
		if (isTdk != null && "1".equals(isTdk.trim())) {
			String countryCodeRaw = request.getParameter("country_code");
			Map<String, Object> tdkMap = tdkGivenSaveService.getGivenSetInfo("list", companyId, countryCodeRaw);
			result.put("tdk_data", tdkMap);
		}
	}

	private static Long resolveSaleCategoryId(HttpServletRequest request) {
		String c1 = request.getParameter("category_id");
		if (ValuePresence.hasEffectiveValue(c1)) {
			try {
				return toLong(c1.trim());
			} catch (Exception ignored) {
				return null;
			}
		}
		String c2 = request.getParameter("category");
		if (ValuePresence.hasEffectiveValue(c2)) {
			try {
				return toLong(c2.trim());
			} catch (Exception ignored) {
				return null;
			}
		}
		return null;
	}

	private static Long parseOptionalLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v == 0L ? null : v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String resolveCountryCode(HttpServletRequest request) {
		String q = request.getParameter("country_code");
		if (StringUtils.hasText(q)) {
			String t = q.trim();
			if (!t.isEmpty()) {
				return t;
			}
		}
		String resolved = RequestLangTag.current(langueProperties);
		if (StringUtils.hasText(resolved) && !"zh-CN".equals(resolved)) {
			return resolved;
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw instanceof Map<?, ?> ud) {
			Object cc = ud.get("country_code");
			if (cc != null) {
				String s = cc.toString().trim();
				if (!s.isEmpty()) {
					return s;
				}
			}
		}
		return "zh-CN";
	}

	/** 前台 SKU 列表仅按 {@code item_id} 降序，忽略 {@code goodsSort}（与默认商品分页列表不同）。 */
	private Map<String, Object> getSkuItemsList(long companyId, Map<String, Object> params, int page, int pageSize) {
		int p = page < 1 ? 1 : page;
		int ps = pageSize > 2000 ? 2000 : pageSize;
		LambdaQueryWrapper<PointsmallItems> countW = new LambdaQueryWrapper<>();
		PointsmallItemsExportQuerySupport.applyParams(countW, params);
		long total = pointsmallItemsMapper.selectCount(countW);

		LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<>();
		PointsmallItemsExportQuerySupport.applyParams(w, params);
		w.orderByDesc(PointsmallItems::getItemId);
		List<PointsmallItems> entities;
		if (ps < 0) {
			entities = pointsmallItemsMapper.selectList(w);
		} else {
			Page<PointsmallItems> pg = new Page<>(p, ps, false);
			entities = pointsmallItemsMapper.selectPage(pg, w).getRecords();
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (PointsmallItems e : entities) {
			list.add(entityToListRow(e));
		}
		applySkuSpecAndStore(companyId, list);
		return Map.of("total_count", total, "list", list);
	}

	private Map<String, Object> getItemsListPaged(long companyId, Map<String, Object> params, int page, int pageSize, String goodsSortRaw) {
		int p = page < 1 ? 1 : page;
		int ps = pageSize > 2000 ? 2000 : pageSize;
		if (ps <= 0) {
			ps = 10;
		}

		LambdaQueryWrapper<PointsmallItems> base = new LambdaQueryWrapper<>();
		PointsmallItemsExportQuerySupport.applyParams(base, params);
		long total = pointsmallItemsMapper.selectCount(base);

		LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<>();
		PointsmallItemsExportQuerySupport.applyParams(w, params);
		applyGoodsSort(w, goodsSortRaw);
		Page<PointsmallItems> pg = new Page<>(p, ps, false);
		List<PointsmallItems> entities = pointsmallItemsMapper.selectPage(pg, w).getRecords();

		List<Long> pageItemIds = entities.stream().map(PointsmallItems::getItemId).filter(Objects::nonNull).toList();
		Map<Long, List<Long>> catsByItem = loadItemCatIds(companyId, pageItemIds);

		List<Map<String, Object>> list = new ArrayList<>();
		for (PointsmallItems e : entities) {
			Map<String, Object> row = entityToListRow(e);
			Long iid = e.getItemId();
			row.put("item_cat_id", catsByItem.getOrDefault(iid, List.of()));
			appendTypeLabels(row);
			list.add(row);
		}
		appendGoodsStoreAsStrings(companyId, list);
		return Map.of("total_count", total, "list", list);
	}

	private static void applyGoodsSort(LambdaQueryWrapper<PointsmallItems> w, String goodsSortRaw) {
		String g = goodsSortRaw == null ? "" : goodsSortRaw.trim();
		switch (g) {
			case "1" -> w.orderByDesc(PointsmallItems::getSales);
			case "2" -> w.orderByDesc(PointsmallItems::getPoint);
			case "3" -> w.orderByAsc(PointsmallItems::getPoint);
			case "4" -> w.orderByDesc(PointsmallItems::getCreated);
			default -> w.orderByDesc(PointsmallItems::getSort);
		}
		w.orderByDesc(PointsmallItems::getItemId);
	}

	private Map<Long, List<Long>> loadItemCatIds(long companyId, List<Long> itemIds) {
		Map<Long, List<Long>> out = new LinkedHashMap<>();
		if (itemIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<PointsmallItemsRelCats> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItemsRelCats::getCompanyId, companyId).in(PointsmallItemsRelCats::getItemId, itemIds);
		for (PointsmallItemsRelCats r : pointsmallItemsRelCatsMapper.selectList(w)) {
			out.computeIfAbsent(r.getItemId(), k -> new ArrayList<>()).add(r.getCategoryId());
		}
		return out;
	}

	private void applySkuSpecAndStore(long companyId, List<Map<String, Object>> rows) {
		if (rows.isEmpty()) {
			return;
		}
		List<Long> itemIds = rows.stream().map(r -> toLong(r.get("item_id"))).filter(id -> id > 0).distinct().toList();
		if (itemIds.isEmpty()) {
			return;
		}

		LambdaQueryWrapper<PointsmallItemRelAttributes> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItemRelAttributes::getCompanyId, companyId).eq(PointsmallItemRelAttributes::getAttributeType, "item_spec")
				.in(PointsmallItemRelAttributes::getItemId, itemIds).orderByAsc(PointsmallItemRelAttributes::getAttributeSort);
		List<PointsmallItemRelAttributes> prel = pointsmallItemRelAttributesMapper.selectList(w);
		Map<Long, List<ItemRelAttributes>> byItem = prel.stream().map(PointsmallItemsFrontListServiceImpl::toGoodsRel)
				.collect(Collectors.groupingBy(ItemRelAttributes::getItemId));

		List<Long> defaultIdsForSum = rows.stream().map(r -> {
			long def = toLong(r.get("default_item_id"));
			long iid = toLong(r.get("item_id"));
			return def > 0 ? def : iid;
		}).filter(id -> id > 0).distinct().toList();
		Map<Long, Integer> goodsStoreByDefault = sumGoodsStore(companyId, defaultIdsForSum);

		for (Map<String, Object> row : rows) {
			row.put("is_gift", 0);
			normalizeItemType(row);
			long itemId = toLong(row.get("item_id"));
			long def = toLong(row.get("default_item_id"));
			if (def <= 0) {
				row.put("default_item_id", itemId);
				def = itemId;
			}
			row.put("goods_store", String.valueOf(goodsStoreByDefault.getOrDefault(def, 0)));

			List<ItemRelAttributes> relRows = byItem.getOrDefault(itemId, List.of());
			if (relRows.isEmpty()) {
				row.put("item_spec", new ArrayList<>());
				row.put("item_spec_desc", "");
			} else {
				ItemsRelAttrValuesQueryService.ItemDetailAttrData ad = itemsRelAttrValuesQueryService.assemble(companyId, relRows);
				List<Map<String, Object>> specList = new ArrayList<>(ad.itemSpecNested.getOrDefault(itemId, Map.of()).values());
				row.put("item_spec", specList);
				StringBuilder desc = new StringBuilder();
				for (Map<String, Object> s : specList) {
					Object n = s.get("spec_name");
					Object v = s.get("spec_value_name");
					if (desc.length() > 0) {
						desc.append(',');
					}
					desc.append(n != null ? n : "").append(':').append(v != null ? v : "");
				}
				row.put("item_spec_desc", desc.toString());
			}
			appendTypeLabels(row);
		}
	}

	private static void appendTypeLabels(Map<String, Object> row) {
		row.put("type_labels", List.of());
	}

	private void appendGoodsStoreAsStrings(long companyId, List<Map<String, Object>> rows) {
		if (rows.isEmpty()) {
			return;
		}
		List<Long> defaultIdsForSum = rows.stream().map(r -> {
			long def = toLong(r.get("default_item_id"));
			long iid = toLong(r.get("item_id"));
			return def > 0 ? def : iid;
		}).filter(id -> id > 0).distinct().toList();
		Map<Long, Integer> goodsStoreByDefault = sumGoodsStore(companyId, defaultIdsForSum);
		for (Map<String, Object> row : rows) {
			long itemId = toLong(row.get("item_id"));
			long def = toLong(row.get("default_item_id"));
			if (def <= 0) {
				def = itemId;
			}
			row.put("goods_store", String.valueOf(goodsStoreByDefault.getOrDefault(def, 0)));
		}
	}

	private Map<Long, Integer> sumGoodsStore(long companyId, List<Long> defaultIds) {
		Map<Long, Integer> out = new LinkedHashMap<>();
		if (defaultIds.isEmpty()) {
			return out;
		}
		for (Map<String, Object> m : pointsmallItemsMapper.sumStoreByDefaultItemIds(companyId, defaultIds)) {
			Object did = m.get("did");
			Object total = m.get("total");
			long key = did instanceof Number n ? n.longValue() : Long.parseLong(did.toString());
			int val = total instanceof Number n ? n.intValue() : Integer.parseInt(total.toString());
			out.put(key, val);
		}
		return out;
	}

	private Object decodePicsForListRow(String raw) {
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

	private Object decodeIntroForListRow(String raw) {
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

	private Map<String, Object> entityToListRow(PointsmallItems e) {
		Map<String, Object> m = new LinkedHashMap<>();
		putNum(m, "item_id", e.getItemId());
		m.put("item_type", e.getItemType());
		m.put("consume_type", e.getConsumeType() != null ? e.getConsumeType() : "every");
		putBool(m, "is_show_specimg", e.getIsShowSpecimg());
		putNum(m, "store", e.getStore());
		m.put("barcode", nzStr(e.getBarcode()));
		putNullableInt(m, "sales", e.getSales());
		m.put("approve_status", nzStr(e.getApproveStatus()));
		putNum(m, "cost_price", e.getCostPrice());
		putNum(m, "point", e.getPoint());
		putNum(m, "goods_id", e.getGoodsId());
		putNum(m, "brand_id", e.getBrandId());
		m.put("item_name", nzStr(e.getItemName()));
		m.put("item_unit", nzStr(e.getItemUnit()));
		m.put("item_bn", nzStr(e.getItemBn()));
		m.put("brief", nzStr(e.getBrief()));
		m.put("intro", decodeIntroForListRow(e.getIntro()));
		putNum(m, "price", e.getPrice());
		putNum(m, "market_price", e.getMarketPrice());
		m.put("special_type", nzStr(e.getSpecialType()));
		m.put("goods_function", nzStr(e.getGoodsFunction()));
		m.put("goods_series", nzStr(e.getGoodsSeries()));
		putNum(m, "volume", e.getVolume());
		m.put("goods_color", nzStr(e.getGoodsColor()));
		m.put("goods_brand", nzStr(e.getGoodsBrand()));
		m.put("item_address_province", nzStr(e.getItemAddressProvince()));
		m.put("item_address_city", nzStr(e.getItemAddressCity()));
		m.put("regions_id", nzStr(e.getRegionsId()));
		m.put("brand_logo", nzStr(e.getBrandLogo()));
		putNum(m, "sort", e.getSort());
		putNullableInt(m, "templates_id", e.getTemplatesId());
		putBool(m, "is_default", e.getIsDefault());
		m.put("nospec", e.getNospec());
		putNum(m, "default_item_id", e.getDefaultItemId());
		m.put("pics", decodePicsForListRow(e.getPics()));
		putNum(m, "company_id", e.getCompanyId());
		putBool(m, "enable_agreement", e.getEnableAgreement());
		m.put("purchase_agreement", nzStr(e.getPurchaseAgreement()));
		m.put("date_type", nzStr(e.getDateType()));
		m.put("item_category", nzStr(e.getItemCategory()));
		putNum(m, "weight", e.getWeight());
		putNum(m, "begin_date", e.getBeginDate());
		putNum(m, "end_date", e.getEndDate());
		putNum(m, "fixed_term", e.getFixedTerm());
		putNum(m, "tax_rate", e.getTaxRate());
		putNum(m, "created", e.getCreated());
		putNum(m, "updated", e.getUpdated());
		m.put("video_type", nzStr(e.getVideoType()));
		m.put("videos", nzStr(e.getVideos()));
		m.put("video_pic_url", nzStr(e.getVideoPicUrl()));
		m.put("audit_status", nzStr(e.getAuditStatus()));
		m.put("audit_reason", nzStr(e.getAuditReason()));
		m.put("crossborder_tax_rate", nzStr(e.getCrossborderTaxRate()));
		putNum(m, "origincountry_id", e.getOrigincountryId());
		putNum(m, "type", e.getType());
		m.put("pay_class", nzStr(e.getPayClass()));

		m.put("itemId", m.get("item_id"));
		m.put("consumeType", m.get("consume_type"));
		m.put("itemName", m.get("item_name"));
		m.put("itemBn", m.get("item_bn"));
		m.put("companyId", m.get("company_id"));
		m.put("item_main_cat_id", m.get("item_category"));
		m.put("nospec", nospecToBool(m.get("nospec")));
		return m;
	}

	private static void normalizeItemType(Map<String, Object> row) {
		Object itype = row.get("item_type");
		if (itype == null || !StringUtils.hasText(itype.toString())) {
			row.put("item_type", "services");
		}
	}

	private static Object nospecToBool(Object nospec) {
		if (nospec instanceof Boolean b) {
			return b;
		}
		String s = nospec == null ? "" : nospec.toString();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static void putNum(Map<String, Object> m, String k, Object v) {
		if (v instanceof Number n) {
			m.put(k, n);
		} else {
			m.put(k, v);
		}
	}

	private static void putNullableInt(Map<String, Object> m, String k, Integer v) {
		m.put(k, v);
	}

	private static void putBool(Map<String, Object> m, String k, Boolean v) {
		m.put(k, v != null ? v : false);
	}

	private static String nzStr(String s) {
		return s != null ? s : "";
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

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return (int) Double.parseDouble(o.toString().trim());
	}

	private static boolean numericNonZero(Object v) {
		if (v == null) {
			return false;
		}
		try {
			if (v instanceof Number n) {
				return n.doubleValue() != 0d;
			}
			return new BigDecimal(v.toString().trim()).compareTo(BigDecimal.ZERO) != 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Object toPointFilterValue(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return new BigDecimal(v.toString().trim()).intValue();
		} catch (Exception e) {
			return v;
		}
	}

	private static boolean isQueryParamTruthyAfterDefaultZero(HttpServletRequest request, String paramName) {
		if (!request.getParameterMap().containsKey(paramName)) {
			return false;
		}
		String raw = request.getParameter(paramName);
		if (raw == null) {
			return false;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return false;
		}
		if ("0".equals(s)) {
			return false;
		}
		try {
			BigDecimal bd = new BigDecimal(s);
			if (bd.compareTo(BigDecimal.ZERO) == 0 && s.indexOf('.') < 0 && s.toLowerCase().indexOf('e') < 0) {
				String digits = s.startsWith("-") ? s.substring(1) : s;
				return digits.length() > 1 && digits.chars().allMatch(ch -> ch == '0');
			}
		} catch (NumberFormatException ignored) {
		}
		return true;
	}

	private static List<Long> readLongListParameter(HttpServletRequest request, String name) {
		String[] vals = request.getParameterValues(name);
		if (vals == null || vals.length == 0) {
			return null;
		}
		List<Long> out = new ArrayList<>();
		for (String v : vals) {
			if (!StringUtils.hasText(v)) {
				continue;
			}
			out.add(toLong(v.trim()));
		}
		return out.isEmpty() ? null : out;
	}
}
