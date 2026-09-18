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

package cn.shopex.ecshopx.companys.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterBasicsUserProcessPort;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EmployeeJobHandler implements DispatchHandler {

	private final MarketingCenterBasicsUserProcessPort marketingCenterBasicsUserProcessPort;

	public EmployeeJobHandler(MarketingCenterBasicsUserProcessPort marketingCenterBasicsUserProcessPort) {
		this.marketingCenterBasicsUserProcessPort = marketingCenterBasicsUserProcessPort;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Map<String, Object> normalized = normalizePayload(payload);
		long companyId = parseCompanyId(normalized.get("company_id"));
		marketingCenterBasicsUserProcessPort.processUser(companyId, normalized);
	}

	private static Map<String, Object> normalizePayload(Map<String, Object> payload) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : payload.entrySet()) {
			out.put(e.getKey(), normalizeFieldValue(e.getValue()));
		}
		return out;
	}

	private static Object normalizeFieldValue(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof Map<?, ?> m) {
			if (m.isEmpty()) {
				return "";
			}
			LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				inner.put(String.valueOf(e.getKey()), normalizeFieldValue(e.getValue()));
			}
			return inner;
		}
		if (v instanceof Iterable<?> it) {
			if (!it.iterator().hasNext()) {
				return "";
			}
			return String.valueOf(v);
		}
		if (v instanceof Boolean b) {
			return b ? "1" : "0";
		}
		if (v instanceof Number n) {
			return numberToPlainString(n);
		}
		return String.valueOf(v);
	}

	private static String numberToPlainString(Number n) {
		if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) {
			return String.valueOf(n.longValue());
		}
		if (n instanceof BigInteger bi) {
			return bi.toString();
		}
		if (n instanceof BigDecimal bd) {
			return bd.stripTrailingZeros().toPlainString();
		}
		double d = n.doubleValue();
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			return "";
		}
		if (d == Math.rint(d) && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
			return String.valueOf((long) d);
		}
		return n.toString();
	}

	private static long parseCompanyId(Object raw) {
		if (raw == null || "".equals(raw)) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		return Long.parseLong(s);
	}
}
