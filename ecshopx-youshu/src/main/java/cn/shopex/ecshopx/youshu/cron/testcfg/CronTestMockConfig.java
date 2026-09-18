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

package cn.shopex.ecshopx.youshu.cron.testcfg;

import cn.shopex.ecshopx.common.cron.mock.NoopWxappDataCubeVisitDistributionPort;
import cn.shopex.ecshopx.common.cron.mock.NoopWxappDataCubeVisitPagePort;
import cn.shopex.ecshopx.common.cron.mock.NoopYoushuAnalysisAddOrderSumPort;
import cn.shopex.ecshopx.common.cron.mock.NoopYoushuAnalysisAddWxappVisitDistributionPort;
import cn.shopex.ecshopx.common.cron.mock.NoopYoushuAnalysisAddWxappVisitPagePort;
import cn.shopex.ecshopx.common.cron.youshu.DnsAwareStubYoushuDataSourceApiPort;
import cn.shopex.ecshopx.common.cron.wechat.WxappDataCubeVisitDistributionPort;
import cn.shopex.ecshopx.common.cron.wechat.WxappDataCubeVisitPagePort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddOrderSumPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddWxappVisitDistributionPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddWxappVisitPagePort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用；仅 spring.profiles.active 含 test-cron 时生效。覆盖微信 DataCube 与有数侧 HTTP
 * Port，避免对公网实调。
 */
@Profile("test-cron")
@Configuration
public class CronTestMockConfig {

	/**
	 * 替换 {@code WxappDataCubeVisitPagePortImpl} 对微信 DataCube 的真实 HTTP。
	 */
	@Bean
	@Primary
	public WxappDataCubeVisitPagePort wxappDataCubeVisitPagePort() {
		return new NoopWxappDataCubeVisitPagePort();
	}

	/**
	 * 替换 {@code WxappDataCubeVisitDistributionPortImpl} 对微信 DataCube 访问分布的真实 HTTP。
	 */
	@Bean
	@Primary
	public WxappDataCubeVisitDistributionPort wxappDataCubeVisitDistributionPort() {
		return new NoopWxappDataCubeVisitDistributionPort();
	}

	/**
	 * 替换 {@code YoushuDataSourceApiClient} 对有数 data source API 的 HTTP。
	 */
	@Bean
	@Primary
	public YoushuDataSourceApiPort youshuDataSourceApiPort() {
		return new DnsAwareStubYoushuDataSourceApiPort();
	}

	/**
	 * 替换 {@code YoushuAnalysisAddWxappVisitPageClient} 对有数 analysis 的 HTTP。
	 */
	@Bean
	@Primary
	public YoushuAnalysisAddWxappVisitPagePort youshuAnalysisAddWxappVisitPagePort() {
		return new NoopYoushuAnalysisAddWxappVisitPagePort();
	}

	/**
	 * 替换 {@code YoushuAnalysisAddWxappVisitDistributionClient} 对有数 analysis 的 HTTP。
	 */
	@Bean
	@Primary
	public YoushuAnalysisAddWxappVisitDistributionPort youshuAnalysisAddWxappVisitDistributionPort() {
		return new NoopYoushuAnalysisAddWxappVisitDistributionPort();
	}

	/**
	 * 替换 {@code YoushuAnalysisAddOrderSumClient} 对有数 {@code add_order_sum} 的 HTTP。
	 */
	@Bean
	@Primary
	public YoushuAnalysisAddOrderSumPort youshuAnalysisAddOrderSumPort() {
		return new NoopYoushuAnalysisAddOrderSumPort();
	}
}
