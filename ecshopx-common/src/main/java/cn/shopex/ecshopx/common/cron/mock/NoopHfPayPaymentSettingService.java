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

import cn.shopex.ecshopx.common.hfpay.payment.HfPayPaymentSettingLoadPort;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 测试用仅实现 {@link HfPayPaymentSettingLoadPort}，避免读 Redis/写证书。日志格式供阶段 4 grep：{@code
 * [cron-mock][hfpay-payment-setting]}。仅在 {@code test-cron} 下经 CronTestMockConfig 装配。
 */
@Slf4j
public class NoopHfPayPaymentSettingService implements HfPayPaymentSettingLoadPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public Map<String, Object> loadForCompany(long companyId) {
		int n = callCount.incrementAndGet();
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("is_open", "true");
		m.put("mer_cust_id", "6666000");
		m.put("acct_id", "0001");
		m.put("user_cust_id", "u_test");
		m.put("pfx_password", "noop");
		m.put("pfx_file", "Zg==");
		m.put("ca_pfx_file", "LS0t");
		m.put("oca31_pfx_file", "LS0t");
		log.info(
				"[cron-mock][hfpay-payment-setting] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {companyId, m.keySet()}));
		return m;
	}
}
