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

package cn.shopex.ecshopx.aliyunsms.cron.testcfg;

import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsSignClient;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsTemplateClient;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySmsTemplateListClient;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsMassTaskSendClient;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySendDetailsClient;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsGetSmsSignClient;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsGetSmsTemplateClient;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsQuerySmsTemplateListClient;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsMassTaskSendClient;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsAddSmsBatchRecordJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsAddSmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsAddSmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsDeleteSmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsDeleteSmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsModifySmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsModifySmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsQuerySendDetailsClient;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsQuerySendDetailJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsQuerySmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsSyncSmsSignsJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsQuerySmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.cron.mock.NoopAliyunsmsSyncSmsTemplatesJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsBatchRecordJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDeleteSmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDeleteSmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsModifySmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsModifySmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySendDetailJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsSyncSmsSignsJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsSyncSmsTemplatesJobDispatchPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务测试专用：将阿里云 GetSmsSign / GetSmsTemplate / QuerySendDetails 调用替换为可观测 Noop，避免 test-cron 下外呼。
 */
@Profile("test-cron")
@Configuration("aliyunsmsCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 覆盖真实 {@link cn.shopex.ecshopx.aliyunsms.integration.DysmsapiAliyunsmsGetSmsSignClient}，避免打阿里云。
	 */
	@Bean
	@Primary
	public AliyunsmsGetSmsSignClient aliyunsmsGetSmsSignClient() {
		return new NoopAliyunsmsGetSmsSignClient();
	}

	/**
	 * 覆盖真实 {@link cn.shopex.ecshopx.aliyunsms.integration.DysmsapiAliyunsmsGetSmsTemplateClient}，避免打阿里云。
	 */
	@Bean
	@Primary
	public AliyunsmsGetSmsTemplateClient aliyunsmsGetSmsTemplateClient() {
		return new NoopAliyunsmsGetSmsTemplateClient();
	}

	@Bean
	@Primary
	public AliyunsmsQuerySmsTemplateListClient aliyunsmsQuerySmsTemplateListClient() {
		return new NoopAliyunsmsQuerySmsTemplateListClient();
	}

	/**
	 * 覆盖真实 {@link cn.shopex.ecshopx.aliyunsms.integration.DysmsapiAliyunsmsQuerySendDetailsClient}，避免打阿里云。
	 */
	@Bean
	@Primary
	public AliyunsmsQuerySendDetailsClient aliyunsmsQuerySendDetailsClient() {
		return new NoopAliyunsmsQuerySendDetailsClient();
	}

	/**
	 * 覆盖真实 {@link cn.shopex.ecshopx.aliyunsms.integration.DysmsapiAliyunsmsMassTaskSendClient}，避免 test-cron 下打阿里云 SendSms。
	 */
	@Bean("aliyunsms-mass-task-send")
	@Primary
	public AliyunsmsMassTaskSendClient aliyunsmsMassTaskSendClient() {
		return new NoopAliyunsmsMassTaskSendClient();
	}

	/**
	 * 覆盖真实 {@link AliyunsmsModifySmsSignJobDispatchPublisher}，避免 test-cron 下入队外呼链。
	 */
	@Bean
	@Primary
	public AliyunsmsModifySmsSignJobDispatchPublisher aliyunsmsModifySmsSignJobDispatchPublisher() {
		return new NoopAliyunsmsModifySmsSignJobDispatchPublisher();
	}

	/**
	 * 覆盖真实 {@link AliyunsmsModifySmsTemplateJobDispatchPublisher}，避免 test-cron 下入队外呼链。
	 */
	@Bean
	@Primary
	public AliyunsmsModifySmsTemplateJobDispatchPublisher aliyunsmsModifySmsTemplateJobDispatchPublisher() {
		return new NoopAliyunsmsModifySmsTemplateJobDispatchPublisher();
	}

	/**
	 * 覆盖真实 {@link AliyunsmsAddSmsSignJobDispatchPublisher}，避免 test-cron 下走 Bus 外呼链。
	 */
	@Bean
	@Primary
	public AliyunsmsAddSmsSignJobDispatchPublisher aliyunsmsAddSmsSignJobDispatchPublisher() {
		return new NoopAliyunsmsAddSmsSignJobDispatchPublisher();
	}

	/**
	 * 覆盖真实 {@link AliyunsmsAddSmsTemplateJobDispatchPublisher}，避免 test-cron 下经 Bus 的外呼链。
	 */
	@Bean
	@Primary
	public AliyunsmsAddSmsTemplateJobDispatchPublisher aliyunsmsAddSmsTemplateJobDispatchPublisher() {
		return new NoopAliyunsmsAddSmsTemplateJobDispatchPublisher();
	}

	/**
	 * 覆盖真实 {@link AliyunsmsAddSmsBatchRecordJobDispatchPublisher}，避免 test-cron 下入队落库链。
	 */
	@Bean
	@Primary
	public AliyunsmsAddSmsBatchRecordJobDispatchPublisher aliyunsmsAddSmsBatchRecordJobDispatchPublisher() {
		return new NoopAliyunsmsAddSmsBatchRecordJobDispatchPublisher();
	}

	/**
	 * 覆盖真实 {@link AliyunsmsDeleteSmsSignJobDispatchPublisher}，避免 test-cron 下入队外呼链。
	 */
	@Bean
	@Primary
	public AliyunsmsDeleteSmsSignJobDispatchPublisher aliyunsmsDeleteSmsSignJobDispatchPublisher() {
		return new NoopAliyunsmsDeleteSmsSignJobDispatchPublisher();
	}

	/**
	 * 覆盖真实 {@link AliyunsmsDeleteSmsTemplateJobDispatchPublisher}，避免 test-cron 下入队外呼链。
	 */
	@Bean
	@Primary
	public AliyunsmsDeleteSmsTemplateJobDispatchPublisher aliyunsmsDeleteSmsTemplateJobDispatchPublisher() {
		return new NoopAliyunsmsDeleteSmsTemplateJobDispatchPublisher();
	}

	/**
	 * 覆盖真实 {@link AliyunsmsQuerySmsSignJobDispatchPublisher}，避免 test-cron 下入队外呼链。
	 */
	@Bean
	@Primary
	public AliyunsmsQuerySmsSignJobDispatchPublisher aliyunsmsQuerySmsSignJobDispatchPublisher() {
		return new NoopAliyunsmsQuerySmsSignJobDispatchPublisher();
	}

	@Bean
	@Primary
	public AliyunsmsSyncSmsSignsJobDispatchPublisher aliyunsmsSyncSmsSignsJobDispatchPublisher() {
		return new NoopAliyunsmsSyncSmsSignsJobDispatchPublisher();
	}

	/**
	 * 覆盖真实 {@link AliyunsmsQuerySmsTemplateJobDispatchPublisher}，避免 test-cron 下入队外呼链。
	 */
	@Bean
	@Primary
	public AliyunsmsQuerySmsTemplateJobDispatchPublisher aliyunsmsQuerySmsTemplateJobDispatchPublisher() {
		return new NoopAliyunsmsQuerySmsTemplateJobDispatchPublisher();
	}

	@Bean
	@Primary
	public AliyunsmsSyncSmsTemplatesJobDispatchPublisher aliyunsmsSyncSmsTemplatesJobDispatchPublisher() {
		return new NoopAliyunsmsSyncSmsTemplatesJobDispatchPublisher();
	}

	/**
	 * 覆盖真实 query-send-detail 发布器，避免 test-cron 下入队与外呼链。
	 */
	@Bean
	@Primary
	public AliyunsmsQuerySendDetailJobDispatchPublisher aliyunsmsQuerySendDetailJobDispatchPublisher() {
		return new NoopAliyunsmsQuerySendDetailJobDispatchPublisher();
	}
}
