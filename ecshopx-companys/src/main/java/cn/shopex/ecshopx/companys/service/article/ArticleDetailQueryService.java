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
import cn.shopex.ecshopx.companys.domain.Article;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.ArticleMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.companys.service.employee.OperatorAccountOutsideLangReadService;
import cn.shopex.ecshopx.wechat.service.WechatOfficialAccountPermanentMaterialClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleDetailQueryService {

	private static final Logger log = LoggerFactory.getLogger(ArticleDetailQueryService.class);

	private final ArticleMapper articleMapper;
	private final CommonLangModReadService commonLangModReadService;
	private final OperatorsMapper operatorsMapper;
	private final OperatorAccountOutsideLangReadService operatorAccountOutsideLangReadService;
	private final ObjectMapper objectMapper;
	private final WechatOfficialAccountPermanentMaterialClient wechatOfficialAccountPermanentMaterialClient;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final StringRedisTemplate companysRedisTemplate;

	public ArticleDetailQueryService(
			ArticleMapper articleMapper,
			CommonLangModReadService commonLangModReadService,
			OperatorsMapper operatorsMapper,
			OperatorAccountOutsideLangReadService operatorAccountOutsideLangReadService,
			ObjectMapper objectMapper,
			WechatOfficialAccountPermanentMaterialClient wechatOfficialAccountPermanentMaterialClient,
			NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.articleMapper = articleMapper;
		this.commonLangModReadService = commonLangModReadService;
		this.operatorsMapper = operatorsMapper;
		this.operatorAccountOutsideLangReadService = operatorAccountOutsideLangReadService;
		this.objectMapper = objectMapper;
		this.wechatOfficialAccountPermanentMaterialClient = wechatOfficialAccountPermanentMaterialClient;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	private static Optional<Long> tryParseArticleIdForQuery(String raw) {
		String p = raw == null ? "" : raw.trim();
		if (!StringUtils.hasText(p)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(p));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public Optional<Map<String, Object>> infoDataArticle(
			long companyId, String authorizerAppid, String articleIdPath, String requestLang) {
		return infoDataArticle(companyId, authorizerAppid, articleIdPath, requestLang, 0L);
	}

	public Optional<Map<String, Object>> infoDataArticle(
			long companyId,
			String authorizerAppid,
			String articleIdPath,
			String requestLang,
			long userId) {
		Optional<Long> idOpt = tryParseArticleIdForQuery(articleIdPath);
		if (idOpt.isEmpty()) {
			return Optional.empty();
		}
		long articleId = idOpt.get();
		Article row =
				articleMapper.selectOne(
						new LambdaQueryWrapper<Article>()
								.eq(Article::getCompanyId, companyId)
								.eq(Article::getArticleId, articleId));
		if (row == null) {
			return Optional.empty();
		}
		Map<String, Object> result = toDetailRowMap(row);
		List<Map<String, Object>> one = List.of(result);
		commonLangModReadService.enrichArticleRowsWithListLang(one, requestLang);

		Long opId = row.getOperatorId();
		if (opId != null && opId > 0L) {
			Operators op =
					operatorsMapper.selectOne(
							new LambdaQueryWrapper<Operators>()
									.eq(Operators::getCompanyId, companyId)
									.eq(Operators::getOperatorId, opId));
			if (op != null) {
				Map<String, Object> opRow = new LinkedHashMap<>();
				opRow.put("operator_id", opId);
				opRow.put("username", op.getUsername() != null ? op.getUsername() : "");
				List<Map<String, Object>> opRows = new ArrayList<>();
				opRows.add(opRow);
				operatorAccountOutsideLangReadService.applyForAccountList(
						companyId, requestLang, opRows);
				Object un = opRows.get(0).get("username");
				String displayUsername =
						un == null ? "" : (un instanceof String s ? s : String.valueOf(un));
				Object curAuthor = result.get("author");
				if (curAuthor == null
						|| (curAuthor instanceof String s && !StringUtils.hasText(s))) {
					result.put("author", displayUsername);
				}
				Object curHp = result.get("head_portrait");
				if (curHp == null || (curHp instanceof String sh && !StringUtils.hasText(sh))) {
					result.put(
							"head_portrait",
							op.getHeadPortrait() != null && StringUtils.hasText(op.getHeadPortrait())
									? op.getHeadPortrait()
									: Integer.valueOf(0));
				}
			}
		}

		Object at = result.get("article_type");
		if (!"bring".equals(at == null ? null : String.valueOf(at))) {
			return Optional.of(result);
		}

		Object content = result.get("content");
		Object processed =
				processBringArticleContent(
						companyId,
						articleId,
						content,
						authorizerAppid == null ? "" : authorizerAppid,
						userId);
		result.put("content", processed);

		long cid = companyId;
		long aid = articleId;
		Map<String, Object> focus = new LinkedHashMap<>();
		focus.put("count", redisCountLikeList("articleFocus:" + cid, aid));
		result.put("articleFocusNum", focus);
		Map<String, Object> praise = new LinkedHashMap<>();
		praise.put("count", redisCountLikeList("articlePraise:" + cid, aid));
		result.put("articlePraiseNum", praise);
		if (userId == 0L) {
			result.put("isPraise", Boolean.FALSE);
		} else {
			String praiseUserKey = "articlePraiseUser:" + cid + ":" + aid;
			Object raw =
					companysRedisTemplate.opsForHash().get(praiseUserKey, String.valueOf(userId));
			boolean isPraise = raw != null && !String.valueOf(raw).trim().isEmpty();
			result.put("isPraise", isPraise);
		}
		return Optional.of(result);
	}

	/**
	 * 会员心愿单详情：主表行 + 列表多语言，不含运营账号补全、带货正文、Redis 点赞。
	 */
	public Optional<Map<String, Object>> findArticleDetailRowForMemberFav(
			long companyId, long articleId, String requestLang) {
		Article row =
				articleMapper.selectOne(
						new LambdaQueryWrapper<Article>()
								.eq(Article::getCompanyId, companyId)
								.eq(Article::getArticleId, articleId));
		if (row == null) {
			return Optional.empty();
		}
		Map<String, Object> result = toDetailRowMap(row);
		List<Map<String, Object>> one = new ArrayList<>();
		one.add(result);
		commonLangModReadService.enrichArticleRowsWithListLang(
				one, requestLang == null ? "" : requestLang);
		return Optional.of(result);
	}

	public Map<String, Object> articlePraiseCheck(long companyId, String articleIdPath, long userId) {
		if (companyId <= 0L || userId <= 0L) {
			throw new BadRequestException("参数错误");
		}
		if (articleIdPath == null || articleIdPath.isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		String key = "articlePraiseUser:" + companyId + ":" + articleIdPath;
		Object raw = companysRedisTemplate.opsForHash().get(key, String.valueOf(userId));
		boolean status = raw != null && !String.valueOf(raw).trim().isEmpty();
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", status);
		return out;
	}

	private long redisCountLikeList(String hashKey, long articleId) {
		Object raw =
				companysRedisTemplate.opsForHash().get(hashKey, String.valueOf(articleId));
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

	private Map<String, Object> toDetailRowMap(Article entity) {
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

	private Object processBringArticleContent(
			long companyId,
			long articleId,
			Object content,
			String authorizerAppid,
			long userId) {
		if (!(content instanceof List<?> rawList)) {
			return content;
		}
		List<Object> blocks = new ArrayList<>(rawList);
		Set<Long> goodsIds = new LinkedHashSet<>();

		for (int i = 0; i < blocks.size(); i++) {
			Object el = blocks.get(i);
			if (!(el instanceof Map<?, ?> rawMap)) {
				continue;
			}
			Map<String, Object> value = stringKeyMap(rawMap);

			Object baseObj = value.get("base");
			if (baseObj instanceof Map<?, ?> baseRaw) {
				Map<String, Object> base = stringKeyMap(baseRaw);
				if (base.containsKey("padded")) {
					base.put("padded", normalizeConfigBool(base.get("padded")));
					value.put("base", base);
				}
			}

			if ("goods".equals(String.valueOf(value.get("name")))) {
				Object goodsDataObj = value.get("data");
				if (goodsDataObj instanceof List<?> dataList && !dataList.isEmpty()) {
					for (Object rowObj : dataList) {
						if (!(rowObj instanceof Map<?, ?> rowRaw)) {
							continue;
						}
						Map<String, Object> itemRow = stringKeyMap(rowRaw);
						Long id = parseItemId(itemRow.get("item_id"));
						if (id != null) {
							goodsIds.add(id);
						}
					}
				}
			}

			if ("film".equals(String.valueOf(value.get("name")))) {
				Object filmDataObj = value.get("data");
				if (!(filmDataObj instanceof List<?> filmData)) {
					normalizeConfigBlock(value);
					blocks.set(i, value);
					continue;
				}
				List<Object> newFilmRows = new ArrayList<>();
				for (Object valObj : filmData) {
					if (!(valObj instanceof Map<?, ?> valRaw)) {
						newFilmRows.add(valObj);
						continue;
					}
					Map<String, Object> val = stringKeyMap(valRaw);
					Object mediaId = val.get("media_id");
					if (mediaId != null) {
						String url =
								wechatOfficialAccountPermanentMaterialClient
										.resolveMaterialDownloadUrl(authorizerAppid, String.valueOf(mediaId))
										.orElse("");
						if (StringUtils.hasText(url)) {
							val.put("url", url);
						}
					}
					newFilmRows.add(val);
				}
				value.put("data", newFilmRows);
			}

			normalizeConfigBlock(value);
			blocks.set(i, value);
		}

		Map<Long, Map<String, Object>> itemIdToRow =
				loadItemsBatchOrEmpty(companyId, articleId, goodsIds, userId);

		if (!itemIdToRow.isEmpty()) {
			for (int i = 0; i < blocks.size(); i++) {
				Object el = blocks.get(i);
				if (!(el instanceof Map<?, ?> rawMap)) {
					continue;
				}
				Map<String, Object> value = stringKeyMap(rawMap);
				if (!"goods".equals(String.valueOf(value.get("name")))) {
					continue;
				}
				if (!(value.get("data") instanceof List<?> dataList) || dataList.isEmpty()) {
					continue;
				}
				List<Object> newRows = new ArrayList<>();
				for (Object rowObj : dataList) {
					if (!(rowObj instanceof Map<?, ?> rowRaw)) {
						newRows.add(rowObj);
						continue;
					}
					Map<String, Object> item = stringKeyMap(rowRaw);
					Long itemId = parseItemId(item.get("item_id"));
					if (itemId == null || !itemIdToRow.containsKey(itemId)) {
						newRows.add(item);
						continue;
					}
					Map<String, Object> rowData = itemIdToRow.get(itemId);
					item.put("item_name", rowData.get("item_name"));
					item.put("img_url", rowData.getOrDefault("img_url", ""));
					item.put("price", rowData.get("price"));
					item.put("sales", rowData.get("sales"));
					item.put("favStatus", Boolean.TRUE.equals(rowData.get("favStatus")));
					item.put("itemStatus", Boolean.TRUE.equals(rowData.get("itemStatus")));
					newRows.add(item);
				}
				value.put("data", newRows);
				blocks.set(i, value);
			}
		}

		return blocks;
	}

	/**
	 * 批量解析会员在正文 goods 块商品上的收藏状态（members_items_fav）。
	 */
	private Set<Long> loadMemberItemFavIds(long companyId, long userId, Set<Long> goodsIds) {
		if (userId <= 0L || goodsIds.isEmpty()) {
			return Set.of();
		}
		String sql =
				"SELECT item_id FROM members_items_fav WHERE company_id = :companyId AND user_id = :userId AND item_id IN (:ids)";
		MapSqlParameterSource ps = new MapSqlParameterSource();
		ps.addValue("companyId", companyId);
		ps.addValue("userId", userId);
		ps.addValue("ids", new ArrayList<>(goodsIds));
		try {
			List<Long> rows =
					namedParameterJdbcTemplate.query(sql, ps, (rs, rowNum) -> rs.getLong("item_id"));
			return new LinkedHashSet<>(rows);
		} catch (DataAccessException ex) {
			log.warn("loadMemberItemFavIds skipped companyId={} userId={}", companyId, userId, ex);
			return Set.of();
		}
	}

	private Map<Long, Map<String, Object>> loadItemsBatchOrEmpty(
			long companyId, long articleId, Set<Long> goodsIds, long userId) {
		if (goodsIds.isEmpty()) {
			return Map.of();
		}
		Set<Long> favIds = loadMemberItemFavIds(companyId, userId, goodsIds);
		String sql =
				"SELECT item_id, item_name, store, brief, approve_status, price, pics, sales "
						+ "FROM items WHERE company_id = :companyId AND item_id IN (:ids)";
		MapSqlParameterSource ps = new MapSqlParameterSource();
		ps.addValue("companyId", companyId);
		ps.addValue("ids", new ArrayList<>(goodsIds));
		try {
			List<Map<String, Object>> rows =
					namedParameterJdbcTemplate.query(
							sql,
							ps,
							new RowMapper<Map<String, Object>>() {
								@Override
								public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
									Map<String, Object> m = new LinkedHashMap<>();
									long id = rs.getLong("item_id");
									m.put("item_id", id);
									m.put("item_name", rs.getString("item_name"));
									m.put("store", rs.getObject("store"));
									m.put("brief", rs.getString("brief"));
									String approve = rs.getString("approve_status");
									m.put("approve_status", approve);
									m.put("price", readScalarFlexible(rs, "price"));
									m.put("sales", readScalarFlexible(rs, "sales"));
									String picsRaw = rs.getString("pics");
									m.put("pics", parsePicsList(picsRaw));
									String imgUrl = firstPicUrl(m.get("pics"));
									m.put("img_url", imgUrl);
									m.put("itemStatus", approve != null && "onsale".equals(approve.trim()));
									m.put("favStatus", Boolean.FALSE);
									return m;
								}
							});
			Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
			for (Map<String, Object> r : rows) {
				Object idObj = r.get("item_id");
				if (idObj instanceof Number n) {
					long id = n.longValue();
					r.put("favStatus", userId > 0L && favIds.contains(id));
					byId.put(id, r);
				}
			}
			return byId;
		} catch (DataAccessException ex) {
			log.warn(
					"processBringArticleContent items batch skipped companyId={} articleId={}",
					companyId,
					articleId,
					ex);
			return Map.of();
		}
	}

	private static Object readScalarFlexible(ResultSet rs, String col) throws SQLException {
		Object o = rs.getObject(col);
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n;
		}
		if (o instanceof java.math.BigDecimal bd) {
			return bd.stripTrailingZeros();
		}
		return o;
	}

	private Object parsePicsList(String picsRaw) {
		if (picsRaw == null || picsRaw.isBlank()) {
			return List.of();
		}
		try {
			JsonNode n = objectMapper.readTree(picsRaw);
			if (n != null && n.isArray()) {
				return objectMapper.convertValue(n, Object.class);
			}
		} catch (JsonProcessingException ignored) {
		}
		return List.of();
	}

	private static String firstPicUrl(Object picsDecoded) {
		if (picsDecoded instanceof List<?> list && !list.isEmpty()) {
			Object first = list.get(0);
			return first == null ? "" : String.valueOf(first);
		}
		return "";
	}

	private void normalizeConfigBlock(Map<String, Object> value) {
		Object cfgObj = value.get("config");
		if (!(cfgObj instanceof Map<?, ?> cfgRaw)) {
			return;
		}
		Map<String, Object> cfg = stringKeyMap(cfgRaw);
		putBoolIfPresent(cfg, "content");
		putBoolIfPresent(cfg, "dot");
		putBoolIfPresent(cfg, "dotCover");
		putBoolIfPresent(cfg, "padded");
		putBoolIfPresent(cfg, "rounded");
		putBoolIfPresent(cfg, "bold");
		putBoolIfPresent(cfg, "italic");
		if (cfg.containsKey("height")) {
			cfg.put("height", intValueLoose(cfg.get("height")));
		}
		if (cfg.containsKey("interval")) {
			cfg.put("interval", intValueLoose(cfg.get("interval")));
		}
		value.put("config", cfg);
	}

	private static void putBoolIfPresent(Map<String, Object> cfg, String key) {
		if (cfg.containsKey(key)) {
			cfg.put(key, normalizeConfigBool(cfg.get(key)));
		}
	}

	private static boolean normalizeConfigBool(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "false".equalsIgnoreCase(t) || "0".equals(t)) {
				return false;
			}
			return true;
		}
		return true;
	}

	private static int intValueLoose(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Long parseItemId(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			return x > 0L ? x : null;
		}
		try {
			long x = Long.parseLong(String.valueOf(v).trim());
			return x > 0L ? x : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Map<String, Object> stringKeyMap(Map<?, ?> raw) {
		Map<String, Object> m = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : raw.entrySet()) {
			if (e.getKey() != null) {
				m.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return m;
	}
}
