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

package cn.shopex.ecshopx.orders.service.statement.export;

import cn.shopex.ecshopx.orders.domain.Statements;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StatementsSummarizedExportQuerySupport {

	private StatementsSummarizedExportQuerySupport() {}

	public static LambdaQueryWrapper<Statements> toWrapper(LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<Statements> w = new LambdaQueryWrapper<>();
		for (Map.Entry<String, Object> e : filter.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			if (val == null) {
				continue;
			}
			switch (key) {
				case "merchant_type" -> w.eq(Statements::getMerchantType, String.valueOf(val).trim());
				case "company_id" -> w.eq(Statements::getCompanyId, toLong(val));
				case "distributor_id" -> w.eq(Statements::getDistributorId, toLong(val));
				case "merchant_id" -> w.eq(Statements::getMerchantId, toLong(val));
				case "supplier_id" -> w.eq(Statements::getSupplierId, toLong(val));
				case "statement_status" -> {
					if (val instanceof Collection<?> col) {
						List<String> strs = new ArrayList<>();
						for (Object o : col) {
							if (o != null) {
								strs.add(String.valueOf(o).trim());
							}
						}
						if (!strs.isEmpty()) {
							w.in(Statements::getStatementStatus, strs);
						}
					} else {
						w.eq(Statements::getStatementStatus, String.valueOf(val).trim());
					}
				}
				case "start_time|gt" -> w.gt(Statements::getStartTime, toInt(val));
				case "end_time|lt" -> w.lt(Statements::getEndTime, toInt(val));
				default -> {
					// ignore unknown keys
				}
			}
		}
		return w;
	}

	private static long toLong(Object val) {
		if (val instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(val).trim());
	}

	private static int toInt(Object val) {
		if (val instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(val).trim());
	}
}
