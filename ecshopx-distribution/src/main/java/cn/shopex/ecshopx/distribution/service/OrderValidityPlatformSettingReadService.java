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

package cn.shopex.ecshopx.distribution.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderValidityPlatformSettingReadService {

	private static final String REDIS_KEY = "order_validity_setting";

	private static final Map<String, Object> DEFAULTS = new LinkedHashMap<>();

	static {
		DEFAULTS.put("order_finish_time", 7);
		DEFAULTS.put("order_cancel_time", 15);
		DEFAULTS.put("notpay_order_wxapp_notice", 0);
		DEFAULTS.put("latest_aftersale_time", 0);
		DEFAULTS.put("aftersale_close_time", 7);
		DEFAULTS.put("auto_refuse_time", 0);
		DEFAULTS.put("auto_aftersales", false);
		DEFAULTS.put("offline_aftersales", false);
		DEFAULTS.put("is_refund_freight", 0);
	}

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public OrderValidityPlatformSettingReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> readOrderValidityPlatformSetting(long companyId) {
		String raw = companysRedisTemplate.<String, String>opsForHash().get(REDIS_KEY, String.valueOf(companyId));
		Map<String, Object> result = new LinkedHashMap<>(DEFAULTS);
		if (StringUtils.hasText(raw)) {
			try {
				Map<String, Object> parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				if (parsed != null) {
					result.putAll(parsed);
				}
			} catch (Exception ignored) {
				// keep defaults merged
			}
		}
		for (Map.Entry<String, Object> e : DEFAULTS.entrySet()) {
			result.putIfAbsent(e.getKey(), e.getValue());
		}
		return result;
	}
}
