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

package cn.shopex.ecshopx.companys.cron;

import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.common.dispatch.CompanysBundleDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleRecordPopularizeStatisticsHandler {

	private static final String SHORT = "record-popularize-statistics";

	private final DispatchFacade dispatchFacade;
	private final ApplicationEventPublisher eventPublisher;

	@XxlJob(SHORT)
	public void execute() {
		long start = System.currentTimeMillis();
		try {
			DispatchOptions options =
					new DispatchOptions(
							DispatchMode.ASYNC,
							DispatchDriverType.REDIS,
							"slow",
							null,
							RetryPolicy.platformDefault());
			dispatchFacade.dispatchJob(
					CompanysBundleDispatchJobNames.RECORD_STATISTICS_JOB_POPULARIZE_SCHEDULE,
					new LinkedHashMap<>(),
					options);
			dispatchFacade.dispatchJob(
					CompanysBundleDispatchJobNames.SALESPERSON_POPULARIZE_RECORD_STATISTICS_JOB,
					new LinkedHashMap<>(),
					options);
			long costMs = System.currentTimeMillis() - start;
			log.info("[cron][record-popularize-statistics] done, costMs={}, jobDispatches=2", costMs);
		} catch (Exception e) {
			long costMs = System.currentTimeMillis() - start;
			log.error("[cron][record-popularize-statistics] failed, costMs={}", costMs, e);
			eventPublisher.publishEvent(new CronAlertEvent(SHORT, e));
			throw e;
		}
	}
}
