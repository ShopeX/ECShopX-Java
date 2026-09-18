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
import java.util.Objects;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class WxappStatsVisitPageService {

	private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final String[] METRIC_KEYS = {
		"page_visit_pv",
		"page_visit_uv",
		"page_staytime_pv",
		"entrypage_pv",
		"exitpage_pv",
		"page_share_pv",
		"page_share_uv"
	};

	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final WxappStatsDataCubeVisitPageClient visitPageClient;
	private final ObjectMapper objectMapper;

	public WxappStatsVisitPageService(
			WechatOpenPlatformAuthorizerTokenService tokenService,
			WxappStatsDataCubeVisitPageClient visitPageClient,
			ObjectMapper objectMapper) {
		this.tokenService = tokenService;
		this.visitPageClient = visitPageClient;
		this.objectMapper = objectMapper;
	}

	public Object getVisitPage(String wxaAppId, Object rawQueryTypeInput) {
		String app = wxaAppId.trim();
		try {
			tokenService.getAuthorizerAccessToken(app);
		} catch (ResourceException e) {
			if (isUnboundAuthorizerResource(e)) {
				throw new ResourceException(e.getMessage(), 500);
			}
			throw e;
		}

		String type = resolveQueryType(rawQueryTypeInput);

		ZoneId zone = ZoneId.systemDefault();
		LocalDate today = LocalDate.now(zone);
		LocalDate yesterday = today.minusDays(1);
		LocalDate begin;
		LocalDate end;
		int days;
		switch (type) {
			case "yesterday" -> {
				begin = yesterday;
				end = yesterday;
				days = 1;
			}
			case "weekly" -> {
				begin = today.minusDays(7);
				end = yesterday;
				days = 7;
			}
			case "monthly" -> {
				begin = today.minusDays(30);
				end = yesterday;
				days = 30;
			}
			default -> throw new BadRequestException("时间间隔不符合要求！");
		}

		String beginYmd = begin.format(BASIC_ISO_DATE);
		String endYmd = end.format(BASIC_ISO_DATE);

		if (begin.equals(end)) {
			JsonNode root = visitPageClient.postVisitPage(app, beginYmd, endYmd);
			return objectMapper.convertValue(root, Object.class);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("ref_date", beginYmd + "-" + endYmd);

		TreeMap<Integer, LinkedHashMap<String, Object>> accByIndex = new TreeMap<>();

		for (int i = 0; i < days; i++) {
			LocalDate nextDate = begin.plusDays(i);
			if (nextDate.isAfter(end)) {
				continue;
			}
			String nextYmd = nextDate.format(BASIC_ISO_DATE);
			if (nextYmd.isEmpty() || nextYmd.compareTo(endYmd) > 0) {
				continue;
			}
			JsonNode data = visitPageClient.postVisitPage(app, nextYmd, nextYmd);
			JsonNode listNode = data.path("list");
			if (!listNode.isArray() || listNode.isNull() || listNode.size() == 0) {
				continue;
			}
			for (int key = 0; key < listNode.size(); key++) {
				JsonNode value = listNode.get(key);
				if (accByIndex.containsKey(key)) {
					LinkedHashMap<String, Object> existing = accByIndex.get(key);
					for (String metricKey : METRIC_KEYS) {
						BigDecimal sum =
								parseBigDecimal(existing.get(metricKey)).add(parseBigDecimal(value.path(metricKey)));
						existing.put(metricKey, toNumber(sum));
					}
				} else {
					LinkedHashMap<String, Object> row =
							objectMapper.convertValue(value, new TypeReference<LinkedHashMap<String, Object>>() {});
					accByIndex.put(key, row);
				}
			}
		}

		ArrayList<LinkedHashMap<String, Object>> mergedList = new ArrayList<>(accByIndex.values());
		out.put("list", mergedList);

		LinkedHashMap<String, Object> total = new LinkedHashMap<>();
		if (mergedList.isEmpty()) {
			for (String metricKey : METRIC_KEYS) {
				total.put(metricKey, 0L);
			}
		} else {
			for (String metricKey : METRIC_KEYS) {
				BigDecimal sum = BigDecimal.ZERO;
				for (LinkedHashMap<String, Object> rowMap : mergedList) {
					sum = sum.add(parseBigDecimal(rowMap.get(metricKey)));
				}
				total.put(metricKey, toNumber(sum));
			}
		}

		out.put("total", total);
		return out;
	}

	private static String resolveQueryType(Object rawQueryTypeInput) {
		if (isEmptyMergedValue(rawQueryTypeInput)) {
			return "yesterday";
		}
		if (rawQueryTypeInput instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if ("yesterday".equals(t) || "weekly".equals(t) || "monthly".equals(t)) {
				return t;
			}
			throw new BadRequestException("时间间隔不符合要求！");
		}
		throw new BadRequestException("时间间隔不符合要求！");
	}

	private static boolean isEmptyMergedValue(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return Boolean.FALSE.equals(b);
		}
		if (raw instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (raw instanceof CharSequence cs) {
			String t = cs.toString().trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (raw instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (raw instanceof Object[] arr) {
			return arr.length == 0;
		}
		String t = String.valueOf(raw).trim();
		return t.isEmpty() || "0".equals(t);
	}

	private static BigDecimal parseBigDecimal(Object o) {
		if (o == null) {
			return BigDecimal.ZERO;
		}
		if (o instanceof JsonNode jn) {
			if (jn.isNull() || jn.isMissingNode()) {
				return BigDecimal.ZERO;
			}
			if (jn.isNumber()) {
				return BigDecimal.valueOf(jn.doubleValue());
			}
			if (jn.isTextual()) {
				return parseBigDecimal(jn.asText());
			}
			return BigDecimal.ZERO;
		}
		if (o instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		if (o instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty()) {
				return BigDecimal.ZERO;
			}
			try {
				return new BigDecimal(s);
			} catch (Exception e) {
				return BigDecimal.ZERO;
			}
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(s);
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private static Number toNumber(BigDecimal sum) {
		BigDecimal s = sum.stripTrailingZeros();
		if (s.scale() <= 0) {
			return s.longValue();
		}
		return s.doubleValue();
	}

	private static boolean isUnboundAuthorizerResource(ResourceException e) {
		return Objects.equals(400, e.getEmbeddedStatusCode())
				&& Objects.equals(400001, e.getEmbeddedBusinessCode());
	}
}
