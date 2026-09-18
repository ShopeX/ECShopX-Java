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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SubscribeReplyRedisService {

	private static final String REDIS_KEY_PREFIX = "subscribeReply:";
	private static final String SHA1_INPUT_SUFFIX = "message_subscribe_autoreply_info";

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public SubscribeReplyRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void setSubscribeReply(String authorizerAppId, Object replyType, Object replyContent) {
		String saltInput = authorizerAppId == null ? "" : authorizerAppId;
		String digest = sha1HexUtf8(saltInput + SHA1_INPUT_SUFFIX);
		String redisKey = REDIS_KEY_PREFIX + digest;
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("reply_type", replyType);
		payload.put("reply_content", replyContent);
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("subscribe reply payload serialization failed", e);
		}
		sharedStringRedisTemplate.opsForValue().set(redisKey, json);
	}

	public Map<String, Object> getSubscribeReplyContent(String authorizerAppId) {
		String saltInput = authorizerAppId == null ? "" : authorizerAppId;
		String digest = sha1HexUtf8(saltInput + SHA1_INPUT_SUFFIX);
		String redisKey = REDIS_KEY_PREFIX + digest;
		String raw = sharedStringRedisTemplate.opsForValue().get(redisKey);
		if (raw == null || raw.isEmpty()) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root == null || !root.isObject()) {
				return null;
			}
			return objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (Exception e) {
			return null;
		}
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
