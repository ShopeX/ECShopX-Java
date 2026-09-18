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

import cn.shopex.ecshopx.common.port.payment.AdapayPaymentSettingsReadPort;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 测试 profile 下替换 {@link AdapayPaymentSettingsReadPort}，返回可驱动费率解析的最小 Map。
 */
@Slf4j
public class NoopAdapayPaymentSettingsReadPort implements AdapayPaymentSettingsReadPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public Map<String, Object> getPaymentSetting(long companyId) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][adapay-payment-setting] called#{}, args={}", n, Arrays.toString(new Object[] {companyId}));
		var m = new HashMap<String, Object>();
		m.put("wxpay_fee_type", "1");
		m.put("wx_pub_1", new BigDecimal("0.6"));
		m.put("alipay_call", new BigDecimal("0.5"));
		m.put("alipay_fee_type", "1");
		m.put("alipay_qr_1", new BigDecimal("0.4"));
		return m;
	}
}
