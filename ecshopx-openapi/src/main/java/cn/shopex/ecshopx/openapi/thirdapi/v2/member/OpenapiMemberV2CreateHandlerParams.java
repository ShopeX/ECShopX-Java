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

import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenapiMemberV2CreateHandlerParams {

	private OpenapiMemberV2CreateHandlerParams() {}

	public static Map<String, Object> mergeAll(
			Map<String, Object> body,
			String mobileParam,
			String inviterMobileParam,
			String salespersonMobileParam,
			String unionIdParam,
			String statusParam,
			String tagNamesParam,
			String tagIdsParam,
			String cardCodeParam,
			String gradeIdParam,
			String usernameParam,
			String avatarParam,
			String sexParam,
			String birthdayParam,
			String eduBackgroundParam,
			String incomeParam,
			String industryParam,
			String emailParam,
			String addressParam,
			String remarksParam,
			String sourceFromParam) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		putMerge(out, body, "mobile", mobileParam);
		putMerge(out, body, "inviter_mobile", inviterMobileParam);
		putMerge(out, body, "salesperson_mobile", salespersonMobileParam);
		putMerge(out, body, "union_id", unionIdParam);
		putMerge(out, body, "status", statusParam);
		putMerge(out, body, "tag_names", tagNamesParam);
		putMerge(out, body, "tag_ids", tagIdsParam);
		putMerge(out, body, "card_code", cardCodeParam);
		putMerge(out, body, "grade_id", gradeIdParam);
		putMerge(out, body, "username", usernameParam);
		putMerge(out, body, "avatar", avatarParam);
		putMerge(out, body, "sex", sexParam);
		putMerge(out, body, "birthday", birthdayParam);
		putMerge(out, body, "edu_background", eduBackgroundParam);
		putMerge(out, body, "income", incomeParam);
		putMerge(out, body, "industry", industryParam);
		putMerge(out, body, "email", emailParam);
		putMerge(out, body, "address", addressParam);
		putMerge(out, body, "remarks", remarksParam);
		putMerge(out, body, "source_from", sourceFromParam);
		return out;
	}

	private static void putMerge(
			LinkedHashMap<String, Object> out, Map<String, Object> body, String key, String queryParam) {
		if (body != null && body.containsKey(key)) {
			out.put(key, body.get(key));
		} else {
			String merged = OpenapiRequestParams.mergeString(queryParam, body, key);
			if (merged != null) {
				out.put(key, merged);
			}
		}
	}
}
