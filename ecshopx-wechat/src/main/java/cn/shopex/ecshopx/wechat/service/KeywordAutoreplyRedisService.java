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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KeywordAutoreplyRedisService {

	private static final String REDIS_KEY_PREFIX = "autoreply:";
	private static final String SHA1_INPUT_SUFFIX = "keyword_autoreply_info";

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public KeywordAutoreplyRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void addKeywordReply(
			String authorizerAppId,
			String ruleNameParam,
			Object keywordsRuleRaw,
			Object replyTypeObj,
			Object replyContentObj) {
		List<Map<String, Object>> keywordRulesList = parseKeywordsRule(keywordsRuleRaw);

		String redisKey = buildRedisKey(authorizerAppId);

		if (ruleNameLooksPresent(ruleNameParam)
				&& Boolean.TRUE.equals(sharedStringRedisTemplate.opsForHash().hasKey(redisKey, ruleNameParam))) {
			throw new ResourceException("当前规则已存在，请换一个规则名称");
		}

		LinkedHashMap<String, Object> rules = new LinkedHashMap<>();
		rules.put("rule_name", ruleNameParam);
		rules.put("keywords_rule", new ArrayList<>(keywordRulesList));
		rules.put("reply_type", replyTypeObj == null ? "" : String.valueOf(replyTypeObj));
		rules.put("reply_content", replyContentObj == null ? "" : String.valueOf(replyContentObj));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mutableKeywordList = (List<Map<String, Object>>) rules.get("keywords_rule");

		validateAndFilterKeywordRules(ruleNameParam, rules, mutableKeywordList);

		String json;
		try {
			json = objectMapper.writeValueAsString(rules);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("keyword autoreply payload serialization failed", e);
		}
		sharedStringRedisTemplate.opsForHash().put(redisKey, ruleNameParam, json);
	}

	public void updateKeywordReply(
			String authorizerAppId,
			String ruleNameParam,
			Object keywordsRuleRaw,
			Object replyTypeObj,
			Object replyContentObj) {
		String redisKey = buildRedisKey(authorizerAppId);
		if (ruleNameParam == null
				|| !Boolean.TRUE.equals(sharedStringRedisTemplate.opsForHash().hasKey(redisKey, ruleNameParam))) {
			throw new ResourceException("当前更新的规则不存在");
		}

		List<Map<String, Object>> keywordRulesList = parseKeywordsRule(keywordsRuleRaw);

		LinkedHashMap<String, Object> rules = new LinkedHashMap<>();
		rules.put("rule_name", ruleNameParam);
		rules.put("keywords_rule", new ArrayList<>(keywordRulesList));
		rules.put("reply_type", replyTypeObj == null ? "" : String.valueOf(replyTypeObj));
		rules.put("reply_content", replyContentObj == null ? "" : String.valueOf(replyContentObj));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> mutableKeywordList = (List<Map<String, Object>>) rules.get("keywords_rule");

		validateAndFilterKeywordRules(ruleNameParam, rules, mutableKeywordList);

		String json;
		try {
			json = objectMapper.writeValueAsString(rules);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("keyword autoreply payload serialization failed", e);
		}
		sharedStringRedisTemplate.opsForHash().put(redisKey, ruleNameParam, json);
	}

	public void deleteKeywordReply(String authorizerAppId, String ruleName) {
		String redisKey = buildRedisKey(authorizerAppId);
		String field = (ruleName == null) ? "" : ruleName;
		sharedStringRedisTemplate.opsForHash().delete(redisKey, field);
	}

	public List<Map<String, Object>> listKeywordAutoreplyRules(String authorizerAppId) {
		String redisKey = buildRedisKey(authorizerAppId);
		Map<Object, Object> entries = sharedStringRedisTemplate.opsForHash().entries(redisKey);
		if (entries == null || entries.isEmpty()) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map.Entry<Object, Object> e : entries.entrySet()) {
			String fieldName = e.getKey() == null ? "" : String.valueOf(e.getKey()).trim();
			Object value = e.getValue();
			String json = value == null ? "" : String.valueOf(value).trim();
			if (!StringUtils.hasText(json)) {
				continue;
			}
			LinkedHashMap<String, Object> decoded;
			try {
				decoded = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
			} catch (JsonProcessingException ex) {
				continue;
			}
			if (decoded == null) {
				continue;
			}
			String jsonRuleName =
					decoded.get("rule_name") == null ? "" : String.valueOf(decoded.get("rule_name")).trim();
			String ruleName = StringUtils.hasText(fieldName) ? fieldName : jsonRuleName;

			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("rule_name", ruleName);
			row.put("keywords_rule", shallowCopyKeywordsRule(decoded.get("keywords_rule")));
			row.put("reply_type", decoded.get("reply_type"));
			row.put("reply_content", decoded.get("reply_content"));
			row.put("isopen", Boolean.FALSE);
			row.put("is_new", Boolean.FALSE);
			result.add(row);
		}
		return result;
	}

	private static List<Map<String, Object>> shallowCopyKeywordsRule(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> m) {
				LinkedHashMap<String, Object> converted = new LinkedHashMap<>();
				for (Map.Entry<?, ?> en : m.entrySet()) {
					converted.put(String.valueOf(en.getKey()), en.getValue());
				}
				out.add(converted);
			}
		}
		return out;
	}

	private String buildRedisKey(String authorizerAppId) {
		String salt = authorizerAppId == null ? "" : authorizerAppId;
		return REDIS_KEY_PREFIX + sha1HexUtf8(salt + SHA1_INPUT_SUFFIX);
	}

	private List<Map<String, Object>> parseKeywordsRule(Object keywordsRuleRaw) {
		if (keywordsRuleRaw == null) {
			throw new ResourceException("请填写必填参数");
		}
		if (keywordsRuleRaw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new ResourceException("请填写必填参数");
			}
			try {
				List<Map<String, Object>> parsed =
						objectMapper.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
				if (parsed == null) {
					throw new ResourceException("请填写必填参数");
				}
				return parsed;
			} catch (JsonProcessingException e) {
				throw new ResourceException("请填写必填参数");
			}
		}
		if (keywordsRuleRaw instanceof List<?> rawList) {
			List<Map<String, Object>> result = new ArrayList<>();
			for (Object item : rawList) {
				if (item instanceof Map<?, ?> m) {
					Map<String, Object> converted = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						converted.put(String.valueOf(e.getKey()), e.getValue());
					}
					result.add(converted);
				} else {
					throw new ResourceException("请填写必填参数");
				}
			}
			return result;
		}
		throw new ResourceException("请填写必填参数");
	}

	private void validateAndFilterKeywordRules(
			String ruleNameParam,
			LinkedHashMap<String, Object> rules,
			List<Map<String, Object>> keywordRulesList) {
		if (keywordRulesList == null || keywordRulesList.isEmpty()) {
			throw new ResourceException("请填写必填参数");
		}
		if (ruleNameParam == null || ruleNameParam.isEmpty() || "0".equals(ruleNameParam)) {
			throw new ResourceException("请填写必填参数");
		}
		String replyType = (String) rules.get("reply_type");
		if (!List.of("text", "image", "news", "card").contains(replyType)) {
			throw new ResourceException("请填写必填参数");
		}
		String rc = (String) rules.get("reply_content");
		if (rc == null || rc.isEmpty() || "0".equals(rc)) {
			throw new ResourceException("请填写必填参数");
		}

		boolean isKeyword = false;
		Iterator<Map<String, Object>> it = keywordRulesList.iterator();
		while (it.hasNext()) {
			Map<String, Object> rule = it.next();
			Object kw = rule.get("keyword");
			if (keywordLooksPresent(kw)) {
				Object mode = rule.get("reply_mode");
				String modeStr = mode == null ? "" : String.valueOf(mode);
				if (!List.of("equal", "contain").contains(modeStr)) {
					throw new ResourceException("关键字匹配模式只支持equal或者contain");
				}
				isKeyword = true;
			} else {
				it.remove();
			}
		}

		if (!isKeyword) {
			throw new ResourceException("请填写关键字");
		}

		rules.put("keywords_rule", keywordRulesList);
	}

	private static boolean ruleNameLooksPresent(String ruleName) {
		return ruleName != null && !ruleName.isEmpty() && !"0".equals(ruleName);
	}

	private static boolean keywordLooksPresent(Object kw) {
		if (kw == null) {
			return false;
		}
		if (kw instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		if (kw instanceof Integer i) {
			return i != 0;
		}
		if (kw instanceof Short s) {
			return s != 0;
		}
		if (kw instanceof Byte b) {
			return b != 0;
		}
		if (kw instanceof Long l) {
			return l != 0L;
		}
		if (kw instanceof Double d) {
			return d.doubleValue() != 0.0;
		}
		if (kw instanceof Float f) {
			return f.floatValue() != 0.0f;
		}
		if (kw instanceof BigDecimal bd) {
			return bd.signum() != 0;
		}
		if (kw instanceof BigInteger bi) {
			return bi.signum() != 0;
		}
		if (kw instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		if (kw instanceof String s) {
			return s != null && !s.isEmpty() && !"0".equals(s);
		}
		String str = String.valueOf(kw);
		return str != null && !str.isEmpty() && !"0".equals(str);
	}

	private static String sha1HexUtf8(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
