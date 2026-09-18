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

package cn.shopex.ecshopx.goods.cron.testcfg;

import cn.shopex.ecshopx.common.cron.mock.NoopEpidemicRegisterExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopExportItemsCodeDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopExportItemsDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopExportItemsTagDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopPointsmallItemsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.EpidemicRegisterExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ExportItemsCodeDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ExportItemsDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ExportItemsTagDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.PointsmallItemsExportFileJobDispatchPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test-cron")
@Configuration
public class CronTestMockConfig {

	@Bean
	@Primary
	public PointsmallItemsExportFileJobDispatchPublisher pointsmallItemsExportFileJobDispatchPublisher() {
		return new NoopPointsmallItemsExportFileJobDispatchPublisher();
	}

	@Bean
	@Primary
	public EpidemicRegisterExportFileJobDispatchPublisher epidemicRegisterExportFileJobDispatchPublisher() {
		return new NoopEpidemicRegisterExportFileJobDispatchPublisher();
	}

	@Bean
	@Primary
	public ExportItemsDataExportFileJobDispatchPublisher exportItemsDataExportFileJobDispatchPublisher() {
		return new NoopExportItemsDataExportFileJobDispatchPublisher();
	}

	@Bean
	@Primary
	public ExportItemsTagDataExportFileJobDispatchPublisher exportItemsTagDataExportFileJobDispatchPublisher() {
		return new NoopExportItemsTagDataExportFileJobDispatchPublisher();
	}

	@Bean
	@Primary
	public ExportItemsCodeDataExportFileJobDispatchPublisher exportItemsCodeDataExportFileJobDispatchPublisher() {
		return new NoopExportItemsCodeDataExportFileJobDispatchPublisher();
	}
}
