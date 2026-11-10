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

/** 前台商品列表 ES 查询参数（与 wxapp 列表编排参数对齐的子集）。 */
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
		long nowEpochSec) {

	public static final int MAX_WINDOW = 5000;

	public boolean hasPriceFilter() {
		return priceMinFen != null || priceMaxFen != null;
	}

	public boolean isPriceSort() {
		return "price_asc".equals(goodsSort) || "price_desc".equals(goodsSort);
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
				System.currentTimeMillis() / 1000L);
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
		String tagIdStr = params.get("tag_id") != null ? params.get("tag_id").toString().trim() : "";
		if (StringUtils.hasText(tagIdStr)) {
			// tag 交集已在编排层写入 item_id；此处仅透传已有 item_id 列表
		}
		@SuppressWarnings("unchecked")
		List<Long> goodsIds = (List<Long>) params.get("goods_id");
		if (goodsIds != null && !goodsIds.isEmpty() && idOrDef.isEmpty()) {
			// goods_id 过滤在 hydrate 阶段由 DB 承接；ES 路径下 promoter 场景会预置 goods_id
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
