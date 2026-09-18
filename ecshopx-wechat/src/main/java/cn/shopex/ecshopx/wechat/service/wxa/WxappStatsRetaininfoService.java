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
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappStatsRetaininfoService {

	private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private static final TypeReference<LinkedHashMap<String, Object>> LINKED_MAP_TYPE = new TypeReference<>() {};

	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final WxappStatsDataCubeRetainClient dataCubeRetainClient;
	private final ObjectMapper objectMapper;

	public WxappStatsRetaininfoService(
			WechatOpenPlatformAuthorizerTokenService tokenService,
			WxappStatsDataCubeRetainClient dataCubeRetainClient,
			ObjectMapper objectMapper) {
		this.tokenService = tokenService;
		this.dataCubeRetainClient = dataCubeRetainClient;
		this.objectMapper = objectMapper;
	}

	public Object getRetaininfo(String wxaAppId, String queryType) {
		String app = wxaAppId == null ? "" : wxaAppId.trim();
		tokenService.getAuthorizerAccessToken(app);

		String t = queryType == null ? "" : queryType.trim();
		if (!StringUtils.hasText(t)) {
			t = "yesterday";
		}

		RetainDateRange range = resolveDateRange(t);
		String beginYmd = range.beginYmd();
		String endYmd = range.endYmd();
		int days = range.days();

		LocalDate beginLd = LocalDate.parse(beginYmd, BASIC_ISO_DATE);

		if (beginYmd.equals(endYmd)) {
			JsonNode r = dataCubeRetainClient.postDailyRetain(app, beginYmd, endYmd);
			return this.objectMapper.convertValue(r, Object.class);
		}
		if (t.equals("weekly") && beginLd.getDayOfWeek() == DayOfWeek.MONDAY) {
			JsonNode r = dataCubeRetainClient.postWeeklyRetain(app, beginYmd, endYmd);
			return this.objectMapper.convertValue(r, Object.class);
		}
		if (t.equals("monthly") && beginLd.getDayOfMonth() == 1) {
			JsonNode r = dataCubeRetainClient.postMonthlyRetain(app, beginYmd, endYmd);
			return this.objectMapper.convertValue(r, Object.class);
		}

		List<Object> result = new ArrayList<>();
		for (int i = 0; i < days; i++) {
			LocalDate nextLd = beginLd.plusDays(i);
			String nextYmd = nextLd.format(BASIC_ISO_DATE);
			if (nextYmd.compareTo(endYmd) > 0) {
				break;
			}
			JsonNode info = dataCubeRetainClient.postDailyRetain(app, nextYmd, nextYmd);
			Map<String, Object> m = this.objectMapper.convertValue(info, LINKED_MAP_TYPE);
			Object refVal = m.get("ref_date");
			if (!m.containsKey("ref_date") || !StringUtils.hasText(String.valueOf(refVal == null ? "" : refVal))) {
				m.put("ref_date", nextYmd);
			}
			result.add(m);
		}
		return result;
	}

	private static RetainDateRange resolveDateRange(String t) {
		ZoneId zone = ZoneId.systemDefault();
		LocalDate yesterday = LocalDate.now(zone).minusDays(1);
		String yesterdayYmd = yesterday.format(BASIC_ISO_DATE);

		if ("yesterday".equals(t)) {
			return new RetainDateRange(yesterdayYmd, yesterdayYmd, 1);
		}
		if ("weekly".equals(t)) {
			LocalDate begin = yesterday.minusDays(6);
			return new RetainDateRange(begin.format(BASIC_ISO_DATE), yesterdayYmd, 7);
		}
		if ("monthly".equals(t)) {
			LocalDate begin = yesterday.minusDays(29);
			return new RetainDateRange(begin.format(BASIC_ISO_DATE), yesterdayYmd, 30);
		}
		throw new BadRequestException("时间间隔不符合要求！");
	}

	private record RetainDateRange(String beginYmd, String endYmd, int days) {}
}
