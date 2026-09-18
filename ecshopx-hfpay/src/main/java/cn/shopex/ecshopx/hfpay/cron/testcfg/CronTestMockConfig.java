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

package cn.shopex.ecshopx.hfpay.cron.testcfg;

import cn.shopex.ecshopx.common.cron.hfpay.HfPayQry008Port;
import cn.shopex.ecshopx.common.cron.mock.NoopHfpayOrderRecordExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopHfpayTradeRecordExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopHfpayWithdrawRecordExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopHfPayAcouJsonPostClient;
import cn.shopex.ecshopx.common.cron.mock.NoopHfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.common.cron.mock.NoopHfPayQry008Port;
import cn.shopex.ecshopx.common.dispatch.HfpayOrderRecordExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.HfpayTradeRecordExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.HfpayWithdrawRecordExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayCfcaKernelService;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

/**
 * 定时任务 test-cron 下用 Noop 覆盖汇付外呼与订单号生成，避免阶段 4 对拍时真调生产；派发与 DB 回写走真实执行服务/Mapper。
 */
@Profile("test-cron")
@Configuration("hfpayCronTestMockConfig")
public class CronTestMockConfig {

	@Bean
	@Primary
	public HfpayTradeRecordExportFileJobDispatchPublisher hfpayTradeRecordExportFileJobDispatchPublisher() {
		return new NoopHfpayTradeRecordExportFileJobDispatchPublisher();
	}

	@Bean
	@Primary
	public HfpayOrderRecordExportFileJobDispatchPublisher hfpayOrderRecordExportFileJobDispatchPublisher() {
		return new NoopHfpayOrderRecordExportFileJobDispatchPublisher();
	}

	@Bean
	@Primary
	public HfpayWithdrawRecordExportFileJobDispatchPublisher hfpayWithdrawRecordExportFileJobDispatchPublisher() {
		return new NoopHfpayWithdrawRecordExportFileJobDispatchPublisher();
	}

	/**
	 * 覆盖 {@link HfPayQry008Port} 为可观测 Noop；日志别名 {@code hfpay-qry008}。其它 {@code HfPayAcouJsonPostClient} 由下方 bean 专门覆盖
	 * qry001/cash01。
	 */
	@Bean("hfpay-qry008")
	@Primary
	public HfPayQry008Port noopHfPayQry008Port() {
		return new NoopHfPayQry008Port();
	}

	/**
	 * 替换真实 {@link HfPayAcouJsonPostClient} 的 qry001/cash01，日志别名 {@code hf-acou-qry001}、{@code hf-acou-cash01}；取现 cash01 调用链同 bean。
	 */
	@Bean
	@Primary
	public HfPayAcouJsonPostClient hfPayAcouJsonPostClient(
			HfPayCfcaKernelService cfcaKernelService,
			ObjectMapper objectMapper,
			@Qualifier("hfpayAcouRestClient") RestClient hfpayAcouRestClient) {
		final NoopHfPayAcouJsonPostClient noop = new NoopHfPayAcouJsonPostClient();
		return new HfPayAcouJsonPostClient(cfcaKernelService, objectMapper, hfpayAcouRestClient) {
			@Override
			public Map<String, Object> qry001(Map<String, Object> setting, Map<String, Object> payload) {
				return noop.qry001(setting, payload);
			}

			@Override
			public Map<String, Object> cash01(Map<String, Object> setting, Map<String, Object> payload) {
				return noop.cash01(setting, payload);
			}
		};
	}

	/**
	 * 替换真实 {@link HfPayOrderApplyIdGenerator}，日志别名 {@code hfpay-order-id}；对拍时单号可重复、稳定递增。
	 */
	@Bean
	@Primary
	public HfPayOrderApplyIdGenerator hfPayOrderApplyIdGenerator(
			@Qualifier("companysRedisTemplate") StringRedisTemplate redis) {
		final NoopHfPayOrderApplyIdGenerator noop = new NoopHfPayOrderApplyIdGenerator();
		return new HfPayOrderApplyIdGenerator(redis) {
			@Override
			public String nextOrderId() {
				return noop.nextOrderId();
			}

			@Override
			public String nextApplyId() {
				return noop.nextApplyId();
			}
		};
	}
}
