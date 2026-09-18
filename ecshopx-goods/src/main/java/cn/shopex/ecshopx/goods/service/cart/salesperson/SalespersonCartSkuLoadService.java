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

package cn.shopex.ecshopx.goods.service.cart.salesperson;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsListSkuSpecApplier;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SalespersonCartSkuLoadService {

	private final WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	private final DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier;
	private final LangueProperties langueProperties;

	public SalespersonCartSkuLoadService(WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator,
			DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier,
			LangueProperties langueProperties) {
		this.wxappGoodsItemsListQueryOrchestrator = wxappGoodsItemsListQueryOrchestrator;
		this.distributorItemsListSkuSpecApplier = distributorItemsListSkuSpecApplier;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> loadSkuItemsList(long companyId, long distributorId, long salespersonId, long userId,
			List<Long> itemIds, HttpServletRequest request) {
		if (itemIds == null || itemIds.isEmpty()) {
			return emptyPack();
		}
		List<Long> orderedDistinct = distinctPreserveOrder(itemIds);
		if (orderedDistinct.isEmpty()) {
			return emptyPack();
		}
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", companyId);
		params.put("user_id", userId);
		params.put("distributor_id", distributorId);
		params.put("item_id", orderedDistinct);
		params.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_ACCEPT_LANGUAGE, resolveAcceptLanguage(request));
		params.put("goodsSort", "");
		Map<String, Object> pack =
				wxappGoodsItemsListQueryOrchestrator.querySalespersonSkuItemsList(companyId, params, List.of());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) pack.get("list");
		if (list == null) {
			list = Collections.emptyList();
		} else {
			distributorItemsListSkuSpecApplier.apply(companyId, list);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", pack.get("total_count"));
		out.put("list", list);
		return out;
	}

	private static Map<String, Object> emptyPack() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", 0);
		out.put("list", Collections.emptyList());
		return out;
	}

	private static List<Long> distinctPreserveOrder(List<Long> itemIds) {
		Set<Long> seen = new LinkedHashSet<>();
		List<Long> out = new ArrayList<>();
		for (Long id : itemIds) {
			if (id == null || id <= 0L) {
				continue;
			}
			if (seen.add(id)) {
				out.add(id);
			}
		}
		return out;
	}

	private String resolveAcceptLanguage(HttpServletRequest request) {
		if (request == null) {
			return "zh-CN";
		}
		return RequestLangTag.current(langueProperties);
	}
}
