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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ItemsCategoryClassificationService {

	private static final int DEFAULT_LEVEL_PARAM = 1;

	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsCategoryDistributorIdResolver distributorIdResolver;
	private final ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier;
	private final ObjectMapper objectMapper;

	public ItemsCategoryClassificationService(ItemsCategoryRepository itemsCategoryRepository,
			ItemsCategoryDistributorIdResolver distributorIdResolver,
			ItemsCategoryMultiLangApplier itemsCategoryMultiLangApplier, ObjectMapper objectMapper) {
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.distributorIdResolver = distributorIdResolver;
		this.itemsCategoryMultiLangApplier = itemsCategoryMultiLangApplier;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void createClassification(long companyId, long jwtDistributorId, Map<String, Object> params, String countryCode) {
		Object rawName = params.get("category_name");
		String categoryName = rawName == null ? "" : rawName.toString().trim();
		if (!StringUtils.hasText(categoryName)) {
			throw new ResourceException("分类名称必填");
		}
		boolean isMainCategory = parseIsMainCategory(params.get("is_main_category"));
		boolean parentIdKeyPresent = params.containsKey("parent_id");
		long distributorId = distributorIdResolver.resolveForClassification(isMainCategory, companyId, jwtDistributorId);

		if (!parentIdKeyPresent) {
			long dupParentId = 0L;
			itemsCategoryRepository.findDuplicateForClassification(companyId, categoryName, dupParentId, isMainCategory, distributorId)
					.ifPresent(c -> {
						throw new ResourceException("分类名称已存在");
					});
			ItemsCategory entity = buildEntityForInsert(params, companyId, categoryName, 0L, 1, "", isMainCategory, distributorId);
			itemsCategoryRepository.insert(entity);
			Long catId = entity.getCategoryId();
			if (catId == null) {
				throw new ResourceException("保存失败");
			}
			itemsCategoryRepository.updatePath(catId, companyId, String.valueOf(catId));
			itemsCategoryMultiLangApplier.upsertCategoryNameLang(companyId, catId, categoryName, countryCode);
			return;
		}

		long parentIdFromRequest = parseLongNonNegative(params.get("parent_id"), "父级ID必须大于等于0");
		itemsCategoryRepository.findDuplicateForClassification(companyId, categoryName, parentIdFromRequest, isMainCategory, distributorId)
				.ifPresent(c -> {
					throw new ResourceException("分类名称已存在");
				});

		ItemsCategory parent = itemsCategoryRepository.findEntityByCompanyAndCategoryId(companyId, parentIdFromRequest)
				.orElseThrow(() -> new ResourceException("父级分类不存在"));

		int categoryLevel;
		String pathPrefix = parent.getPath() != null ? parent.getPath() : "";
		if (parent.getParentId() == null || parent.getParentId() == 0L) {
			categoryLevel = DEFAULT_LEVEL_PARAM + 1;
		} else {
			int pl = parent.getCategoryLevel() != null ? parent.getCategoryLevel() : 1;
			categoryLevel = pl + 1;
		}

		ItemsCategory entity = buildEntityForInsert(params, companyId, categoryName, parentIdFromRequest, categoryLevel, "", isMainCategory,
				distributorId);
		itemsCategoryRepository.insert(entity);
		Long catId = entity.getCategoryId();
		if (catId == null) {
			throw new ResourceException("保存失败");
		}
		String newPath = (pathPrefix != null && !pathPrefix.isEmpty()) ? pathPrefix + "," + catId : String.valueOf(catId);
		itemsCategoryRepository.updatePath(catId, companyId, newPath);
		itemsCategoryMultiLangApplier.upsertCategoryNameLang(companyId, catId, categoryName, countryCode);
	}

	private ItemsCategory buildEntityForInsert(Map<String, Object> params, long companyId, String categoryName, long parentId, int categoryLevel,
			String pathForInsert, boolean isMainCategory, long distributorId) {
		ItemsCategory e = new ItemsCategory();
		e.setCompanyId(companyId);
		e.setCategoryName(categoryName);
		e.setParentId(parentId);
		e.setCategoryLevel(categoryLevel);
		e.setPath(pathForInsert != null ? pathForInsert : "");
		Object codeObj = params.get("category_code");
		e.setCategoryCode(codeObj != null && StringUtils.hasText(codeObj.toString()) ? codeObj.toString().trim() : "");
		Object sortObj = params.get("sort");
		e.setSort(sortObj != null ? parseLongStrict(sortObj) : 0L);
		e.setIsMainCategory(isMainCategory);
		if (params.containsKey("is_show_front")) {
			e.setIsShowFront(parseIntegerOrDefault(params.get("is_show_front"), 1));
		}
		e.setGoodsParams(jsonishToString(params.get("goods_params")));
		e.setGoodsSpec(jsonishToString(params.get("goods_spec")));
		if (params.get("image_url") != null) {
			e.setImageUrl(params.get("image_url").toString());
		}
		e.setCategoryIdTaobao(parseLongOrZero(params.get("category_id_taobao")));
		e.setParentIdTaobao(parseLongOrZero(params.get("parent_id_taobao")));
		e.setTaobaoCategoryInfo(jsonishToString(params.get("taobao_category_info")));
		if (params.containsKey("customize_page_id")) {
			e.setCustomizePageId(parseLongOrZero(params.get("customize_page_id")));
		}
		e.setDistributorId(distributorId);
		int now = (int) (System.currentTimeMillis() / 1000L);
		e.setCreated(now);
		e.setUpdated(now);
		return e;
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
