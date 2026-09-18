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

import cn.shopex.ecshopx.common.port.point.PointMemberRuleReadPort;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下替代真实规则读取；返回稳定 Map 供快照与单测。
 */
@Slf4j
public class NoopPointMemberRuleReadPort implements PointMemberRuleReadPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public Map<String, Object> getPointRule(long companyId) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][point-member-rule] called#{}, args={}", n, Arrays.toString(new Object[] {companyId}));
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("isOpenMemberPoint", "true");
		m.put("gain_point", 1);
		m.put("gain_limit", 9999999);
		m.put("gain_time", 7);
		m.put("access", "order");
		m.put("include_freight", false);
		return m;
	}
}
