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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 读取推广配置中的返佣类型（commission_type），供积分规则等接口展示。
 */
@Service
public class PopularizeCommissionTypeReadService {

	private static final String REDIS_KEY_PREFIX = "popularizeConfig:";
	private static final Set<String> ALLOWED = Set.of("money", "point");

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public PopularizeCommissionTypeReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public String resolvePopularizeCommissionType(long companyId) {
		String raw = stringRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + companyId);
		if (!StringUtils.hasText(raw)) {
			return "money";
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (Exception e) {
			return "money";
		}
		if (root == null || !root.isObject()) {
			return "money";
		}
		JsonNode ct = root.get("commission_type");
		if (ct == null || !ct.isTextual()) {
			return "money";
		}
		String v = ct.asText();
		if (!StringUtils.hasText(v)) {
			return "money";
		}
		v = v.trim();
		if (ALLOWED.contains(v)) {
			return v;
		}
		return "money";
	}
}
