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

import cn.shopex.ecshopx.common.cron.CommunitySettingReadPort;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 4 对拍用：返回与合票默认结构一致，不访问 Redis；bean-alias {@code community-setting-read}。
 */
@Slf4j
public class NoopCommunitySettingReadPort implements CommunitySettingReadPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public Map<String, Object> getSetting(long companyId, boolean distributorBranch, Object distributorIdRaw) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][community-setting-read] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {companyId, distributorBranch, distributorIdRaw}));
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("condition_type", "num");
		merged.put("condition_money", 0);
		merged.put("aggrement", "");
		merged.put("explanation", "");
		merged.put("rebate_ratio", 0);
		return merged;
	}
}
