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

package cn.shopex.ecshopx.datacube.service.companydata;

import cn.shopex.ecshopx.datacube.service.CompanyDataService;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CompanyDataStatisticAsyncExecutor {

	private final CompanyDataService companyDataService;
	private final StatisticJobEnqueuePort enqueuePort;
	private final boolean syncInline;

	public CompanyDataStatisticAsyncExecutor(
			@Lazy CompanyDataService companyDataService,
			StatisticJobEnqueuePort enqueuePort,
			@Value("${datacube.cron.daily-statistics.sync-inline:false}") boolean syncInline) {
		this.companyDataService = companyDataService;
		this.enqueuePort = enqueuePort;
		this.syncInline = syncInline;
	}

	/**
	 * 默认经 Dispatch Bus 投递 slow 队列异步执行；{@code datacube.cron.daily-statistics.sync-inline=true}
	 * 时同线程内联 {@link CompanyDataService#runStatistics(long, LocalDate, String, long)}，便于与 test-cron 及
	 * sync-inline 开关对账。
	 */
	public void runStatisticsAsync(long companyId, LocalDate countDate, String orderClass, long actId) {
		if (syncInline) {
			try {
				companyDataService.runStatistics(companyId, countDate, orderClass, actId);
			} catch (Exception e) {
				log.error(
						"inline company statistics failed, companyId={}, countDate={}",
						companyId,
						countDate,
						e);
			}
			return;
		}
		try {
			enqueuePort.enqueue(companyId, countDate, orderClass, actId);
		} catch (Exception e) {
			log.error(
					"company statistic job enqueue failed, companyId={}, countDate={}",
					companyId,
					countDate,
					e);
		}
	}
}
