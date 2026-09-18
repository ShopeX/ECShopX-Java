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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 当积分规则未同时开启获取与抵扣时，将推广配置中的积分返佣改为金额返佣。
 */
@Service
public class PopularizeConfigPointCommissionCloseService {

	private static final Logger log = LoggerFactory.getLogger(PopularizeConfigPointCommissionCloseService.class);

	private static final String REDIS_KEY_PREFIX = "popularizeConfig:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public PopularizeConfigPointCommissionCloseService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void closeIfCommissionTypeIsPoint(long companyId) {
		String key = REDIS_KEY_PREFIX + companyId;
		String json = stringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(json)) {
			return;
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(json);
		} catch (JsonProcessingException e) {
			log.warn(
					"popularizeConfig JSON parse failed, companyId={}, key={}, message={}",
					companyId,
					key,
					e.getMessage());
			return;
		}
		if (root == null || !root.isObject()) {
			log.warn("popularizeConfig root is not object, companyId={}, key={}", companyId, key);
			return;
		}
		ObjectNode obj = (ObjectNode) root;
		JsonNode ct = obj.get("commission_type");
		if (ct == null || !ct.isTextual()) {
			return;
		}
		if (!"point".equals(ct.asText())) {
			return;
		}
		obj.put("commission_type", "money");
		try {
			stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(obj));
		} catch (JsonProcessingException e) {
			log.warn(
					"popularizeConfig JSON write failed, companyId={}, key={}, message={}",
					companyId,
					key,
					e.getMessage());
		}
	}
}
