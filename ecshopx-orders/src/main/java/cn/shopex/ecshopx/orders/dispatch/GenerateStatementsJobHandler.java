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

package cn.shopex.ecshopx.orders.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.orders.statement.StatementPeriodValue;
import cn.shopex.ecshopx.orders.statement.StatementTimeWindowCalculator;
import cn.shopex.ecshopx.orders.statement.generate.StatementGenerateJobInliner;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GenerateStatementsJobHandler implements DispatchHandler {

	private final StatementGenerateJobInliner statementGenerateJobInliner;
	private final Clock clock;

	public GenerateStatementsJobHandler(
			StatementGenerateJobInliner statementGenerateJobInliner,
			@Autowired(required = false) Clock clock) {
		this.statementGenerateJobInliner = statementGenerateJobInliner;
		this.clock = clock == null ? Clock.system(StatementTimeWindowCalculator.SHANGHAI) : clock;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		if (payload == null) {
			return;
		}
		Long companyId = parseLong(payload.get("company_id"));
		if (companyId == null || companyId <= 0) {
			return;
		}
		StatementPeriodValue period = parsePeriod(payload.get("period"));
		if (period == null) {
			return;
		}
		Long lastEndTime = parseLong(payload.get("last_end_time"));
		if (lastEndTime == null) {
			return;
		}
		String merchantType = str(payload.get("merchant_type"));
		if (merchantType.isEmpty()) {
			return;
		}
		if ("distributor".equals(merchantType)) {
			Long distributorId = parseLong(payload.get("distributor_id"));
			if (distributorId == null || distributorId <= 0) {
				return;
			}
			statementGenerateJobInliner.runJob(clock, companyId, distributorId, period, lastEndTime, merchantType);
			return;
		}
		if ("supplier".equals(merchantType)) {
			Long supplierId = parseLong(payload.get("supplier_id"));
			if (supplierId == null || supplierId <= 0) {
				return;
			}
			statementGenerateJobInliner.runJob(clock, companyId, supplierId, period, lastEndTime, merchantType);
		}
	}

	private static Long parseLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static StatementPeriodValue parsePeriod(Object periodObj) {
		if (!(periodObj instanceof List<?> list) || list.size() < 2) {
			return null;
		}
		Object n0 = list.get(0);
		Object u0 = list.get(1);
		if (!(n0 instanceof Number)) {
			return null;
		}
		int n = ((Number) n0).intValue();
		String u = u0 == null ? null : u0.toString().toLowerCase(Locale.ROOT);
		if (u == null
				|| u.isEmpty()
				|| (!u.equals("day") && !u.equals("week") && !u.equals("month"))
				|| n <= 0) {
			return null;
		}
		return new StatementPeriodValue(n, u);
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
