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

package cn.shopex.ecshopx.goods.search.query;

import cn.shopex.ecshopx.goods.search.price.SearchMemberTier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.util.StringUtils;

/** 前台商品列表 ES 查询参数。列表硬过滤与微信小程序默认商品列表的数据库条件对齐。 */
public record GoodsSearchQuery(
		long companyId,
		long distributorId,
		String keywords,
		List<Long> defaultItemIdAllowList,
		List<String> itemCategoryKeys,
		List<String> approveStatuses,
		String auditStatus,
		Integer priceMinFen,
		Integer priceMaxFen,
		String goodsSort,
		int page,
		int pageSize,
		SearchMemberTier tier,
		long nowEpochSec,
		GoodsSearchListFilter listFilter) {

	/** 标准店可售：回表前由编排写入，查询时过滤 {@code store_listable}。 */
	public static final String PARAM_ES_STORE_LISTABLE = "__es_store_listable";

	/** 非标准店或未开启可售：按商品自身 {@code items.distributor_id} 过滤。 */
	public static final String PARAM_ES_ITEM_DISTRIBUTOR_EQ = "__es_item_distributor_eq";

	public static final int MAX_WINDOW = 5000;

	public GoodsSearchQuery {
		if (listFilter == null) {
			listFilter = GoodsSearchListFilter.none();
		}
	}

	public GoodsSearchQuery withKeywords(String newKeywords) {
		return new GoodsSearchQuery(
				companyId,
				distributorId,
				newKeywords,
				defaultItemIdAllowList,
				itemCategoryKeys,
				approveStatuses,
				auditStatus,
				priceMinFen,
				priceMaxFen,
				goodsSort,
				page,
				pageSize,
				tier,
				nowEpochSec,
				listFilter);
	}

	public boolean hasPriceFilter() {
		return priceMinFen != null || priceMaxFen != null;
	}

	public boolean isPriceSort() {
		return "price_asc".equals(goodsSort)
				|| "price_desc".equals(goodsSort)
				|| "2".equals(goodsSort)
				|| "3".equals(goodsSort);
	}

	public boolean isPriceAscending() {
		return "price_asc".equals(goodsSort) || "3".equals(goodsSort);
	}

	public boolean isSalesSort() {
		return "sales".equals(goodsSort) || "1".equals(goodsSort) || "sales_desc".equals(goodsSort);
	}

	public boolean isNewSort() {
		return "new".equals(goodsSort) || "4".equals(goodsSort);
	}

	public static GoodsSearchQuery fromWxappParams(
			long companyId, LinkedHashMap<String, Object> params, int page, int pageSize, SearchMemberTier tier) {
		long distributorId = resolvePrimaryDistributorId(params.get("distributor_id"));
		String keywords = null;
		Object kwo = params.get("keywords");
		if (kwo != null && StringUtils.hasText(kwo.toString())) {
			keywords = kwo.toString().trim();
		}
		List<Long> allowIds = mergeIdAllowList(params);
		@SuppressWarnings("unchecked")
		List<String> itemCat = (List<String>) params.get("item_category");
		List<String> approve = List.of("onsale", "only_show");
		Object ap = params.get("approve_status");
		if (ap instanceof List<?> list && !list.isEmpty()) {
			approve = list.stream().filter(Objects::nonNull).map(Object::toString).toList();
		}
		String audit = params.getOrDefault("audit_status", "approved").toString();
		Integer priceMin = toIntObj(params.get("price|gte"));
		Integer priceMax = toIntObj(params.get("price|lte"));
		String goodsSort = params.get("goodsSort") != null ? params.get("goodsSort").toString().trim() : "";
		GoodsSearchListFilter listFilter = listFilterFromParams(params);
		return new GoodsSearchQuery(
				companyId,
				distributorId,
				keywords,
				allowIds,
				itemCat != null ? List.copyOf(itemCat) : List.of(),
				approve,
				audit,
				priceMin,
				priceMax,
				goodsSort,
				page,
				pageSize,
				tier,
				System.currentTimeMillis() / 1000L,
				listFilter);
	}

	private static GoodsSearchListFilter listFilterFromParams(LinkedHashMap<String, Object> params) {
		String itemType = textOrNull(params.get("item_type"));
		Boolean gift = params.containsKey("is_gift") ? truthyOne(params.get("is_gift")) : null;
		Boolean defaultItem = Boolean.TRUE.equals(params.get("is_default")) ? Boolean.TRUE : null;
		Long brandId = null;
		Integer brand = toIntObj(params.get("brand_id"));
		if (brand != null && brand != 0) {
			brandId = brand.longValue();
		}
		Integer itemCrossType = params.containsKey("type") ? toIntObj(params.get("type")) : null;
		boolean excludeCrossBorder = Boolean.TRUE.equals(params.get("__salesperson_exclude_type_1"));
		String regionsId = textOrNull(params.get("regions_id"));
		Integer rebate = params.containsKey("rebate") ? toIntObj(params.get("rebate")) : null;
		List<String> rebateTypes = stringList(params.get("rebate_type"));
		List<Long> goodsIds = longList(params.get("goods_id"));
		Boolean storeListable = Boolean.TRUE.equals(params.get(PARAM_ES_STORE_LISTABLE)) ? Boolean.TRUE : null;
		boolean matchItemDistributorId = Boolean.TRUE.equals(params.get(PARAM_ES_ITEM_DISTRIBUTOR_EQ));
		String storeStatus = textOrNull(params.get("store_status"));
		if (storeStatus != null && !"positive".equals(storeStatus) && !"zero".equals(storeStatus)) {
			storeStatus = null;
		}
		return new GoodsSearchListFilter(
				itemType,
				gift,
				defaultItem,
				brandId,
				itemCrossType,
				excludeCrossBorder,
				regionsId,
				rebate,
				rebateTypes,
				goodsIds,
				storeListable,
				matchItemDistributorId,
				storeStatus);
	}

	private static String textOrNull(Object v) {
		if (v == null) {
			return null;
		}
		String s = v.toString().trim();
		return StringUtils.hasText(s) ? s : null;
	}

	private static boolean truthyOne(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		if (v == null) {
			return false;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static List<String> stringList(Object raw) {
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (Object o : list) {
			if (o != null && StringUtils.hasText(o.toString())) {
				out.add(o.toString().trim());
			}
		}
		return List.copyOf(out);
	}

	private static List<Long> longList(Object raw) {
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object o : list) {
			long v = longVal(o);
			if (v > 0) {
				out.add(v);
			}
		}
		return List.copyOf(out);
	}

	private static List<Long> mergeIdAllowList(LinkedHashMap<String, Object> params) {
		List<Long> idOrDef = new ArrayList<>();
		@SuppressWarnings("unchecked")
		List<Long> itemIds = (List<Long>) params.get("item_id");
		if (itemIds != null) {
			itemIds.stream().filter(Objects::nonNull).filter(id -> id > 0).forEach(idOrDef::add);
		}
		@SuppressWarnings("unchecked")
		List<Long> catIds = (List<Long>) params.get("category_resolved_item_ids");
		if (catIds != null && !catIds.isEmpty()) {
			if (idOrDef.isEmpty()) {
				idOrDef.addAll(catIds);
			} else {
				var catSet = catIds.stream().filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
				idOrDef.removeIf(id -> !catSet.contains(id));
			}
		}
		if (idOrDef.isEmpty()) {
			return List.of();
		}
		if (idOrDef.size() == 1 && idOrDef.get(0) == -1L) {
			return List.of(-1L);
		}
		return List.copyOf(idOrDef);
	}

	private static long resolvePrimaryDistributorId(Object dist) {
		if (dist instanceof List<?> dl && !dl.isEmpty()) {
			return longVal(dl.get(0));
		}
		return longVal(dist);
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim().split(",")[0]);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Integer toIntObj(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
