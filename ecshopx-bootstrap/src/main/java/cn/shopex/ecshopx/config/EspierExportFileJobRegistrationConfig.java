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

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EspierExportFileJobRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final EspierExportFileJobDispatchHandler espierExportFileJobDispatchHandler;
	private final OrderListExportFileJobDispatchHandler orderListExportFileJobDispatchHandler;

	public EspierExportFileJobRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			EspierExportFileJobDispatchHandler espierExportFileJobDispatchHandler,
			OrderListExportFileJobDispatchHandler orderListExportFileJobDispatchHandler) {
		this.dispatchRegistry = dispatchRegistry;
		this.espierExportFileJobDispatchHandler = espierExportFileJobDispatchHandler;
		this.orderListExportFileJobDispatchHandler = orderListExportFileJobDispatchHandler;
	}

	@PostConstruct
	public void registerEspierExportFileJob() {
		dispatchRegistry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_REGISTRATION_RECORD, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_EPIDEMIC_REGISTER, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_DATA, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_TAG_DATA, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_EXPORT_ITEMS_CODE_DATA, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_MEMBER_POINT_LOGS, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_ADMIN_MEMBER_EXPORT, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_PROMOTER_LIST, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_ORDER, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_STATIC, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_BSPAY_TRADE_DATA, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_BSPAY_WITHDRAW_DATA, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_ADAPAY_TRADE_DATA, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_OFFLINE_PAYMENT, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_STATEMENTS_SUMMARIZED, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_STATEMENTS_DETAIL, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_LUCKDRAW_LOG, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_DELIVERY_STAFF_DATA, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_CHINAUMS_DIVISION, espierExportFileJobDispatchHandler);
		dispatchRegistry.registerJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST, orderListExportFileJobDispatchHandler);
	}
}
