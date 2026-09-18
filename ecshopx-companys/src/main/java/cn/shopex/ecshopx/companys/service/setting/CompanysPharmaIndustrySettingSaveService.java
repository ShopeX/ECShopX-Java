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

package cn.shopex.ecshopx.companys.service.setting;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.domain.CompanyRelKuaizhen;
import cn.shopex.ecshopx.thirdparty.mapper.CompanyRelKuaizhenMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanysPharmaIndustrySettingSaveService {

	private static final String KEY_PREFIX = "PharmaIndustrySetting:";
	private static final String COUNT_RX_SQL =
			"SELECT COUNT(1) FROM items WHERE company_id = ? AND is_medicine = 1 AND is_prescription = 1 AND approve_status <> 'instock'";
	private static final String MSG_IS_PHARMA_REQUIRED = "是否为医药行业必填";
	private static final String MSG_KZ_FORMAT = "kuaizhen580 配置格式无效";
	private static final String MSG_KZ_EMPTY = "kuaizhen580 配置不能为空";
	private static final String MSG_CLOSE_BLOCKED = "关闭医药行业开关需要下架所有处方药商品";
	private static final String MSG_UPDATE_NO_ROW = "未查询到更新数据";

	private final JdbcTemplate jdbcTemplate;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final CompanyRelKuaizhenMapper companyRelKuaizhenMapper;
	private final boolean isDevMode;
	private final long pharmaIndustrySettingTtlSeconds;

	public CompanysPharmaIndustrySettingSaveService(
			JdbcTemplate jdbcTemplate,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			CompanyRelKuaizhenMapper companyRelKuaizhenMapper,
			@Value("${ecshopx.is-dev-mode:false}") boolean isDevMode,
			@Value("${ecshopx.companys.pharma-industry-setting-ttl-seconds:0}") long pharmaIndustrySettingTtlSeconds) {
		this.jdbcTemplate = jdbcTemplate;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.companyRelKuaizhenMapper = companyRelKuaizhenMapper;
		this.isDevMode = isDevMode;
		this.pharmaIndustrySettingTtlSeconds = pharmaIndustrySettingTtlSeconds;
	}

	public Map<String, Object> save(long companyId, Map<String, Object> input) {
		if (input == null) {
			input = Collections.emptyMap();
		}
		validateIsPharmaIndustryPresent(input);

		Object rawIsPharma = input.get("is_pharma_industry");
		String useThirdParty = normalizeUseThirdPartySystem(input);

		Map<String, Object> redisPayload = new LinkedHashMap<>(2);
		redisPayload.put("is_pharma_industry", rawIsPharma);
		redisPayload.put("use_third_party_system", useThirdParty);

		if (isPharmaIndustryOff(rawIsPharma)) {
			Long rx = jdbcTemplate.queryForObject(COUNT_RX_SQL, Long.class, companyId);
			long rxCount = rx == null ? 0L : rx;
			if (rxCount > 0L) {
				throw new ResourceException(MSG_CLOSE_BLOCKED);
			}
		}

		String key = KEY_PREFIX + companyId;
		String json;
		try {
			json = objectMapper.writeValueAsString(redisPayload);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(MSG_KZ_FORMAT);
		}
		if (pharmaIndustrySettingTtlSeconds <= 0L) {
			companysRedisTemplate.opsForValue().set(key, json);
		} else {
			companysRedisTemplate.opsForValue()
					.set(key, json, Duration.ofSeconds(pharmaIndustrySettingTtlSeconds));
		}

		Map<String, Object> out = new LinkedHashMap<>(3);
		out.put("is_pharma_industry", redisPayload.get("is_pharma_industry"));
		out.put("use_third_party_system", redisPayload.get("use_third_party_system"));

		if (shouldUpsertKuaizhen580(useThirdParty, input)) {
			Map<String, Object> snake = upsertKuaizhen580(companyId, input);
			out.put("kuaizhen580", snake);
		}
		return out;
	}

	private static void validateIsPharmaIndustryPresent(Map<String, Object> input) {
		if (!input.containsKey("is_pharma_industry")) {
			throw new BadRequestException(MSG_IS_PHARMA_REQUIRED);
		}
		Object v = input.get("is_pharma_industry");
		if (v == null) {
			throw new BadRequestException(MSG_IS_PHARMA_REQUIRED);
		}
		if (v instanceof CharSequence cs && cs.toString().trim().isEmpty()) {
			throw new BadRequestException(MSG_IS_PHARMA_REQUIRED);
		}
		if (v instanceof Collection<?> c && c.isEmpty()) {
			throw new BadRequestException(MSG_IS_PHARMA_REQUIRED);
		}
		if (v instanceof Map<?, ?> m && m.isEmpty()) {
			throw new BadRequestException(MSG_IS_PHARMA_REQUIRED);
		}
	}

	private static String normalizeUseThirdPartySystem(Map<String, Object> input) {
		Object u = input.get("use_third_party_system");
		if (u == null) {
			return "";
		}
		if (u instanceof String s) {
			String t = s.trim();
			return StringUtils.hasText(t) ? t : "";
		}
		String s = String.valueOf(u).trim();
		return StringUtils.hasText(s) ? s : "";
	}

	private static boolean isPharmaIndustryOff(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if (t.isEmpty() || "0".equals(t)) {
				return true;
			}
			if ("false".equalsIgnoreCase(t)) {
				return true;
			}
			if ("1".equals(t) || "true".equalsIgnoreCase(t)) {
				return false;
			}
			try {
				return Long.parseLong(t) == 0L;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	private boolean shouldUpsertKuaizhen580(String useThirdParty, Map<String, Object> input) {
		if (!"kuaizhen580".equals(useThirdParty)) {
			return false;
		}
		Object kc = input.get("kuaizhen580_config");
		if (kc == null) {
			return false;
		}
		if (kc instanceof String s) {
			return StringUtils.hasText(s.trim());
		}
		if (kc instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (kc instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		return true;
	}

	private Map<String, Object> upsertKuaizhen580(long companyId, Map<String, Object> input) {
		Map<String, Object> cfg = parseKuaizhen580Config(input.get("kuaizhen580_config"));

		String clientId = requiredTrimmed(cfg, "clientId", "请填写580clientId");
		String clientSecret = requiredTrimmed(cfg, "clientSecret", "请填写580clientSecret");
		String storeRaw = requiredTrimmed(cfg, "storeId", "请填写580门店Id");
		long storeId;
		try {
			storeId = Long.parseLong(storeRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("请填写580门店Id");
		}

		Boolean online = isDevMode ? Boolean.FALSE : Boolean.TRUE;
		int now = (int) Instant.now().getEpochSecond();

		LambdaQueryWrapper<CompanyRelKuaizhen> q = new LambdaQueryWrapper<CompanyRelKuaizhen>()
				.eq(CompanyRelKuaizhen::getCompanyId, companyId)
				.last("LIMIT 1");
		CompanyRelKuaizhen existing = companyRelKuaizhenMapper.selectOne(q);
		if (existing != null) {
			existing.setClientId(clientId);
			existing.setClientSecret(clientSecret);
			existing.setKuaizhenStoreId(storeId);
			existing.setOnline(online);
			existing.setUpdated(now);
			int rows = companyRelKuaizhenMapper.updateById(existing);
			if (rows != 1) {
				throw new ResourceException(MSG_UPDATE_NO_ROW);
			}
			return toSnakeKuaizhenMap(existing);
		}
		CompanyRelKuaizhen entity = new CompanyRelKuaizhen();
		entity.setCompanyId(companyId);
		entity.setClientId(clientId);
		entity.setClientSecret(clientSecret);
		entity.setKuaizhenStoreId(storeId);
		entity.setOnline(online);
		entity.setIsOpen(Boolean.TRUE);
		entity.setCreated(now);
		entity.setUpdated(now);
		companyRelKuaizhenMapper.insert(entity);
		return toSnakeKuaizhenMap(entity);
	}

	private Map<String, Object> parseKuaizhen580Config(Object rawConfig) {
		if (rawConfig instanceof Map<?, ?> m) {
			return copyStringKeyMap(m);
		}
		if (rawConfig instanceof String s) {
			String trimmed = s.trim();
			if (!StringUtils.hasText(trimmed)) {
				throw new BadRequestException(MSG_KZ_EMPTY);
			}
			try {
				JsonNode node = objectMapper.readTree(trimmed);
				if (!node.isObject()) {
					throw new BadRequestException(MSG_KZ_FORMAT);
				}
				Map<String, Object> conv =
						objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
				if (conv == null) {
					throw new BadRequestException(MSG_KZ_FORMAT);
				}
				return conv;
			} catch (JsonProcessingException e) {
				throw new BadRequestException(MSG_KZ_FORMAT);
			}
		}
		if (rawConfig instanceof Collection<?>
				|| (rawConfig != null && rawConfig.getClass().isArray())) {
			throw new BadRequestException(MSG_KZ_FORMAT);
		}
		throw new BadRequestException(MSG_KZ_FORMAT);
	}

	private static Map<String, Object> copyStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			if (e.getKey() == null) {
				continue;
			}
			out.put(e.getKey().toString(), e.getValue());
		}
		return out;
	}

	private static String requiredTrimmed(Map<String, Object> cfg, String key, String emptyMessage) {
		if (!cfg.containsKey(key) || cfg.get(key) == null) {
			throw new BadRequestException(emptyMessage);
		}
		String t = cfg.get(key).toString().trim();
		if (!StringUtils.hasText(t)) {
			throw new BadRequestException(emptyMessage);
		}
		return t;
	}

	private static Map<String, Object> toSnakeKuaizhenMap(CompanyRelKuaizhen e) {
		Map<String, Object> snake = new LinkedHashMap<>();
		snake.put("id", e.getId());
		snake.put("company_id", e.getCompanyId());
		snake.put("client_id", e.getClientId());
		snake.put("client_secret", e.getClientSecret());
		snake.put("online", boolToTinyInt(e.getOnline()));
		snake.put("is_open", boolToTinyInt(e.getIsOpen()));
		snake.put("kuaizhen_store_id", e.getKuaizhenStoreId());
		snake.put("created", e.getCreated());
		snake.put("updated", e.getUpdated());
		return snake;
	}

	private static int boolToTinyInt(Boolean b) {
		return Boolean.TRUE.equals(b) ? 1 : 0;
	}
}
