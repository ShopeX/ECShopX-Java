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
import cn.shopex.ecshopx.companys.domain.Article;
import cn.shopex.ecshopx.companys.mapper.ArticleMapper;
import cn.shopex.ecshopx.companys.mapper.dto.ArticleProvinceGroupRow;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleListQueryService {

	private final ArticleMapper articleMapper;
	private final CommonLangModReadService commonLangModReadService;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public ArticleListQueryService(
			ArticleMapper articleMapper,
			CommonLangModReadService commonLangModReadService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.articleMapper = articleMapper;
		this.commonLangModReadService = commonLangModReadService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("unused")
	public Map<String, Object> listDataArticle(
			long companyId,
			String authorizerAppidIgnored,
			boolean titleSearchPresent,
			String titleNeedle,
			long distributorId,
			List<Long> requestedArticleIdsOrNull,
			boolean releaseStatusQueryKeyPresent,
			String releaseStatusRaw,
			String articleType,
			int page,
			int pageSize,
			String requestLang) {
		Page<Article> mpPage = new Page<>(page, pageSize, false);
		LambdaQueryWrapper<Article> w = new LambdaQueryWrapper<>();
		w.eq(Article::getCompanyId, companyId);
		if (distributorId == 0L) {
			w.and(
					q ->
							q.eq(Article::getDistributorId, 0L)
									.or()
									.isNull(Article::getDistributorId));
		} else {
			w.eq(Article::getDistributorId, distributorId);
		}
		w.eq(Article::getArticleType, articleType == null ? "general" : articleType);

		if (releaseStatusQueryKeyPresent) {
			String rsTrim = releaseStatusRaw == null ? "" : releaseStatusRaw.trim();
			if (!"2".equals(rsTrim)) {
				w.eq(Article::getReleaseStatus, truthyBoolFromQueryValue(releaseStatusRaw));
			}
		}

		boolean titleBranchMergedIn = false;
		if (titleSearchPresent) {
			List<Long> langHitIds =
					commonLangModReadService.filterArticleIdsByTitleContainsForList(
							requestLang, "companys_article", "title", titleNeedle);
			if (!langHitIds.isEmpty()) {
				titleBranchMergedIn = true;
				if (requestedArticleIdsOrNull != null && !requestedArticleIdsOrNull.isEmpty()) {
					LinkedHashSet<Long> merged = new LinkedHashSet<>(requestedArticleIdsOrNull);
					merged.addAll(langHitIds);
					w.in(Article::getArticleId, merged);
				} else {
					w.in(Article::getArticleId, langHitIds);
				}
			} else {
				w.like(Article::getTitle, "%" + escapeLike(titleNeedle) + "%");
			}
		}
		if (!titleBranchMergedIn
				&& requestedArticleIdsOrNull != null
				&& !requestedArticleIdsOrNull.isEmpty()) {
			w.in(Article::getArticleId, requestedArticleIdsOrNull);
		}

		w.orderByAsc(Article::getSort).orderByDesc(Article::getArticleId);

		Long total = articleMapper.selectCount(w);
		int totalCount = total == null ? 0 : total.intValue();
		if (totalCount == 0) {
			return Map.of("total_count", 0, "list", List.of());
		}

		articleMapper.selectPage(mpPage, w);
		List<Article> records = mpPage.getRecords();
		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (Article entity : records) {
			listMaps.add(articleRowToMap(entity));
		}

		if (!listMaps.isEmpty()) {
			commonLangModReadService.enrichArticleRowsWithListLang(listMaps, requestLang);
		}

		for (Map<String, Object> row : listMaps) {
			if (!Objects.equals(row.get("article_type"), "bring")) {
				continue;
			}
			long companyIdRow = ((Number) row.get("company_id")).longValue();
			long articleId = ((Number) row.get("article_id")).longValue();
			Map<String, Object> focus = new LinkedHashMap<>();
			focus.put(
					"count",
					redisCount("articleFocus:" + String.valueOf(companyIdRow), articleId));
			row.put("articleFocusNum", focus);
			Map<String, Object> praise = new LinkedHashMap<>();
			praise.put(
					"count",
					redisCount("articlePraise:" + String.valueOf(companyIdRow), articleId));
			row.put("articlePraiseNum", praise);
			row.put("isPraise", false);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listMaps);
		return out;
	}

	public Map<String, Object> listDataArticle(
			long companyId,
			long userIdForPraise,
			boolean titleSearchPresent,
			String titleNeedle,
			List<Long> requestedArticleIdsOrNull,
			Long categoryIdOrNull,
			String provinceOrNull,
			String cityOrNull,
			String areaOrNull,
			String articleType,
			int page,
			int pageSize,
			String requestLang) {
		Page<Article> mpPage = new Page<>(page, pageSize, false);
		LambdaQueryWrapper<Article> w = new LambdaQueryWrapper<>();
		w.eq(Article::getCompanyId, companyId);
		w.eq(Article::getArticleType, articleType == null ? "general" : articleType);
		w.eq(Article::getReleaseStatus, Boolean.TRUE);
		if (categoryIdOrNull != null) {
			w.eq(Article::getCategoryId, categoryIdOrNull);
		}
		if (StringUtils.hasText(provinceOrNull)) {
			w.eq(Article::getProvince, provinceOrNull.trim());
		}
		if (StringUtils.hasText(cityOrNull)) {
			w.eq(Article::getCity, cityOrNull.trim());
		}
		if (StringUtils.hasText(areaOrNull)) {
			w.eq(Article::getArea, areaOrNull.trim());
		}

		boolean titleBranchMergedIn = false;
		if (titleSearchPresent) {
			List<Long> langHitIds =
					commonLangModReadService.filterArticleIdsByTitleContainsForList(
							requestLang, "companys_article", "title", titleNeedle);
			if (!langHitIds.isEmpty()) {
				titleBranchMergedIn = true;
				if (requestedArticleIdsOrNull != null && !requestedArticleIdsOrNull.isEmpty()) {
					LinkedHashSet<Long> merged = new LinkedHashSet<>(requestedArticleIdsOrNull);
					merged.addAll(langHitIds);
					w.in(Article::getArticleId, merged);
				} else {
					w.in(Article::getArticleId, langHitIds);
				}
			} else {
				w.like(Article::getTitle, "%" + escapeLike(titleNeedle) + "%");
			}
		}
		if (!titleBranchMergedIn
				&& requestedArticleIdsOrNull != null
				&& !requestedArticleIdsOrNull.isEmpty()) {
			w.in(Article::getArticleId, requestedArticleIdsOrNull);
		}

		w.orderByAsc(Article::getSort).orderByDesc(Article::getReleaseTime);

		Long total = articleMapper.selectCount(w);
		int totalCount = total == null ? 0 : total.intValue();
		List<Map<String, Object>> listMaps;
		if (totalCount == 0) {
			listMaps = List.of();
		} else {
			articleMapper.selectPage(mpPage, w);
			List<Article> records = mpPage.getRecords();
			listMaps = new ArrayList<>();
			for (Article entity : records) {
				listMaps.add(articleRowToMap(entity));
			}
			if (!listMaps.isEmpty()) {
				commonLangModReadService.enrichArticleRowsWithListLang(listMaps, requestLang);
			}
			for (Map<String, Object> row : listMaps) {
				if (!Objects.equals(row.get("article_type"), "bring")) {
					continue;
				}
				long companyIdRow = ((Number) row.get("company_id")).longValue();
				long articleId = ((Number) row.get("article_id")).longValue();
				Map<String, Object> focus = new LinkedHashMap<>();
				focus.put(
						"count",
						redisCount("articleFocus:" + String.valueOf(companyIdRow), articleId));
				row.put("articleFocusNum", focus);
				Map<String, Object> praise = new LinkedHashMap<>();
				praise.put(
						"count",
						redisCount("articlePraise:" + String.valueOf(companyIdRow), articleId));
				row.put("articlePraiseNum", praise);
				if (userIdForPraise != 0L) {
					String praiseUserKey =
							"articlePraiseUser:" + companyIdRow + ":" + articleId;
					Object raw =
							companysRedisTemplate
									.opsForHash()
									.get(praiseUserKey, String.valueOf(userIdForPraise));
					boolean isPraise =
							raw != null && !String.valueOf(raw).trim().isEmpty();
					row.put("isPraise", isPraise);
				} else {
					row.put("isPraise", Boolean.FALSE);
				}
			}
		}

		List<Object> provinceList = buildProvinceListForCompany(companyId);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listMaps);
		out.put("province_list", provinceList);
		return out;
	}

	private List<Object> buildProvinceListForCompany(long companyId) {
		List<Object> provinceList = new ArrayList<>();
		List<ArticleProvinceGroupRow> rows =
				articleMapper.selectProvinceRegionsGroupByProvince(companyId);
		for (ArticleProvinceGroupRow row : rows) {
			String regionsId = row.getRegionsId();
			if (!StringUtils.hasText(regionsId)) {
				continue;
			}
			try {
				JsonNode node = objectMapper.readTree(regionsId);
				if (node != null && node.isArray() && node.size() > 0) {
					provinceList.add(objectMapper.convertValue(node.get(0), Object.class));
				}
			} catch (JsonProcessingException ignored) {
				// skip malformed row
			}
		}
		return provinceList;
	}

	public List<Object> getAllProvince(long companyId) {
		return buildProvinceListForCompany(companyId);
	}

	public Map<String, Object> listArticlesByIdsForMemberFav(List<Long> articleIds, String requestLang) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (articleIds == null || articleIds.isEmpty()) {
			out.put("total_count", 0);
			out.put("list", List.of());
			return out;
		}
		LambdaQueryWrapper<Article> w = new LambdaQueryWrapper<>();
		w.in(Article::getArticleId, articleIds);
		w.orderByDesc(Article::getCreated);
		Page<Article> mpPage = new Page<>(1, 100, false);
		Long total = articleMapper.selectCount(w);
		int totalCount = total == null ? 0 : total.intValue();
		List<Map<String, Object>> listMaps;
		if (totalCount == 0) {
			listMaps = List.of();
		} else {
			articleMapper.selectPage(mpPage, w);
			List<Article> records = mpPage.getRecords();
			listMaps = new ArrayList<>();
			for (Article entity : records) {
				listMaps.add(articleRowToMap(entity));
			}
			if (!listMaps.isEmpty()) {
				commonLangModReadService.enrichArticleRowsWithListLang(listMaps, requestLang);
			}
		}
		out.put("total_count", totalCount);
		out.put("list", listMaps);
		return out;
	}

	public Map<String, Object> articleFocus(long companyId, long articleId) {
		if (companyId <= 0L || articleId < 1L) {
			throw new BadRequestException("参数错误");
		}
		LambdaQueryWrapper<Article> w = new LambdaQueryWrapper<>();
		w.eq(Article::getArticleId, articleId).eq(Article::getCompanyId, companyId);
		Article row = articleMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("文章不存在");
		}
		String key = "articleFocus:" + row.getCompanyId();
		String field = String.valueOf(row.getArticleId());
		long count =
				Objects.requireNonNull(companysRedisTemplate.opsForHash().increment(key, field, 1L));
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("count", count);
		return out;
	}

	public Map<String, Object> articlePraise(long companyId, long articleId, long userId) {
		if (companyId <= 0L || articleId < 1L || userId <= 0L) {
			throw new BadRequestException("参数错误");
		}
		LambdaQueryWrapper<Article> w = new LambdaQueryWrapper<>();
		w.eq(Article::getArticleId, articleId).eq(Article::getCompanyId, companyId);
		Article row = articleMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("文章不存在");
		}
		String praiseKey = "articlePraise:" + companyId;
		String fieldArticleId = String.valueOf(articleId);
		String userKey = "articlePraiseUser:" + companyId + ":" + articleId;
		Object rawPraised =
				companysRedisTemplate.opsForHash().get(userKey, String.valueOf(userId));
		boolean alreadyPraised = rawPraised != null && !String.valueOf(rawPraised).trim().isEmpty();
		Map<String, Object> out = new LinkedHashMap<>();
		if (alreadyPraised) {
			long count =
					Objects.requireNonNull(
							companysRedisTemplate
									.opsForHash()
									.increment(praiseKey, fieldArticleId, -1L));
			companysRedisTemplate.opsForHash().delete(userKey, String.valueOf(userId));
			out.put("count", count);
			out.put("status", Boolean.FALSE);
		} else {
			long count =
					Objects.requireNonNull(
							companysRedisTemplate
									.opsForHash()
									.increment(praiseKey, fieldArticleId, 1L));
			String ts = Long.toString(Instant.now().getEpochSecond());
			companysRedisTemplate.opsForHash().put(userKey, String.valueOf(userId), ts);
			out.put("count", count);
			out.put("status", Boolean.TRUE);
		}
		return out;
	}

	public Map<String, Object> articleFocusNum(long companyId, String articleIdRaw) {
		if (articleIdRaw == null) {
			throw new BadRequestException("参数错误");
		}
		String trimmed = articleIdRaw.trim();
		if (trimmed.isEmpty() || "0".equals(trimmed)) {
			throw new BadRequestException("参数错误");
		}
		if (companyId <= 0L) {
			throw new BadRequestException("参数错误");
		}
		long count = redisHashFieldCount("articleFocus:" + companyId, trimmed);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("count", count);
		return out;
	}

	public Map<String, Object> articlePraiseNum(long companyId, String articleIdRaw) {
		if (articleIdRaw == null) {
			throw new BadRequestException("参数错误");
		}
		String trimmed = articleIdRaw.trim();
		if (trimmed.isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		if ("0".equals(trimmed)) {
			throw new ResourceException("参数错误");
		}
		if (companyId <= 0L) {
			throw new BadRequestException("参数错误");
		}
		long count = redisHashFieldCount("articlePraise:" + companyId, trimmed);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("count", count);
		return out;
	}

	public Map<String, Object> getArticlePraiseResult(
			long companyId,
			List<String> articleIdStrings,
			boolean includeUserStatus,
			long userId) {
		if (articleIdStrings == null || articleIdStrings.isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		if (companyId <= 0L) {
			return new LinkedHashMap<>();
		}
		if (includeUserStatus && userId <= 0L) {
			throw new BadRequestException("参数错误");
		}
		if (!includeUserStatus && userId != 0L) {
			throw new BadRequestException("参数错误");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (String articleIdStr : articleIdStrings) {
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			long count = redisHashFieldCount("articlePraise:" + companyId, articleIdStr);
			item.put("count", count);
			if (includeUserStatus) {
				String userKey = "articlePraiseUser:" + companyId + ":" + articleIdStr;
				Object raw =
						companysRedisTemplate.opsForHash().get(userKey, String.valueOf(userId));
				boolean status = raw != null && !String.valueOf(raw).trim().isEmpty();
				item.put("status", status);
			}
			out.put(articleIdStr, item);
		}
		return out;
	}

	private long redisHashFieldCount(String fullHashKey, String field) {
		Object raw = companysRedisTemplate.opsForHash().get(fullHashKey, field);
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private long redisCount(String hashKeyPrefixWithCompanyId, long articleId) {
		return redisHashFieldCount(hashKeyPrefixWithCompanyId, String.valueOf(articleId));
	}

	private Map<String, Object> articleRowToMap(Article entity) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("article_id", entity.getArticleId());
		row.put("company_id", entity.getCompanyId());
		row.put("title", entity.getTitle());
		row.put("summary", entity.getSummary());
		row.put("content", "");
		row.put("sort", entity.getSort());
		row.put("image_url", entity.getImageUrl());
		row.put("share_image_url", entity.getShareImageUrl());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		row.put("author", entity.getAuthor());
		row.put("operator_id", entity.getOperatorId());
		row.put("release_status", entity.getReleaseStatus());
		row.put("release_time", entity.getReleaseTime());
		row.put("article_type", entity.getArticleType());
		row.put("distributor_id", entity.getDistributorId() != null ? entity.getDistributorId() : 0L);
		row.put("head_portrait", entity.getHeadPortrait());
		row.put("province", entity.getProvince());
		row.put("city", entity.getCity());
		row.put("area", entity.getArea());
		row.put("regions", regionsValueForListResponse(entity.getRegions()));
		row.put("regions_id", regionsIdValueForListResponse(entity.getRegionsId()));
		row.put("category_id", entity.getCategoryId());
		row.put("is_ai", entity.getIsAi() != null ? entity.getIsAi() : false);
		return row;
	}

	private Object regionsValueForListResponse(String raw) {
		return regionsStructuredValueForListResponse(raw);
	}

	private Object regionsIdValueForListResponse(String raw) {
		return regionsStructuredValueForListResponse(raw);
	}

	/** List response: blank or JSON empty array becomes an empty list; parse errors keep the raw string. */
	private Object regionsStructuredValueForListResponse(String raw) {
		if (raw == null || raw.isBlank()) {
			return Collections.emptyList();
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			if (n != null && n.isArray() && n.size() == 0) {
				return Collections.emptyList();
			}
			return objectMapper.convertValue(n, Object.class);
		} catch (JsonProcessingException e) {
			return raw;
		}
	}

	private static String escapeLike(String needle) {
		if (needle == null) {
			return "";
		}
		return needle
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}

	private static boolean truthyBoolFromQueryValue(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.intValue() != 0;
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			return true;
		}
		String t = String.valueOf(o).trim();
		if (t.isEmpty() || "0".equals(t)) {
			return false;
		}
		return true;
	}
}
