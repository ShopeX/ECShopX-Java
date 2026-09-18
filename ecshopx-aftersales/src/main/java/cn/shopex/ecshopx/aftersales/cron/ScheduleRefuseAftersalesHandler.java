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

package cn.shopex.ecshopx.aftersales.cron;

import cn.shopex.ecshopx.aftersales.service.AftersalesService;
import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * XXL-Job entry for timed auto-refusal of return aftersales. Delegates batch work to {@link AftersalesService}; when
 * a row completes successfully, order process logs are emitted through {@link OrderProcessLogPublishPort} (including
 * post-commit dispatch). Handler-level failures are surfaced via {@link CronAlertEvent}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleRefuseAftersalesHandler {

	private static final String HANDLER_SHORT_NAME = "refuse-aftersales";

	private final AftersalesService aftersalesService;
	private final ApplicationEventPublisher eventPublisher;

	@XxlJob(HANDLER_SHORT_NAME)
	public void execute() {
		long start = System.currentTimeMillis();
		try {
			boolean done = aftersalesService.scheduleAutoRefuse();
			log.info(
					"[cron][{}] done, result={}, cost={}ms",
					HANDLER_SHORT_NAME, done, System.currentTimeMillis() - start);
		} catch (Exception e) {
			log.error("[cron][{}] failed", HANDLER_SHORT_NAME, e);
			eventPublisher.publishEvent(new CronAlertEvent(HANDLER_SHORT_NAME, e));
			throw e;
		}
	}
}
