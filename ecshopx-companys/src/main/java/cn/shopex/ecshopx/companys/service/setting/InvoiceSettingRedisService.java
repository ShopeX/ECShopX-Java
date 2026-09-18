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

package cn.shopex.ecshopx.companys.service.setting;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Companys Redis payload for per-company invoice UI options. Redis key is the literal prefix
 * {@code InvoiceSetting:} plus the numeric company id; stored value is a JSON object string.
 * {@link #getInvoiceSetting} serves the admin GET invoice-setting endpoint and the POST read path
 * when the body does not
 * indicate a write for {@code invoice_status}; {@link #setInvoiceSetting} applies only on the
 * write branch. Redis or parse errors on read are logged and surfaced as an empty JSON array
 * ({@code []}) in the response payload.
 */
@Service
public class InvoiceSettingRedisService {

	private static final Logger log = LoggerFactory.getLogger(InvoiceSettingRedisService.class);

	private static final String KEY_PREFIX = "InvoiceSetting:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public InvoiceSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private String key(long companyId) {
		return KEY_PREFIX + companyId;
	}

	public void setInvoiceSetting(long companyId, Map<String, Object> inputdata) {
		if (inputdata == null) {
			throw new BadRequestException("companys 发票选项设置 inputdata 不能为空");
		}
		String redisKey = key(companyId);
		String json;
		try {
			json = objectMapper.writeValueAsString(inputdata);
		} catch (JsonProcessingException e) {
			throw new ResourceException("companys 发票选项设置 JSON 序列化失败");
		}
		log.info("InvoiceSetting redis key: {}", redisKey);
		log.info("InvoiceSetting input json: {}", json);
		try {
			companysRedisTemplate.opsForValue().set(redisKey, json);
		} catch (DataAccessException e) {
			throw new ResourceException("companys 发票选项设置 Redis 写入失败");
		}
	}

	public Object getInvoiceSetting(long companyId) {
		String raw;
		try {
			raw = companysRedisTemplate.opsForValue().get(key(companyId));
		} catch (Exception e) {
			log.warn("InvoiceSetting Redis read failed for companyId={}", companyId, e);
			return Collections.emptyList();
		}
		if (raw == null || raw.isBlank()) {
			return Collections.emptyList();
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (!root.isObject()) {
				return Collections.emptyList();
			}
			Map<String, Object> map =
					objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
			if (map.isEmpty()) {
				return Collections.emptyList();
			}
			return map;
		} catch (Exception e) {
			log.warn("InvoiceSetting read parse failed for companyId={}", companyId, e);
			return Collections.emptyList();
		}
	}
}
