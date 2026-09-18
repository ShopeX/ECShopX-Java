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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopSalespersonBindUserService {

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;
	private final int promoterInfoExtimeSeconds;

	public ShopSalespersonBindUserService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper,
			@Value("${promoter.info.extime-setting:86400}") int promoterInfoExtimeSeconds) {
		this.redis = redis;
		this.objectMapper = objectMapper;
		this.promoterInfoExtimeSeconds = promoterInfoExtimeSeconds;
	}

	public Map<String, Object> bindusersalesperson(long memberUserId, Map<String, Object> requestDataThreeKeys) {
		validatePromoterUserId(requestDataThreeKeys.get("promoter_user_id"));

		String key = "promoter_user_info_dayset:" + memberUserId;
		String payload;
		try {
			payload = objectMapper.writeValueAsString(requestDataThreeKeys);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}

		redis.opsForValue().set(key, payload, Duration.ofSeconds(promoterInfoExtimeSeconds));

		String promoterinfo = redis.opsForValue().get(key);
		if (promoterinfo == null) {
			promoterinfo = "";
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("status", Integer.valueOf(1));
		out.put("code", Integer.valueOf(0));
		out.put("data", requestDataThreeKeys);
		out.put("key", key);
		out.put("extime", Integer.valueOf(promoterInfoExtimeSeconds));
		out.put("promoterinfo", promoterinfo);
		return out;
	}

	private static void validatePromoterUserId(Object v) {
		if (v == null) {
			throw new ResourceException("导购更新业务员信息错误");
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s) || "0".equals(s.trim())) {
				throw new ResourceException("导购更新业务员信息错误");
			}
			return;
		}
		if (v instanceof Number n) {
			if (n.doubleValue() == 0.0) {
				throw new ResourceException("导购更新业务员信息错误");
			}
			return;
		}
		if (v instanceof Boolean b && !b) {
			throw new ResourceException("导购更新业务员信息错误");
		}
	}
}
