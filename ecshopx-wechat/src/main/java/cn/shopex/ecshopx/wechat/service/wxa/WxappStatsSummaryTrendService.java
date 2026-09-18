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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappStatsSummaryTrendService {

	private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final TypeReference<LinkedHashMap<String, Object>> LINKED_MAP_TYPE = new TypeReference<>() {};

	private final WxappStatsDataCubeSummaryClient summaryClient;
	private final ObjectMapper objectMapper;

	public WxappStatsSummaryTrendService(
			WxappStatsDataCubeSummaryClient summaryClient, ObjectMapper objectMapper) {
		this.summaryClient = summaryClient;
		this.objectMapper = objectMapper;
	}

	public List<LinkedHashMap<String, Object>> getSummaryTrend(String wxaAppId, Object rawQueryTypeInput) {
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
			return buildSingleDay(wxaAppId, begin, beginYmd, endYmd);
		}
		return buildMultiDay(wxaAppId, begin, end, days);
	}

	private List<LinkedHashMap<String, Object>> buildSingleDay(
			String wxaAppId, LocalDate begin, String beginYmd, String endYmd) {
		JsonNode root = summaryClient.postDailySummaryTrend(wxaAppId, beginYmd, endYmd);
		JsonNode list = root.path("list");
		if (!list.isArray() || list.size() == 0) {
			return new ArrayList<>();
		}
		List<LinkedHashMap<String, Object>> rows = new ArrayList<>();
		for (JsonNode node : list) {
			rows.add(objectMapper.convertValue(node, LINKED_MAP_TYPE));
		}
		if (!rows.isEmpty()) {
			rows.get(0).put("ref_date", begin.format(ISO_DATE));
		}
		return rows;
	}

	private List<LinkedHashMap<String, Object>> buildMultiDay(
			String wxaAppId, LocalDate begin, LocalDate end, int days) {
		ArrayList<LinkedHashMap<String, Object>> result = new ArrayList<>();
		for (int i = 0; i < days; i++) {
			LocalDate day = begin.plusDays(i);
			if (day.isAfter(end)) {
				continue;
			}
			String nextYmd = day.format(BASIC_ISO_DATE);
			JsonNode root = summaryClient.postDailySummaryTrend(wxaAppId, nextYmd, nextYmd);
			JsonNode list = root.path("list");
			String refYmd = day.format(ISO_DATE);
			if (list.isArray() && list.size() > 0) {
				LinkedHashMap<String, Object> row =
						objectMapper.convertValue(list.get(0), LINKED_MAP_TYPE);
				row.put("ref_date", refYmd);
				result.add(row);
			} else {
				LinkedHashMap<String, Object> placeholder = new LinkedHashMap<>();
				placeholder.put("ref_date", refYmd);
				placeholder.put("visit_total", 0);
				placeholder.put("share_pv", 0);
				placeholder.put("share_uv", 0);
				result.add(placeholder);
			}
		}
		return result;
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
}
