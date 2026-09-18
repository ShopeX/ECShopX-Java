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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WxaCartremindSettingService {

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public WxaCartremindSettingService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void setCartremindSetting(long companyId, boolean isOpen, String remindContent) {
		String content = remindContent == null ? "" : remindContent;
		if (isOpen) {
			int byteLen = content.getBytes(StandardCharsets.UTF_8).length;
			if (byteLen <= 0) {
				throw new BadRequestException("提醒内容必填", 400);
			}
			if (byteLen > 100) {
				if (content.codePoints().allMatch(cp -> cp <= 0x7F)) {
					throw new ResourceException("提醒内容必填");
				}
				throw new BadRequestException("提醒内容长度最大100字符", 400);
			}
		}
		String redisKey = "wxaCartremind:" + companyId;
		String field = String.valueOf(companyId);
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("is_open", Boolean.valueOf(isOpen));
		params.put("remind_content", content);
		String json;
		try {
			json = objectMapper.writeValueAsString(params);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("wxa cartremind setting JSON serialization failed", e);
		}
		sharedStringRedisTemplate.opsForHash().put(redisKey, field, json);
	}

	public Map<String, Object> getCartremindSetting(long companyId) {
		LinkedHashMap<String, Object> defaults = new LinkedHashMap<>();
		defaults.put("is_open", Boolean.FALSE);
		defaults.put("remind_content", "");
		String redisKey = "wxaCartremind:" + companyId;
		String field = String.valueOf(companyId);
		Object rawObj = sharedStringRedisTemplate.opsForHash().get(redisKey, field);
		String raw = rawObj instanceof String s ? s : rawObj == null ? null : String.valueOf(rawObj);
		if (raw == null || raw.trim().isEmpty()) {
			return new LinkedHashMap<>(defaults);
		}
		Map<String, Object> parsed;
		try {
			parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			return new LinkedHashMap<>(defaults);
		}
		if (parsed == null) {
			return new LinkedHashMap<>(defaults);
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(defaults);
		for (Map.Entry<String, Object> e : parsed.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			if ("is_open".equals(k)) {
				out.put(k, normalizeIsOpen(v));
			} else if ("remind_content".equals(k)) {
				out.put(k, normalizeRemindContent(v));
			} else {
				out.put(k, v);
			}
		}
		out.putIfAbsent("is_open", Boolean.FALSE);
		out.putIfAbsent("remind_content", "");
		return out;
	}

	private static Boolean normalizeIsOpen(Object v) {
		if (v == null) {
			return Boolean.FALSE;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		if (v instanceof String s) {
			String t = s.trim().toLowerCase();
			return "true".equals(t) || "1".equals(t) || "yes".equals(t);
		}
		return Boolean.FALSE;
	}

	private static String normalizeRemindContent(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v);
	}
}
