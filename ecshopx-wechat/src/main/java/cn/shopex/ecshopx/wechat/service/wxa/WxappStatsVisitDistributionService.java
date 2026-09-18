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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappStatsVisitDistributionService {

	private static final DateTimeFormatter BASIC_ISO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final WxappStatsDataCubeVisitDistributionClient visitDistributionClient;
	private final ObjectMapper objectMapper;

	public WxappStatsVisitDistributionService(
			WxappStatsDataCubeVisitDistributionClient visitDistributionClient, ObjectMapper objectMapper) {
		this.visitDistributionClient = visitDistributionClient;
		this.objectMapper = objectMapper;
	}

	public Object getVisitDistribution(String wxaAppId, Object rawQueryTypeInput) {
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

		String app = wxaAppId.trim();

		if (begin.equals(end)) {
			JsonNode root = visitDistributionClient.postVisitDistribution(app, beginYmd, endYmd);
			return objectMapper.convertValue(root, Object.class);
		}

		JsonNode lastResponse = null;
		for (int i = 0; i < days; i++) {
			LocalDate day = begin.plusDays(i);
			if (day.isAfter(end)) {
				continue;
			}
			String nextYmd = day.format(BASIC_ISO_DATE);
			if (!nextYmd.isEmpty() && nextYmd.compareTo(endYmd) <= 0) {
				lastResponse = visitDistributionClient.postVisitDistribution(app, nextYmd, nextYmd);
			}
		}

		if (lastResponse == null) {
			return new ArrayList<>();
		}
		return objectMapper.convertValue(lastResponse, Object.class);
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
