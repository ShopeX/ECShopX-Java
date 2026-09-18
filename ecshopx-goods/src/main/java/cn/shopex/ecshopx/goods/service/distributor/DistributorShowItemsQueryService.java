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

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorShowItemsQueryService {

	private static final List<String> APPROVE_OK = List.of("onsale", "offline_sale", "only_show");

	private final ItemsMapper itemsMapper;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ObjectMapper objectMapper;

	public DistributorShowItemsQueryService(
			ItemsMapper itemsMapper, ItemsRelTagsRepository itemsRelTagsRepository, ObjectMapper objectMapper) {
		this.itemsMapper = itemsMapper;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.objectMapper = objectMapper;
	}

	public void appendItemListPerDistributor(
			long companyId, List<Map<String, Object>> rows, List<Long> itemTagIds, int maxPerShop) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> filterItemIds = null;
		if (itemTagIds != null && !itemTagIds.isEmpty()) {
			filterItemIds = itemsRelTagsRepository.listItemIdsByCompanyIdAndTagIds(companyId, itemTagIds);
		}
		for (Map<String, Object> row : rows) {
			Object didObj = row.get("distributor_id");
			Long distributorId = parseDistributorIdOrNull(didObj);
			// Only rows with a real shop id get an itemList; skip head-office placeholder (null / 0).
			if (distributorId == null || distributorId == 0L) {
				continue;
			}
			LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
			w.eq(Items::getCompanyId, companyId)
					.eq(Items::getDistributorId, distributorId.intValue())
					.eq(Items::getAuditStatus, "approved")
					.in(Items::getApproveStatus, APPROVE_OK)
					.apply("item_id = default_item_id");
			if (filterItemIds != null && !filterItemIds.isEmpty()) {
				w.in(Items::getItemId, filterItemIds);
			}
			w.orderByDesc(Items::getCreated).orderByDesc(Items::getItemId).last("LIMIT " + maxPerShop);
			List<Items> items = itemsMapper.selectList(w);
			List<Map<String, Object>> itemList = new ArrayList<>();
			for (Items it : items) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("item_id", it.getItemId() != null ? it.getItemId().intValue() : null);
				m.put("item_name", it.getItemName());
				m.put("price", it.getPrice());
				m.put("market_price", it.getMarketPrice());
				m.put("store", it.getStore());
				m.put("pics", firstPic(it.getPics()));
				itemList.add(m);
			}
			row.put("itemList", itemList);
		}
	}

	private static Long parseDistributorIdOrNull(Object didObj) {
		if (didObj == null) {
			return null;
		}
		if (didObj instanceof Number n) {
			return n.longValue();
		}
		String s = didObj.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String firstPic(String picsJson) {
		if (!StringUtils.hasText(picsJson)) {
			return "";
		}
		String t = picsJson.trim();
		if (!t.startsWith("[")) {
			return t;
		}
		try {
			List<Object> arr = objectMapper.readValue(t, new TypeReference<List<Object>>() {});
			if (arr == null || arr.isEmpty()) {
				return "";
			}
			Object first = arr.get(0);
			return first == null ? "" : first.toString();
		} catch (Exception e) {
			return "";
		}
	}
}
