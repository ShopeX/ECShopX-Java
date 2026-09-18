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

import cn.shopex.ecshopx.companys.domain.ArticleCategory;
import cn.shopex.ecshopx.companys.mapper.ArticleCategoryMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleCategoryQueryService {

	/**
	 * Stable ordering for flat category rows before tree assembly: {@code sort} descending,
	 * then {@code created} ascending, then {@code category_id} ascending as the final tie-breaker.
	 */
	private static final Comparator<ArticleCategory> ARTICLE_CATEGORY_LIST_ORDER =
			(a, b) -> {
				int c = Long.compare(longOrZero(b.getSort()), longOrZero(a.getSort()));
				if (c != 0) {
					return c;
				}
				c = Integer.compare(intOrZero(a.getCreated()), intOrZero(b.getCreated()));
				if (c != 0) {
					return c;
				}
				return Long.compare(a.getCategoryId(), b.getCategoryId());
			};

	private final ArticleCategoryMapper articleCategoryMapper;
	private final CommonLangModReadService commonLangModReadService;

	public ArticleCategoryQueryService(
			ArticleCategoryMapper articleCategoryMapper,
			CommonLangModReadService commonLangModReadService) {
		this.articleCategoryMapper = articleCategoryMapper;
		this.commonLangModReadService = commonLangModReadService;
	}

	public List<Map<String, Object>> getCategory(long companyId, String categoryType, String requestLang) {
		return getCategory(companyId, categoryType, requestLang, true);
	}

	public List<Map<String, Object>> getCategory(
			long companyId, String categoryType, String requestLang, boolean includeCategoryNameLang) {
		LambdaQueryWrapper<ArticleCategory> w = new LambdaQueryWrapper<>();
		w.eq(ArticleCategory::getCompanyId, companyId)
				.eq(ArticleCategory::getCategoryType, categoryType)
				.orderByDesc(ArticleCategory::getSort)
				.orderByAsc(ArticleCategory::getCreated)
				.orderByAsc(ArticleCategory::getCategoryId);
		Page<ArticleCategory> page = new Page<>(1, 1000);
		articleCategoryMapper.selectPage(page, w);
		List<ArticleCategory> records = new ArrayList<>(page.getRecords());
		records.sort(ARTICLE_CATEGORY_LIST_ORDER);

		Map<Long, ArticleCategory> byId = new HashMap<>(records.size() * 2);
		for (ArticleCategory e : records) {
			byId.put(e.getCategoryId(), e);
		}

		List<Map<String, Object>> flatRows = new ArrayList<>();
		for (ArticleCategory e : records) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("category_id", e.getCategoryId());
			row.put("company_id", e.getCompanyId());
			row.put("category_name", e.getCategoryName());
			row.put("parent_id", e.getParentId() == null ? 0L : e.getParentId());
			row.put("category_level", e.getCategoryLevel());
			row.put("path", e.getPath());
			row.put("sort", e.getSort());
			row.put("category_type", e.getCategoryType());
			flatRows.add(row);
		}

		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> row : flatRows) {
			Object idObj = row.get("category_id");
			if (idObj instanceof Number n) {
				long id = n.longValue();
				if (id > 0L) {
					ids.add(id);
				}
			}
		}
		Map<Long, String> translated =
				commonLangModReadService.findArticleCategoryCategoryNamesByLocale(companyId, ids, requestLang);
		Map<Long, Map<String, String>> allCategoryNameLangs =
				includeCategoryNameLang
						? commonLangModReadService.findArticleCategoryCategoryNamesAllLocales(companyId, ids)
						: Map.of();
		for (Map<String, Object> row : flatRows) {
			Object idObj = row.get("category_id");
			Long id = idObj instanceof Number n ? n.longValue() : null;
			if (id == null) {
				continue;
			}
			String t = translated.get(id);
			if (t != null && StringUtils.hasText(t)) {
				row.put("category_name", t);
			}
			if (includeCategoryNameLang) {
				Map<String, String> langMap = allCategoryNameLangs.get(id);
				if (langMap != null && !langMap.isEmpty()) {
					row.put("category_name_lang", new LinkedHashMap<>(langMap));
				}
			}
		}

		return getTree(flatRows, byId, 0L, 0);
	}

	private List<Map<String, Object>> getTree(
			List<Map<String, Object>> items, Map<Long, ArticleCategory> byId, long parentId, int level) {
		List<Map<String, Object>> matching = new ArrayList<>();
		for (Map<String, Object> row : items) {
			if (longOrNull(row.get("parent_id")) != parentId) {
				continue;
			}
			matching.add(row);
		}
		Comparator<Map<String, Object>> byCategoryEntityOrder =
				Comparator.comparing(
						(Map<String, Object> row) -> byId.get(longOrNull(row.get("category_id"))),
						Comparator.nullsLast(ARTICLE_CATEGORY_LIST_ORDER));
		matching.sort(byCategoryEntityOrder);

		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> row : matching) {
			LinkedHashMap<String, Object> node = new LinkedHashMap<>(row);
			node.put("level", level);
			List<Map<String, Object>> childList =
					getTree(items, byId, longOrNull(node.get("category_id")), level + 1);
			if (!childList.isEmpty() || parentId == 0) {
				node.put("children", new ArrayList<>(childList));
			}
			out.add(node);
		}
		return out;
	}

	private static long longOrZero(Long v) {
		return v == null ? 0L : v;
	}

	private static int intOrZero(Integer v) {
		return v == null ? 0 : v;
	}

	private static long longOrNull(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
