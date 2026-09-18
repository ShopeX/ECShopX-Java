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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderInvoiceProtocolService {

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public OrderInvoiceProtocolService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void setInvoiceProtocol(long companyId, Map<String, Object> data) {
		if (!data.containsKey("special_invoice_confirm_open")) {
			throw new ResourceException("专用发票确认书不能为空");
		}
		Object v = data.get("special_invoice_confirm_open");
		if (v == null) {
			throw new ResourceException("专用发票确认书不能为空");
		}
		if (!isValidSpecialInvoiceConfirmOpen(v)) {
			throw new ResourceException("专用发票确认书不能为空");
		}

		if (isUnsetOrEmpty(data.get("title"))) {
			throw new ResourceException("发票协议标题不能为空");
		}
		if (isUnsetOrEmpty(data.get("content"))) {
			throw new ResourceException("发票协议信息不能为空");
		}

		String json;
		try {
			json = objectMapper.copy()
					.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false)
					.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new ResourceException("保存失败");
		}

		sharedStringRedisTemplate.opsForValue().set(invoiceProtocolRedisKey(companyId), json);
	}

	/**
	 * Loads the stored invoice protocol for the company. Cached JSON may occasionally be stored as a
	 * single-key wrapper object {@code {"data": { ... protocol fields ... }}}; in that case the inner
	 * object is returned so callers receive the protocol field map directly. Root JSON arrays or
	 * non-object roots yield {@code null}. When nothing is cached or parsing fails, returns {@code null}.
	 */
	public Map<String, Object> getInvoiceProtocol(long companyId) {
		Object decoded = decodeRootFromRaw(readInvoiceProtocolRaw(companyId));
		if (decoded instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) m;
			return map;
		}
		return null;
	}

	/**
	 * Decodes cached JSON for H5: root objects (after optional single-key {@code data} unwrap) and root
	 * arrays are returned as map or list; missing, invalid, null root, or scalar root yields {@code null}.
	 */
	public Object getInvoiceProtocolForH5(long companyId) {
		return decodeRootFromRaw(readInvoiceProtocolRaw(companyId));
	}

	private String readInvoiceProtocolRaw(long companyId) {
		return sharedStringRedisTemplate.opsForValue().get(invoiceProtocolRedisKey(companyId));
	}

	private Object decodeRootFromRaw(String raw) {
		if (raw == null || raw.isEmpty()) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root.isNull()) {
				return null;
			}
			if (root.isArray()) {
				if (root.isEmpty()) {
					return List.of();
				}
				return objectMapper.convertValue(root, new TypeReference<List<Object>>() {});
			}
			if (root.isObject()) {
				Map<String, Object> m =
						objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
				return unwrapSingleDataEnvelope(m);
			}
			return null;
		} catch (JsonProcessingException | IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * If {@code map} has exactly one entry {@code "data"} whose value is a nested map, returns that
	 * inner map; otherwise returns {@code map} unchanged.
	 */
	private static Map<String, Object> unwrapSingleDataEnvelope(Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		if (map.size() == 1 && map.containsKey("data")) {
			Object inner = map.get("data");
			if (inner instanceof Map<?, ?> nested) {
				@SuppressWarnings("unchecked")
				Map<String, Object> asStringObjectMap = (Map<String, Object>) nested;
				return asStringObjectMap;
			}
		}
		return map;
	}

	private static String invoiceProtocolRedisKey(long companyId) {
		return "invoice_protocol_" + companyId;
	}

	private static boolean isValidSpecialInvoiceConfirmOpen(Object v) {
		if (v instanceof Boolean) {
			return true;
		}
		if (v instanceof Number n) {
			int i = n.intValue();
			return i == 0 || i == 1;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return "0".equals(t) || "1".equals(t);
		}
		return false;
	}

	private static boolean isUnsetOrEmpty(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (v instanceof CharSequence s) {
			return s.length() == 0 || "0".contentEquals(s);
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}
}
