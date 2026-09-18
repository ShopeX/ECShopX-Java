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

package cn.shopex.ecshopx.goods.service.pagestemplate;

import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PcTemplateDecorationItemPriceResolveService {

	private final PagesTemplateDecorationItemsQueryService pagesTemplateDecorationItemsQueryService;
	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final ObjectMapper objectMapper;

	public Map<Long, JsonNode> resolvePriceByItemIds(
			long companyId,
			long userId,
			List<Long> itemIds,
			String acceptLanguageHeader) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		LinkedHashSet<Long> dedup = new LinkedHashSet<>();
		for (Long id : itemIds) {
			if (id != null && id > 0L) {
				dedup.add(id);
			}
		}
		if (dedup.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Long> ids = new ArrayList<>(dedup);
		List<Map<String, Object>> rows =
				pagesTemplateDecorationItemsQueryService.queryItemListData(companyId, ids, 0L, Collections.emptyMap());
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyMap();
		}
		wxappGoodsItemsListMemberPriceApplyService.applyForRows(companyId, userId, rows, acceptLanguageHeader);
		Map<Long, JsonNode> out = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			Object idObj = row.get("item_id");
			if (idObj == null) {
				continue;
			}
			long itemId = toLongItemId(idObj);
			if (itemId <= 0L) {
				continue;
			}
			Object p = row.get("price");
			Object mp = row.get("member_price");
			Object ap = row.get("activity_price");
			if (toCents(mp) > 0L) {
				p = mp;
			}
			if (toCents(ap) > 0L) {
				p = ap;
			}
			out.put(itemId, objectMapper.valueToTree(p));
		}
		return out;
	}

	private static long toLongItemId(Object idObj) {
		if (idObj instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(idObj).trim());
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static long toCents(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return new BigDecimal(t).movePointRight(2).longValue();
			} catch (NumberFormatException | ArithmeticException ex) {
				return 0L;
			}
		}
		return 0L;
	}
}
