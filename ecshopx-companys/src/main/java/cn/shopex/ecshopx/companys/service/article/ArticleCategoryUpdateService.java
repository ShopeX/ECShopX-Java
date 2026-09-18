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
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.domain.ArticleCategory;
import cn.shopex.ecshopx.companys.mapper.ArticleCategoryMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleCategoryUpdateService {

	private static final String TABLE_LANG = "companys_article_category";
	private static final String MODULE_LANG = "companys_article_category";
	private static final List<String> WHITELIST =
			List.of(
					"category_name",
					"parent_id",
					"category_level",
					"path",
					"sort",
					"created",
					"updated",
					"category_type");
	private static final List<String> LANG_STRIP_KEYS = List.of("category_name");

	private final ArticleCategoryMapper articleCategoryMapper;
	private final LangueProperties langueProperties;
	private final CommonLangModWriteService commonLangModWriteService;

	public ArticleCategoryUpdateService(
			ArticleCategoryMapper articleCategoryMapper,
			LangueProperties langueProperties,
			CommonLangModWriteService commonLangModWriteService) {
		this.articleCategoryMapper = articleCategoryMapper;
		this.langueProperties = langueProperties;
		this.commonLangModWriteService = commonLangModWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateCategory(
			String categoryIdPath,
			Map<String, Object> mergedInput,
			long companyId,
			String requestLang) {
		Map<String, Object> input = mergedInput == null ? Map.of() : mergedInput;

		String p = categoryIdPath == null ? "" : categoryIdPath.trim();
		Long categoryIdParsed = null;
		try {
			if (!p.isEmpty()) {
				long v = Long.parseLong(p);
				if (v > 0L && v <= (long) Integer.MAX_VALUE) {
					categoryIdParsed = v;
				}
			}
		} catch (NumberFormatException ignored) {
			categoryIdParsed = null;
		}

		if (categoryIdParsed == null) {
			articleCategoryMapper.selectOne(
					new LambdaQueryWrapper<ArticleCategory>()
							.eq(ArticleCategory::getCompanyId, companyId)
							.eq(ArticleCategory::getCategoryId, -1L));
			throw new ResourceException("未查询到更新数据");
		}

		long categoryId = categoryIdParsed;
		LambdaQueryWrapper<ArticleCategory> w =
				new LambdaQueryWrapper<ArticleCategory>()
						.eq(ArticleCategory::getCategoryId, categoryId)
						.eq(ArticleCategory::getCompanyId, companyId);
		ArticleCategory entity = articleCategoryMapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}

		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : input.entrySet()) {
			String key = e.getKey();
			if (WHITELIST.contains(key)) {
				params.put(key, e.getValue());
			}
		}

		boolean defaultLang = langueProperties.isDefaultLang(requestLang);
		Map<String, Object> dataForMain = defaultLang ? params : stripLangFields(params);
		Map<String, Object> m = dataForMain;

		entity.setCompanyId(companyId);

		boolean anyUpdatableColumnChanged = false;

		if (m.containsKey("category_name") && ValuePresence.hasEffectiveValue(m.get("category_name"))) {
			String newName = String.valueOf(m.get("category_name")).trim();
			String oldName = entity.getCategoryName();
			String oldNorm = oldName == null ? null : oldName.trim();
			if (!Objects.equals(oldNorm, newName)) {
				entity.setCategoryName(newName);
				anyUpdatableColumnChanged = true;
			}
		}
		if (m.containsKey("parent_id") && m.get("parent_id") != null) {
			long newPid = parseParentId(m.get("parent_id"));
			long oldPid = entity.getParentId() == null ? 0L : entity.getParentId();
			if (newPid != oldPid) {
				entity.setParentId(newPid);
				anyUpdatableColumnChanged = true;
			}
		}
		if (m.containsKey("category_level") && m.get("category_level") != null) {
			int newLevel = parseCategoryLevel(m.get("category_level"));
			int oldLevel = entity.getCategoryLevel() == null ? 0 : entity.getCategoryLevel();
			if (newLevel != oldLevel) {
				entity.setCategoryLevel(newLevel);
				anyUpdatableColumnChanged = true;
			}
		}
		if (m.containsKey("path") && ValuePresence.hasEffectiveValue(m.get("path"))) {
			String newPath = String.valueOf(m.get("path"));
			if (!Objects.equals(newPath, entity.getPath())) {
				entity.setPath(newPath);
				anyUpdatableColumnChanged = true;
			}
		}
		if (m.containsKey("sort") && m.get("sort") != null) {
			Object rawSort = m.get("sort");
			long newSort;
			if (rawSort instanceof Boolean b) {
				newSort = b ? 1L : 0L;
			} else {
				newSort = coerceSortNonBooleanToLong(rawSort);
			}
			long oldSort = entity.getSort() == null ? 0L : entity.getSort();
			if (newSort != oldSort) {
				entity.setSort(newSort);
				anyUpdatableColumnChanged = true;
			}
		}
		if (m.containsKey("created") && ValuePresence.hasEffectiveValue(m.get("created"))) {
			int newCreated = parseUnixSeconds(m.get("created"), "created");
			int oldCreated = entity.getCreated() == null ? 0 : entity.getCreated();
			if (newCreated != oldCreated) {
				entity.setCreated(newCreated);
				anyUpdatableColumnChanged = true;
			}
		}
		if (m.containsKey("updated") && ValuePresence.hasEffectiveValue(m.get("updated"))) {
			int newUpdated = parseUnixSeconds(m.get("updated"), "updated");
			int oldUpdated = entity.getUpdated() == null ? 0 : entity.getUpdated();
			if (newUpdated != oldUpdated) {
				entity.setUpdated(newUpdated);
				anyUpdatableColumnChanged = true;
			}
		}
		if (m.containsKey("category_type") && ValuePresence.hasEffectiveValue(m.get("category_type"))) {
			String newType = String.valueOf(m.get("category_type"));
			if (!Objects.equals(newType, entity.getCategoryType())) {
				entity.setCategoryType(newType);
				anyUpdatableColumnChanged = true;
			}
		}

		if (anyUpdatableColumnChanged) {
			int rows = articleCategoryMapper.updateById(entity);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		Map<String, Object> langSource = buildLangSource(params, entity, requestLang);
		Map<String, String> langBag = buildLangBag(langSource);
		if (!langueProperties.isDefaultLang(requestLang) && !langBag.isEmpty()) {
			commonLangModWriteService.updateLangData(
					(int) companyId, langBag, TABLE_LANG, (int) categoryId, MODULE_LANG, requestLang);
		}

		return toColumnNamesData(entity);
	}

	private Map<String, Object> buildLangSource(
			Map<String, Object> params, ArticleCategory entity, String requestLang) {
		Map<String, Object> rowAfter = toColumnNamesData(entity);
		if (!langueProperties.isDefaultLang(requestLang)) {
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>(rowAfter);
			for (Map.Entry<String, Object> e : params.entrySet()) {
				merged.put(e.getKey(), e.getValue());
			}
			return merged;
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(params);
		for (Map.Entry<String, Object> e : rowAfter.entrySet()) {
			merged.putIfAbsent(e.getKey(), e.getValue());
		}
		return merged;
	}

	private static Map<String, String> buildLangBag(Map<String, Object> langSource) {
		Map<String, String> bag = new LinkedHashMap<>();
		if (!langSource.containsKey("category_name")) {
			return bag;
		}
		Object v = langSource.get("category_name");
		if (isEffectivelyEmptyCategoryNameForLang(v)) {
			return bag;
		}
		bag.put("category_name", String.valueOf(v));
		return bag;
	}

	private static Map<String, Object> stripLangFields(Map<String, Object> params) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>(params);
		for (String k : LANG_STRIP_KEYS) {
			copy.remove(k);
		}
		return copy;
	}

	private static long coerceSortNonBooleanToLong(Object raw) {
		if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
			throw new ResourceException("排序必须为数字类型");
		}
		String s;
		if (raw instanceof Number n) {
			if (n instanceof BigDecimal bd) {
				s = bd.stripTrailingZeros().toPlainString();
			} else {
				s = n.toString();
			}
		} else if (raw instanceof String str) {
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

	private static boolean isEffectivelyEmptyCategoryNameForLang(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (v instanceof CharSequence cs) {
			String s = cs.toString().trim();
			return s.isEmpty() || "0".equals(s);
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (v instanceof Object[] a) {
			return a.length == 0;
		}
		return false;
	}

	private static long parseParentId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String str) {
			try {
				return Long.parseLong(str.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("parent_id 格式错误");
			}
		}
		throw new BadRequestException("parent_id 格式错误");
	}

	private static int parseCategoryLevel(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw instanceof String str) {
			try {
				return Integer.parseInt(str.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("category_level 格式错误");
			}
		}
		throw new BadRequestException("category_level 格式错误");
	}

	private static int parseUnixSeconds(Object raw, String label) {
		if (raw instanceof Number n) {
			return (int) n.longValue();
		}
		if (raw instanceof String str) {
			try {
				return Integer.parseInt(str.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException(label + " 格式错误");
			}
		}
		throw new BadRequestException(label + " 格式错误");
	}

	private static Map<String, Object> toColumnNamesData(ArticleCategory entity) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("category_id", entity.getCategoryId());
		out.put("company_id", entity.getCompanyId());
		out.put("category_name", entity.getCategoryName());
		out.put("parent_id", entity.getParentId() == null ? 0L : entity.getParentId());
		out.put("category_level", entity.getCategoryLevel());
		out.put("category_type", entity.getCategoryType());
		out.put("path", entity.getPath());
		out.put("sort", entity.getSort());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		return out;
	}
}
