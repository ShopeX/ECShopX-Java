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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseFastBuyRedisService {

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public EmployeePurchaseFastBuyRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> setFastBuyCart(
			long companyId,
			long enterpriseId,
			long activityId,
			long userId,
			Map<String, Object> params) {
		String key =
				"employee_purchase_fastbuy:"
						+ sha1Hex(
								String.valueOf(companyId)
										+ enterpriseId
										+ activityId
										+ userId);
		if (params == null || params.isEmpty()) {
			stringRedisTemplate.opsForValue().set(key, "[]", Duration.ofSeconds(600));
			return new java.util.LinkedHashMap<>();
		}
		params.put("cart_id", 0L);
		String json;
		try {
			json = objectMapper.writeValueAsString(params);
		} catch (JsonProcessingException e) {
			throw new ResourceException("购物车数据序列化失败");
		}
		stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(600));
		return params;
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format(Locale.ROOT, "%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new ResourceException("系统环境不支持摘要算法，无法保存快购购物车");
		}
	}
}
