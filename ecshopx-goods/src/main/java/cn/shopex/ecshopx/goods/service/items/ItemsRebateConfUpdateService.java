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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsRebateConfUpdateService {

	private final ItemsRepository itemsRepository;
	private final ObjectMapper objectMapper;

	public ItemsRebateConfUpdateService(ItemsRepository itemsRepository, ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.objectMapper = objectMapper;
	}

	public void updateRebateConf(long companyId, String rebateTypeFromController, Object rebateConfRaw) {
		String rebateTypeEffective = StringUtils.hasText(rebateTypeFromController) ? rebateTypeFromController : "default";

		if (rebateConfRaw == null) {
			throw new BadRequestException("rebateConf 不能为空");
		}
		if (rebateConfRaw instanceof String s && !StringUtils.hasText(s.trim())) {
			throw new BadRequestException("rebateConf 不能为空");
		}

		List<Map<String, Object>> rows = parseRebateConfRows(rebateConfRaw);

		for (Map<String, Object> sourceRow : rows) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>(sourceRow);
			long itemId = parseRequiredItemId(row.get("item_id"));
			row.remove("item_id");
			String confJson;
			try {
				confJson = objectMapper.writeValueAsString(row);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("rebateConf 行内容无法序列化为 JSON");
			}
			Items patch = new Items();
			patch.setRebateConf(confJson);
			patch.setRebateType(rebateTypeEffective);
			itemsRepository.updateByItemId(itemId, companyId, patch);
		}
	}

	private List<Map<String, Object>> parseRebateConfRows(Object rebateConfRaw) {
		if (rebateConfRaw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>(list.size());
			for (Object elem : list) {
				if (!(elem instanceof Map<?, ?> m)) {
					throw new BadRequestException("rebateConf 须为对象数组");
				}
				LinkedHashMap<String, Object> map = new LinkedHashMap<>();
				for (Map.Entry<?, ?> e : m.entrySet()) {
					map.put(String.valueOf(e.getKey()), e.getValue());
				}
				out.add(map);
			}
			return out;
		}
		if (rebateConfRaw instanceof String s) {
			try {
				return objectMapper.readValue(s.trim(), new TypeReference<List<Map<String, Object>>>() {});
			} catch (JsonProcessingException e) {
				throw new BadRequestException("rebateConf 不是合法 JSON 数组");
			}
		}
		throw new BadRequestException("rebateConf 格式错误");
	}

	private static long parseRequiredItemId(Object v) {
		if (v == null) {
			throw new BadRequestException("每项必须包含有效的 item_id");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("每项必须包含有效的 item_id");
		}
	}
}
