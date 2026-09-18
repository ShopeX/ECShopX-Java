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
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappStatsUserPortraitService {

	private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final String[] DIMENSION_ORDER =
			new String[] {"genders", "province", "city", "platforms", "devices", "ages"};

	private static final TypeReference<LinkedHashMap<String, Object>> ROOT_MAP_TYPE = new TypeReference<>() {};

	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final WxappStatsDataCubeUserPortraitClient userPortraitClient;
	private final ObjectMapper objectMapper;

	public WxappStatsUserPortraitService(
			WechatOpenPlatformAuthorizerTokenService tokenService,
			WxappStatsDataCubeUserPortraitClient userPortraitClient,
			ObjectMapper objectMapper) {
		this.tokenService = tokenService;
		this.userPortraitClient = userPortraitClient;
		this.objectMapper = objectMapper;
	}

	public Object getUserPortrait(String wxaAppId, String queryType) {
		String t = queryType == null ? "" : queryType.trim();
		if (!StringUtils.hasText(t)) {
			t = "yesterday";
		}
		if (!"yesterday".equals(t) && !"weekly".equals(t) && !"monthly".equals(t)) {
			throw new BadRequestException("时间间隔不符合要求！");
		}

		ZoneId zone = ZoneId.systemDefault();
		LocalDate yesterday = LocalDate.now(zone).minusDays(1);
		String yesterdayYmd = yesterday.format(BASIC_ISO_DATE);
		String beginYmd;
		String endYmd;
		if ("yesterday".equals(t)) {
			beginYmd = yesterdayYmd;
			endYmd = yesterdayYmd;
		} else if ("weekly".equals(t)) {
			beginYmd = yesterday.minusDays(6).format(BASIC_ISO_DATE);
			endYmd = yesterdayYmd;
		} else {
			beginYmd = yesterday.minusDays(29).format(BASIC_ISO_DATE);
			endYmd = yesterdayYmd;
		}

		String app = wxaAppId == null ? "" : wxaAppId.trim();
		try {
			tokenService.getAuthorizerAccessToken(app);
		} catch (ResourceException e) {
			if (Objects.equals(400, e.getEmbeddedStatusCode())
					&& Objects.equals(400001, e.getEmbeddedBusinessCode())) {
				throw new ResourceException(e.getMessage(), 500);
			}
			throw e;
		}
		JsonNode root = userPortraitClient.postUserPortrait(app, beginYmd, endYmd);

		LinkedHashMap<String, Object> rootMap;
		try {
			rootMap = objectMapper.convertValue(root, ROOT_MAP_TYPE);
		} catch (IllegalArgumentException e) {
			rootMap = new LinkedHashMap<>();
		}
		if (rootMap == null) {
			rootMap = new LinkedHashMap<>();
		}

		for (String visitKey : new String[] {"visit_uv_new", "visit_uv"}) {
			Object visitObj = rootMap.get(visitKey);
			if (!(visitObj instanceof Map<?, ?>)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> visitMap = (Map<String, Object>) visitObj;
			Map<String, Number> totals = new LinkedHashMap<>();
			for (String dim : DIMENSION_ORDER) {
				Object dimVal = visitMap.get(dim);
				if (!(dimVal instanceof List<?> rawList)) {
					continue;
				}
				List<Map<String, Object>> processed = processDimensionList(dim, rawList);
				visitMap.put(dim, processed);
				double sum = 0.0;
				for (Map<String, Object> row : processed) {
					sum += parseNumericValue(row.get("value"));
				}
				totals.put(dim, sum);
			}
			visitMap.put("total", totals);
		}

		return objectMapper.convertValue(rootMap, Object.class);
	}

	private static List<Map<String, Object>> processDimensionList(String dimName, List<?> rawList) {
		List<Map<String, Object>> filtered = new ArrayList<>();
		for (Object item : rawList) {
			if (!(item instanceof Map<?, ?> rawMap)) {
				continue;
			}
			if (!rawMap.containsKey("value")) {
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rawMap.entrySet()) {
				m.put(String.valueOf(e.getKey()), e.getValue());
			}
			double v = parseNumericValue(m.get("value"));
			if (v == 0.0) {
				continue;
			}
			filtered.add(m);
		}

		filtered.sort(
				Comparator.comparingDouble((Map<String, Object> row) -> parseNumericValue(row.get("value")))
						.reversed());

		if ("devices".equals(dimName)) {
			filtered = moveUnknownToEnd(filtered);
			filtered = truncateDevicesWithOther(filtered);
		}
		return filtered;
	}

	private static List<Map<String, Object>> moveUnknownToEnd(List<Map<String, Object>> list) {
		List<Map<String, Object>> head = new ArrayList<>();
		List<Map<String, Object>> unknownTail = new ArrayList<>();
		for (Map<String, Object> m : list) {
			if (isUnknownName(m.get("name"))) {
				unknownTail.add(m);
			} else {
				head.add(m);
			}
		}
		head.addAll(unknownTail);
		return head;
	}

	private static boolean isUnknownName(Object nameObj) {
		String name =
				nameObj == null
						? ""
						: (nameObj instanceof CharSequence cs ? cs.toString() : String.valueOf(nameObj));
		return "未知".equals(name);
	}

	private static List<Map<String, Object>> truncateDevicesWithOther(List<Map<String, Object>> list) {
		if (list.size() <= 9) {
			return list;
		}
		List<Map<String, Object>> out = new ArrayList<>(list.subList(0, 9));
		double sumOther = 0.0;
		for (int i = 9; i < list.size(); i++) {
			sumOther += parseNumericValue(list.get(i).get("value"));
		}
		Map<String, Object> otherRow = new LinkedHashMap<>();
		otherRow.put("name", "其他");
		otherRow.put("value", sumOther);
		out.add(otherRow);
		return out;
	}

	private static double parseNumericValue(Object value) {
		if (value == null) {
			return 0.0;
		}
		if (value instanceof Number n) {
			return n.doubleValue();
		}
		if (value instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty()) {
				return 0.0;
			}
			try {
				return Double.parseDouble(s);
			} catch (NumberFormatException e) {
				return 0.0;
			}
		}
		try {
			return Double.parseDouble(String.valueOf(value).trim());
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
}
