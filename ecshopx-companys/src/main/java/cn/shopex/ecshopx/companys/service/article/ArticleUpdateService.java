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
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.domain.Article;
import cn.shopex.ecshopx.companys.mapper.ArticleMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleUpdateService {

	private static final String TABLE_LANG = "companys_article";
	private static final String MODULE_LANG = "companys_article";

	private static final List<String> WHITELIST =
			List.of(
					"title",
					"author",
					"summary",
					"content",
					"sort",
					"share_image_url",
					"image_url",
					"article_type",
					"release_status",
					"head_portrait",
					"category_id",
					"regions_id",
					"regions");

	private static final List<String> LANG_STRIP_KEYS =
			List.of("title", "summary", "content", "author", "province", "city", "area", "regions");

	private final ArticleMapper articleMapper;
	private final LangueProperties langueProperties;
	private final CommonLangModWriteService commonLangModWriteService;
	private final ObjectMapper objectMapper;

	public ArticleUpdateService(
			ArticleMapper articleMapper,
			LangueProperties langueProperties,
			CommonLangModWriteService commonLangModWriteService,
			ObjectMapper objectMapper) {
		this.articleMapper = articleMapper;
		this.langueProperties = langueProperties;
		this.commonLangModWriteService = commonLangModWriteService;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateDataArticle(
			String articleIdPath,
			Map<String, Object> mergedInput,
			long companyId,
			long operatorId,
			String requestLang) {
		Map<String, Object> input = mergedInput == null ? Map.of() : mergedInput;

		String p = articleIdPath == null ? "" : articleIdPath.trim();
		Long articleIdParsed = null;
		try {
			if (!p.isEmpty()) {
				long v = Long.parseLong(p);
				if (v > 0L && v <= (long) Integer.MAX_VALUE) {
					articleIdParsed = v;
				}
			}
		} catch (NumberFormatException ignored) {
			articleIdParsed = null;
		}

		if (articleIdParsed == null) {
			articleMapper.selectOne(
					new LambdaQueryWrapper<Article>()
							.eq(Article::getCompanyId, companyId)
							.eq(Article::getArticleId, -1L));
			throw new ResourceException("未查询到更新数据");
		}

		long articleId = articleIdParsed;
		Article entity =
				articleMapper.selectOne(
						new LambdaQueryWrapper<Article>()
								.eq(Article::getArticleId, articleId)
								.eq(Article::getCompanyId, companyId));
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

		if (allValuesRemovedByArrayFilter(params)) {
			throw new BadRequestException("文章编辑出错");
		}
		if (!params.containsKey("content") || isContentEmpty(params.get("content"))) {
			throw new BadRequestException("文章内容不能为空");
		}

		if (params.containsKey("regions_id") && params.containsKey("regions")) {
			applyRegionsToParams(params, params.get("regions"));
		}
		if (params.containsKey("regions") && normalizeToList(params.get("regions")).size() == 2) {
			params.put("area", "");
		}

		params.put("company_id", companyId);
		params.put("operator_id", operatorId);

		params.put("summary", Objects.toString(params.get("summary"), ""));
		params.put("author", Objects.toString(params.get("author"), ""));
		params.put("head_portrait", Objects.toString(params.get("head_portrait"), ""));
		if ("0".equals(Objects.toString(params.get("head_portrait"), ""))) {
			params.put("head_portrait", "");
		}

		if (params.containsKey("release_status")) {
			applyReleaseStatusRules(params);
		}

		boolean defaultLang = langueProperties.isDefaultLang(requestLang);
		Map<String, Object> dataForMain = defaultLang ? params : stripLangFields(params);
		Map<String, Object> m = dataForMain;

		boolean anyUpdatableColumnChanged = applyTruthyMainColumnUpdates(entity, m, companyId);

		if (anyUpdatableColumnChanged) {
			entity.setUpdated((int) (System.currentTimeMillis() / 1000L));
			int rows = articleMapper.updateById(entity);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		Map<String, Object> langSource = buildLangSource(params, entity, requestLang);
		Map<String, String> langBag = buildLangBag(langSource);
		if (!langueProperties.isDefaultLang(requestLang) && !langBag.isEmpty()) {
			commonLangModWriteService.updateLangData(
					(int) companyId, langBag, TABLE_LANG, (int) articleId, MODULE_LANG, requestLang);
		}

		return toColumnNamesData(entity);
	}

	/**
	 * 批量按项更新文章的发布状态/排序；{@code companyId} 与历史接口签名对齐，不作为行定位条件使用。
	 */
	public void updateArticleStatusOrSort(
			@SuppressWarnings("unused") long companyId, List<Map<String, Object>> inputdata) {
		if (inputdata == null) {
			throw new BadRequestException("文章编辑参数有误");
		}
		if (inputdata.isEmpty()) {
			return;
		}
		for (Map<String, Object> value : inputdata) {
			if (value == null
					|| !value.containsKey("article_id")
					|| value.get("article_id") == null) {
				throw new BadRequestException("文章编辑参数有误");
			}
			long articleIdLong = parseArticleIdStrictPositiveIntInRange(value.get("article_id"));

			boolean hasRs =
					value.containsKey("release_status") && value.get("release_status") != null;
			boolean hasSort = value.containsKey("sort") && value.get("sort") != null;
			if (!hasRs && !hasSort) {
				throw new BadRequestException("文章编辑参数有误");
			}

			Article entity =
					articleMapper.selectOne(
							new LambdaQueryWrapper<Article>().eq(Article::getArticleId, articleIdLong));
			if (entity == null) {
				throw new ResourceException("未查询到更新数据");
			}

			Boolean snapRs = entity.getReleaseStatus();
			Integer snapRt = entity.getReleaseTime();
			Integer snapSort = entity.getSort();

			if (hasRs) {
				Object v = value.get("release_status");
				if (isReleaseStatusFalseBranch(v)) {
					entity.setReleaseStatus(false);
				} else {
					entity.setReleaseStatus(true);
					entity.setReleaseTime((int) (System.currentTimeMillis() / 1000L));
				}
			}

			if (hasSort) {
				Object sortRaw = value.get("sort");
				int sortParsed = parseSortAsIntStrict(sortRaw);
				if (isScalarNumberTruthyForWrite(sortRaw)) {
					entity.setSort(sortParsed);
				}
			}

			boolean changed =
					!Objects.equals(entity.getReleaseStatus(), snapRs)
							|| !Objects.equals(entity.getReleaseTime(), snapRt)
							|| !Objects.equals(entity.getSort(), snapSort);

			if (changed) {
				entity.setUpdated((int) (System.currentTimeMillis() / 1000L));
				int rows = articleMapper.updateById(entity);
				if (rows == 0) {
					throw new ResourceException("未查询到更新数据");
				}
			}
		}
	}

	private boolean applyTruthyMainColumnUpdates(Article entity, Map<String, Object> m, long companyId) {
		boolean any = false;

		entity.setCompanyId(companyId);

		if (m.containsKey("title") && isScalarStringTruthyForWrite(m.get("title"))) {
			String nv = String.valueOf(m.get("title")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getTitle()))) {
				entity.setTitle(nv);
				any = true;
			}
		}
		if (m.containsKey("author") && isScalarStringTruthyForWrite(m.get("author"))) {
			String nv = String.valueOf(m.get("author")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getAuthor()))) {
				entity.setAuthor(nv);
				any = true;
			}
		}
		if (m.containsKey("summary") && isScalarStringTruthyForWrite(m.get("summary"))) {
			String nv = String.valueOf(m.get("summary")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getSummary()))) {
				entity.setSummary(nv);
				any = true;
			}
		}
		if (m.containsKey("content") && !isContentEmpty(m.get("content"))) {
			String encoded;
			try {
				encoded = objectMapper.writeValueAsString(m.get("content"));
			} catch (JsonProcessingException ex) {
				throw new ResourceException("文章内容格式错误");
			}
			if (!Objects.equals(encoded, entity.getContent())) {
				entity.setContent(encoded);
				any = true;
			}
		}
		if (m.containsKey("sort") && isScalarNumberTruthyForWrite(m.get("sort"))) {
			int nv = toInt(m.get("sort"));
			int ov = entity.getSort() == null ? 0 : entity.getSort();
			if (nv != ov) {
				entity.setSort(nv);
				any = true;
			}
		}
		if (m.containsKey("image_url") && isScalarStringTruthyForWrite(m.get("image_url"))) {
			String nv = String.valueOf(m.get("image_url")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getImageUrl()))) {
				entity.setImageUrl(nv);
				any = true;
			}
		}
		if (m.containsKey("share_image_url") && isScalarStringTruthyForWrite(m.get("share_image_url"))) {
			String nv = String.valueOf(m.get("share_image_url")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getShareImageUrl()))) {
				entity.setShareImageUrl(nv);
				any = true;
			}
		}
		if (m.containsKey("article_type") && isScalarStringTruthyForWrite(m.get("article_type"))) {
			String nv = String.valueOf(m.get("article_type")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getArticleType()))) {
				entity.setArticleType(nv);
				any = true;
			}
		}
		if (m.containsKey("head_portrait") && isScalarStringTruthyForWrite(m.get("head_portrait"))) {
			String nv = String.valueOf(m.get("head_portrait")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getHeadPortrait()))) {
				entity.setHeadPortrait(nv);
				any = true;
			}
		}
		if (m.containsKey("category_id") && isScalarNumberTruthyForWrite(m.get("category_id"))) {
			long nv = toLongStrict(m.get("category_id"));
			long ov = entity.getCategoryId() == null ? 0L : entity.getCategoryId();
			if (nv != ov) {
				entity.setCategoryId(nv);
				any = true;
			}
		}
		if (m.containsKey("province") && isScalarStringTruthyForWrite(m.get("province"))) {
			String nv = String.valueOf(m.get("province")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getProvince()))) {
				entity.setProvince(nv);
				any = true;
			}
		}
		if (m.containsKey("city") && isScalarStringTruthyForWrite(m.get("city"))) {
			String nv = String.valueOf(m.get("city")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getCity()))) {
				entity.setCity(nv);
				any = true;
			}
		}
		if (m.containsKey("area") && isScalarStringTruthyForWrite(m.get("area"))) {
			String nv = String.valueOf(m.get("area")).trim();
			if (!Objects.equals(nv, nullSafeTrim(entity.getArea()))) {
				entity.setArea(nv);
				any = true;
			}
		}
		if (m.containsKey("regions") && isStructuredValueTruthyForWrite(m.get("regions"))) {
			String encoded;
			try {
				encoded = objectMapper.writeValueAsString(m.get("regions"));
			} catch (JsonProcessingException ex) {
				throw new ResourceException("地区数据格式错误");
			}
			if (!Objects.equals(encoded, entity.getRegions())) {
				entity.setRegions(encoded);
				any = true;
			}
		}
		if (m.containsKey("regions_id") && isStructuredValueTruthyForWrite(m.get("regions_id"))) {
			String encoded;
			try {
				encoded = objectMapper.writeValueAsString(m.get("regions_id"));
			} catch (JsonProcessingException ex) {
				throw new ResourceException("地区编号格式错误");
			}
			if (!Objects.equals(encoded, entity.getRegionsId())) {
				entity.setRegionsId(encoded);
				any = true;
			}
		}
		if (m.containsKey("release_status")) {
			Boolean nb = toBooleanStrict(m.get("release_status"));
			if (!Objects.equals(nb, entity.getReleaseStatus())) {
				entity.setReleaseStatus(nb);
				any = true;
			}
		}
		if (m.containsKey("release_time") && isScalarNumberTruthyForWrite(m.get("release_time"))) {
			int nv = toInt(m.get("release_time"));
			int ov = entity.getReleaseTime() == null ? 0 : entity.getReleaseTime();
			if (nv != ov) {
				entity.setReleaseTime(nv);
				any = true;
			}
		}
		if (m.containsKey("operator_id") && isScalarNumberTruthyForWrite(m.get("operator_id"))) {
			long nv = toLongStrict(m.get("operator_id"));
			long ov = entity.getOperatorId() == null ? 0L : entity.getOperatorId();
			if (nv != ov) {
				entity.setOperatorId(nv);
				any = true;
			}
		}

		return any;
	}

	private static String nullSafeTrim(String s) {
		return s == null ? null : s.trim();
	}

	private static boolean isScalarStringTruthyForWrite(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof CharSequence cs) {
			String s = cs.toString().trim();
			return !s.isEmpty() && !"0".equals(s);
		}
		return false;
	}

	private static boolean isScalarNumberTruthyForWrite(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty() || "0".equals(s)) {
				return false;
			}
			try {
				return Long.parseLong(s) != 0L;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	private static boolean isStructuredValueTruthyForWrite(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof CharSequence cs) {
			String s = cs.toString().trim();
			return !s.isEmpty() && !"0".equals(s);
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> mp) {
			return !mp.isEmpty();
		}
		if (v instanceof Object[] a) {
			return a.length > 0;
		}
		return false;
	}

	private Map<String, Object> buildLangSource(
			Map<String, Object> params, Article entity, String requestLang) {
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

	private Map<String, String> buildLangBag(Map<String, Object> langSource) {
		Map<String, String> bag = new LinkedHashMap<>();
		String[] fields = {"title", "summary", "content", "author", "province", "city", "area", "regions"};
		for (String field : fields) {
			if (!langSource.containsKey(field)) {
				continue;
			}
			Object v = langSource.get(field);
			if (isEffectivelyEmpty(v)) {
				continue;
			}
			try {
				if ("content".equals(field)) {
					bag.put(field, objectMapper.writeValueAsString(v));
				} else if (v instanceof Map || v instanceof Collection<?>) {
					bag.put(field, objectMapper.writeValueAsString(v));
				} else {
					bag.put(field, String.valueOf(v));
				}
			} catch (JsonProcessingException ex) {
				throw new ResourceException("多语言字段序列化失败");
			}
		}
		return bag;
	}

	private static boolean isEffectivelyEmpty(Object v) {
		return isContentEmpty(v);
	}

	private static Map<String, Object> stripLangFields(Map<String, Object> params) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>(params);
		for (String k : LANG_STRIP_KEYS) {
			copy.remove(k);
		}
		return copy;
	}

	private Map<String, Object> toColumnNamesData(Article entity) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("article_id", entity.getArticleId());
		out.put("company_id", entity.getCompanyId());
		out.put("title", entity.getTitle());
		out.put("summary", entity.getSummary());
		out.put("content", decodeContentField(entity.getContent()));
		out.put("sort", entity.getSort());
		out.put("image_url", entity.getImageUrl());
		out.put("share_image_url", entity.getShareImageUrl());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		out.put("author", entity.getAuthor());
		out.put("operator_id", entity.getOperatorId());
		out.put("release_status", entity.getReleaseStatus());
		out.put("release_time", entity.getReleaseTime());
		out.put("article_type", entity.getArticleType());
		out.put("distributor_id", entity.getDistributorId() != null ? entity.getDistributorId() : 0L);
		out.put("head_portrait", entity.getHeadPortrait());
		out.put("province", entity.getProvince());
		out.put("city", entity.getCity());
		out.put("area", entity.getArea());
		out.put("regions", regionsJsonForResponse(entity.getRegions()));
		out.put("regions_id", regionsJsonForResponse(entity.getRegionsId()));
		out.put("category_id", entity.getCategoryId());
		out.put("is_ai", entity.getIsAi() != null ? entity.getIsAi() : false);
		return out;
	}

	private Object decodeContentField(String raw) {
		if (raw == null || raw.isEmpty()) {
			return raw;
		}
		try {
			JsonNode node = objectMapper.readTree(raw);
			return objectMapper.convertValue(node, Object.class);
		} catch (JsonProcessingException e) {
			return raw;
		}
	}

	private Object decodeJsonMaybe(String raw) {
		if (raw == null || raw.isEmpty()) {
			return raw;
		}
		try {
			JsonNode node = objectMapper.readTree(raw);
			return objectMapper.convertValue(node, Object.class);
		} catch (JsonProcessingException e) {
			return raw;
		}
	}

	private Object regionsJsonForResponse(String raw) {
		Object decoded = decodeJsonMaybe(raw);
		if (decoded == null) {
			return List.of();
		}
		if (decoded instanceof String s && s.isEmpty()) {
			return List.of();
		}
		return decoded;
	}

	private static boolean isContentEmpty(Object content) {
		if (content == null) {
			return true;
		}
		if (content instanceof Boolean b) {
			return !b;
		}
		if (content instanceof Number n) {
			return n.doubleValue() == 0;
		}
		if (content instanceof CharSequence cs) {
			String s = cs.toString().trim();
			return s.isEmpty() || "0".equals(s);
		}
		if (content instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (content instanceof Map<?, ?> mp) {
			return mp.isEmpty();
		}
		if (content instanceof Object[] a) {
			return a.length == 0;
		}
		return false;
	}

	private static boolean isArrayFilterRemovesAll(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0;
		}
		if (v instanceof CharSequence cs) {
			String s = cs.toString();
			return s.isEmpty() || "0".equals(s);
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> mp) {
			return mp.isEmpty();
		}
		if (v instanceof Object[] a) {
			return a.length == 0;
		}
		return false;
	}

	private static boolean allValuesRemovedByArrayFilter(Map<String, Object> params) {
		for (Object v : params.values()) {
			if (!isArrayFilterRemovesAll(v)) {
				return false;
			}
		}
		return true;
	}

	private static void applyRegionsToParams(Map<String, Object> params, Object regionsRaw) {
		List<?> list = normalizeToList(regionsRaw);
		String[] keys = {"province", "city", "area"};
		for (int i = 0; i < keys.length && i < list.size(); i++) {
			params.put(keys[i], list.get(i));
		}
	}

	private static List<?> normalizeToList(Object regionsRaw) {
		if (regionsRaw == null) {
			return List.of();
		}
		if (regionsRaw instanceof List<?> l) {
			return l;
		}
		if (regionsRaw instanceof Collection<?> c) {
			return new ArrayList<>(c);
		}
		if (regionsRaw instanceof Object[] a) {
			return List.of(a);
		}
		return List.of();
	}

	private static void applyReleaseStatusRules(Map<String, Object> params) {
		Object v = params.get("release_status");
		if (isReleaseStatusFalseBranch(v)) {
			params.put("release_status", false);
			params.put("release_time", 0);
		} else {
			params.put("release_status", true);
			params.put("release_time", (int) (System.currentTimeMillis() / 1000L));
		}
	}

	private static boolean isReleaseStatusFalseBranch(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (v instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty()) {
				return true;
			}
			return "false".equals(s.toLowerCase(Locale.ROOT));
		}
		return false;
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString().trim());
	}

	private static long toLongStrict(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static Boolean toBooleanStrict(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.intValue() != 0;
		}
		return Boolean.parseBoolean(o.toString().trim());
	}

	private static long parseArticleIdStrictPositiveIntInRange(Object o) {
		long id;
		if (o instanceof Number n) {
			id = n.longValue();
		} else if (o instanceof CharSequence cs) {
			try {
				id = Long.parseLong(cs.toString().trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("文章编辑参数有误");
			}
		} else {
			throw new BadRequestException("文章编辑参数有误");
		}
		if (id <= 0L || id > (long) Integer.MAX_VALUE) {
			throw new BadRequestException("文章编辑参数有误");
		}
		return id;
	}

	private static int parseSortAsIntStrict(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o instanceof CharSequence cs) {
			try {
				return Integer.parseInt(cs.toString().trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("文章编辑参数有误");
			}
		}
		throw new BadRequestException("文章编辑参数有误");
	}
}
