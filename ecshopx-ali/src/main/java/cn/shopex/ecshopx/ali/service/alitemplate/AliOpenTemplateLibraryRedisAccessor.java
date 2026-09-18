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

package cn.shopex.ecshopx.ali.service.alitemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AliOpenTemplateLibraryRedisAccessor {

	private static final Logger log = LoggerFactory.getLogger(AliOpenTemplateLibraryRedisAccessor.class);

	private static final String KEY_PREFIX = "aliopen_template_library:";

	private final StringRedisTemplate redis;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public AliOpenTemplateLibraryRedisAccessor(@Qualifier("companysRedisTemplate") StringRedisTemplate redis) {
		this.redis = redis;
	}

	public Optional<Map<String, Object>> getTemplate(int companyId, String scenesName) {
		String key = KEY_PREFIX + companyId + ":" + scenesName;
		String raw = redis.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Optional.empty();
		}
		try {
			Map<String, Object> map = objectMapper.readValue(raw, new TypeReference<>() {});
			if (map == null || map.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(map);
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}

	/**
	 * Delay before an async job is eligible for dispatch when the listener path uses non-immediate scheduling and the
	 * stored template defines {@code send_time_desc.value} as minutes-to-wait.
	 */
	public Optional<Duration> resolveDispatchDelayMinutesFromTemplate(int companyId, String scenesName) {
		return getTemplate(companyId, scenesName).flatMap(AliOpenTemplateLibraryRedisAccessor::delayFromSendTimeDesc);
	}

	private static Optional<Duration> delayFromSendTimeDesc(Map<String, Object> templateMap) {
		Object std = templateMap.get("send_time_desc");
		if (std instanceof Map<?, ?> sm) {
			Object v = sm.get("value");
			if (v instanceof Number n) {
				int minutes = n.intValue();
				if (minutes > 0) {
					return Optional.of(Duration.ofSeconds(minutes * 60L));
				}
			}
		}
		return Optional.empty();
	}

	public void saveOpenTemplate(int companyId, String scenesName, String templateId, long sendTime) {
		String key = KEY_PREFIX + companyId + ":" + scenesName;
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("template_id", templateId);
		payload.put("send_time", sendTime);
		try {
			redis.opsForValue().set(key, objectMapper.writeValueAsString(payload));
		} catch (JsonProcessingException ex) {
			log.error("序列化支付宝通知模板 Redis 配置失败, companyId={}, scenesName={}", companyId, scenesName, ex);
			throw new IllegalStateException("无法将支付宝通知模板配置序列化为 JSON");
		}
	}

	public void deleteOpenTemplate(int companyId, String scenesName) {
		redis.delete(KEY_PREFIX + companyId + ":" + scenesName);
	}
}
