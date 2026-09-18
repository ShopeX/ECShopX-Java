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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.operatorcart.OperatorCartSkuLoadFacade;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardCardIdsByGoodsForListService {

	private static final int MAX_ITEM_TAGS = 100;

	private final JdbcTemplate jdbcTemplate;
	private final RelItemsMapper relItemsMapper;
	private final DiscountCardsMapper discountCardsMapper;
	private final OperatorCartSkuLoadFacade operatorCartSkuLoadFacade;

	public DiscountCardCardIdsByGoodsForListService(
			JdbcTemplate jdbcTemplate,
			RelItemsMapper relItemsMapper,
			DiscountCardsMapper discountCardsMapper,
			OperatorCartSkuLoadFacade operatorCartSkuLoadFacade) {
		this.jdbcTemplate = jdbcTemplate;
		this.relItemsMapper = relItemsMapper;
		this.discountCardsMapper = discountCardsMapper;
		this.operatorCartSkuLoadFacade = operatorCartSkuLoadFacade;
	}

	@SuppressWarnings("unchecked")
	public List<Long> resolveCardIdsByGoods(Map<String, Object> itemFilter) {
		long companyId = toLong(itemFilter.get("company_id"));
		Set<Long> cardIds = new LinkedHashSet<>();
		List<Long> defaultItemIds = castLongList(itemFilter.get("default_item_id"));
		if (defaultItemIds != null && !defaultItemIds.isEmpty()) {
			List<Long> spuIdsForSkuExpansion = positiveLongIds(defaultItemIds);
			List<Long> skuItemIds = spuIdsForSkuExpansion.isEmpty()
					? List.of()
					: queryItemIdsByDefaultItemIds(companyId, spuIdsForSkuExpansion);
			List<Long> mergedItemIds = new ArrayList<>();
			mergedItemIds.add(0L);
			mergedItemIds.addAll(skuItemIds);
			itemFilter.put("item_id", mergedItemIds);
			addAll(cardIds, getCardIds(companyId, mergedItemIds, "normal"));
		}

		checkGoodsInfo(itemFilter);

		itemFilter.put("tag_ids", new ArrayList<Long>());
		List<Long> tagSourceDefaultIds = castLongList(itemFilter.get("default_item_id"));
		if (tagSourceDefaultIds != null && !tagSourceDefaultIds.isEmpty()) {
			List<Long> tagQuerySpuIds = positiveLongIds(tagSourceDefaultIds);
			if (!tagQuerySpuIds.isEmpty()) {
				List<Long> tagIds = operatorCartSkuLoadFacade.listTagIdsByItemIds(companyId, tagQuerySpuIds);
				if (tagIds.size() > MAX_ITEM_TAGS) {
					tagIds = tagIds.subList(0, MAX_ITEM_TAGS);
				}
				itemFilter.put("tag_ids", new ArrayList<>(tagIds));
				if (!tagIds.isEmpty()) {
					addAll(cardIds, getCardIds(companyId, tagIds, "tag"));
				}
			}
		}

		Object rawItemId = itemFilter.get("item_id");
		if (rawItemId != null) {
			List<Long> itemIds = normalizeItemIdList(rawItemId);
			if (!itemIds.isEmpty()) {
				if (!itemIds.contains(0L)) {
					itemIds = new ArrayList<>(itemIds);
					itemIds.add(0L);
				}
				addAll(cardIds, getCardIds(companyId, itemIds, "normal"));
				List<Long> afterAnti = removeAntiSelection(companyId, new ArrayList<>(cardIds));
				cardIds.clear();
				cardIds.addAll(afterAnti);
			}
		}

		List<Long> mainCats = castLongList(itemFilter.get("item_main_cat_id"));
		if (mainCats != null && !mainCats.isEmpty()) {
			addAll(cardIds, getCardIds(companyId, mainCats, "category"));
		}

		List<Integer> brands = castIntList(itemFilter.get("brand_id"));
		if (brands != null && !brands.isEmpty()) {
			List<Long> brandLongs = brands.stream().map(Integer::longValue).toList();
			addAll(cardIds, getCardIds(companyId, brandLongs, "brand"));
		}

		return new ArrayList<>(cardIds);
	}

	private void checkGoodsInfo(Map<String, Object> itemFilter) {
		if (operatorCartSkuLoadFacade.hasCompleteGoodsScopeKeys(itemFilter)) {
			return;
		}
		long companyId = toLong(itemFilter.get("company_id"));
		Object raw = itemFilter.get("item_id");
		List<Long> itemIds = raw == null ? List.of() : normalizeItemIdList(raw);
		if (itemIds.isEmpty()) {
			return;
		}
		StringBuilder in = new StringBuilder();
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		for (int i = 0; i < itemIds.size(); i++) {
			if (i > 0) {
				in.append(",");
			}
			in.append("?");
			args.add(itemIds.get(i));
		}
		String sql = "SELECT default_item_id, item_category, brand_id FROM items WHERE company_id = ? AND item_id IN ("
				+ in + ")";
		jdbcTemplate.query(sql, rs -> {
			while (rs.next()) {
				Long def = rs.getObject("default_item_id") != null ? rs.getLong("default_item_id") : null;
				Integer cat = rs.getObject("item_category") != null ? rs.getInt("item_category") : null;
				Integer brand = rs.getObject("brand_id") != null ? rs.getInt("brand_id") : null;
				appendLong(itemFilter, "default_item_id", def);
				appendInt(itemFilter, "item_main_cat_id", cat);
				appendInt(itemFilter, "brand_id", brand);
			}
		}, args.toArray());
	}

	private static void appendLong(Map<String, Object> f, String key, Long v) {
		if (v == null) {
			return;
		}
		List<Long> list = castLongList(f.get(key));
		if (list == null) {
			list = new ArrayList<>();
			f.put(key, list);
		}
		list.add(v);
	}

	private static void appendInt(Map<String, Object> f, String key, Integer v) {
		if (v == null) {
			return;
		}
		List<Integer> list = castIntList(f.get(key));
		if (list == null) {
			list = new ArrayList<>();
			f.put(key, list);
		}
		list.add(v);
	}

	private List<Long> queryItemIdsByDefaultItemIds(long companyId, List<Long> defaultItemIds) {
		StringBuilder in = new StringBuilder();
		List<Object> args = new ArrayList<>();
		args.add(companyId);
		for (int i = 0; i < defaultItemIds.size(); i++) {
			if (i > 0) {
				in.append(",");
			}
			in.append("?");
			args.add(defaultItemIds.get(i));
		}
		String sql = "SELECT item_id FROM items WHERE company_id = ? AND default_item_id IN (" + in + ")";
		return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("item_id"), args.toArray());
	}

	private List<Long> getCardIds(long companyId, List<Long> itemIds, String itemType) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		return relItemsMapper.selectDistinctCardIdsByGoodsScope(companyId, itemType, itemIds);
	}

	private List<Long> removeAntiSelection(long companyId, List<Long> cardIds) {
		if (cardIds == null || cardIds.isEmpty()) {
			return cardIds;
		}
		List<Long> hasCardIdList = new ArrayList<>();
		List<DiscountCards> first = discountCardsMapper.selectList(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCompanyId, companyId)
				.eq(DiscountCards::getUseBound, 5)
				.in(DiscountCards::getCardId, cardIds));
		for (DiscountCards d : first) {
			if (d.getCardId() != null) {
				hasCardIdList.add(d.getCardId());
			}
		}
		List<Long> remaining = new ArrayList<>(cardIds);
		remaining.removeAll(hasCardIdList);
		LambdaQueryWrapper<DiscountCards> w = new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCompanyId, companyId)
				.eq(DiscountCards::getUseBound, 5);
		if (!hasCardIdList.isEmpty()) {
			w.notIn(DiscountCards::getCardId, hasCardIdList);
		}
		List<DiscountCards> second = discountCardsMapper.selectList(w);
		List<Long> merge = new ArrayList<>(remaining);
		for (DiscountCards d : second) {
			if (d.getCardId() != null) {
				merge.add(d.getCardId());
			}
		}
		return merge.stream().distinct().toList();
	}

	private static void addAll(Set<Long> acc, List<Long> ids) {
		if (ids != null) {
			acc.addAll(ids);
		}
	}

	/** 仅正数 default_item_id 参与同 SPU 的 SKU 与标签扩展；0 表示无有效主商品，不参与 IN 条件以免匹配过量 SKU。 */
	private static List<Long> positiveLongIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Long id : ids) {
			if (id != null && id > 0L) {
				out.add(id);
			}
		}
		return out;
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s && StringUtils.hasText(s)) {
			return Long.parseLong(s.trim());
		}
		return 0L;
	}

	private static List<Long> normalizeItemIdList(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else if (o != null && StringUtils.hasText(o.toString())) {
				try {
					out.add(Long.parseLong(o.toString().trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static List<Long> castLongList(Object o) {
		if (!(o instanceof List<?> list)) {
			return null;
		}
		List<Long> out = new ArrayList<>();
		for (Object x : list) {
			if (x instanceof Number n) {
				out.add(n.longValue());
			}
		}
		return out.isEmpty() ? null : out;
	}

	@SuppressWarnings("unchecked")
	private static List<Integer> castIntList(Object o) {
		if (!(o instanceof List<?> list)) {
			return null;
		}
		List<Integer> out = new ArrayList<>();
		for (Object x : list) {
			if (x instanceof Number n) {
				out.add(n.intValue());
			}
		}
		return out.isEmpty() ? null : out;
	}
}
