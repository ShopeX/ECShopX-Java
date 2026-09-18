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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardRelItemNamesPort;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiDiscountCardV2ListPresentationService {

	private final RelItemsMapper relItemsMapper;
	private final OpenapiDiscountCardRelItemNamesPort relItemNamesPort;

	public OpenapiDiscountCardV2ListPresentationService(
			RelItemsMapper relItemsMapper,
			OpenapiDiscountCardRelItemNamesPort relItemNamesPort) {
		this.relItemsMapper = relItemsMapper;
		this.relItemNamesPort = relItemNamesPort;
	}

	public void apply(List<Map<String, Object>> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> item = list.get(i);
			list.set(i, toSlimRow(item));
		}
	}

	private Map<String, Object> toSlimRow(Map<String, Object> item) {
		String useAllItems;
		List<String> relData;

		String bound = String.valueOf(item.get("use_bound"));
		switch (bound) {
			case "1" -> {
				useAllItems = "false";
				long cardId = longValue(item.get("card_id"));
				List<Long> itemIds = listRelItemIds(cardId);
				relData = relItemNamesPort.listItemNamesByItemIds(itemIds);
			}
			case "2" -> {
				useAllItems = "category";
				relData = relCategory(stringValue(item.get("apply_scope")));
			}
			case "3" -> {
				useAllItems = "tag";
				relData = relTagOrBrand(stringValue(item.get("apply_scope")));
			}
			case "4" -> {
				useAllItems = "brand";
				relData = relTagOrBrand(stringValue(item.get("apply_scope")));
			}
			default -> {
				useAllItems = "true";
				relData = List.of();
			}
		}

		LinkedHashMap<String, Object> slim = new LinkedHashMap<>();
		slim.put("card_id", intValue(item.get("card_id")));
		slim.put("card_type", stringValue(coalesce(item, "card_type", "0")));
		slim.put("title", stringValue(coalesce(item, "title", "")));
		slim.put("description", stringValue(coalesce(item, "description", 0)));
		slim.put("discount", stringValue(coalesce(item, "discount", 0)));
		slim.put("reduce_cost", OpenapiDiscountCardV2ListFormatSupport.bcdivCentsToYuanString(item.get("reduce_cost")));
		slim.put("date_type", stringValue(coalesce(item, "date_type", "")));
		slim.put("begin_time", stringValue(coalesce(item, "begin_date", "")));
		slim.put("end_time", stringValue(coalesce(item, "end_date", "")));
		slim.put("fixed_term", stringValue(coalesce(item, "fixed_term", "")));
		slim.put("least_cost", OpenapiDiscountCardV2ListFormatSupport.bcdivCentsToYuanString(item.get("least_cost")));
		slim.put("most_cost", OpenapiDiscountCardV2ListFormatSupport.bcdivCentsToYuanString(item.get("most_cost")));
		slim.put("use_all_items", useAllItems);
		slim.put("rel_data", relData != null ? relData : List.of());
		slim.put("left_quantity", Math.max(intValue(item.get("quantity")) - intValue(item.get("get_num")), 0));
		return slim;
	}

	private List<Long> listRelItemIds(long cardId) {
		if (cardId <= 0L) {
			return List.of();
		}
		LambdaQueryWrapper<RelItems> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RelItems::getCardId, cardId).eq(RelItems::getItemType, "normal");
		List<RelItems> rows = relItemsMapper.selectList(wrapper);
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		List<Long> itemIds = new ArrayList<>(rows.size());
		for (RelItems row : rows) {
			if (row.getItemId() != null) {
				itemIds.add(row.getItemId());
			}
		}
		return itemIds;
	}

	private static List<String> relCategory(String applyScope) {
		if (!StringUtils.hasText(applyScope)) {
			return List.of();
		}
		String[] parts = applyScope.split(",", -1);
		Arrays.sort(parts);
		return List.of(String.join("/", parts));
	}

	private static List<String> relTagOrBrand(String applyScope) {
		if (!StringUtils.hasText(applyScope)) {
			return List.of();
		}
		return Arrays.asList(applyScope.split(",", -1));
	}

	private static int intValue(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number number) {
			return number.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longValue(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringValue(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw);
	}

	private static Object coalesce(Map<String, Object> item, String key, Object defaultValue) {
		Object value = item.get(key);
		return value != null ? value : defaultValue;
	}
}
