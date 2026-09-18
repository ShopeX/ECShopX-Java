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

package cn.shopex.ecshopx.goods.service.promotion;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.service.items.EmployeePurchaseItemsSkuListService;
import cn.shopex.ecshopx.promotions.port.LimitPromotionAdminGoodsSupportPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LimitPromotionAdminGoodsSupportPortImpl implements LimitPromotionAdminGoodsSupportPort {

	private final EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService;
	private final PromotionDetailTagBrandListService promotionDetailTagBrandListService;
	private final ItemsMapper itemsMapper;
	private final ItemsRelTagsRepository itemsRelTagsRepository;

	public LimitPromotionAdminGoodsSupportPortImpl(
			EmployeePurchaseItemsSkuListService employeePurchaseItemsSkuListService,
			PromotionDetailTagBrandListService promotionDetailTagBrandListService,
			ItemsMapper itemsMapper,
			ItemsRelTagsRepository itemsRelTagsRepository) {
		this.employeePurchaseItemsSkuListService = employeePurchaseItemsSkuListService;
		this.promotionDetailTagBrandListService = promotionDetailTagBrandListService;
		this.itemsMapper = itemsMapper;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
	}

	@Override
	public Map<String, Object> loadSkuItemsList(long companyId, List<Long> itemIds) {
		return employeePurchaseItemsSkuListService.loadSkuItemsList(companyId, itemIds);
	}

	@Override
	public Map<String, Object> loadSkuItemsListForPromotionDetail(long companyId, List<Long> itemIds) {
		return employeePurchaseItemsSkuListService.loadSkuItemsListForMarketingGift(companyId, itemIds);
	}

	@Override
	public List<Map<String, Object>> formatItemsList(List<Map<String, Object>> enrichedSkuRows) {
		return MultiSpecItemsTreeFormatter.formatItemsList(enrichedSkuRows);
	}

	@Override
	public List<Map<String, Object>> loadTagList(long companyId, List<Long> tagIds) {
		return promotionDetailTagBrandListService.listTagsForDetail(companyId, tagIds);
	}

	@Override
	public List<Map<String, Object>> loadBrandList(long companyId, List<Long> brandIds) {
		return promotionDetailTagBrandListService.listBrandsForDetail(companyId, brandIds);
	}

	@Override
	public List<Long> listItemIdsByCompanyAndItemBnContainsCapped(
			long companyId, String itemBnContains, int maxResults) {
		if (!StringUtils.hasText(itemBnContains)) {
			return List.of();
		}
		String escaped = escapeSqlLike(itemBnContains.trim());
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.apply("item_bn LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'", escaped)
				.orderByAsc(Items::getItemId)
				.select(Items::getItemId)
				.last("LIMIT " + maxResults);
		return itemsMapper.selectList(w).stream()
				.map(Items::getItemId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
	}

	@Override
	public List<Map<String, Object>> loadItemNameBnRowsByItemIds(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId)
				.in(Items::getItemId, itemIds)
				.select(Items::getItemId, Items::getItemName, Items::getItemBn);
		List<Items> rows = itemsMapper.selectList(w);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Items it : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("item_id", it.getItemId());
			m.put("item_name", it.getItemName());
			m.put("item_bn", it.getItemBn());
			out.add(m);
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> loadItemsRelTagJoinRows(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		List<ItemsRelTags> rels = itemsRelTagsRepository.getLists(companyId, itemIds);
		if (rels.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<Long> tagIdSet = new LinkedHashSet<>();
		for (ItemsRelTags rel : rels) {
			if (rel.getTagId() != null && rel.getTagId() > 0L) {
				tagIdSet.add(rel.getTagId());
			}
		}
		List<Long> tagIds = new ArrayList<>(tagIdSet);
		List<Map<String, Object>> tagRows = promotionDetailTagBrandListService.listTagsForDetail(companyId, tagIds);
		Map<Long, Map<String, Object>> tagById = new LinkedHashMap<>();
		for (Map<String, Object> tr : tagRows) {
			Object tid = tr.get("tag_id");
			if (tid instanceof Number n) {
				tagById.put(n.longValue(), tr);
			}
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (ItemsRelTags rel : rels) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("item_id", rel.getItemId());
			Long tid = rel.getTagId();
			if (tid != null) {
				Map<String, Object> tag = tagById.get(tid);
				if (tag != null) {
					row.putAll(tag);
				}
			}
			out.add(row);
		}
		return out;
	}

	private static String escapeSqlLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
