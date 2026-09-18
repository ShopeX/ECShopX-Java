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

package cn.shopex.ecshopx.promotions.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

/**
 * Encode/decode {@code promotions_member_price.price} the same way as legacy PHP storage.
 */
public final class MemberPriceColumnCodec {

	private MemberPriceColumnCodec() {
	}

	public static String encodeForStorage(ObjectMapper objectMapper, ObjectNode value) throws JsonProcessingException {
		String inner = objectMapper.writeValueAsString(value);
		return objectMapper.writeValueAsString(inner);
	}

	public static JsonNode parseRoot(ObjectMapper objectMapper, String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(raw.trim());
			if (node.isTextual()) {
				return objectMapper.readTree(node.asText());
			}
			return node;
		} catch (Exception e) {
			return null;
		}
	}
}
