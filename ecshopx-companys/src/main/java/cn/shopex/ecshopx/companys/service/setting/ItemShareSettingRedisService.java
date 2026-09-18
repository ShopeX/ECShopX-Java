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

import cn.shopex.ecshopx.companys.domain.setting.ItemShareSettingView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ItemShareSettingRedisService {

	private static final String REDIS_KEY_PREFIX = "ItemShareSetting:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public ItemShareSettingRedisService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	private String key(long companyId) {
		return REDIS_KEY_PREFIX + companyId;
	}

	public ItemShareSettingView getEffective(long companyId) {
		ObjectNode merged = objectMapper.createObjectNode();
		merged.put("is_open", "false");
		merged.set("valid_grade", objectMapper.createArrayNode());
		merged.put("msg", "");
		merged.set("page", objectMapper.createArrayNode());

		String raw = companysRedisTemplate.opsForValue().get(key(companyId));
		if (raw != null && !raw.isBlank()) {
			try {
				JsonNode stored = objectMapper.readTree(raw);
				if (stored.isObject()) {
					for (Iterator<String> it = stored.fieldNames(); it.hasNext();) {
						String name = it.next();
						merged.set(name, stored.get(name));
					}
				}
			} catch (Exception ignored) {
				// keep defaults on parse failure
			}
		}

		ItemShareSettingView view = new ItemShareSettingView();
		JsonNode isOpenNode = merged.get("is_open");
		boolean openFlag = !"false".equals(textualOpenValue(isOpenNode));
		view.setOpen(openFlag);

		List<String> grades = new ArrayList<>();
		JsonNode vg = merged.get("valid_grade");
		if (vg != null && vg.isArray()) {
			for (JsonNode el : vg) {
				grades.add(String.valueOf(el.isNull() ? "" : el.asText()));
			}
		}
		view.setValidGrade(grades);

		JsonNode msgNode = merged.get("msg");
		view.setMsg(msgNode == null || msgNode.isNull() ? "" : msgNode.asText(""));

		JsonNode pageNode = merged.get("page");
		view.setPage(jsonNodeToJava(pageNode));

		return view;
	}

	private static String textualOpenValue(JsonNode isOpenNode) {
		if (isOpenNode == null || isOpenNode.isNull()) {
			return "false";
		}
		if (isOpenNode.isTextual()) {
			return isOpenNode.asText();
		}
		if (isOpenNode.isBoolean()) {
			return isOpenNode.booleanValue() ? "true" : "false";
		}
		return isOpenNode.asText();
	}

	private Object jsonNodeToJava(JsonNode node) {
		if (node == null || node.isNull()) {
			return Collections.emptyList();
		}
		try {
			return objectMapper.readValue(objectMapper.writeValueAsBytes(node), Object.class);
		} catch (Exception e) {
			return node.asText("");
		}
	}

	public void save(long companyId, Map<String, Object> rawBody) throws Exception {
		String json = objectMapper.writeValueAsString(rawBody);
		companysRedisTemplate.opsForValue().set(key(companyId), json);
	}
}
