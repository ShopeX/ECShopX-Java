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

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncExecutorConfig {

	@Bean(name = "omeFromOmeSyncExecutor")
	public Executor omeFromOmeSyncExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("ome-from-ome-sync-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "employeePurchaseExportExecutor")
	public Executor employeePurchaseExportExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("employee-purchase-export-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "jushuitanUploadExecutor")
	public Executor jushuitanUploadExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(8);
		ex.setQueueCapacity(500);
		ex.setThreadNamePrefix("jushuitan-upload-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "vipGradeBatchExecutor")
	public Executor vipGradeBatchExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("vipgrade-batch-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "communityOrderExportExecutor")
	public Executor communityOrderExportExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("community-order-export-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "ordersFinancialSalesreportExportExecutor")
	public Executor ordersFinancialSalesreportExportExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("orders-financial-salesreport-export-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "ordersRightsExportExecutor")
	public Executor ordersRightsExportExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("orders-rights-export-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "ordersOrderExportDataExecutor")
	public Executor ordersOrderExportDataExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("orders-order-export-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "salespersonSubtaskSlowExecutor")
	public Executor salespersonSubtaskSlowExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(500);
		ex.setThreadNamePrefix("salesperson-subtask-slow-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "promotionsGiveSlowExecutor")
	public Executor promotionsGiveSlowExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(500);
		ex.setThreadNamePrefix("promotions-give-slow-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "espierUploadFileSlowExecutor")
	public Executor espierUploadFileSlowExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("espier-upload-file-slow-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "aftersalesShopApplyExecutor")
	public Executor aftersalesShopApplyExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("aftersales-shop-apply-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "aftersalesFinancialExportSlowExecutor")
	public Executor aftersalesFinancialExportSlowExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(500);
		ex.setThreadNamePrefix("aftersales-financial-export-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "dispatchBusInternalExecutor")
	public Executor dispatchBusInternalExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(500);
		ex.setThreadNamePrefix("dispatch-bus-internal-");
		ex.initialize();
		return ex;
	}

	@Bean(name = "shuyunOfflineBenefitSendExecutor")
	public Executor shuyunOfflineBenefitSendExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("shuyun-offline-benefit-send-");
		ex.initialize();
		return ex;
	}
}

