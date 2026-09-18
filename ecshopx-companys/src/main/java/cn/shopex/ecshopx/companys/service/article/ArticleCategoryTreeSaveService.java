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

package cn.shopex.ecshopx.companys.service.article;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.ArticleCategory;
import cn.shopex.ecshopx.companys.mapper.ArticleCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleCategoryTreeSaveService {

	private final ArticleCategoryMapper articleCategoryMapper;
	private final ObjectMapper objectMapper;

	public ArticleCategoryTreeSaveService(
			ArticleCategoryMapper articleCategoryMapper, ObjectMapper objectMapper) {
		this.articleCategoryMapper = articleCategoryMapper;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void saveArticleCategory(Object form, long companyId) {
		try {
			List<Map<String, Object>> rows = parseFormToRowList(form);
			saveCategoryLevel(rows, companyId, 1, 0L, "");
		} catch (ResourceException | BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		} catch (Throwable t) {
			throw new ResourceException("保存失败");
		}
	}

	private List<Map<String, Object>> parseFormToRowList(Object form) {
		if (form == null) {
			throw new BadRequestException("请求体 form 无效或缺失");
		}
		if (form instanceof String s) {
			String trimmed = s.trim();
			if (trimmed.isEmpty()) {
				throw new BadRequestException("请求体 form 无效或缺失");
			}
			try {
				JsonNode node = objectMapper.readTree(trimmed);
				if (!node.isArray()) {
					throw new BadRequestException("请求体 form 无效或缺失");
				}
				return objectMapper.convertValue(node, new TypeReference<List<Map<String, Object>>>() {});
			} catch (BadRequestException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequestException("请求体 form 无效或缺失");
			}
		}
		if (form instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object elem : list) {
				if (elem == null) {
					throw new BadRequestException("请求体 form 无效或缺失");
				}
				try {
					out.add(objectMapper.convertValue(elem, new TypeReference<Map<String, Object>>() {}));
				} catch (IllegalArgumentException e) {
					throw new BadRequestException("请求体 form 无效或缺失");
				}
			}
			return out;
		}
		throw new BadRequestException("请求体 form 无效或缺失");
	}

	private void saveCategoryLevel(
			List<Map<String, Object>> data,
			long companyId,
			int level,
			long parentId,
			String path) {
		for (Map<String, Object> row : data) {
			String categoryName = readRequiredCategoryName(row);
			Object sortRaw = row.get("sort");
			Object normalizedSort = normalizeSortRaw(sortRaw);
			long sortValue = validateSortToLong(normalizedSort);
			String categoryType =
					Objects.requireNonNullElse(stringifyNullable(row.get("category_type")), "bring");

			ArticleCategory persisted;
			long categoryId;
			if (row.get("category_id") != null) {
				categoryId = parseLongForBody(row.get("category_id"));
				persisted =
						updateExistingRow(
								companyId,
								level,
								path,
								parentId,
								categoryId,
								categoryName,
								sortValue,
								categoryType);
			} else {
				persisted = insertNewRow(companyId, parentId, level, path, categoryName, sortValue, categoryType);
				categoryId = Objects.requireNonNull(persisted.getCategoryId());
			}

			String currentPath = persisted.getPath();
			boolean hasAncestorPath =
					currentPath != null
							&& !currentPath.isEmpty()
							&& !"0".equals(currentPath);
			String newPath = hasAncestorPath ? currentPath + "," + categoryId : String.valueOf(categoryId);
			applyPathSecondUpdate(categoryId, newPath);

			Object childrenObj = row.get("children");
			if (childrenObj instanceof List<?> childList && !childList.isEmpty()) {
				List<Map<String, Object>> childMaps = parseChildMaps(childList);
				saveCategoryLevel(childMaps, companyId, level + 1, categoryId, newPath);
			}
		}
	}

	private List<Map<String, Object>> parseChildMaps(List<?> childList) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object c : childList) {
			if (c == null) {
				throw new BadRequestException("请求体 form 无效或缺失");
			}
			try {
				out.add(objectMapper.convertValue(c, new TypeReference<Map<String, Object>>() {}));
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("请求体 form 无效或缺失");
			}
		}
		return out;
	}

	private String readRequiredCategoryName(Map<String, Object> row) {
		Object raw = row.get("category_name");
		if (raw == null) {
			throw new BadRequestException("类目名称不能为空");
		}
		String name = raw instanceof String s ? s.trim() : raw.toString().trim();
		if (name.isEmpty()) {
			throw new BadRequestException("类目名称不能为空");
		}
		return name;
	}

	private static String stringifyNullable(Object o) {
		if (o == null) {
			return null;
		}
		return o instanceof String s ? s : o.toString();
	}

	private static Object normalizeSortRaw(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Boolean b) {
			return b ? raw : 0L;
		}
		if (raw instanceof Number n) {
			if (n.doubleValue() == 0.0d) {
				return 0L;
			}
			return raw;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return 0L;
			}
			return raw;
		}
		return raw;
	}

	private static long validateSortToLong(Object v) {
		if (v instanceof Boolean) {
			throw new ResourceException("排序必须为数字类型");
		}
		if (v instanceof Map<?, ?> || v instanceof List<?>) {
			throw new ResourceException("排序必须为数字类型");
		}
		String s;
		if (v instanceof Number n) {
			if (n instanceof BigDecimal bd) {
				s = bd.stripTrailingZeros().toPlainString();
			} else {
				s = n.toString();
			}
		} else if (v instanceof String str) {
			s = str.trim();
		} else {
			throw new ResourceException("排序必须为数字类型");
		}
		if (s.isEmpty()) {
			return 0L;
		}
		rejectHexSortString(s);
		BigDecimal bd;
		try {
			bd = new BigDecimal(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("排序必须为数字类型");
		}
		BigDecimal truncated = bd.setScale(0, RoundingMode.DOWN);
		return truncated.longValue();
	}

	private static void rejectHexSortString(String s) {
		String t = s.trim();
		if (t.length() >= 2 && (t.startsWith("0x") || t.startsWith("0X"))) {
			throw new ResourceException("排序必须为数字类型");
		}
	}

	private long parseLongForBody(Object o) {
		try {
			if (o instanceof Number n) {
				long v = n.longValue();
				if (v <= 0L) {
					throw new BadRequestException("请求体 form 无效或缺失");
				}
				return v;
			}
			if (o instanceof String str) {
				long v = Long.parseLong(str.trim());
				if (v <= 0L) {
					throw new BadRequestException("请求体 form 无效或缺失");
				}
				return v;
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException("请求体 form 无效或缺失");
		}
		throw new BadRequestException("请求体 form 无效或缺失");
	}

	private ArticleCategory updateExistingRow(
			long companyId,
			int level,
			String path,
			long parentIdRecursive,
			long categoryId,
			String categoryName,
			long sortValue,
			String categoryType) {
		LambdaQueryWrapper<ArticleCategory> w =
				new LambdaQueryWrapper<ArticleCategory>()
						.eq(ArticleCategory::getCategoryId, categoryId)
						.eq(ArticleCategory::getCompanyId, companyId);
		ArticleCategory entity = articleCategoryMapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}
		entity.setCategoryName(categoryName);
		entity.setCompanyId(companyId);
		entity.setParentId(parentIdRecursive);
		entity.setCategoryLevel(level);
		entity.setPath(path);
		entity.setSort(sortValue);
		entity.setCategoryType(categoryType);
		entity.setUpdated((int) Instant.now().getEpochSecond());
		articleCategoryMapper.updateById(entity);
		return entity;
	}

	private ArticleCategory insertNewRow(
			long companyId,
			long parentId,
			int level,
			String path,
			String categoryName,
			long sortValue,
			String categoryType) {
		ArticleCategory entity = new ArticleCategory();
		entity.setCompanyId(companyId);
		entity.setParentId(parentId);
		entity.setCategoryLevel(level);
		entity.setPath(path);
		entity.setCategoryName(categoryName);
		entity.setSort(sortValue);
		entity.setCategoryType(categoryType);
		int now = (int) Instant.now().getEpochSecond();
		entity.setCreated(now);
		entity.setUpdated(now);
		articleCategoryMapper.insert(entity);
		return entity;
	}

	private void applyPathSecondUpdate(long categoryId, String newPath) {
		LambdaQueryWrapper<ArticleCategory> w =
				new LambdaQueryWrapper<ArticleCategory>().eq(ArticleCategory::getCategoryId, categoryId);
		ArticleCategory entity = articleCategoryMapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}
		entity.setPath(newPath);
		entity.setUpdated((int) Instant.now().getEpochSecond());
		articleCategoryMapper.updateById(entity);
	}
}
