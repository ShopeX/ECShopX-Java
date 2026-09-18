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

package cn.shopex.ecshopx.datacube.service.distributordata;

import cn.shopex.ecshopx.datacube.service.DistributorDataService;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DistributorDataStatisticAsyncExecutor {

	private final DistributorDataService distributorDataService;
	private final DistributorDataJobEnqueuePort enqueuePort;
	private final boolean syncInline;

	public DistributorDataStatisticAsyncExecutor(
			@Lazy DistributorDataService distributorDataService,
			DistributorDataJobEnqueuePort enqueuePort,
			@Value("${datacube.cron.distributor-daily-statistic.sync-inline:false}") boolean syncInline) {
		this.distributorDataService = distributorDataService;
		this.enqueuePort = enqueuePort;
		this.syncInline = syncInline;
	}

	/**
	 * 默认经 Dispatch Bus 投递 slow 队列异步执行；{@code datacube.cron.distributor-daily-statistic.sync-inline=true} 时同线程内联。
	 */
	public void runStatisticsAsync(long companyId, long distributorId, long merchantId, LocalDate countDate) {
		if (syncInline) {
			try {
				distributorDataService.runStatistics(companyId, distributorId, merchantId, countDate);
			} catch (Exception e) {
				log.error(
						"inline distributor statistics failed, companyId={}, distributorId={}, merchantId={}, countDate={}",
						companyId,
						distributorId,
						merchantId,
						countDate,
						e);
			}
			return;
		}
		try {
			enqueuePort.enqueue(companyId, distributorId, merchantId, countDate);
		} catch (Exception e) {
			log.error(
					"distributor data job enqueue failed, companyId={}, distributorId={}, merchantId={}, countDate={}",
					companyId,
					distributorId,
					merchantId,
					countDate,
					e);
		}
	}
}
