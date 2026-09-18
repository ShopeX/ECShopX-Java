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

package cn.shopex.ecshopx.community.cron.testcfg;

import cn.shopex.ecshopx.common.cron.CommunitySettingReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopCancelActivityOrdersJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopCommunityNormalOrderActivityExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopCommunitySettingReadPort;
import cn.shopex.ecshopx.common.dispatch.CancelActivityOrdersJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.CommunityNormalOrderActivityExportFileJobDispatchPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用 bean 覆盖配置；仅在 spring.profiles.active 包含 test-cron 时生效。
 */
@Profile("test-cron")
@Configuration("communityCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 替换真实 {@code CommunitySettingService} 的只读接口，避免阶段 4 读共享 Redis 漂移。
	 */
	@Bean
	@Primary
	public CommunitySettingReadPort communitySettingReadPort() {
		return new NoopCommunitySettingReadPort();
	}

	@Bean
	@Primary
	public CancelActivityOrdersJobDispatchPublisher cancelActivityOrdersJobDispatchPublisher() {
		return new NoopCancelActivityOrdersJobDispatchPublisher();
	}

	@Bean
	@Primary
	public CommunityNormalOrderActivityExportFileJobDispatchPublisher
			communityNormalOrderActivityExportFileJobDispatchPublisher() {
		return new NoopCommunityNormalOrderActivityExportFileJobDispatchPublisher();
	}
}
