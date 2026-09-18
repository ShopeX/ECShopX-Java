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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.CompanysBundleDispatchJobNames;
import cn.shopex.ecshopx.companys.dispatch.RecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonActiveArticleRecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonCommissionRecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonGiveCouponsRecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonPopularizeRecordStatisticsJobHandler;
import cn.shopex.ecshopx.companys.dispatch.SalespersonRecordStatisticsJobHandler;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecordStatisticsJobRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final RecordStatisticsJobHandler recordStatisticsJobHandler;
	private final SalespersonRecordStatisticsJobHandler salespersonRecordStatisticsJobHandler;
	private final SalespersonActiveArticleRecordStatisticsJobHandler
			salespersonActiveArticleRecordStatisticsJobHandler;
	private final SalespersonCommissionRecordStatisticsJobHandler
			salespersonCommissionRecordStatisticsJobHandler;
	private final SalespersonPopularizeRecordStatisticsJobHandler
			salespersonPopularizeRecordStatisticsJobHandler;
	private final SalespersonGiveCouponsRecordStatisticsJobHandler
			salespersonGiveCouponsRecordStatisticsJobHandler;

	public RecordStatisticsJobRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			RecordStatisticsJobHandler recordStatisticsJobHandler,
			SalespersonRecordStatisticsJobHandler salespersonRecordStatisticsJobHandler,
			SalespersonActiveArticleRecordStatisticsJobHandler salespersonActiveArticleRecordStatisticsJobHandler,
			SalespersonCommissionRecordStatisticsJobHandler salespersonCommissionRecordStatisticsJobHandler,
			SalespersonPopularizeRecordStatisticsJobHandler salespersonPopularizeRecordStatisticsJobHandler,
			SalespersonGiveCouponsRecordStatisticsJobHandler salespersonGiveCouponsRecordStatisticsJobHandler) {
		this.dispatchRegistry = dispatchRegistry;
		this.recordStatisticsJobHandler = recordStatisticsJobHandler;
		this.salespersonRecordStatisticsJobHandler = salespersonRecordStatisticsJobHandler;
		this.salespersonActiveArticleRecordStatisticsJobHandler = salespersonActiveArticleRecordStatisticsJobHandler;
		this.salespersonCommissionRecordStatisticsJobHandler = salespersonCommissionRecordStatisticsJobHandler;
		this.salespersonPopularizeRecordStatisticsJobHandler = salespersonPopularizeRecordStatisticsJobHandler;
		this.salespersonGiveCouponsRecordStatisticsJobHandler = salespersonGiveCouponsRecordStatisticsJobHandler;
	}

	@PostConstruct
	public void registerRecordStatisticsJob() {
		dispatchRegistry.registerJob(CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB, recordStatisticsJobHandler);
		dispatchRegistry.registerJob(
				CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_ACTIVE_ARTICLE_SCHEDULE,
				recordStatisticsJobHandler);
		dispatchRegistry.registerJob(
				CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_COMMISSION_SCHEDULE,
				recordStatisticsJobHandler);
		dispatchRegistry.registerJob(
				CompanysBundleDispatchJobNames.SALESPERSON_RECORD_STATISTICS_JOB,
				salespersonRecordStatisticsJobHandler);
		dispatchRegistry.registerJob(
				CompanysBundleDispatchJobNames.SALESPERSON_ACTIVE_ARTICLE_RECORD_STATISTICS_JOB,
				salespersonActiveArticleRecordStatisticsJobHandler);
		dispatchRegistry.registerJob(
				CompanysBundleDispatchJobNames.SALESPERSON_COMMISSION_RECORD_STATISTICS_JOB,
				salespersonCommissionRecordStatisticsJobHandler);
		dispatchRegistry.registerJob(
				CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_POPULARIZE_SCHEDULE,
				recordStatisticsJobHandler);
		dispatchRegistry.registerJob(
				CompanysBundleDispatchJobNames.SALESPERSON_POPULARIZE_RECORD_STATISTICS_JOB,
				salespersonPopularizeRecordStatisticsJobHandler);
		dispatchRegistry.registerJob(
				CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_GIVE_COUPONS_SCHEDULE,
				recordStatisticsJobHandler);
		dispatchRegistry.registerJob(
				CompanysBundleDispatchJobNames.SALESPERSON_GIVE_COUPONS_RECORD_STATISTICS_JOB,
				salespersonGiveCouponsRecordStatisticsJobHandler);
	}
}
