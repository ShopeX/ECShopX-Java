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

package cn.shopex.ecshopx.common.cron.mock;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下委托给本类，替代真实汇付 qry001/cash01 HTTP；阶段 4 用 {@code [cron-mock][hf-acou-qry001]} / {@code [cron-mock][hf-acou-cash01]} grep 断言。
 */
@Slf4j
public class NoopHfPayAcouJsonPostClient {

	private static final String FIXED_BALANCE_YUAN = "100.00";
	private static final String FIXED_ORDER_ID = "NOOP-ORDER-1";

	private final AtomicInteger qryCount = new AtomicInteger();
	private final AtomicInteger cashCount = new AtomicInteger();

	public Map<String, Object> qry001(Map<String, Object> setting, Map<String, Object> payload) {
		int n = qryCount.incrementAndGet();
		log.info(
				"[cron-mock][hf-acou-qry001] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {setting, payload}));
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("resp_code", "C00000");
		m.put("balance", FIXED_BALANCE_YUAN);
		return m;
	}

	/**
	 * 固定 C00001 等字段，与 {@code HfpayDistributorWithdrawDispatchExecutionService} 写回 {@code hfpay_cash_record} 终态的语义对齐。
	 */
	public Map<String, Object> cash01(Map<String, Object> setting, Map<String, Object> payload) {
		int n = cashCount.incrementAndGet();
		log.info(
				"[cron-mock][hf-acou-cash01] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {setting, payload}));
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("resp_code", "C00001");
		m.put("fee_amt", "0.00");
		m.put("order_id", FIXED_ORDER_ID);
		m.put("order_date", "20240101");
		m.put("resp_desc", "noop-ok");
		return m;
	}
}
