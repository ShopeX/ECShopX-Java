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

package cn.shopex.ecshopx.goods.service.distributor;

import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorItemsExportFilterBuilder {

	public Map<String, Object> build(long companyId, Map<String, Object> merged) {
		long distributorId = longVal(merged.get("distributor_id"));
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("distributor_id", distributorId);
		filter.put("item_type", "normal");

		List<Long> goodsIds = parseGoodsIds(merged.get("goods_ids"));
		if (goodsIds != null && !goodsIds.isEmpty()) {
			filter.put(ItemsListQueryRepository.KEY_ITEM_ID_OR_DEFAULT_IDS, goodsIds);
		}

		String keywords = str(merged.get("keywords"));
		if (StringUtils.hasText(keywords)) {
			filter.put("item_name", keywords.trim());
		}

		Object canSale = merged.get("is_can_sale");
		if (canSale != null) {
			String s = canSale.toString().trim();
			if ("true".equals(s)) {
				filter.put("__dist_is_can_sale_filter", Boolean.TRUE);
			} else if ("false".equals(s)) {
				filter.put("__dist_is_can_sale_filter", Boolean.FALSE);
			}
		}

		return filter;
	}

	private static List<Long> parseGoodsIds(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				if (o instanceof Map<?, ?> mm && mm.get("item_id") != null) {
					out.add(longVal(mm.get("item_id")));
				} else {
					out.add(longVal(o));
				}
			}
			out = out.stream().filter(id -> id > 0).distinct().toList();
			return out.isEmpty() ? null : new ArrayList<>(out);
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		List<Long> out = new ArrayList<>();
		for (String part : s.split(",")) {
			String t = part.trim();
			if (StringUtils.hasText(t)) {
				out.add(Long.parseLong(t));
			}
		}
		return out.isEmpty() ? null : out;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
