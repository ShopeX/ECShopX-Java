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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatStatsUserWeekSummaryService {

	private final WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService;
	private final WechatOfficialAccountDataCubeUserStatsClient dataCubeUserStatsClient;
	private final String businessZoneIdProperty;

	public WechatStatsUserWeekSummaryService(
			WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService,
			WechatOfficialAccountDataCubeUserStatsClient dataCubeUserStatsClient,
			@Value("${ecshopx.wechat.stats.business-zone-id:}") String businessZoneIdProperty) {
		this.wechatOpenPlatformAuthorizerTokenService = wechatOpenPlatformAuthorizerTokenService;
		this.dataCubeUserStatsClient = dataCubeUserStatsClient;
		this.businessZoneIdProperty = businessZoneIdProperty;
	}

	public List<Map<String, Object>> userWeekSummary(String authorizerAppid) {
		String token = wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid);

		ZoneId zone;
		if (!StringUtils.hasText(businessZoneIdProperty)) {
			zone = ZoneId.systemDefault();
		} else {
			zone = ZoneId.of(businessZoneIdProperty.trim());
		}

		LocalDate today = LocalDate.now(zone);
		String from = today.minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE);
		String to = today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

		JsonNode userSummaryRoot = dataCubeUserStatsClient.postUserSummary(token, from, to);
		Map<String, AggRow> userSummaryList = new HashMap<>();
		JsonNode listNode = userSummaryRoot.get("list");
		if (listNode != null && listNode.isArray()) {
			for (JsonNode value : listNode) {
				String date = textOf(value, "ref_date");
				if (!StringUtils.hasText(date)) {
					continue;
				}
				int newUser = wxInt(value, "new_user");
				int cancelUser = wxInt(value, "cancel_user");
				int addUser = newUser - cancelUser;
				AggRow existing = userSummaryList.get(date);
				if (existing == null) {
					userSummaryList.put(date, new AggRow(date, newUser, cancelUser, addUser));
				} else {
					existing.newUser += newUser;
					existing.cancelUser += cancelUser;
					existing.addUser += addUser;
				}
			}
		}

		JsonNode userCumulateRoot = dataCubeUserStatsClient.postUserCumulate(token, from, to);
		Map<String, JsonNode> userCumulateList = new HashMap<>();
		JsonNode cumListNode = userCumulateRoot.get("list");
		if (cumListNode != null && cumListNode.isArray()) {
			for (JsonNode row : cumListNode) {
				String key = textOf(row, "ref_date");
				if (StringUtils.hasText(key)) {
					userCumulateList.put(key, row);
				}
			}
		}

		List<Map<String, Object>> out = new ArrayList<>(7);
		for (int i = 7; i > 0; i--) {
			LocalDate d = today.minusDays(i);
			String date = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
			AggRow agg = userSummaryList.get(date);
			String refDate = agg != null ? agg.refDate : date;
			int newUserVal = agg != null ? agg.newUser : 0;
			int cancelUserVal = agg != null ? agg.cancelUser : 0;
			int addUserVal = agg != null ? agg.addUser : 0;
			int cumulateUser = 0;
			if (userCumulateList.containsKey(date)) {
				cumulateUser = wxInt(userCumulateList.get(date), "cumulate_user");
			}
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("ref_date", refDate);
			row.put("new_user", newUserVal);
			row.put("cancel_user", cancelUserVal);
			row.put("add_user", addUserVal);
			row.put("cumulate_user", cumulateUser);
			out.add(row);
		}
		return out;
	}

	private static String textOf(JsonNode node, String field) {
		JsonNode n = node.get(field);
		if (n == null || n.isNull()) {
			return "";
		}
		return n.asText("").trim();
	}

	private static int wxInt(JsonNode node, String field) {
		JsonNode n = node.get(field);
		if (n == null || n.isNull()) {
			return 0;
		}
		if (n.isNumber()) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(n.asText("0").trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static final class AggRow {
		final String refDate;
		int newUser;
		int cancelUser;
		int addUser;

		AggRow(String refDate, int newUser, int cancelUser, int addUser) {
			this.refDate = refDate;
			this.newUser = newUser;
			this.cancelUser = cancelUser;
			this.addUser = addUser;
		}
	}
}
