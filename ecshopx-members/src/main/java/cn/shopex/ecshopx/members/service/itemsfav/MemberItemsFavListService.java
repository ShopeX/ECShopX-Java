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

package cn.shopex.ecshopx.members.service.itemsfav;

import cn.shopex.ecshopx.common.members.port.MemberItemsFavListGoodsEnrichmentPort;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.members.domain.MemberItemsFav;
import cn.shopex.ecshopx.members.mapper.MemberItemsFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberItemsFavListService {

	private final MemberItemsFavMapper memberItemsFavMapper;
	private final MemberItemsFavListGoodsEnrichmentPort memberItemsFavListGoodsEnrichmentPort;

	public MemberItemsFavListService(
			MemberItemsFavMapper memberItemsFavMapper,
			MemberItemsFavListGoodsEnrichmentPort memberItemsFavListGoodsEnrichmentPort) {
		this.memberItemsFavMapper = memberItemsFavMapper;
		this.memberItemsFavListGoodsEnrichmentPort = memberItemsFavListGoodsEnrichmentPort;
	}

	public Map<String, Object> getItemsFavList(
			long companyId,
			long userId,
			int page,
			int pageSize,
			Long distributorIdOrNull,
			String acceptLanguageHeader) {
		LambdaQueryWrapper<MemberItemsFav> w = new LambdaQueryWrapper<>();
		w.eq(MemberItemsFav::getCompanyId, companyId)
				.eq(MemberItemsFav::getUserId, userId)
				.orderByDesc(MemberItemsFav::getFavId);
		Page<MemberItemsFav> mp = new Page<>(page, pageSize);
		memberItemsFavMapper.selectPage(mp, w);
		long totalCount = mp.getTotal();
		List<MemberItemsFav> records = mp.getRecords();
		if (records == null) {
			records = List.of();
		}

		List<Map<String, Object>> baseListMaps = new ArrayList<>();
		for (MemberItemsFav rec : records) {
			baseListMaps.add(favEntityToBaseRowMap(rec));
		}

		LinkedHashSet<Long> itemIds = new LinkedHashSet<>();
		for (MemberItemsFav r : records) {
			Long iid = r.getItemId();
			if (iid != null && iid > 0L) {
				itemIds.add(iid);
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		if (totalCount == 0L || itemIds.isEmpty()) {
			result.put("total_count", totalCount);
			result.put("list", baseListMaps);
			return result;
		}

		List<Map<String, Object>> enriched =
				memberItemsFavListGoodsEnrichmentPort.loadEnrichedRows(
						companyId,
						userId,
						new ArrayList<>(itemIds),
						distributorIdOrNull,
						pageSize,
						acceptLanguageHeader);

		Map<Long, Map<String, Object>> itemByItemId = new LinkedHashMap<>();
		for (Map<String, Object> erow : enriched) {
			long eid = longVal(erow.get("item_id"));
			if (eid > 0L) {
				itemByItemId.put(eid, erow);
			}
		}

		for (Map<String, Object> row : baseListMaps) {
			Long iid = null;
			Object rawId = row.get("item_id");
			if (rawId instanceof Number n) {
				iid = n.longValue();
			}
			Map<String, Object> item = iid != null ? itemByItemId.get(iid) : null;
			if (item != null) {
				if (ValuePresence.hasEffectiveValue(item.get("distributor_id"))) {
					row.put("distributor_id", item.get("distributor_id"));
				}
				if (item.containsKey("market_price") && item.get("market_price") != null) {
					putMarketPriceScalar(row, item.get("market_price"));
				}
				long basePrice = longVal(item.get("price"));
				long display;
				if (nonEmptyInt(item.get("activity_price"))) {
					display = intVal(item.get("activity_price"));
				} else if (nonEmptyInt(item.get("member_price"))) {
					display = intVal(item.get("member_price"));
				} else {
					display = basePrice;
				}
				row.put("price", display);
			}
			if (distributorIdOrNull != null && !ValuePresence.hasEffectiveValue(row.get("distributor_id"))) {
				row.put("distributor_id", distributorIdOrNull);
			}
		}

		result.put("total_count", totalCount);
		result.put("list", baseListMaps);
		return result;
	}

	private static void putMarketPriceScalar(Map<String, Object> row, Object raw) {
		try {
			BigDecimal bd;
			if (raw instanceof BigDecimal b) {
				bd = b;
			} else if (raw instanceof Number n) {
				bd = new BigDecimal(n.toString());
			} else {
				bd = new BigDecimal(raw.toString().trim());
			}
			if (bd.scale() <= 0) {
				row.put("market_price", bd.longValue());
			} else {
				row.put("market_price", bd.stripTrailingZeros().doubleValue());
			}
		} catch (NumberFormatException | ArithmeticException e) {
			row.put("market_price", raw);
		}
	}

	private static Map<String, Object> favEntityToBaseRowMap(MemberItemsFav e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("fav_id", e.getFavId());
		m.put("company_id", e.getCompanyId());
		m.put("user_id", e.getUserId());
		m.put("item_id", e.getItemId());
		m.put("item_name", e.getItemName());
		m.put("item_image", e.getItemImage());
		m.put("item_price", e.getItemPrice());
		m.put("item_type", e.getItemType());
		m.put("point", e.getPoint());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}

	private static boolean nonEmptyInt(Object v) {
		return intVal(v) > 0;
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
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
