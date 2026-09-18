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

import cn.shopex.ecshopx.common.port.point.PointMemberDmPointMemberInfoReadPort;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下替代会员主档只读，避免达摩分支 NPE。
 */
@Slf4j
public class NoopPointMemberDmPointMemberInfoReadPort implements PointMemberDmPointMemberInfoReadPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public Map<String, Object> getMemberInfo(long userId, long companyId) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][point-dm-member-info] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {userId, companyId}));
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("user_id", String.valueOf(userId));
		m.put("mobile", "13800000000");
		m.put("dm_card_no", "MOCK");
		return m;
	}
}
