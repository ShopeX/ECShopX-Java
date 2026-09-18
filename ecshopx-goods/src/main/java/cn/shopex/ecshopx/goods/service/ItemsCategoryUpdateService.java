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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformCategorySyncPort;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ItemsCategoryUpdateService {

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsQueryRepository itemsQueryRepository;
	private final ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier;
	private final ItemsCategoryInfoService itemsCategoryInfoService;
	private final ObjectMapper objectMapper;
	private final ShuyunOpenPlatformCategorySyncPort openPlatformCategorySyncPort;

	public ItemsCategoryUpdateService(ItemsCategoryRepository itemsCategoryRepository,
			ItemsQueryRepository itemsQueryRepository, ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier,
			ItemsCategoryInfoService itemsCategoryInfoService, ObjectMapper objectMapper,
			ShuyunOpenPlatformCategorySyncPort openPlatformCategorySyncPort) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsQueryRepository = itemsQueryRepository;
		this.itemsCategoryMultiLangApplier = itemsCategoryMultiLangApplier;
		this.itemsCategoryInfoService = itemsCategoryInfoService;
		this.objectMapper = objectMapper;
		this.openPlatformCategorySyncPort = openPlatformCategorySyncPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateCategory(long companyId, long categoryId, Map<String, Object> body, String countryCode) {
		Optional<ItemsCategory> opt = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, categoryId);
		if (opt.isEmpty()) {
			throw new ResourceException("更新的分类不存在");
		}
		ItemsCategory existing = opt.get();

		boolean goodsSpecTruthy = body.containsKey("goods_spec") && isTruthyFormInput(body.get("goods_spec"));
		boolean goodsParamsTruthy = body.containsKey("goods_params") && isTruthyFormInput(body.get("goods_params"));

		if (goodsSpecTruthy) {
			List<Long> oldIdsSpec = parseAttributeIdsFromCategoryJson(existing.getGoodsSpec());
			List<Long> newIdsSpec = extractNewAttributeIdsFromDecoded(decodeStructureForDiff(body.get("goods_spec"), "goods_spec"));
			List<Long> removedSpec = diffRemoved(oldIdsSpec, newIdsSpec);
			ensureNoAssociatedItems(companyId, categoryId, removedSpec);
		}
		if (goodsParamsTruthy) {
			List<Long> oldIdsParams = parseAttributeIdsFromCategoryJson(existing.getGoodsParams());
			List<Long> newIdsParams = extractNewAttributeIdsFromDecoded(decodeStructureForDiff(body.get("goods_params"), "goods_params"));
			List<Long> removedParams = diffRemoved(oldIdsParams, newIdsParams);
			ensureNoAssociatedItems(companyId, categoryId, removedParams);
		}

		String encodedGoodsSpec = goodsSpecTruthy ? encodeGoodsFieldForPersist(body.get("goods_spec"), "goods_spec") : null;
		String encodedGoodsParams = goodsParamsTruthy ? encodeGoodsFieldForPersist(body.get("goods_params"), "goods_params") : null;

		if (body.containsKey("category_name")) {
			String newName = body.get("category_name") == null ? "" : body.get("category_name").toString().trim();
			// Always upsert current-locale shard (align PHP updateLangData); do not gate on main-table equality.
			itemsCategoryMultiLangApplier.upsertCategoryNameLang(companyId, categoryId, newName, countryCode);
		}

		itemsCategoryRepository.updateAdminCategoryPartial(companyId, categoryId, u -> {
			if (encodedGoodsSpec != null) {
				u.set(ItemsCategory::getGoodsSpec, encodedGoodsSpec);
			}
			if (encodedGoodsParams != null) {
				u.set(ItemsCategory::getGoodsParams, encodedGoodsParams);
			}
			applyFlatFieldsToWrapper(body, u);
		});

		Object info = itemsCategoryInfoService.getCategoryInfo(companyId, categoryId, Optional.empty(), 0, countryCode);
		if (info instanceof List<?> list && list.isEmpty()) {
			throw new ResourceException("更新的分类不存在");
		}
		if (!(info instanceof Map<?, ?>)) {
			throw new ResourceException("更新的分类不存在");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> out = (Map<String, Object>) info;
		openPlatformCategorySyncPort.dispatchIfAuthAllows(companyId, categoryId);
		return out;
	}

	private String encodeGoodsFieldForPersist(Object raw, String fieldLabel) {
		Object decoded = decodeStructureForPersist(raw, fieldLabel);
		try {
			return objectMapper.writeValueAsString(decoded);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(fieldLabel + " 序列化失败");
		}
	}

	private Object decodeStructureForDiff(Object raw, String fieldLabel) {
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			try {
				return objectMapper.readValue(s, Object.class);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("JSON 格式错误");
			}
		}
		if (raw instanceof JsonNode n) {
			try {
				return objectMapper.readValue(objectMapper.writeValueAsString(n), Object.class);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("JSON 格式错误");
			}
		}
		return raw;
	}

	private Object decodeStructureForPersist(Object raw, String fieldLabel) {
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException(fieldLabel + " JSON 无法解析");
			}
			try {
				return objectMapper.readValue(s, Object.class);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("JSON 格式错误");
			}
		}
		if (raw instanceof JsonNode n) {
			try {
				return objectMapper.readValue(objectMapper.writeValueAsString(n), Object.class);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("JSON 格式错误");
			}
		}
		return raw;
	}

	private void applyFlatFieldsToWrapper(Map<String, Object> body, LambdaUpdateWrapper<ItemsCategory> u) {
		if (body.containsKey("category_name")) {
			Object v = body.get("category_name");
			u.set(ItemsCategory::getCategoryName, v == null ? null : v.toString().trim());
		}
		if (body.containsKey("sort")) {
			u.set(ItemsCategory::getSort, parseLongStrict(body.get("sort")));
		}
		if (body.containsKey("image_url") && body.get("image_url") != null) {
			u.set(ItemsCategory::getImageUrl, body.get("image_url").toString());
		}
		if (body.containsKey("is_show_front")) {
			u.set(ItemsCategory::getIsShowFront, parseIntegerOrDefault(body.get("is_show_front"), 1));
		}
		if (body.containsKey("category_code")) {
			Object codeObj = body.get("category_code");
			u.set(ItemsCategory::getCategoryCode,
					codeObj != null && StringUtils.hasText(codeObj.toString()) ? codeObj.toString().trim() : "");
		}
		if (body.containsKey("is_main_category")) {
			u.set(ItemsCategory::getIsMainCategory, parseIsMainCategory(body.get("is_main_category")));
		}
		if (body.containsKey("parent_id")) {
			u.set(ItemsCategory::getParentId, parseLongNonNegative(body.get("parent_id"), "父级ID必须大于等于0"));
		}
		if (body.containsKey("taobao_category_info")) {
			u.set(ItemsCategory::getTaobaoCategoryInfo, jsonishToString(body.get("taobao_category_info")));
		}
		if (body.containsKey("category_id_taobao")) {
			u.set(ItemsCategory::getCategoryIdTaobao, parseLongOrZero(body.get("category_id_taobao")));
		}
		if (body.containsKey("parent_id_taobao")) {
			u.set(ItemsCategory::getParentIdTaobao, parseLongOrZero(body.get("parent_id_taobao")));
		}
		if (body.containsKey("customize_page_id")) {
			u.set(ItemsCategory::getCustomizePageId, parseLongOrZero(body.get("customize_page_id")));
		}
	}

	private static boolean isTruthyFormInput(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return false;
			}
			return !"0".equals(t);
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (v instanceof JsonNode node) {
			if (node.isNull() || node.isMissingNode()) {
				return false;
			}
			if (node.isTextual()) {
				return isTruthyFormInput(node.asText());
			}
			if (node.isArray()) {
				return node.size() > 0;
			}
			if (node.isObject()) {
				return node.size() > 0;
			}
			return true;
		}
		return true;
	}

	private List<Long> parseAttributeIdsFromCategoryJson(String json) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			List<Object> arr = objectMapper.readValue(json, new TypeReference<List<Object>>() {});
			List<Long> out = new ArrayList<>();
			for (Object el : arr) {
				Long id = extractAttributeId(el);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private List<Long> extractNewAttributeIdsFromDecoded(Object decoded) {
		List<Long> out = new ArrayList<>();
		if (decoded == null) {
			return out;
		}
		if (decoded instanceof JsonNode n) {
			if (n.isArray()) {
				for (JsonNode el : n) {
					addIdIfPresent(out, extractAttributeIdFromJsonNode(el));
				}
			} else {
				addIdIfPresent(out, extractAttributeIdFromJsonNode(n));
			}
			return out;
		}
		if (decoded instanceof List<?> list) {
			for (Object el : list) {
				addIdIfPresent(out, extractAttributeId(el));
			}
			return out;
		}
		addIdIfPresent(out, extractAttributeId(decoded));
		return out;
	}

	private static void addIdIfPresent(List<Long> out, Long id) {
		if (id != null && id != 0L) {
			out.add(id);
		}
	}

	private Long extractAttributeIdFromJsonNode(JsonNode el) {
		if (el == null || el.isNull()) {
			return null;
		}
		if (el.isNumber()) {
			return el.longValue();
		}
		if (el.isTextual()) {
			try {
				return Long.parseLong(el.asText().trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		if (el.isObject()) {
			JsonNode aid = el.get("attribute_id");
			if (aid != null && aid.isNumber()) {
				return aid.longValue();
			}
			if (aid != null && aid.isTextual()) {
				try {
					return Long.parseLong(aid.asText().trim());
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
		return null;
	}

	private static Long extractAttributeId(Object el) {
		if (el instanceof Number n) {
			return n.longValue();
		}
		if (el instanceof Map<?, ?> m) {
			Object aid = m.get("attribute_id");
			if (aid instanceof Number n) {
				return n.longValue();
			}
			if (aid != null) {
				try {
					return Long.parseLong(aid.toString());
				} catch (NumberFormatException e) {
					return null;
				}
			}
		}
		return null;
	}

	private static List<Long> diffRemoved(List<Long> oldIds, List<Long> newIds) {
		Set<Long> newSet = new HashSet<>(newIds);
		List<Long> removed = new ArrayList<>();
		for (Long old : oldIds) {
			if (old != null && !newSet.contains(old)) {
				removed.add(old);
			}
		}
		return removed;
	}

	private void ensureNoAssociatedItems(long companyId, long categoryId, List<Long> removedIds) {
		if (removedIds == null || removedIds.isEmpty()) {
			return;
		}
		long c = itemsQueryRepository.countItemsLinkedToAttributesInCategory(companyId, categoryId, removedIds);
		if (c > 0) {
			throw new ResourceException("分类下存在商品已关联拟移除的属性");
		}
	}

	private static long parseLongStrict(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("排序必须大于等于0");
		}
	}

	private static long parseLongNonNegative(Object v, String err) {
		long x = parseLongNonNegativeOrThrow(v, err);
		if (x < 0L) {
			throw new ResourceException(err);
		}
		return x;
	}

	private static long parseLongNonNegativeOrThrow(Object v, String err) {
		if (v == null) {
			throw new ResourceException(err);
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(err);
		}
	}

	private static long parseLongOrZero(Object v) {
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

	private static int parseIntegerOrDefault(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static boolean parseIsMainCategory(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim();
		if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
			return true;
		}
		return false;
	}

	private String jsonishToString(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof String s) {
			return s;
		}
		try {
			return objectMapper.writeValueAsString(v);
		} catch (JsonProcessingException e) {
			throw new ResourceException("goods 参数序列化失败");
		}
	}
}
