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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.ActiveArticles;
import cn.shopex.ecshopx.promotions.mapper.ActiveArticlesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ActiveArticlesService {

	private static final Pattern PATH_LEADING_INTEGER_PATTERN =
			Pattern.compile("^[-+]?\\d+");

	private final ActiveArticlesMapper activeArticlesMapper;
	private final MessageSource messageSource;

	public ActiveArticlesService(ActiveArticlesMapper activeArticlesMapper, MessageSource messageSource) {
		this.activeArticlesMapper = activeArticlesMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> saveActiveArticle(Map<String, Object> input, Locale locale) {
		validateActiveArticleCoreFields(input, locale);

		Object rawTitle = input.get("article_title");
		String title = rawTitle.toString().trim();

		String subtitle = Optional.ofNullable(textOrNull(input.get("article_subtitle"))).orElse("");
		int isShowInt = parseIntLoose(input.get("is_show"), 1, locale);
		long sortVal = parseLongLoose(input.get("sort"), 0L, locale);

		Object cid = input.get("company_id");
		long companyId =
				cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString().trim());

		ActiveArticles entity = new ActiveArticles();
		entity.setCompanyId(companyId);
		entity.setArticleTitle(title);
		entity.setArticleSubtitle(subtitle);
		entity.setArticleContent(textOrNull(input.get("article_content")));
		entity.setArticleCover(textOrNull(input.get("article_cover")));
		entity.setDirectionalUrl(textOrNull(input.get("directional_url")));
		entity.setIsShow(isShowInt != 0);
		entity.setSort(sortVal);

		int now = (int) Instant.now().getEpochSecond();
		entity.setCreated(now);
		entity.setUpdated(now);

		int rows = activeArticlesMapper.insert(entity);
		if (rows != 1) {
			throw new ResourceException(
					messageSource.getMessage("promotions.active_article.persist_failed", null, locale));
		}

		return Map.of("success", Boolean.TRUE);
	}

	public Map<String, Object> updateActiveArticle(Map<String, Object> input, Locale locale) {
		long articleId = parseArticleIdLoose(input.get("id"));
		if (articleId == 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.please_select_article", null, locale));
		}
		validateActiveArticleCoreFields(input, locale);

		Object rawTitle = input.get("article_title");
		String title = rawTitle.toString().trim();

		String subtitle = Optional.ofNullable(textOrNull(input.get("article_subtitle"))).orElse("");
		int isShowInt = parseIntLoose(input.get("is_show"), 1, locale);
		long sortVal = parseLongLoose(input.get("sort"), 0L, locale);

		Object cid = input.get("company_id");
		long companyId =
				cid instanceof Number n ? n.longValue() : Long.parseLong(cid.toString().trim());

		var wrapper =
				Wrappers.<ActiveArticles>lambdaUpdate()
						.eq(ActiveArticles::getCompanyId, companyId)
						.eq(ActiveArticles::getId, articleId)
						.set(ActiveArticles::getArticleTitle, title)
						.set(ActiveArticles::getArticleSubtitle, subtitle)
						.set(ActiveArticles::getArticleContent, textOrNull(input.get("article_content")))
						.set(ActiveArticles::getArticleCover, textOrNull(input.get("article_cover")))
						.set(ActiveArticles::getDirectionalUrl, textOrNull(input.get("directional_url")))
						.set(ActiveArticles::getIsShow, isShowInt != 0)
						.set(ActiveArticles::getSort, sortVal)
						.set(ActiveArticles::getCompanyId, companyId)
						.set(ActiveArticles::getIsDelete, false);

		int rows = activeArticlesMapper.update(null, wrapper);
		boolean success = rows > 0;
		return Map.of("success", success);
	}

	public Map<String, Object> deleteActiveArticle(long companyId, String idRaw, Locale locale) {
		long articleId = parseLeadingIntegerFromPathSegment(idRaw);
		if (articleId <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.please_select_article", null, locale));
		}
		var wrapper =
				Wrappers.<ActiveArticles>lambdaUpdate()
						.eq(ActiveArticles::getCompanyId, companyId)
						.eq(ActiveArticles::getId, articleId)
						.eq(ActiveArticles::getIsDelete, Boolean.FALSE)
						.set(ActiveArticles::getIsDelete, Boolean.TRUE);
		int rows = activeArticlesMapper.update(null, wrapper);
		boolean success = rows > 0;
		return Map.of("success", success);
	}

	public Map<String, Object> getActiveArticleList(
			long companyId,
			String pageRaw,
			String pageSizeRaw,
			String articleTitle,
			String articleSubtitle,
			String articleContent,
			String updateStart,
			String updateEnd,
			Locale locale) {
		int page = parsePageOrDefault(pageRaw, locale);
		int pageSize = parsePageSizeOrDefault(pageSizeRaw, locale);

		LambdaQueryWrapper<ActiveArticles> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ActiveArticles::getCompanyId, companyId).eq(ActiveArticles::getIsDelete, Boolean.FALSE);

		String titleTrimmed = articleTitle == null ? "" : articleTitle.trim();
		if (StringUtils.hasText(titleTrimmed)) {
			wrapper.like(ActiveArticles::getArticleTitle, titleTrimmed);
		}
		String subtitleTrimmed = articleSubtitle == null ? "" : articleSubtitle.trim();
		if (StringUtils.hasText(subtitleTrimmed)) {
			wrapper.like(ActiveArticles::getArticleSubtitle, subtitleTrimmed);
		}
		String contentTrimmed = articleContent == null ? "" : articleContent.trim();
		if (StringUtils.hasText(contentTrimmed)) {
			wrapper.eq(ActiveArticles::getArticleContent, contentTrimmed);
		}

		String updateStartNorm = textOrNull(updateStart);
		if (updateStartNorm != null) {
			int startSec = parseUnixSeconds(updateStartNorm, locale);
			wrapper.ge(ActiveArticles::getUpdated, startSec);
		}
		String updateEndNorm = textOrNull(updateEnd);
		if (updateEndNorm != null) {
			int endSec = parseUnixSeconds(updateEndNorm, locale);
			wrapper.le(ActiveArticles::getUpdated, endSec);
		}

		wrapper.orderByDesc(ActiveArticles::getSort).orderByDesc(ActiveArticles::getId);

		Page<ActiveArticles> p = new Page<>(page, pageSize);
		activeArticlesMapper.selectPage(p, wrapper);
		long totalCount = p.getTotal();
		List<ActiveArticles> records = p.getRecords() == null ? List.of() : p.getRecords();

		List<Map<String, Object>> list = new ArrayList<>();
		for (ActiveArticles e : records) {
			list.add(toActiveArticleRowMap(e));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}

	public Optional<Map<String, Object>> getActiveArticleDetail(long companyId, String idRaw) {
		long id = parseLeadingIntegerFromPathSegment(idRaw);
		if (id == 0L) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ActiveArticles> w = Wrappers.lambdaQuery();
		w.eq(ActiveArticles::getCompanyId, companyId)
				.eq(ActiveArticles::getId, id)
				.eq(ActiveArticles::getIsDelete, Boolean.FALSE);
		ActiveArticles row = activeArticlesMapper.selectOne(w);
		if (row == null) {
			return Optional.empty();
		}
		return Optional.of(toActiveArticleDetailMap(row));
	}

	private static long parseLeadingIntegerFromPathSegment(String raw) {
		if (raw == null) {
			return 0L;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0L;
		}
		var m = PATH_LEADING_INTEGER_PATTERN.matcher(s);
		if (!m.find() || m.start() != 0) {
			return 0L;
		}
		String matched = m.group();
		try {
			return Long.parseLong(matched);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, Object> toActiveArticleDetailMap(ActiveArticles e) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("id", e.getId());
		map.put("company_id", e.getCompanyId());
		map.put("article_title", e.getArticleTitle() == null ? "" : e.getArticleTitle());
		map.put("article_subtitle", e.getArticleSubtitle());
		map.put("article_content", e.getArticleContent() == null ? "" : e.getArticleContent());
		map.put("article_cover", e.getArticleCover() == null ? "" : e.getArticleCover());
		map.put("directional_url", e.getDirectionalUrl() == null ? "" : e.getDirectionalUrl());
		map.put("is_show", Boolean.valueOf(Boolean.TRUE.equals(e.getIsShow())));
		map.put("is_delete", Boolean.valueOf(Boolean.TRUE.equals(e.getIsDelete())));
		map.put("sort", e.getSort());
		map.put("created", e.getCreated() == null ? Integer.valueOf(0) : e.getCreated());
		map.put("updated", e.getUpdated());
		return map;
	}

	private int parsePageOrDefault(String pageRaw, Locale locale) {
		if (pageRaw == null || pageRaw.trim().isEmpty()) {
			return 1;
		}
		return parseStrictPositiveInt(pageRaw.trim(), locale);
	}

	private int parsePageSizeOrDefault(String pageSizeRaw, Locale locale) {
		if (pageSizeRaw == null || pageSizeRaw.trim().isEmpty()) {
			return 10;
		}
		return parseStrictPositiveInt(pageSizeRaw.trim(), locale);
	}

	private int parseStrictPositiveInt(String s, Locale locale) {
		try {
			long v = Long.parseLong(s);
			if (v < 1L || v > Integer.MAX_VALUE) {
				throw new BadRequestException(
						messageSource.getMessage("promotions.active_article.pagination_invalid", null, locale));
			}
			return (int) v;
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.pagination_invalid", null, locale));
		}
	}

	private int parseUnixSeconds(String s, Locale locale) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.update_time_invalid", null, locale));
		}
	}

	private static Map<String, Object> toActiveArticleRowMap(ActiveArticles e) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", e.getId());
		row.put("company_id", e.getCompanyId());
		row.put("article_title", e.getArticleTitle() == null ? "" : e.getArticleTitle());
		row.put("article_subtitle", e.getArticleSubtitle());
		row.put("article_content", e.getArticleContent() == null ? "" : e.getArticleContent());
		row.put("article_cover", e.getArticleCover() == null ? "" : e.getArticleCover());
		row.put("directional_url", e.getDirectionalUrl() == null ? "" : e.getDirectionalUrl());
		row.put("is_show", Boolean.TRUE.equals(e.getIsShow()) ? 1 : 0);
		row.put("is_delete", 0);
		row.put("sort", e.getSort());
		row.put("created", e.getCreated() == null ? 0 : e.getCreated());
		row.put("updated", e.getUpdated());
		return row;
	}

	private void validateActiveArticleCoreFields(Map<String, Object> input, Locale locale) {
		Object rawTitle = input.get("article_title");
		if (rawTitle == null || !StringUtils.hasText(rawTitle.toString())) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.title_required_max_200", null, locale));
		}
		String title = rawTitle.toString().trim();
		if (title.codePointCount(0, title.length()) > 200) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.title_required_max_200", null, locale));
		}

		if (textOrNull(input.get("article_content")) == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.content_required", null, locale));
		}
		if (textOrNull(input.get("article_cover")) == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.cover_required", null, locale));
		}
		if (textOrNull(input.get("directional_url")) == null) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.redirect_url_required", null, locale));
		}
	}

	private static long parseArticleIdLoose(Object rawId) {
		if (rawId == null) {
			return 0L;
		}
		if (rawId instanceof Number n) {
			long v = n.longValue();
			return v <= 0L ? 0L : v;
		}
		String s = rawId.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(s);
			return v <= 0L ? 0L : v;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String textOrNull(Object o) {
		if (o == null) {
			return null;
		}
		String s = o.toString().trim();
		return s.isEmpty() ? null : s;
	}

	private int parseIntLoose(Object o, int defaultValue, Locale locale) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = textOrNull(o);
		if (s == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.sort_invalid", null, locale));
		}
	}

	private long parseLongLoose(Object o, long defaultValue, Locale locale) {
		if (o == null) {
			return defaultValue;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = textOrNull(o);
		if (s == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.active_article.sort_invalid", null, locale));
		}
	}
}
