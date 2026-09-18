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

package cn.shopex.ecshopx.point.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 读取会员积分规则（默认配置与 Redis 合并）。
 */
@Service
public class PointMemberRuleReadService {

	private static final Logger log = LoggerFactory.getLogger(PointMemberRuleReadService.class);

	private static final String REDIS_RULE_PREFIX = "memeberpoint:rule:";
	private static final String REDIS_LANG_PREFIX = "memeberpoint:rule:lang:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public PointMemberRuleReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getPointRule(long companyId) {
		return getPointRule(companyId, "zh-CN");
	}

	public Map<String, Object> getPointRule(long companyId, String countryCode) {
		String lang = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";

		Map<String, Object> config = new LinkedHashMap<>();
		config.put("isOpenMemberPoint", false);
		config.put("gain_point", 1);
		config.put("gain_limit", 9999999);
		config.put("gain_time", 7);
		config.put("isOpenDeductPoint", false);
		config.put("deduct_proportion_limit", 100);
		config.put("deduct_point", 0);
		config.put("access", "order");
		config.put("rule_desc", "");
		config.put("point_pay_first", 0);
		config.put("can_deduct_freight", 1);
		config.put("name", "积分");

		String key = REDIS_RULE_PREFIX + companyId;
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (StringUtils.hasText(raw)) {
			try {
				Map<String, Object> fromRedis = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (fromRedis != null) {
					config.putAll(fromRedis);
				}
			} catch (Exception e) {
				log.warn("point member rule json parse failed companyId={}", companyId, e);
			}
		}
		String langKey = REDIS_LANG_PREFIX + companyId + "_" + lang.replace("-", "");
		String nameOverride = stringRedisTemplate.opsForValue().get(langKey);
		if (StringUtils.hasText(nameOverride)) {
			config.put("name", nameOverride);
		}
		return config;
	}

	public boolean getIsOpenPoint(long companyId) {
		Map<String, Object> rule = getPointRule(companyId);
		return redisFlagTrue(rule.get("isOpenMemberPoint")) && redisFlagTrue(rule.get("isOpenDeductPoint"));
	}

	private static boolean redisFlagTrue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		return "true".equals(String.valueOf(v));
	}
}
