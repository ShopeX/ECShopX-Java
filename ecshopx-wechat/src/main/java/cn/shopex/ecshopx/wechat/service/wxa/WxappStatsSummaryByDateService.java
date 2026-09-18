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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class WxappStatsSummaryByDateService {

	private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final TypeReference<LinkedHashMap<String, Object>> LINKED_MAP_TYPE = new TypeReference<>() {};

	private static final List<String> RATE_BASE_KEYS =
			List.of("session_cnt", "visit_pv", "visit_uv", "visit_uv_new", "share_pv", "share_uv");

	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final WxappStatsDataCubeSummaryClient summaryClient;
	private final ObjectMapper objectMapper;

	public WxappStatsSummaryByDateService(
			WechatOpenPlatformAuthorizerTokenService tokenService,
			WxappStatsDataCubeSummaryClient summaryClient,
			ObjectMapper objectMapper) {
		this.tokenService = tokenService;
		this.summaryClient = summaryClient;
		this.objectMapper = objectMapper;
	}

	public Object getSummaryByDate(String wxaAppId, String dateYmd) {
		if (wxaAppId == null || wxaAppId.isEmpty()) {
			throw new BadRequestException("wxappid 必填");
		}
		LocalDate anchor;
		try {
			anchor = LocalDate.parse(dateYmd, BASIC_ISO_DATE);
		} catch (DateTimeParseException e) {
			throw new BadRequestException("date 须为 yyyyMMdd 格式");
		}

		String y0 = dateYmd;
		String y1 = anchor.minusDays(1).format(BASIC_ISO_DATE);
		String y7 = anchor.minusDays(7).format(BASIC_ISO_DATE);
		String yM = anchor.minusMonths(1).format(BASIC_ISO_DATE);

		try {
			tokenService.getAuthorizerAccessToken(wxaAppId);
		} catch (ResourceException e) {
			if (Objects.equals(400, e.getEmbeddedStatusCode())
					&& Objects.equals(400001, e.getEmbeddedBusinessCode())) {
				throw new ResourceException(e.getMessage(), 500);
			}
			throw e;
		}
		LinkedHashMap<String, Object> data = loadSummaryRow(y0, wxaAppId);
		LinkedHashMap<String, Object> yesterdayData = loadSummaryRow(y1, wxaAppId);
		LinkedHashMap<String, Object> lastWeekData = loadSummaryRow(y7, wxaAppId);
		LinkedHashMap<String, Object> lastMonthData = loadSummaryRow(yM, wxaAppId);

		LinkedHashMap<String, Object> result = new LinkedHashMap<>(data);
		for (String key : RATE_BASE_KEYS) {
			double dataVal = toLooseDouble(data, key);
			putRate(result, key + "_dayRate", dataVal, toLooseDouble(yesterdayData, key));
			putRate(result, key + "_weekRate", dataVal, toLooseDouble(lastWeekData, key));
			putRate(result, key + "_monthRate", dataVal, toLooseDouble(lastMonthData, key));
		}
		return result;
	}

	private void putRate(LinkedHashMap<String, Object> result, String rateKey, double dataVal, double baseVal) {
		if (baseVal == 0.0) {
			result.put(rateKey, "");
		} else {
			double rate = (dataVal - baseVal) * 100.0 / baseVal;
			double rounded = BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
			result.put(rateKey, rounded);
		}
	}

	private LinkedHashMap<String, Object> loadSummaryRow(String ymd, String wxaAppId) {
		JsonNode summaryTrendData = summaryClient.postDailySummaryTrend(wxaAppId, ymd, ymd);
		JsonNode visitTrendData = summaryClient.postDailyVisitTrend(wxaAppId, ymd, ymd);

		LinkedHashMap<String, Object> summaryMap = firstListRowOrSummaryPlaceholder(ymd, summaryTrendData);
		LinkedHashMap<String, Object> visitMap = firstListRowOrVisitPlaceholder(ymd, visitTrendData);

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(visitMap);
		merged.putAll(summaryMap);
		return merged;
	}

	private LinkedHashMap<String, Object> firstListRowOrSummaryPlaceholder(String ymd, JsonNode root) {
		JsonNode list = root.path("list");
		if (list.isArray() && list.size() > 0) {
			return objectMapper.convertValue(list.get(0), LINKED_MAP_TYPE);
		}
		LinkedHashMap<String, Object> placeholder = new LinkedHashMap<>();
		placeholder.put("ref_date", ymd);
		placeholder.put("visit_total", 0);
		placeholder.put("share_pv", 0);
		placeholder.put("share_uv", 0);
		return placeholder;
	}

	private LinkedHashMap<String, Object> firstListRowOrVisitPlaceholder(String ymd, JsonNode root) {
		JsonNode list = root.path("list");
		if (list.isArray() && list.size() > 0) {
			return objectMapper.convertValue(list.get(0), LINKED_MAP_TYPE);
		}
		LinkedHashMap<String, Object> placeholder = new LinkedHashMap<>();
		placeholder.put("ref_date", ymd);
		placeholder.put("session_cnt", 0);
		placeholder.put("visit_pv", 0);
		placeholder.put("visit_uv", 0);
		placeholder.put("visit_uv_new", 0);
		placeholder.put("stay_time_session", 0);
		placeholder.put("visit_depth", 0);
		return placeholder;
	}

	private double toLooseDouble(Map<String, Object> map, String key) {
		if (!map.containsKey(key)) {
			return 0.0;
		}
		Object v = map.get(key);
		if (v == null) {
			return 0.0;
		}
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		if (v instanceof CharSequence cs) {
			String t = cs.toString().trim();
			if (t.isEmpty() || "0".equals(t)) {
				return 0.0;
			}
			try {
				return Double.parseDouble(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("指标字段无法解析为数字: " + key);
			}
		}
		if (v instanceof Boolean b) {
			return Boolean.TRUE.equals(b) ? 1.0 : 0.0;
		}
		try {
			return Double.parseDouble(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("指标字段无法解析为数字: " + key);
		}
	}
}
