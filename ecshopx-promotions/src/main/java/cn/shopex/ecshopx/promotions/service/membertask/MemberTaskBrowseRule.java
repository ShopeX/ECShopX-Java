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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

public record MemberTaskBrowseRule(String page, int staySecondsRequired) {

	public boolean matchesReport(String reportedPage, int reportedStaySeconds) {
		if (!StringUtils.hasText(page) || !page.equals(reportedPage)) {
			return false;
		}
		return reportedStaySeconds >= staySecondsRequired;
	}

	public static MemberTaskBrowseRule parse(String ruleConfigJson, ObjectMapper objectMapper) {
		if (!StringUtils.hasText(ruleConfigJson)) {
			return new MemberTaskBrowseRule("", 0);
		}
		try {
			JsonNode node = objectMapper.readTree(ruleConfigJson);
			String page = node.path("page").asText("").trim();
			int stay = node.path("stay_seconds").asInt(0);
			if (stay < 0) {
				stay = 0;
			}
			return new MemberTaskBrowseRule(page, stay);
		} catch (Exception e) {
			return new MemberTaskBrowseRule("", 0);
		}
	}
}
