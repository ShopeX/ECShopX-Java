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

package cn.shopex.ecshopx.systemlink.cron.testcfg;

import cn.shopex.ecshopx.common.cron.mock.NoopWdtErpInventoryPort;
import cn.shopex.ecshopx.common.cron.mock.NoopWdtErpLogisticsPort;
import cn.shopex.ecshopx.common.port.wdterp.WdtErpInventoryPort;
import cn.shopex.ecshopx.common.port.wdterp.WdtErpLogisticsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用 bean 覆盖配置；仅在 spring.profiles.active 包含 test-cron 时生效。 作用：将旺店通物流 HTTP
 * 替换为 Noop，避免阶段 4 快照对比时外呼。
 */
@Profile("test-cron")
@Configuration("systemLinkCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 替换生产 {@code WdtErpLogisticsPortImpl} 对旺店通的拉单与回写，避免 test-cron 下外呼第三方。
	 */
	@Bean
	@Primary
	public WdtErpLogisticsPort wdtErpLogisticsPort() {
		return new NoopWdtErpLogisticsPort();
	}

	/**
	 * 替换生产 {@code WdtErpInventoryPortImpl} 对旺店通库存拉取与回写，避免 test-cron 下外呼（bean-alias
	 * wdt-inventory，与 wdt-logistics 分离）。
	 */
	@Bean
	@Primary
	public WdtErpInventoryPort wdtErpInventoryPort() {
		return new NoopWdtErpInventoryPort();
	}
}
