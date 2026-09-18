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

package cn.shopex.ecshopx.companys.service.wxshops;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WxShopsJwtShopIdWhitelist {

	private final ObjectMapper objectMapper;

	public WxShopsJwtShopIdWhitelist(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<Long> allowedShopIds(Object shopIdsRawFromJwt) {
		return parseAllowedShopIds(shopIdsRawFromJwt);
	}

	private List<Long> parseAllowedShopIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Boolean) {
			return List.of();
		}
		if (raw instanceof Number n) {
			if (n.longValue() == 0L) {
				return List.of();
			}
			return List.of(n.longValue());
		}
		if (raw instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty() || "0".contentEquals(s)) {
				return List.of();
			}
			try {
				List<?> parsed = objectMapper.readValue(s, new TypeReference<List<?>>() {});
				if (parsed == null || parsed.isEmpty()) {
					return List.of();
				}
				return expandElementsToLongIds(parsed);
			} catch (JsonProcessingException e) {
				return List.of();
			}
		}
		if (raw instanceof Map<?, ?> map) {
			if (map.isEmpty()) {
				return List.of();
			}
			if (map.containsKey("shop_id")) {
				Long id = parseLongNullable(map.get("shop_id"));
				return id != null && id > 0L ? List.of(id) : List.of();
			}
			return expandElementsToLongIds(map.values());
		}
		if (raw instanceof Collection<?> c) {
			if (c.isEmpty()) {
				return List.of();
			}
			return expandElementsToLongIds(c);
		}
		if (raw instanceof Object[] arr) {
			if (arr.length == 0) {
				return List.of();
			}
			return expandElementsToLongIds(Arrays.asList(arr));
		}
		return List.of();
	}

	private List<Long> expandElementsToLongIds(Collection<?> items) {
		List<Long> out = new ArrayList<>();
		for (Object o : items) {
			Long id = extractLongFromWhitelistItem(o);
			if (id != null && id > 0L) {
				out.add(id);
			}
		}
		return out;
	}

	private Long extractLongFromWhitelistItem(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Map<?, ?> m) {
			Object sid = m.get("shop_id");
			if (sid == null) {
				return null;
			}
			return parseLongNullable(sid);
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof CharSequence cs) {
			return parseLongNullable(cs.toString());
		}
		return null;
	}

	private static Long parseLongNullable(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
