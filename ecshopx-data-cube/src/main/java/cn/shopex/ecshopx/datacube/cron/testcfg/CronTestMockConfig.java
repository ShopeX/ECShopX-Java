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

package cn.shopex.ecshopx.datacube.cron.testcfg;

import cn.shopex.ecshopx.common.cron.mock.NoopDeliveryStaffDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DeliveryStaffDataExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.datacube.cron.mock.NoopDistributorDataJobEnqueuePort;
import cn.shopex.ecshopx.datacube.cron.mock.NoopGoodsStatisticJobEnqueuePort;
import cn.shopex.ecshopx.datacube.cron.mock.NoopMerchantStatisticJobEnqueuePort;
import cn.shopex.ecshopx.datacube.cron.mock.NoopStatisticJobEnqueuePort;
import cn.shopex.ecshopx.datacube.service.companydata.StatisticJobEnqueuePort;
import cn.shopex.ecshopx.datacube.service.distributordata.DistributorDataJobEnqueuePort;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticJobEnqueuePort;
import cn.shopex.ecshopx.datacube.service.merchantdata.MerchantStatisticJobEnqueuePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用 bean 覆盖配置；仅在 spring.profiles.active 包含 test-cron 时生效。
 */
@Profile("test-cron")
@Configuration("datacubeCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.DistributorDataJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 真投递门店日统计 Bus job。
	 */
	@Bean
	@Primary
	public DistributorDataJobEnqueuePort distributorDataJobEnqueuePort() {
		return new NoopDistributorDataJobEnqueuePort();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.GoodsStatisticJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 真投递商品日统计 Bus job。
	 */
	@Bean
	@Primary
	public GoodsStatisticJobEnqueuePort goodsStatisticJobEnqueuePort() {
		return new NoopGoodsStatisticJobEnqueuePort();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.MerchantStatisticJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 真投递商户日统计 Bus job。
	 */
	@Bean
	@Primary
	public MerchantStatisticJobEnqueuePort merchantStatisticJobEnqueuePort() {
		return new NoopMerchantStatisticJobEnqueuePort();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.StatisticJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 真投递全站商城日统计 Bus job。
	 */
	@Bean
	@Primary
	public StatisticJobEnqueuePort statisticJobEnqueuePort() {
		return new NoopStatisticJobEnqueuePort();
	}

	/**
	 * 替换生产 {@link cn.shopex.ecshopx.config.DeliveryStaffDataExportFileJobDispatchPublisherImpl}，避免 test-cron 向 Redis slow
	 * 真投递配送员数据导出 file job。
	 */
	@Bean
	@Primary
	public DeliveryStaffDataExportFileJobDispatchPublisher deliveryStaffDataExportFileJobDispatchPublisher() {
		return new NoopDeliveryStaffDataExportFileJobDispatchPublisher();
	}
}
