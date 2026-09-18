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

package cn.shopex.ecshopx.adapay.cron.testcfg;

import cn.shopex.ecshopx.common.cron.mock.NoopAdapayAutoCashConfigReadWritePort;
import cn.shopex.ecshopx.common.port.adapay.AdapayAutoCashConfigReadWritePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用 bean 覆盖配置；仅在 spring.profiles.active 包含 test-cron 时生效。
 * 以 Noop 覆盖自动提现配置 Redis 端口，避免阶段 4 与真实环境交叉污染。
 */
@Profile("test-cron")
@Configuration("adapayCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 覆盖 {@code AdapayAutoCashConfigReadWritePortImpl} 对业务 Redis 的读写。
	 */
	@Bean
	@Primary
	public AdapayAutoCashConfigReadWritePort adapayAutoCashConfigReadWritePort() {
		return new NoopAdapayAutoCashConfigReadWritePort();
	}
}
