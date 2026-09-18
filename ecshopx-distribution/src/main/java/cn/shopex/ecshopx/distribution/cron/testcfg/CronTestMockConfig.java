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

package cn.shopex.ecshopx.distribution.cron.testcfg;

import cn.shopex.ecshopx.common.cron.mock.NoopAddDistributorItemsJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopDistributorItemsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopDistributorWhiteListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AddDistributorItemsJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributorItemsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributorWhiteListExportFileJobDispatchPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test-cron")
@Configuration
public class CronTestMockConfig {

	@Bean
	@Primary
	public AddDistributorItemsJobDispatchPublisher addDistributorItemsJobDispatchPublisher() {
		return new NoopAddDistributorItemsJobDispatchPublisher();
	}

	@Bean
	@Primary
	public DistributorItemsExportFileJobDispatchPublisher distributorItemsExportFileJobDispatchPublisher() {
		return new NoopDistributorItemsExportFileJobDispatchPublisher();
	}

	@Bean
	@Primary
	public DistributorWhiteListExportFileJobDispatchPublisher distributorWhiteListExportFileJobDispatchPublisher() {
		return new NoopDistributorWhiteListExportFileJobDispatchPublisher();
	}
}
