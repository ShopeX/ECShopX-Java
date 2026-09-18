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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MarketingActivityItemListRelItemsQuerySupport {

	private final MarketingActivityItemsMapper marketingActivityItemsMapper;

	public MarketingActivityItemListRelItemsQuerySupport(MarketingActivityItemsMapper marketingActivityItemsMapper) {
		this.marketingActivityItemsMapper = marketingActivityItemsMapper;
	}

	public Optional<MarketingActivityItems> findFirstRelForType(
			long companyId, Long marketingId, boolean onlyIsShowRelRows) {
		if (marketingId == null) {
			return Optional.empty();
		}
		LambdaQueryWrapper<MarketingActivityItems> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivityItems::getCompanyId, companyId)
				.eq(MarketingActivityItems::getMarketingId, marketingId);
		if (onlyIsShowRelRows) {
			w.eq(MarketingActivityItems::getIsShow, Boolean.TRUE);
			w.orderByDesc(MarketingActivityItems::getItemId).last("LIMIT 1");
		} else {
			w.orderByAsc(MarketingActivityItems::getId).last("LIMIT 1");
		}
		return Optional.ofNullable(marketingActivityItemsMapper.selectOne(w));
	}

	public Map<String, Object> listAllForTagBrandCategory(
			long companyId, Long marketingId, boolean onlyIsShowRelRows) {
		if (marketingId == null) {
			return emptyRel();
		}
		LambdaQueryWrapper<MarketingActivityItems> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivityItems::getCompanyId, companyId).eq(MarketingActivityItems::getMarketingId, marketingId);
		if (onlyIsShowRelRows) {
			w.eq(MarketingActivityItems::getIsShow, Boolean.TRUE);
			w.orderByDesc(MarketingActivityItems::getItemId);
		}
		List<MarketingActivityItems> rows = marketingActivityItemsMapper.selectList(w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (MarketingActivityItems row : rows) {
			list.add(relRowToMap(row));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", (long) list.size());
		out.put("list", list);
		return out;
	}

	public Map<String, Object> listPagedForNormal(
			long companyId, Long marketingId, int page, int pageSize, boolean onlyIsShowRelRows) {
		if (marketingId == null) {
			return emptyRel();
		}
		int p = Math.max(1, page);
		int ps = Math.max(1, pageSize);
		LambdaQueryWrapper<MarketingActivityItems> w = new LambdaQueryWrapper<>();
		w.eq(MarketingActivityItems::getCompanyId, companyId).eq(MarketingActivityItems::getMarketingId, marketingId);
		if (onlyIsShowRelRows) {
			w.eq(MarketingActivityItems::getIsShow, Boolean.TRUE);
			w.orderByDesc(MarketingActivityItems::getItemId);
		}
		Page<MarketingActivityItems> pg = new Page<>(p, ps);
		Page<MarketingActivityItems> result = marketingActivityItemsMapper.selectPage(pg, w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (MarketingActivityItems row : result.getRecords()) {
			list.add(relRowToMap(row));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", result.getTotal());
		out.put("list", list);
		return out;
	}

	private static Map<String, Object> relRowToMap(MarketingActivityItems row) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (row.getItemId() != null) {
			m.put("item_id", row.getItemId());
		}
		return m;
	}

	private static Map<String, Object> emptyRel() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("total_count", 0L);
		m.put("list", List.of());
		return m;
	}
}
