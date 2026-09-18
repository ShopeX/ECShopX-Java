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

import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.port.LimitPromotionAdminGoodsSupportPort;
import cn.shopex.ecshopx.promotions.service.seckill.SeckillAssocUniqueByGoodsId;
import cn.shopex.ecshopx.promotions.service.seckill.SeckillRelGoodsListRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SeckillActivityItemListService {

	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort;

	public SeckillActivityItemListService(
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort) {
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.limitPromotionAdminGoodsSupportPort = limitPromotionAdminGoodsSupportPort;
	}

	public Map<String, Object> getSeckillItemList(
			long companyId, Long seckillId, int page, int pageSize, boolean isSku) {
		int effectivePageSize = pageSize;
		if (!isSku) {
			effectivePageSize = 2000;
		}

		LambdaQueryWrapper<SeckillRelGoods> w = new LambdaQueryWrapper<>();
		w.eq(SeckillRelGoods::getCompanyId, companyId).eq(SeckillRelGoods::getSeckillId, seckillId);
		w.orderByDesc(SeckillRelGoods::getSort)
				.orderByDesc(SeckillRelGoods::getItemId)
				.orderByDesc(SeckillRelGoods::getActivityStartTime);

		Page<SeckillRelGoods> p = new Page<>(page, effectivePageSize);
		seckillRelGoodsMapper.selectPage(p, w);
		long total = p.getTotal();
		if (total == 0) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", "");
			empty.put("activity", "");
			return empty;
		}

		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		Map<String, Object> lastActivity = new LinkedHashMap<>();
		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (SeckillRelGoods e : p.getRecords()) {
			listMaps.add(
					SeckillRelGoodsListRowMapper.toRow(
							e,
							nowSec,
							payload -> {
								lastActivity.clear();
								lastActivity.putAll(payload);
							}));
		}

		List<Long> itemIds =
				listMaps.stream()
						.map(m -> m.get("item_id"))
						.filter(Objects::nonNull)
						.map(SeckillActivityItemListService::toLong)
						.filter(id -> id > 0L)
						.collect(Collectors.toList());

		Map<String, Object> skuPack =
				limitPromotionAdminGoodsSupportPort.loadSkuItemsList(companyId, itemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuList =
				(List<Map<String, Object>>) skuPack.getOrDefault("list", List.of());
		Map<Long, Map<String, Object>> itemByItemId = new LinkedHashMap<>();
		for (Map<String, Object> row : skuList) {
			long iid = toLong(row.get("item_id"));
			if (iid > 0L) {
				itemByItemId.put(iid, row);
			}
		}

		for (Map<String, Object> value : listMaps) {
			long itemId = toLong(value.get("item_id"));
			if (itemId <= 0L) {
				continue;
			}
			Map<String, Object> it = itemByItemId.get(itemId);
			if (it == null || it.get("goods_id") == null) {
				continue;
			}
			value.put("item_pic", firstElementOfPics(it.get("pics")));
			value.put("pics", it.get("pics"));
			value.put("price", intFromMerge(it.get("price")));
			value.put("market_price", intFromMerge(it.get("market_price")));
			value.put("item_name", it.get("item_name"));
			value.put("nospec", it.get("nospec"));
			String brandLogo = it.get("brand_logo") != null ? it.get("brand_logo").toString() : "";
			if (!StringUtils.hasText(brandLogo)) {
				brandLogo = "";
			}
			value.put("brand_logo", brandLogo);
			Object st = it.get("special_type");
			value.put("special_type", st != null ? st.toString() : "normal");
			value.put("goods_id", intFromMerge(it.get("goods_id")));
			value.put("distributor_id", intFromMerge(it.get("distributor_id")));

			int actStore = intStore(value.get("activity_store"));
			int sales = intStore(value.get("sales_store"));
			value.put("activity_store", actStore - sales);
		}

		long totalCount = total;
		if (!isSku) {
			listMaps = SeckillAssocUniqueByGoodsId.apply(listMaps);
			listMaps.sort(Comparator.comparingInt(SeckillActivityItemListService::sortKeyStable));
			totalCount = listMaps.size();
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listMaps);
		if (seckillId != null) {
			out.put("activity", lastActivity.isEmpty() ? null : lastActivity);
		}
		return out;
	}

	private static int sortKeyStable(Map<String, Object> row) {
		Object s = row.get("sort");
		if (s instanceof Number n) {
			return n.intValue();
		}
		if (s == null) {
			return 0;
		}
		try {
			return Integer.parseInt(s.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String firstElementOfPics(Object pics) {
		if (pics == null) {
			return "";
		}
		if (pics instanceof Collection<?> c) {
			for (Object o : c) {
				if (o == null) {
					continue;
				}
				String s = o.toString().trim();
				if (StringUtils.hasText(s)) {
					return s;
				}
			}
			return "";
		}
		if (pics instanceof String s) {
			return firstPicFromString(s);
		}
		return pics.toString();
	}

	private static String firstPicFromString(String s) {
		s = s.trim();
		if (s.startsWith("[") && s.endsWith("]")) {
			String inner = s.substring(1, s.length() - 1).trim();
			if (inner.isEmpty()) {
				return "";
			}
			String[] parts = inner.split(",");
			if (parts.length > 0) {
				String p0 = parts[0].trim();
				if ((p0.startsWith("\"") && p0.endsWith("\"")) || (p0.startsWith("'") && p0.endsWith("'"))) {
					return p0.substring(1, p0.length() - 1);
				}
				return p0;
			}
		}
		return s;
	}

	private static int intFromMerge(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int intStore(Object o) {
		return intFromMerge(o);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
