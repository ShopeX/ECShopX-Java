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

package cn.shopex.ecshopx.goods.search;

import cn.shopex.ecshopx.goods.search.query.GoodsSearchEsException;
import cn.shopex.ecshopx.goods.search.query.GoodsSearchHit;
import cn.shopex.ecshopx.goods.search.query.GoodsSearchPageResult;
import cn.shopex.ecshopx.goods.search.query.GoodsSearchQuery;
import cn.shopex.ecshopx.goods.search.query.GoodsSearchQueryService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 前台商品列表 ES 入口：查询、按 ES 顺序回源拼装列表行。 */
@Service
public class GoodsSearchFacade {

	private static final Logger log = LoggerFactory.getLogger(GoodsSearchFacade.class);

	private final GoodsSearchQueryService queryService;
	private final WxappGoodsItemsListQueryOrchestrator listQueryOrchestrator;

	public GoodsSearchFacade(
			GoodsSearchQueryService queryService, WxappGoodsItemsListQueryOrchestrator listQueryOrchestrator) {
		this.queryService = queryService;
		this.listQueryOrchestrator = listQueryOrchestrator;
	}

	/**
	 * ES 召回 + DB hydrate；列表主价写入 {@code price}（分）。
	 * 调用方仍应走会员价展示字段回写（{@code member_price}/{@code vip_price} 等），勿覆盖 {@code price}。
	 *
	 * @throws GoodsSearchEsException ES 不可用且未走 fallback 时由上层处理
	 */
	public Map<String, Object> queryListWithHydration(
			long companyId, GoodsSearchQuery query, String acceptLanguage) {
		GoodsSearchPageResult page;
		try {
			page = queryService.search(query);
		} catch (GoodsSearchEsException e) {
			log.warn("goods ES search failed companyId={}: {}", companyId, e.getMessage());
			throw e;
		}
		if (page.hits().isEmpty()) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", page.totalCount());
			empty.put("list", List.of());
			return empty;
		}
		List<Long> orderedIds = page.hits().stream().map(GoodsSearchHit::defaultItemId).toList();
		boolean skipDealListStore = Boolean.TRUE.equals(query.listFilter().storeListable());
		List<Map<String, Object>> rows = listQueryOrchestrator.hydrateWxappItemsByIds(
				companyId, orderedIds, skipDealListStore, acceptLanguage);
		if (rows == null) {
			rows = List.of();
		}
		Map<Long, GoodsSearchHit> hitById =
				page.hits().stream().collect(Collectors.toMap(GoodsSearchHit::defaultItemId, h -> h, (a, b) -> a));
		List<Map<String, Object>> ordered = reorderAndApplyEsPrices(rows, orderedIds, hitById);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", page.totalCount());
		out.put("list", ordered);
		return out;
	}

	/** ES 水合写入的销售价；enrichment 未命中改价活动时用于兜底主价并清理内部字段。 */
	public static final String KEY_LIST_SALE_PRICE = "__list_sale_price";

	private static List<Map<String, Object>> reorderAndApplyEsPrices(
			List<Map<String, Object>> rows, List<Long> orderedIds, Map<Long, GoodsSearchHit> hitById) {
		Map<Long, Map<String, Object>> byKey = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			putRowKey(byKey, longVal(row.get("item_id")), row);
		}
		for (Map<String, Object> row : rows) {
			putRowKey(byKey, longVal(row.get("default_item_id")), row);
		}
		List<Map<String, Object>> ordered = new ArrayList<>(orderedIds.size());
		for (Long id : orderedIds) {
			Map<String, Object> row = byKey.get(id);
			if (row == null) {
				continue;
			}
			GoodsSearchHit hit = hitById.get(id);
			if (hit != null) {
				// 列表主价对齐 PHP：写销售价；活动价由 enrichment 单独填 activity_price。
				// effectivePriceFen 仅用于 ES 排序/筛选，不可覆盖 price，否则前端原价/划线价消失。
				long saleFen =
						hit.sourceDocument() != null ? hit.sourceDocument().salePrice() : 0L;
				if (saleFen > 0L) {
					row.put(KEY_LIST_SALE_PRICE, saleFen);
					row.put("price", saleFen);
				}
			}
			ordered.add(row);
		}
		return ordered;
	}

	private static void putRowKey(Map<Long, Map<String, Object>> byKey, long id, Map<String, Object> row) {
		if (id > 0 && !byKey.containsKey(id)) {
			byKey.put(id, row);
		}
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
