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
import cn.shopex.ecshopx.companys.domain.Article;
import cn.shopex.ecshopx.companys.mapper.ArticleMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleCreateService {

	private static final String TABLE = "companys_article";
	private static final String MODULE = "companys_article";

	private static final List<String> WHITELIST_KEYS =
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

	private final LangueProperties langueProperties;
	private final ArticleMapper articleMapper;
	private final ObjectMapper objectMapper;
	private final CommonLangModWriteService commonLangModWriteService;

	public ArticleCreateService(
			LangueProperties langueProperties,
			ArticleMapper articleMapper,
			ObjectMapper objectMapper,
			CommonLangModWriteService commonLangModWriteService) {
		this.langueProperties = langueProperties;
		this.articleMapper = articleMapper;
		this.objectMapper = objectMapper;
		this.commonLangModWriteService = commonLangModWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(Map<String, Object> body, long companyId, long operatorId, String requestLang) {
		if (body == null) {
			body = new LinkedHashMap<>();
		}
		LinkedHashMap<String, Object> params = extractWhitelisted(body);

		if (!params.containsKey("content") || isContentEmpty(params.get("content"))) {
			throw new BadRequestException("文章内容不能为空");
		}
		if (allValuesRemovedByArrayFilter(params)) {
			throw new BadRequestException("文章编辑出错");
		}

		if (isMissingRequestScalar(body.get("category_id"))) {
			throw new BadRequestException("文章栏目必填");
		}
		if (isMissingRequestScalar(body.get("title"))) {
			throw new BadRequestException("文章标题必填");
		}
		Object contentForStep5 = body.containsKey("content") ? body.get("content") : params.get("content");
		if (isContentEmpty(contentForStep5)) {
			throw new BadRequestException("文章内容必填");
		}

		boolean isBring = "bring".equalsIgnoreCase(String.valueOf(params.get("article_type")));
		if (isBring) {
			Object c = body.containsKey("content") ? body.get("content") : params.get("content");
			if (isContentEmpty(c)) {
				throw new BadRequestException("文章内容必填");
			}
			if (params.containsKey("regions_id") && params.containsKey("regions")) {
				applyRegionsToParams(params, params.get("regions"));
			}
		}

		params.put("company_id", companyId);
		params.put("operator_id", operatorId);
		if (body.containsKey("distributor_id")) {
			Long id = toLong(body.get("distributor_id"));
			params.put("distributor_id", id != null ? id : 0L);
		} else {
			params.put("distributor_id", 0L);
		}

		applyReleaseStatusRules(params);

		Map<String, Object> dataForMain =
				langueProperties.isDefaultLang(requestLang) ? params : stripLangFields(params);

		Article entity = new Article();
		applySetColumnNamesData(entity, dataForMain);
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		if (entity.getCreated() == null) {
			entity.setCreated(nowSec);
		}
		if (entity.getUpdated() == null) {
			entity.setUpdated(nowSec);
		}

		int rows = articleMapper.insert(entity);
		if (rows != 1) {
			throw new ResourceException("文章保存失败");
		}

		Map<String, Object> langSource = buildLangSource(params, entity, requestLang);
		Map<String, String> langBag = buildLangBag(langSource);
		if (!langBag.isEmpty()) {
			commonLangModWriteService.saveLang(
					(int) companyId,
					langBag,
					TABLE,
					entity.getArticleId().intValue(),
					MODULE,
					requestLang);
		}

		return toColumnNamesData(entity);
	}

	private static LinkedHashMap<String, Object> extractWhitelisted(Map<String, Object> body) {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		for (String key : WHITELIST_KEYS) {
			if (body.containsKey(key)) {
				params.put(key, body.get(key));
			}
		}
		return params;
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
		if (content instanceof Map<?, ?> m) {
			return m.isEmpty();
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
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
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

	private static boolean isMissingRequestScalar(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof CharSequence cs) {
			return cs.toString().trim().isEmpty();
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return true;
		}
		if ("0".equals(s)) {
			return true;
		}
		try {
			return Long.parseLong(s) == 0L;
		} catch (NumberFormatException e) {
			return false;
		}
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
		if (!params.containsKey("release_status")) {
			return;
		}
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

	private static Map<String, Object> stripLangFields(Map<String, Object> params) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>(params);
		for (String k : LANG_STRIP_KEYS) {
			copy.remove(k);
		}
		return copy;
	}

	private Map<String, Object> buildLangSource(
			Map<String, Object> params, Article entity, String requestLang) {
		Map<String, Object> entityMap = entityToSnakeMap(entity);
		if (!langueProperties.isDefaultLang(requestLang)) {
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>(entityMap);
			for (Map.Entry<String, Object> e : params.entrySet()) {
				merged.put(e.getKey(), e.getValue());
			}
			return merged;
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(params);
		for (Map.Entry<String, Object> e : entityMap.entrySet()) {
			merged.putIfAbsent(e.getKey(), e.getValue());
		}
		return merged;
	}

	private Map<String, Object> entityToSnakeMap(Article e) {
		Map<String, Object> m = new LinkedHashMap<>();
		putIfNonNull(m, "article_id", e.getArticleId());
		putIfNonNull(m, "company_id", e.getCompanyId());
		putIfNonNull(m, "title", e.getTitle());
		putIfNonNull(m, "summary", e.getSummary());
		putIfNonNull(m, "content", e.getContent());
		putIfNonNull(m, "sort", e.getSort());
		putIfNonNull(m, "image_url", e.getImageUrl());
		putIfNonNull(m, "share_image_url", e.getShareImageUrl());
		putIfNonNull(m, "created", e.getCreated());
		putIfNonNull(m, "updated", e.getUpdated());
		putIfNonNull(m, "author", e.getAuthor());
		putIfNonNull(m, "operator_id", e.getOperatorId());
		putIfNonNull(m, "release_status", e.getReleaseStatus());
		putIfNonNull(m, "release_time", e.getReleaseTime());
		putIfNonNull(m, "article_type", e.getArticleType());
		putIfNonNull(m, "distributor_id", e.getDistributorId());
		putIfNonNull(m, "head_portrait", e.getHeadPortrait());
		putIfNonNull(m, "province", e.getProvince());
		putIfNonNull(m, "city", e.getCity());
		putIfNonNull(m, "area", e.getArea());
		putIfNonNull(m, "regions", e.getRegions());
		putIfNonNull(m, "regions_id", e.getRegionsId());
		putIfNonNull(m, "category_id", e.getCategoryId());
		putIfNonNull(m, "is_ai", e.getIsAi());
		return m;
	}

	private static void putIfNonNull(Map<String, Object> m, String k, Object v) {
		if (v != null) {
			m.put(k, v);
		}
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

	private void applySetColumnNamesData(Article e, Map<String, Object> data) {
		if (mapHas(data, "article_id") && ValuePresence.hasEffectiveValue(data.get("article_id"))) {
			e.setArticleId(toLong(data.get("article_id")));
		}
		if (mapHas(data, "company_id") && ValuePresence.hasEffectiveValue(data.get("company_id"))) {
			e.setCompanyId(toLong(data.get("company_id")));
		}
		if (mapHas(data, "title") && ValuePresence.hasEffectiveValue(data.get("title"))) {
			e.setTitle(String.valueOf(data.get("title")));
		}
		if (mapHas(data, "author")) {
			Object v = data.get("author");
			e.setAuthor(v == null ? null : String.valueOf(v));
		}
		if (mapHas(data, "summary")) {
			Object v = data.get("summary");
			e.setSummary(v == null ? null : String.valueOf(v));
		}
		if (mapHas(data, "content") && ValuePresence.hasEffectiveValue(data.get("content"))) {
			try {
				e.setContent(objectMapper.writeValueAsString(data.get("content")));
			} catch (JsonProcessingException ex) {
				throw new ResourceException("文章内容格式错误");
			}
		}
		if (mapHas(data, "image_url") && ValuePresence.hasEffectiveValue(data.get("image_url"))) {
			e.setImageUrl(String.valueOf(data.get("image_url")));
		}
		if (mapHas(data, "share_image_url") && ValuePresence.hasEffectiveValue(data.get("share_image_url"))) {
			e.setShareImageUrl(String.valueOf(data.get("share_image_url")));
		}
		if (mapHas(data, "created") && ValuePresence.hasEffectiveValue(data.get("created"))) {
			e.setCreated(toInt(data.get("created")));
		}
		if (mapHas(data, "updated") && ValuePresence.hasEffectiveValue(data.get("updated"))) {
			e.setUpdated(toInt(data.get("updated")));
		}
		if (mapHas(data, "operator_id") && ValuePresence.hasEffectiveValue(data.get("operator_id"))) {
			e.setOperatorId(toLong(data.get("operator_id")));
		}
		if (mapHas(data, "release_status")) {
			e.setReleaseStatus(toBooleanStrict(data.get("release_status")));
		}
		if (mapHas(data, "sort") && ValuePresence.hasEffectiveValue(data.get("sort"))) {
			e.setSort(toInt(data.get("sort")));
		}
		if (mapHas(data, "release_time") && ValuePresence.hasEffectiveValue(data.get("release_time"))) {
			e.setReleaseTime(toInt(data.get("release_time")));
		}
		if (mapHas(data, "article_type") && ValuePresence.hasEffectiveValue(data.get("article_type"))) {
			e.setArticleType(String.valueOf(data.get("article_type")));
		}
		if (mapHas(data, "head_portrait")) {
			Object v = data.get("head_portrait");
			e.setHeadPortrait(v == null ? null : String.valueOf(v));
		}
		if (mapHas(data, "category_id")) {
			e.setCategoryId(toLong(data.get("category_id")));
		}
		if (mapHas(data, "province") && ValuePresence.hasEffectiveValue(data.get("province"))) {
			e.setProvince(String.valueOf(data.get("province")));
		}
		if (mapHas(data, "city") && ValuePresence.hasEffectiveValue(data.get("city"))) {
			e.setCity(String.valueOf(data.get("city")));
		}
		if (mapHas(data, "area") && ValuePresence.hasEffectiveValue(data.get("area"))) {
			e.setArea(String.valueOf(data.get("area")));
		}
		if (mapHas(data, "distributor_id")) {
			e.setDistributorId(toLong(data.get("distributor_id")));
		}
		if (mapHas(data, "regions") && ValuePresence.hasEffectiveValue(data.get("regions"))) {
			try {
				e.setRegions(objectMapper.writeValueAsString(data.get("regions")));
			} catch (JsonProcessingException ex) {
				throw new ResourceException("地区数据格式错误");
			}
		}
		if (mapHas(data, "regions_id") && ValuePresence.hasEffectiveValue(data.get("regions_id"))) {
			try {
				e.setRegionsId(objectMapper.writeValueAsString(data.get("regions_id")));
			} catch (JsonProcessingException ex) {
				throw new ResourceException("地区编号格式错误");
			}
		}
		if (mapHas(data, "is_ai")) {
			e.setIsAi(toBooleanStrict(data.get("is_ai")));
		}
	}

	private static boolean mapHas(Map<String, Object> data, String key) {
		return data.containsKey(key);
	}

	private static Long toLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		return Long.parseLong(s);
	}

	private static Integer toInt(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString().trim());
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
			if (node.isObject() || node.isArray()) {
				return objectMapper.convertValue(node, Object.class);
			}
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
}
