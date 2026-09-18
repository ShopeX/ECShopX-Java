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

import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenapiMemberV2ListHandlerParams {

	private OpenapiMemberV2ListHandlerParams() {}

	public static Map<String, Object> mergePresentKeys(
			Map<String, Object> body,
			String mobileParam,
			String sourceFromParam,
			String inviterMobileParam,
			String salespersonMobileParam,
			String statusParam,
			String tagIdParam,
			String tagNameParam,
			String haveConsumeParam,
			String cardCodeParam,
			String userCardCodeParam,
			String gradeIdParam,
			String gradeNameParam,
			String vipGradeIdParam,
			String vipGradeNameParam,
			String startDateParam,
			String endDateParam) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		putIfPresent(out, body, "inviter_mobile", inviterMobileParam);
		putIfPresent(out, body, "salesperson_mobile", salespersonMobileParam);
		putIfPresent(out, body, "tag_id", tagIdParam);
		putIfPresent(out, body, "tag_name", tagNameParam);
		putIfPresent(out, body, "grade_id", gradeIdParam);
		putIfPresent(out, body, "grade_name", gradeNameParam);
		putIfPresent(out, body, "vip_grade_id", vipGradeIdParam);
		putIfPresent(out, body, "vip_grade_name", vipGradeNameParam);
		putIfPresent(out, body, "have_consume", haveConsumeParam);
		putIfPresent(out, body, "mobile", mobileParam);
		putIfPresent(out, body, "source_from", sourceFromParam);
		putIfPresent(out, body, "status", statusParam);
		putIfPresent(out, body, "start_date", startDateParam);
		putIfPresent(out, body, "end_date", endDateParam);
		mergeCardCodeKeys(out, body, cardCodeParam, userCardCodeParam);
		return out;
	}

	private static void mergeCardCodeKeys(
			LinkedHashMap<String, Object> out,
			Map<String, Object> body,
			String cardCodeParam,
			String userCardCodeParam) {
		boolean hasCardCode =
				(body != null && body.containsKey("card_code")) || cardCodeParam != null;
		boolean hasUserCardCode =
				(body != null && body.containsKey("user_card_code")) || userCardCodeParam != null;
		if (hasCardCode) {
			putIfPresent(out, body, "card_code", cardCodeParam);
		} else if (hasUserCardCode) {
			putIfPresent(out, body, "user_card_code", userCardCodeParam);
			if (out.containsKey("user_card_code") && !out.containsKey("card_code")) {
				out.put("card_code", out.get("user_card_code"));
				out.remove("user_card_code");
			}
		}
	}

	private static void putIfPresent(
			LinkedHashMap<String, Object> out, Map<String, Object> body, String key, String queryParam) {
		boolean inBody = body != null && body.containsKey(key);
		boolean inQuery = queryParam != null;
		if (!inBody && !inQuery) {
			return;
		}
		if (inBody) {
			out.put(key, body.get(key));
		} else {
			out.put(key, queryParam);
		}
	}
}
