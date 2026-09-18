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

package cn.shopex.ecshopx.orders.service.offline;

import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OfflinePaymentAdminQueryFilterBuilder {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final MembersUserIdByMobileLookupService membersUserIdByMobileLookupService;

	public OfflinePaymentAdminQueryFilterBuilder(
			MembersUserIdByMobileLookupService membersUserIdByMobileLookupService) {
		this.membersUserIdByMobileLookupService = membersUserIdByMobileLookupService;
	}

	public LinkedHashMap<String, Object> buildFilter(long companyId, Map<String, Object> params) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);

		if (params.containsKey("check_status")) {
			Object raw = params.get("check_status");
			if (raw != null) {
				String s = String.valueOf(raw).trim();
				if (StringUtils.hasText(s)) {
					int cs;
					try {
						cs = Integer.parseInt(s);
					} catch (NumberFormatException e) {
						cs = 0;
					}
					filter.put("check_status", cs);
				}
			}
		}

		for (Map.Entry<String, Object> en : params.entrySet()) {
			String key = en.getKey();
			Object value = en.getValue();
			if (shouldSkipParamValueInForeach(value)) {
				continue;
			}
			switch (key) {
				case "order_id" -> filter.put("order_id", String.valueOf(value).trim());
				case "begin_date" -> {
					Long sec = DateExpressionParser.parseToEpochSecond(value, SHANGHAI);
					if (sec != null && sec > 0) {
						filter.put("create_time|gte", sec.intValue());
					}
				}
				case "end_date" -> {
					Long sec = DateExpressionParser.parseToEpochSecond(value, SHANGHAI);
					if (sec != null && sec > 0) {
						filter.put("create_time|lte", sec.intValue());
					}
				}
				case "user_mobile" -> {
					Long uid = membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(
							companyId, String.valueOf(value).trim());
					filter.put("user_id", uid != null ? uid : 0L);
				}
				case "pay_sn" -> filter.put("pay_sn", String.valueOf(value).trim());
				case "pay_account_no" -> filter.put("pay_account_no", String.valueOf(value).trim());
				case "pay_account_bank" -> filter.put("pay_account_bank", String.valueOf(value).trim());
				case "bank_account_name" -> filter.put("bank_account_name", String.valueOf(value).trim());
				default -> {
				}
			}
		}

		return filter;
	}

	private static boolean shouldSkipParamValueInForeach(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return true;
			}
			return "0".equals(s.trim());
		}
		if (value instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (value instanceof Boolean b) {
			return !b;
		}
		return false;
	}
}
