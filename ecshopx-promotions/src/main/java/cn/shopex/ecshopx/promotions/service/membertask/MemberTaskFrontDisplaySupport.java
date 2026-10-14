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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskUserTaskStatus;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskUserTaskStatusResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/** C 端列表/历史展示字段（VshopAPI 0.2.0 · Figma 对齐）。 */
public final class MemberTaskFrontDisplaySupport {

	private MemberTaskFrontDisplaySupport() {}

	public static String frequencyText(String frequencyType, String frequencyPeriod) {
		if (!StringUtils.hasText(frequencyType) || "once".equals(frequencyType)) {
			return "任务周期内仅可完成一次";
		}
		if (!"cycle".equals(frequencyType)) {
			return null;
		}
		if (!StringUtils.hasText(frequencyPeriod)) {
			return "任务周期内可循环完成";
		}
		return switch (frequencyPeriod) {
			case "daily" -> "每日可完成";
			case "weekly" -> "每周可完成";
			case "monthly" -> "每月可完成";
			default -> "任务周期内可循环完成";
		};
	}

	public static Map<String, String> shareFieldsFromRuleConfig(String ruleConfigJson, ObjectMapper objectMapper) {
		Map<String, String> out = new LinkedHashMap<>();
		if (!StringUtils.hasText(ruleConfigJson)) {
			return out;
		}
		try {
			JsonNode node = objectMapper.readTree(ruleConfigJson);
			putText(out, "share_cover", node.get("share_cover"));
			putText(out, "share_title", node.get("share_title"));
			putText(out, "share_jump_page", node.get("share_jump_page"));
		} catch (Exception ignored) {
			// ignore malformed rule_config in list
		}
		return out;
	}

	public static String frontGrantStatusText(String grantStatus) {
		if (!StringUtils.hasText(grantStatus)) {
			return "";
		}
		return switch (grantStatus) {
			case "pending" -> "待发奖";
			case "granted" -> "奖励已发放";
			case "failed" -> "发放失败";
			case "cancelled" -> "已作废";
			default -> grantStatus;
		};
	}

	public static String historyUserTaskStatus(String grantStatus) {
		String resolved = MemberTaskUserTaskStatusResolver.resolve(grantStatus);
		if (MemberTaskUserTaskStatus.TODO.equals(resolved)) {
			return MemberTaskUserTaskStatus.PENDING_REWARD;
		}
		return resolved;
	}

	private static void putText(Map<String, String> out, String key, JsonNode node) {
		if (node == null || node.isNull() || !node.isTextual()) {
			return;
		}
		String v = node.asText().trim();
		if (!v.isEmpty()) {
			out.put(key, v);
		}
	}
}
