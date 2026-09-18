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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OpenapiMemberV2UpdateDetailHandlerParams {

	private static final ObjectMapper JSON = new ObjectMapper();

	private OpenapiMemberV2UpdateDetailHandlerParams() {}

	public static Map<String, Object> mergeAll(
			Map<String, Object> body,
			String userIdParam,
			String mobileParam,
			String inviterMobileParam,
			String statusParam,
			String remarksParam,
			String usernameParam,
			String avatarParam,
			String sexParam,
			String birthdayParam,
			String eduBackgroundParam,
			String incomeParam,
			String industryParam,
			String emailParam,
			String addressParam) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		putMerge(out, body, "user_id", userIdParam);
		putMerge(out, body, "mobile", mobileParam);
		putMerge(out, body, "inviter_mobile", inviterMobileParam);
		putMerge(out, body, "status", statusParam);
		putMerge(out, body, "remarks", remarksParam);
		putMerge(out, body, "username", usernameParam);
		putMerge(out, body, "avatar", avatarParam);
		putMerge(out, body, "sex", sexParam);
		putMerge(out, body, "birthday", birthdayParam);
		putMerge(out, body, "edu_background", eduBackgroundParam);
		putMerge(out, body, "income", incomeParam);
		putMerge(out, body, "industry", industryParam);
		putMerge(out, body, "email", emailParam);
		putMerge(out, body, "address", addressParam);
		return out;
	}

	public static Map<String, Object> buildRequestData(Map<String, Object> merged) {
		Map<String, Object> in = merged == null ? Map.of() : merged;
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("inviter_mobile", String.valueOf(in.getOrDefault("inviter_mobile", "0")));
		out.put("status", parseIntDefault(in.get("status"), 1));
		out.put("remakes", stringValue(in.get("remakes")) == null ? "" : stringValue(in.get("remakes")));
		out.put("username", defaultString(stringValue(in.get("username"))));
		out.put("avatar", defaultString(stringValue(in.get("avatar"))));
		out.put("sex", String.valueOf(in.getOrDefault("sex", "0")));
		out.put("birthday", defaultString(stringValue(in.get("birthday"))));
		out.put("habbit", decodeHabbit(in.get("habbit")));
		out.put("edu_background", parseIntDefault(in.get("edu_background"), 4));
		out.put("income", parseIntDefault(in.get("income"), 4));
		out.put("industry", parseIntDefault(in.get("industry"), 12));
		out.put("email", defaultString(stringValue(in.get("email"))));
		out.put("address", defaultString(stringValue(in.get("address"))));
		return out;
	}

	private static void putMerge(
			LinkedHashMap<String, Object> out, Map<String, Object> body, String key, String queryParam) {
		if (body != null && body.containsKey(key)) {
			out.put(key, body.get(key));
		} else {
			String merged = OpenapiRequestParams.mergeString(queryParam, body, key);
			if (merged != null) {
				out.put(key, merged);
			}
		}
	}

	private static List<Map<String, Object>> decodeHabbit(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			return normalizeHabbitList(list);
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			try {
				List<Map<String, Object>> parsed =
						JSON.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
				return parsed == null ? List.of() : parsed;
			} catch (Exception e) {
				return List.of();
			}
		}
		return List.of();
	}

	private static List<Map<String, Object>> normalizeHabbitList(List<?> list) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> map) {
				Map<String, Object> copy = new LinkedHashMap<>();
				copy.put("name", map.get("name"));
				copy.put("ischecked", map.get("ischecked"));
				out.add(copy);
			}
		}
		return out;
	}

	private static int parseIntDefault(Object raw, int defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static String stringValue(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		return String.valueOf(raw);
	}

	private static String defaultString(String raw) {
		return raw == null ? "" : raw;
	}
}
