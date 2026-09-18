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

package cn.shopex.ecshopx.adapay.cron;

import cn.shopex.ecshopx.adapay.service.OpenAccountService;
import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleGetSubmitLicenseStatusHandler {

	private static final String HANDLER_SHORT_NAME = "adapay-get-submit-license-audit";

	private final OpenAccountService openAccountService;
	private final ApplicationEventPublisher eventPublisher;

	@XxlJob(HANDLER_SHORT_NAME)
	public void execute() {
		long start = System.currentTimeMillis();
		try {
			openAccountService.scheduleGetSubmitLicenseStatus();
			long costMs = System.currentTimeMillis() - start;
			int processed = 0;
			log.info("[cron][{}] done, costMs={}, processed={}", HANDLER_SHORT_NAME, costMs, processed);
		} catch (Exception e) {
			long costMs = System.currentTimeMillis() - start;
			log.error("[cron][{}] failed, costMs={}", HANDLER_SHORT_NAME, costMs, e);
			eventPublisher.publishEvent(new CronAlertEvent(HANDLER_SHORT_NAME, e));
			throw e;
		}
	}
}
