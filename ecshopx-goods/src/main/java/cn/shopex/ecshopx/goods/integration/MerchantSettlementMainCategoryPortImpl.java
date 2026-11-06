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

package cn.shopex.ecshopx.goods.integration;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.port.MerchantSettlementMainCategoryPort;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MerchantSettlementMainCategoryPortImpl implements MerchantSettlementMainCategoryPort {

	private static final TypeReference<List<Map<String, Object>>> SELECTION_LIST_TYPE = new TypeReference<>() {};

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ObjectMapper objectMapper;

	public MerchantSettlementMainCategoryPortImpl(
			ItemsCategoryRepository itemsCategoryRepository, ObjectMapper objectMapper) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public String validateNormalizeAndSerialize(long companyId, Object rawSelection) {
		List<SelectionItem> items = parseRequestItems(rawSelection);
		if (items.isEmpty()) {
			throw new ResourceException("商户类目必填");
		}
		Set<Long> selectedLevel2 = new LinkedHashSet<>();
		List<SelectionItem> normalized = new ArrayList<>();
		for (SelectionItem item : items) {
			ItemsCategory cat =
					itemsCategoryRepository
							.findEntityByCompanyAndCategoryId(companyId, item.categoryId())
							.orElseThrow(() -> new ResourceException("商户类目不存在或不可用"));
			assertMainCategory(cat);
			int level = cat.getCategoryLevel() != null ? cat.getCategoryLevel() : 0;
			if (level != 2 && level != 3) {
				throw new ResourceException("商户类目级别不正确");
			}
			if (item.level() != level) {
				throw new ResourceException("商户类目级别不正确");
			}
			if (level == 2) {
				selectedLevel2.add(item.categoryId());
			}
			if (level == 3) {
				Long parentId = cat.getParentId();
				if (parentId != null && parentId > 0 && selectedLevel2.contains(parentId)) {
					continue;
				}
			}
			normalized.add(item);
		}
		if (normalized.isEmpty()) {
			throw new ResourceException("商户类目必填");
		}
		try {
			List<Map<String, Object>> compact = new ArrayList<>();
			for (SelectionItem it : normalized) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("category_id", it.categoryId());
				m.put("level", it.level());
				compact.add(m);
			}
			return objectMapper.writeValueAsString(compact);
		} catch (Exception e) {
			throw new ResourceException("商户类目格式不正确");
		}
	}

	@Override
	public void enrichDetailCategoryFields(long companyId, String storedSelectionJson, Map<String, Object> targetRow) {
		List<SelectionItem> items = parseStoredJson(storedSelectionJson);
		List<Map<String, Object>> selectionOut = new ArrayList<>();
		List<String> names = new ArrayList<>();
		Set<Long> resolvedLevel3 = new LinkedHashSet<>();
		for (SelectionItem item : items) {
			ItemsCategory cat =
					itemsCategoryRepository
							.findEntityByCompanyAndCategoryId(companyId, item.categoryId())
							.orElse(null);
			Map<String, Object> node = new LinkedHashMap<>();
			node.put("category_id", item.categoryId());
			node.put("level", item.level());
			if (cat != null) {
				node.put("name", cat.getCategoryName());
				node.put("parent_id", cat.getParentId());
				node.put("path", cat.getPath());
				names.add(cat.getCategoryName() != null ? cat.getCategoryName() : "");
			} else {
				node.put("name", "");
				node.put("parent_id", 0);
				node.put("path", "");
				names.add("");
			}
			selectionOut.add(node);
			resolveLevel3Ids(companyId, item, resolvedLevel3);
		}
		targetRow.put("main_category_selection", selectionOut);
		targetRow.put("main_category_names", names);
		targetRow.put("main_category_resolved_level3_ids", new ArrayList<>(resolvedLevel3));
	}

	private void resolveLevel3Ids(long companyId, SelectionItem item, Set<Long> resolvedLevel3) {
		if (item.level() == 3) {
			resolvedLevel3.add(item.categoryId());
			return;
		}
		List<ItemsCategory> children =
				itemsCategoryRepository.listMainCategoryChildrenByParentId(companyId, item.categoryId(), 0L);
		for (ItemsCategory c : children) {
			Integer lv = c.getCategoryLevel();
			if (lv != null && lv == 3 && c.getCategoryId() != null) {
				resolvedLevel3.add(c.getCategoryId());
			}
		}
	}

	private static void assertMainCategory(ItemsCategory cat) {
		if (cat.getIsMainCategory() == null || !cat.getIsMainCategory()) {
			throw new ResourceException("请选择管理分类下的商户类目");
		}
	}

	private List<SelectionItem> parseRequestItems(Object rawSelection) {
		if (rawSelection == null) {
			return List.of();
		}
		try {
			if (rawSelection instanceof String s) {
				if (!StringUtils.hasText(s)) {
					return List.of();
				}
				return parseStoredJson(s.trim());
			}
			if (rawSelection instanceof JsonNode node) {
				return mapJsonArray(node);
			}
			if (rawSelection instanceof Collection<?> col) {
				List<SelectionItem> out = new ArrayList<>();
				for (Object o : col) {
					out.add(parseOneItem(o));
				}
				return out;
			}
			return mapJsonArray(objectMapper.valueToTree(rawSelection));
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("商户类目格式不正确");
		}
	}

	private List<SelectionItem> parseStoredJson(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			List<Map<String, Object>> list = objectMapper.readValue(json.trim(), SELECTION_LIST_TYPE);
			List<SelectionItem> out = new ArrayList<>();
			for (Map<String, Object> m : list) {
				out.add(parseOneItem(m));
			}
			return out;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("商户类目格式不正确");
		}
	}

	private SelectionItem parseOneItem(Object o) {
		if (o instanceof Map<?, ?> map) {
			long id = toLong(map.get("category_id"), "商户类目必填");
			int level = (int) toLong(map.get("level"), "商户类目级别不正确");
			return new SelectionItem(id, level);
		}
		if (o instanceof JsonNode n && n.isObject()) {
			long id = toLong(n.get("category_id"), "商户类目必填");
			int level = (int) toLong(n.get("level"), "商户类目级别不正确");
			return new SelectionItem(id, level);
		}
		throw new ResourceException("商户类目格式不正确");
	}

	private List<SelectionItem> mapJsonArray(JsonNode node) {
		if (node == null || !node.isArray()) {
			throw new ResourceException("商户类目格式不正确");
		}
		List<SelectionItem> out = new ArrayList<>();
		for (JsonNode n : node) {
			out.add(parseOneItem(n));
		}
		return out;
	}

	private static long toLong(Object v, String message) {
		if (v == null) {
			throw new ResourceException(message);
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof JsonNode j) {
			if (j.isNumber()) {
				return j.longValue();
			}
			if (j.isTextual()) {
				return Long.parseLong(j.asText().trim());
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(message);
		}
	}

	private record SelectionItem(long categoryId, int level) {}
}
