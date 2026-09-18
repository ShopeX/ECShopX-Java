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

package cn.shopex.ecshopx.companys.cron.testcfg;

import cn.shopex.ecshopx.common.cron.CompanysRecordStatisticsRedisPort;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogPort;
import cn.shopex.ecshopx.common.cron.mock.NoopCompanysRecordStatisticsRedisPort;
import cn.shopex.ecshopx.common.cron.mock.NoopSalespersonStatisticsCronLogPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试 profile 下将不影响 mysqldump 的副作用 bean 换为 Noop，便于双端对账。
 */
@Profile("test-cron")
@Configuration("companysCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 覆盖 {@link cn.shopex.ecshopx.companys.service.statistics.CompanysRecordStatisticsRedisPortImpl}，避免阶段 4 真连业务 Redis。
	 */
	@Bean
	@Primary
	public CompanysRecordStatisticsRedisPort companysRecordStatisticsRedisPort() {
		return new NoopCompanysRecordStatisticsRedisPort();
	}

	/**
	 * 覆盖 {@link cn.shopex.ecshopx.salesperson.service.Slf4jSalespersonStatisticsCronLogPort}，以统一 {@code [cron-mock][…]} 打点作断言。
	 */
	@Bean
	@Primary
	public SalespersonStatisticsCronLogPort salespersonStatisticsCronLogPort() {
		return new NoopSalespersonStatisticsCronLogPort();
	}
}
