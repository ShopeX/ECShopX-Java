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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AftersalesReasonSaveService {

	private static final List<String> FRONT_DEFAULT_AFTER_SALES_REASONS = List.of(
			"物流破损",
			"产品描述与实物不符",
			"质量问题",
			"皮肤过敏");

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public AftersalesReasonSaveService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private String aftersalesReasonRedisKey(long companyId, String lang) {
		if ("zh".equals(lang)) {
			return "aftersalesreason_" + companyId;
		}
		return "aftersalesreason_" + lang + companyId;
	}

	public List<String> getSreasonList(long companyId, String lang, boolean isAdmin) {
		String key = aftersalesReasonRedisKey(companyId, lang);
		String raw = companysRedisTemplate.opsForValue().get(key);
		String t = raw == null ? "" : raw.trim();
		if (raw == null || t.isEmpty() || "null".equals(t) || "0".equals(t)) {
			if (isAdmin) {
				return Collections.emptyList();
			}
			return FRONT_DEFAULT_AFTER_SALES_REASONS;
		}
		try {
			JsonNode root = objectMapper.readTree(t);
			if (!root.isArray()) {
				return Collections.emptyList();
			}
			List<String> out = new ArrayList<>();
			for (JsonNode n : root) {
				if (!n.isTextual()) {
					return Collections.emptyList();
				}
				out.add(n.asText());
			}
			if (out.isEmpty() && !isAdmin) {
				return FRONT_DEFAULT_AFTER_SALES_REASONS;
			}
			return out;
		} catch (JsonProcessingException e) {
			return Collections.emptyList();
		}
	}

	public void Saveset(long companyId, Object reasonPayload, String lang) {
		String key = aftersalesReasonRedisKey(companyId, lang);

		String json;
		try {
			json = objectMapper.writeValueAsString(reasonPayload);
		} catch (JsonProcessingException e) {
			throw new ResourceException("保存失败");
		}

		try {
			Boolean ok = companysRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
				byte[] kb = key.getBytes(StandardCharsets.UTF_8);
				byte[] vb = json.getBytes(StandardCharsets.UTF_8);
				Boolean written = connection.set(kb, vb);
				if (Boolean.TRUE.equals(written)) {
					return Boolean.TRUE;
				}
				byte[] readBack = connection.get(kb);
				if (readBack != null && Arrays.equals(readBack, vb)) {
					return Boolean.TRUE;
				}
				return Boolean.FALSE;
			});
			if (!Boolean.TRUE.equals(ok)) {
				throw new ResourceException("保存失败");
			}
		} catch (DataAccessException e) {
			throw new ResourceException("保存失败");
		}
	}
}
