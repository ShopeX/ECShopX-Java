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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfitWithdrawalListService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");

	private final ProfitWithdrawalListQueryService profitWithdrawalListQueryService;

	public ProfitWithdrawalListService(ProfitWithdrawalListQueryService profitWithdrawalListQueryService) {
		this.profitWithdrawalListQueryService = profitWithdrawalListQueryService;
	}

	public Map<String, Object> lists(String profitType, String distributor, String salesperson, String dealer,
			String date, Integer page, Integer pageSize) {
		LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
		if (profitType != null) {
			fields.put("profit_user_type", profitType);
		}
		if (distributor != null) {
			fields.put("distributor", distributor);
		}
		if (salesperson != null) {
			fields.put("salesperson", salesperson);
		}
		if (dealer != null) {
			fields.put("dealer", dealer);
		}
		if (date != null) {
			fields.put("date", date);
		}

		if (fields.containsKey("1") || fields.containsKey("2") || fields.containsKey("3")) {
			if (!profitUserTypeTextPresent(fields.get("profit_user_type"))) {
				throw new ResourceException("分润成员类型必填");
			}
		}

		Object dateObj = fields.get("date");
		String dateStr = dateObj == null ? "" : String.valueOf(dateObj).trim();
		if (!StringUtils.hasText(dateStr) || "0".equals(dateStr)) {
			fields.put("date", YearMonth.now(SHANGHAI).minusMonths(1).format(YM));
		}

		String name = "";

		Object rawPut = fields.get("profit_user_type");
		Long profitUserTypeVal = null;
		if (rawPut == null || !StringUtils.hasText(String.valueOf(rawPut).trim())) {
			// empty semantic: null in query filter
		} else {
			try {
				long v = Long.parseLong(String.valueOf(rawPut).trim());
				profitUserTypeVal = v;
				if (v == 1L) {
					name = salesperson != null ? salesperson : "";
				} else if (v == 2L) {
					name = distributor != null ? distributor : "";
				} else if (v == 3L) {
					name = dealer != null ? dealer : "";
				}
			} catch (NumberFormatException e) {
				throw new ResourceException("分润成员类型必填");
			}
		}

		Map<String, Object> queryFilter = new LinkedHashMap<>();
		queryFilter.put("date", fields.get("date"));
		queryFilter.put("profit_user_type", profitUserTypeVal);

		if (StringUtils.hasText(name)) {
			queryFilter.put("nameContains", name.trim());
		}

		return profitWithdrawalListQueryService.query(queryFilter, page, pageSize);
	}

	private static boolean profitUserTypeTextPresent(Object profitUserType) {
		if (profitUserType == null) {
			return false;
		}
		return StringUtils.hasText(String.valueOf(profitUserType).trim());
	}
}
