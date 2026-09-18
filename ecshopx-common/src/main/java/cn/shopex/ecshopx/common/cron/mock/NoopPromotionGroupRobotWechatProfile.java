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

import cn.shopex.ecshopx.common.cron.GroupRobotWechatInfo;
import cn.shopex.ecshopx.common.cron.PromotionGroupRobotWechatProfilePort;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link PromotionGroupRobotWechatProfilePort} 的 Noop 实现；阶段 4 通过 {@code [cron-mock][wechat-robot]} 断言。
 */
@Slf4j
public class NoopPromotionGroupRobotWechatProfile implements PromotionGroupRobotWechatProfilePort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public List<GroupRobotWechatInfo> getRandUserInfo(int limit) {
		int n = callCount.incrementAndGet();
		log.info("[cron-mock][wechat-robot] called#{}, args={}", n, Arrays.toString(new Object[] { limit }));
		return Collections.emptyList();
	}
}
