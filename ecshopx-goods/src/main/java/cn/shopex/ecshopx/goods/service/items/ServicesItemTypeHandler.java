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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsRelType;
import cn.shopex.ecshopx.goods.repository.ItemsRelTypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ServicesItemTypeHandler implements ItemTypeHandler {

	private final ItemsRelTypeRepository itemsRelTypeRepository;
	private final ObjectMapper objectMapper;

	public ServicesItemTypeHandler(ItemsRelTypeRepository itemsRelTypeRepository, ObjectMapper objectMapper) {
		this.itemsRelTypeRepository = itemsRelTypeRepository;
		this.objectMapper = objectMapper;
	}

	public void deleteRelTypesForItem(long itemId) {
		itemsRelTypeRepository.deleteAllByItemId(itemId);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> createRelItem(Map<String, Object> itemsResult, Map<String, Object> skuParams, ItemsCreateContext ctx) {
		Object labels = skuParams.get("type_labels");
		if (labels == null) {
			throw new ResourceException("请选择商品内容");
		}
		List<Map<String, Object>> list;
		if (labels instanceof List<?> raw) {
			list = (List<Map<String, Object>>) (List<?>) raw;
		} else if (labels instanceof String s) {
			try {
				JsonNode arr = objectMapper.readTree(s);
				list = objectMapper.convertValue(arr, objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
			} catch (Exception e) {
				throw new ResourceException("请选择商品内容");
			}
		} else {
			throw new ResourceException("请选择商品内容");
		}
		if (list == null || list.isEmpty()) {
			throw new ResourceException("请选择商品内容");
		}
		long itemId = toLong(itemsResult.get("item_id"));
		String consumeType = str(skuParams.get("consume_type"));
		for (Map<String, Object> v : list) {
			if (v.get("labelId") == null) {
				throw new ResourceException("缺少参数数值属性ID");
			}
			long labelId = toLong(v.get("labelId"));
			ItemsRelType row = new ItemsRelType();
			row.setItemId(itemId);
			row.setLabelId(labelId);
			row.setLabelName(v.get("labelName") != null ? v.get("labelName").toString() : "");
			row.setLabelPrice(moneyToFen(v.get("labelPrice")));
			row.setNumType("plus");
			int isNotLimit = v.get("isNotLimitNum") != null ? (int) toLong(v.get("isNotLimitNum")) : 2;
			Object numObj = v.get("num");
			long num = numObj != null ? toLong(numObj) : 0L;
			Object isNotLimitLegacy = v.get("isNotLimit");
			if (isNotLimitLegacy != null && toLong(isNotLimitLegacy) == 1L) {
				num = 0L;
			}
			row.setNum(num);
			row.setIsNotLimitNum(isNotLimit);
			long limitTime = 0L;
			if ("every".equals(consumeType) && v.get("limitTime") != null) {
				limitTime = toLong(v.get("limitTime"));
			}
			row.setLimitTime(limitTime);
			row.setCompanyId(ctx.getCompanyId());
			itemsRelTypeRepository.insert(row);
		}
		return itemsResult;
	}

	private static int moneyToFen(Object v) {
		if (v == null) {
			return 0;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0;
		}
		return new BigDecimal(s).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
