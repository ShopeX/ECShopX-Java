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

package cn.shopex.ecshopx.popularize.cron.testcfg;

import cn.shopex.ecshopx.common.cron.PopularizeBrokerageSettleRedisPort;
import cn.shopex.ecshopx.common.cron.mock.NoopPopularizeBrokerageSettleRedisPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试环境覆盖：在 test-cron 下用 Noop 挡掉不影响 mysqldump 的副作用；真实 DB 写入仍由业务与集成测试验证。
 * 达摩相关 Noop（如 {@code dmCrmManualPointChangePort}）由 orders 模块 {@code CronTestMockConfig} 统一提供，避免 Bean 重复定义。
 */
@Profile("test-cron")
@Configuration("popularizeCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.popularize.port.PopularizeBrokerageSettleRedisPortImpl}，避免阶段 4 对 Redis 真写；断言依赖 [cron-mock][popularize-brokerage-redis]。
	 */
	@Bean
	@Primary
	public PopularizeBrokerageSettleRedisPort popularizeBrokerageSettleRedisPort() {
		return new NoopPopularizeBrokerageSettleRedisPort();
	}
}
