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

import cn.shopex.ecshopx.orders.domain.StatementDetails;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StatementDetailsExportQuerySupport {

	private StatementDetailsExportQuerySupport() {}

	public static LambdaQueryWrapper<StatementDetails> toWrapper(LinkedHashMap<String, Object> filter) {
		LambdaQueryWrapper<StatementDetails> w = new LambdaQueryWrapper<>();
		for (Map.Entry<String, Object> e : filter.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			if (val == null) {
				continue;
			}
			if ("merchant_type".equals(key)) {
				continue;
			}
			switch (key) {
				case "company_id" -> w.eq(StatementDetails::getCompanyId, toLong(val));
				case "statement_id" -> {
					if (val instanceof Number n) {
						w.eq(StatementDetails::getStatementId, n.longValue());
					} else {
						w.apply("statement_id = {0}", String.valueOf(val));
					}
				}
				case "created|gt" -> w.gt(StatementDetails::getCreated, toInt(val));
				case "created|lt" -> w.lt(StatementDetails::getCreated, toInt(val));
				case "distributor_id" -> w.eq(StatementDetails::getDistributorId, toLong(val));
				case "merchant_id" -> w.eq(StatementDetails::getMerchantId, toLong(val));
				case "supplier_id" -> w.eq(StatementDetails::getSupplierId, toLong(val));
				case "order_id|contains" -> {
					String pattern = String.valueOf(val).trim();
					if (!pattern.isEmpty()) {
						String escaped = escapeLikeForContains(pattern);
						w.apply("CAST(order_id AS CHAR) LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'", escaped);
					}
				}
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

	private static String escapeLikeForContains(String raw) {
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
