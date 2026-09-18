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

import cn.shopex.ecshopx.common.cron.hfpay.HfPayQry008Port;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下替代真实 qry008 外呼；阶段 4 通过 grep {@code [cron-mock][hfpay-qry008]} 检索。
 */
@Slf4j
public class NoopHfPayQry008Port implements HfPayQry008Port {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public Map<String, Object> qry008(Map<String, Object> setting, Map<String, Object> payload) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][hfpay-qry008] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {setting, payload}));
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("resp_code", "C00001");
		return m;
	}
}
