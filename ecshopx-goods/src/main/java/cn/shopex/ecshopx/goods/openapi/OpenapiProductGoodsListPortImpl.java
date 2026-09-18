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

package cn.shopex.ecshopx.goods.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiProductGoodsListPort;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenapiProductGoodsListPortImpl implements OpenapiProductGoodsListPort {

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ObjectMapper objectMapper;

	public OpenapiProductGoodsListPortImpl(
			ItemsListQueryRepository itemsListQueryRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ObjectMapper objectMapper) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> listGoods(long companyId, int page, int pageSize) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);

		long totalCount = itemsListQueryRepository.countByParams(filter);
		List<Map<String, Object>> rowMaps = List.of();
		if (totalCount > 0) {
			List<Items> rows;
			if (pageSize > 0) {
				int offset = (page - 1) * pageSize;
				rows = itemsListQueryRepository.selectPageByParamsWithoutOrder(filter, offset, pageSize);
			} else {
				rows = itemsListQueryRepository.selectAllByParamsWithoutOrder(filter);
			}
			rowMaps = new ArrayList<>(rows.size());
			for (Items row : rows) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("item_id", row.getItemId());
				m.put("item_bn", row.getItemBn());
				m.put("item_name", row.getItemName());
				m.put("price", row.getPrice());
				m.put("approve_status", row.getApproveStatus());
				m.put("pics", row.getPics());
				rowMaps.add(m);
			}
			itemsListMultiLangApplier.applyToRows(companyId, "zh-CN", rowMaps);
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("page", page);
		data.put("page_size", pageSize);
		data.put("total_count", totalCount);
		data.put("goods_list", new ArrayList<>());
		if (rowMaps.isEmpty()) {
			return data;
		}

		List<Long> itemIds = new ArrayList<>(rowMaps.size());
		for (Map<String, Object> row : rowMaps) {
			Object id = row.get("item_id");
			if (id instanceof Number n) {
				itemIds.add(n.longValue());
			}
		}

		List<ItemRelAttributes> relRows =
				itemRelAttributesRepository.listByCompanyAndItemIdsLimit100WithoutOrder(companyId, itemIds);
		Map<Long, List<String>> skuArr = new LinkedHashMap<>();
		for (ItemRelAttributes rel : relRows) {
			Long itemId = rel.getItemId();
			if (itemId == null) {
				continue;
			}
			String val = rel.getCustomAttributeValue();
			skuArr.computeIfAbsent(itemId, k -> new ArrayList<>()).add(val != null ? val : "");
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> goodsList = (List<Map<String, Object>>) data.get("goods_list");
		for (Map<String, Object> row : rowMaps) {
			Long itemId = row.get("item_id") instanceof Number n ? n.longValue() : null;
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("item_name", str(row.get("item_name")));
			entry.put("item_price", intOrZero(row.get("price")));
			entry.put("pic", firstPicFromPicsJson((String) row.get("pics")));
			List<String> skuParts = itemId != null ? skuArr.get(itemId) : null;
			entry.put("sku", (skuParts == null || skuParts.isEmpty()) ? "" : String.join(" ", skuParts));
			entry.put("goods_bn", itemId != null ? itemId.intValue() : 0);
			entry.put("item_bn", str(row.get("item_bn")));
			entry.put("approve_status", str(row.get("approve_status")));
			goodsList.add(entry);
		}

		return data;
	}

	private String firstPicFromPicsJson(String picsJson) {
		if (!StringUtils.hasText(picsJson)) {
			return "";
		}
		try {
			JsonNode root = objectMapper.readTree(picsJson.trim());
			if (root.isArray() && root.size() > 0) {
				JsonNode first = root.get(0);
				if (first.isTextual()) {
					return first.asText("");
				}
			}
		} catch (Exception ignored) {
		}
		return "";
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intOrZero(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}
}
