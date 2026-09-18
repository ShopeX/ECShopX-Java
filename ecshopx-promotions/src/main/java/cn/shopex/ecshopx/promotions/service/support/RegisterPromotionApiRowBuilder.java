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

package cn.shopex.ecshopx.promotions.service.support;

import cn.shopex.ecshopx.promotions.domain.RegisterPromotions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RegisterPromotionApiRowBuilder {

	private final ObjectMapper objectMapper;

	public RegisterPromotionApiRowBuilder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> toRowMap(RegisterPromotions row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("is_open", row.getIsOpen() != null ? row.getIsOpen() : "");
		m.put("register_type", row.getRegisterType() != null ? row.getRegisterType() : "");
		m.put("ad_title", row.getAdTitle() != null ? row.getAdTitle() : "");
		m.put("ad_pic", row.getAdPic() != null ? row.getAdPic() : "");
		m.put("promotions_value", parsePromotionsValueColumn(row.getPromotionsValue()));
		m.put("register_jump_path", parseJsonColumn(row.getRegisterJumpPath()));
		return m;
	}

	public Object parsePromotionsValueColumn(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return Collections.emptyList();
		}
		String trimmed = raw.trim();
		try {
			JsonNode node = objectMapper.readTree(trimmed);
			if (node.isObject()) {
				if (node.size() == 0) {
					return Collections.emptyList();
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> map = objectMapper.convertValue(node, Map.class);
				return map != null ? map : Collections.emptyList();
			}
			if (node.isArray()) {
				@SuppressWarnings("unchecked")
				List<Object> list = objectMapper.convertValue(node, List.class);
				return list != null ? list : Collections.emptyList();
			}
			if (node.isTextual()) {
				return node.asText();
			}
			if (node.isNull()) {
				return "";
			}
			if (node.isNumber() || node.isBoolean()) {
				return node.asText();
			}
			return "";
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public Object parseJsonColumn(String raw) {
		if (!StringUtils.hasText(raw)) {
			return Collections.emptyList();
		}
		try {
			JsonNode node = objectMapper.readTree(raw.trim());
			if (node.isObject()) {
				if (node.size() == 0) {
					return Collections.emptyList();
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> map = objectMapper.convertValue(node, Map.class);
				return map != null ? map : Collections.emptyList();
			}
			if (node.isArray()) {
				@SuppressWarnings("unchecked")
				List<Object> list = objectMapper.convertValue(node, List.class);
				return list != null ? list : Collections.emptyList();
			}
			return Collections.emptyList();
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}
}
