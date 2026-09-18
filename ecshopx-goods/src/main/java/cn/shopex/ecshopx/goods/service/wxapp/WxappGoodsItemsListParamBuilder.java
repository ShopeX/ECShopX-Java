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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsTagsQueryService;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardCardIdsByGoodsForListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxappGoodsItemsListParamBuilder {

	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsTagsQueryService itemsTagsQueryService;
	private final DiscountCardCardIdsByGoodsForListService discountCardCardIdsByGoodsForListService;

	public WxappGoodsItemsListParamBuilder(ItemsCategoryItemIdResolver itemsCategoryItemIdResolver,
			ItemsListQueryRepository itemsListQueryRepository,
			ItemsTagsQueryService itemsTagsQueryService,
			DiscountCardCardIdsByGoodsForListService discountCardCardIdsByGoodsForListService) {
		this.itemsCategoryItemIdResolver = itemsCategoryItemIdResolver;
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsTagsQueryService = itemsTagsQueryService;
		this.discountCardCardIdsByGoodsForListService = discountCardCardIdsByGoodsForListService;
	}

	public LinkedHashMap<String, Object> build(HttpServletRequest request, long companyId, long userId) {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("user_id", userId);

		String[] itemIdArr = request.getParameterValues("item_id");
		if (itemIdArr != null && itemIdArr.length > 0) {
			List<Long> itemIds = new ArrayList<>();
			for (String s : itemIdArr) {
				if (!StringUtils.hasText(s)) {
					continue;
				}
				try {
					itemIds.add(Long.parseLong(s.trim()));
				} catch (NumberFormatException ignored) {
					// skip invalid
				}
			}
			if (!itemIds.isEmpty()) {
				params.put("item_id", itemIds);
			}
		}

		String goodsIdParam = request.getParameter("goods_id");
		if (StringUtils.hasText(goodsIdParam)) {
			List<Long> gids = parseLongListCsv(goodsIdParam);
			if (!gids.isEmpty()) {
				params.put("goods_id", gids);
			}
		}

		String approveRaw = request.getParameter("approve_status");
		if (StringUtils.hasText(approveRaw)) {
			String[] parts = approveRaw.split(",", -1);
			List<String> tokens = new ArrayList<>();
			for (String p : parts) {
				tokens.add(p.trim());
			}
			boolean anyWhitelist = tokens.stream().anyMatch(t -> "onsale".equals(t) || "only_show".equals(t));
			if (anyWhitelist) {
				params.put("approve_status", tokens);
			} else {
				params.put("approve_status", List.of("onsale", "only_show"));
			}
		} else {
			params.put("approve_status", List.of("onsale", "only_show"));
		}

		params.put("audit_status", "approved");

		String categoryAlias = request.getParameter("category");
		if (ValuePresence.hasEffectiveValue(categoryAlias)) {
			params.put("category_id", categoryAlias.trim());
		}

		String keywords = request.getParameter("keywords");
		if (ValuePresence.hasEffectiveValue(keywords)) {
			String kw = keywords.trim();
			params.put("keywords", kw);
			LinkedHashSet<Long> merged = new LinkedHashSet<>();
			merged.addAll(itemsListQueryRepository.mergeDefaultItemIdsByItemNameOrBrief(companyId, kw));
			merged.addAll(itemsListQueryRepository.mergeDefaultItemIdsByItemBnOrBarcodeContains(companyId, kw));
			List<Long> tagIds = resolveTagIdsByKeyword(companyId, kw);
			if (!tagIds.isEmpty()) {
				List<String> itemIdStrs = itemsTagsQueryService.getItemIdsByTagIds(companyId, tagIds);
				for (String is : itemIdStrs) {
					try {
						merged.add(Long.parseLong(is.trim()));
					} catch (NumberFormatException ignored) {
						// skip
					}
				}
			}
			// 无命中时也必须写入空列表，否则编排层会跳过关键词过滤，退回全量商品
			params.put("__keyword_default_item_ids", new ArrayList<>(merged));
		}

		String itemName = request.getParameter("item_name");
		if (ValuePresence.hasEffectiveValue(itemName)) {
			params.put("item_name", itemName.trim());
		}

		String mainCat = request.getParameter("main_category");
		if (ValuePresence.hasEffectiveValue(mainCat)) {
			try {
				long mid = Long.parseLong(mainCat.trim());
				List<String> keys = itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, List.of(mid));
				params.put("item_category", keys);
			} catch (NumberFormatException ignored) {
				// skip
			}
		}

		String tagId = request.getParameter("tag_id");
		if (ValuePresence.hasEffectiveValue(tagId)) {
			params.put("tag_id", tagId.trim());
		}

		String[] itemParams = request.getParameterValues("item_params");
		if (itemParams != null && itemParams.length > 0) {
			params.put("item_params", List.of(itemParams));
		}

		Object regionsVal = firstOrArrayParam(request, "regions_id");
		if (regionsVal instanceof List<?> rlist && !rlist.isEmpty()) {
			String joined = rlist.stream().map(Object::toString).filter(StringUtils::hasText).map(String::trim).reduce((a, b) -> a + "," + b)
					.orElse("");
			if (StringUtils.hasText(joined)) {
				params.put("regions_id", joined);
			}
		} else if (regionsVal != null && StringUtils.hasText(regionsVal.toString())) {
			params.put("regions_id", regionsVal.toString().trim());
		}

		if (truthyNonZero(request.getParameter("start_price"))) {
			try {
				long yuan = Long.parseLong(request.getParameter("start_price").trim());
				params.put("price|gte", yuan * 100L);
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		if (truthyNonZero(request.getParameter("end_price"))) {
			try {
				long yuan = Long.parseLong(request.getParameter("end_price").trim());
				params.put("price|lte", yuan * 100L);
			} catch (NumberFormatException ignored) {
				// skip
			}
		}

		if (truthyNonZero(request.getParameter("brand_id"))) {
			try {
				params.put("brand_id", Integer.parseInt(request.getParameter("brand_id").trim()));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}

		String type = request.getParameter("type");
		if ("0".equals(type) || "1".equals(type)) {
			params.put("type", Integer.parseInt(type));
		}

		String itemType = request.getParameter("item_type");
		params.put("item_type", StringUtils.hasText(itemType) ? itemType.trim() : "services");

		String distributorRaw = request.getParameter("distributor_id");
		if (StringUtils.hasText(distributorRaw) && !"false".equalsIgnoreCase(distributorRaw.trim())) {
			if (distributorRaw.contains(",")) {
				List<Long> ds = parseLongListCsv(distributorRaw);
				if (!ds.isEmpty()) {
					params.put("distributor_id", ds);
					params.put("is_can_sale", Boolean.TRUE);
				}
			} else {
				try {
					long d = Long.parseLong(distributorRaw.trim());
					params.put("distributor_id", d);
					if (d != 0L) {
						params.put("is_can_sale", Boolean.TRUE);
					}
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}

		if ("0".equals(StringUtils.hasText(distributorRaw) ? distributorRaw.trim() : "")) {
			params.put("distributor_id", 0L);
		}

		params.put("is_gift", 0);

		String goodsSort = request.getParameter("goodsSort");
		params.put("goodsSort", StringUtils.hasText(goodsSort) ? goodsSort.trim() : "");

		if (truthyFlag(request.getParameter("is_promoter"))) {
			params.put("is_promoter", Boolean.TRUE);
			params.put("distributor_id", 0L);
		}

		String rebateType = request.getParameter("rebate_type");
		if (StringUtils.hasText(rebateType)) {
			if (rebateType.contains(",")) {
				params.put("rebate_type", List.of(rebateType.split(",")));
			} else {
				params.put("rebate_type", List.of(rebateType.trim()));
			}
		}

		String isDefault = request.getParameter("is_default");
		boolean defaultTrue = isDefault == null || !"false".equalsIgnoreCase(isDefault.trim());
		if (defaultTrue) {
			params.put("is_default", Boolean.TRUE);
		}

		String categoryId = request.getParameter("category_id");
		if (ValuePresence.hasEffectiveValue(categoryId)) {
			params.put("category_id", categoryId.trim());
		}

		Object categoryIdForTree = params.get("category_id");
		if (categoryIdForTree != null && ValuePresence.hasEffectiveValue(categoryIdForTree.toString())) {
			try {
				long cid = Long.parseLong(categoryIdForTree.toString().trim());
				List<Long> treeIds = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, cid);
				params.put("category_resolved_item_ids", treeIds.isEmpty() ? List.of(-1L) : treeIds);
			} catch (NumberFormatException ignored) {
				// skip
			}
		}

		String cardId = request.getParameter("card_id");
		if (ValuePresence.hasEffectiveValue(cardId)) {
			applyCardIdSideEffects(request, companyId, params, cardId.trim());
		}

		String isTdk = request.getParameter("is_tdk");
		if ("1".equals(isTdk != null ? isTdk.trim() : "")) {
			params.put("is_tdk", "1");
		}

		String isSalesmanPage = request.getParameter("isSalesmanPage");
		if (StringUtils.hasText(isSalesmanPage)) {
			params.put("isSalesmanPage", isSalesmanPage.trim());
		}
		String storeStatus = request.getParameter("store_status");
		if (StringUtils.hasText(storeStatus)) {
			params.put("store_status", storeStatus.trim());
		}

		return params;
	}

	private void applyCardIdSideEffects(HttpServletRequest request, long companyId, LinkedHashMap<String, Object> params, String cardId) {
		Map<String, Object> itemFilter = new LinkedHashMap<>();
		itemFilter.put("company_id", companyId);
		itemFilter.put("card_id", cardId);
		for (Map.Entry<String, Object> en : params.entrySet()) {
			String k = en.getKey();
			if (k.startsWith("__")) {
				continue;
			}
			if ("company_id".equals(k) || "user_id".equals(k)) {
				continue;
			}
			itemFilter.put(k, en.getValue());
		}
		discountCardCardIdsByGoodsForListService.resolveCardIdsByGoods(itemFilter);
		for (Map.Entry<String, Object> en : itemFilter.entrySet()) {
			String k = en.getKey();
			if ("card_id".equals(k)) {
				continue;
			}
			params.put(k, en.getValue());
		}
		params.put("card_id", cardId);
	}

	/**
	 * 按关键词拉取前台标签列表并收集 {@code tag_id}，用于与名称/货号等关键词命中结果合并召回商品。
	 */
	private List<Long> resolveTagIdsByKeyword(long companyId, String keyword) {
		Map<String, Object> env = itemsTagsQueryService.getTagsList(companyId, 0L, 1, 100, keyword, false, null, "zh-CN", null, null, null);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) env.get("list");
		if (list == null || list.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Object tid = row.get("tag_id");
			if (tid instanceof Number n) {
				out.add(n.longValue());
			} else if (tid != null) {
				try {
					out.add(Long.parseLong(tid.toString()));
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		return out;
	}

	private static Object firstOrArrayParam(HttpServletRequest request, String name) {
		String[] vals = request.getParameterValues(name);
		if (vals == null || vals.length == 0) {
			return null;
		}
		if (vals.length == 1) {
			return vals[0];
		}
		List<String> parts = new ArrayList<>();
		for (String v : vals) {
			if (StringUtils.hasText(v)) {
				parts.add(v.trim());
			}
		}
		return parts;
	}

	private static List<Long> parseLongListCsv(String raw) {
		String[] parts = raw.split(",");
		List<Long> out = new ArrayList<>();
		for (String p : parts) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				out.add(Long.parseLong(p.trim()));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}

	private static boolean truthyNonZero(String raw) {
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		try {
			return Long.parseLong(raw.trim()) != 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static boolean truthyFlag(String raw) {
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		String t = raw.trim();
		return "1".equals(t) || "true".equalsIgnoreCase(t);
	}
}
