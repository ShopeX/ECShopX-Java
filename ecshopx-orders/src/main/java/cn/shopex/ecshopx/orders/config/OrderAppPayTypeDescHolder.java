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

package cn.shopex.ecshopx.orders.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrderAppPayTypeDescHolder {

	private final ObjectMapper objectMapper;

	@Value("${ecshopx.orders.app-pay-type-desc-json:}")
	private String raw;

	private Map<String, String> map = Collections.emptyMap();
	private boolean valid;

	public OrderAppPayTypeDescHolder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	void init() {
		parse();
	}

	private void parse() {
		if (!StringUtils.hasText(raw)) {
			valid = false;
			map = Collections.emptyMap();
			return;
		}
		try {
			Map<String, String> m = objectMapper.readValue(raw, new TypeReference<Map<String, String>>() {});
			if (m == null || m.isEmpty()) {
				valid = false;
				map = Collections.emptyMap();
			} else {
				valid = true;
				map = m;
			}
		} catch (Exception e) {
			valid = false;
			map = Collections.emptyMap();
		}
	}

	public boolean hasValidMap() {
		return valid;
	}

	public String descForAppPayTypeOrNull(String appPayType) {
		if (!valid || !StringUtils.hasText(appPayType)) {
			return null;
		}
		return map.get(appPayType);
	}
}
