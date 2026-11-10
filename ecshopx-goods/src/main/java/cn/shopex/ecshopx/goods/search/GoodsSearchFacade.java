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
	 * ES 召回 + DB hydrate；列表主价写入 {@code price}（分），调用方应跳过会员价主价重算。
	 *
	 * @throws GoodsSearchEsException ES 不可用且未走 fallback 时由上层处理
	 */
	public Map<String, Object> queryListWithHydration(
			long companyId,
			LinkedHashMap<String, Object> orchestrationParams,
			GoodsSearchQuery query,
			String acceptLanguage) {
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
		LinkedHashMap<String, Object> hydrateParams = new LinkedHashMap<>(orchestrationParams);
		hydrateParams.remove("__keyword_default_item_ids");
		hydrateParams.put("item_id", new ArrayList<>(orderedIds));
		hydrateParams.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE, 1);
		hydrateParams.put(
				WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE_SIZE, Math.min(50, Math.max(orderedIds.size(), 1)));
		hydrateParams.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_ACCEPT_LANGUAGE, acceptLanguage);
		Map<String, Object> dbPage = listQueryOrchestrator.queryItemListData(companyId, hydrateParams, List.of());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rows = (List<Map<String, Object>>) dbPage.get("list");
		if (rows == null) {
			rows = List.of();
		}
		Map<Long, Integer> priceByDefaultId =
				page.hits().stream().collect(Collectors.toMap(GoodsSearchHit::defaultItemId, GoodsSearchHit::effectivePriceFen, (a, b) -> a));
		List<Map<String, Object>> ordered = reorderAndApplyEsPrices(rows, orderedIds, priceByDefaultId);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", page.totalCount());
		out.put("list", ordered);
		return out;
	}

	private static List<Map<String, Object>> reorderAndApplyEsPrices(
			List<Map<String, Object>> rows, List<Long> orderedIds, Map<Long, Integer> priceByDefaultId) {
		Map<Long, Map<String, Object>> byKey = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			long key = rowDefaultItemId(row);
			if (key > 0) {
				byKey.put(key, row);
			}
		}
		List<Map<String, Object>> ordered = new ArrayList<>(orderedIds.size());
		for (Long id : orderedIds) {
			Map<String, Object> row = byKey.get(id);
			if (row == null) {
				continue;
			}
			Integer esPrice = priceByDefaultId.get(id);
			if (esPrice != null && esPrice > 0) {
				row.put("price", esPrice);
			}
			ordered.add(row);
		}
		return ordered;
	}

	private static long rowDefaultItemId(Map<String, Object> row) {
		Object def = row.get("default_item_id");
		if (def != null) {
			return longVal(def);
		}
		return longVal(row.get("item_id"));
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
