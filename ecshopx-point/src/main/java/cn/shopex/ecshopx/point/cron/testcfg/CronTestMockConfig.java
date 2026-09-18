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

package cn.shopex.ecshopx.point.cron.testcfg;

import cn.shopex.ecshopx.common.cron.mock.NoopPointMemberDmPointMemberInfoReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopPointMemberRuleReadPort;
import cn.shopex.ecshopx.common.port.point.PointMemberDmPointMemberInfoReadPort;
import cn.shopex.ecshopx.common.port.point.PointMemberRuleReadPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * test-cron：将「不影响 DB 快照 diff」的 Redis/会员只读替换为 Noop，供阶段 4 双端对比。
 */
@Profile("test-cron")
@Configuration("pointCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 覆盖真实 {@link PointMemberRuleReadPort}，避免读 companysRedisTemplate。
	 */
	@Bean
	@Primary
	public PointMemberRuleReadPort pointMemberRuleReadPort() {
		return new NoopPointMemberRuleReadPort();
	}

	/**
	 * 覆盖 {@link PointMemberDmPointMemberInfoReadPort}，避免达摩分支读会员库。
	 */
	@Bean
	@Primary
	public PointMemberDmPointMemberInfoReadPort pointMemberDmPointMemberInfoReadPort() {
		return new NoopPointMemberDmPointMemberInfoReadPort();
	}
}
