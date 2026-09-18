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

package cn.shopex.ecshopx.aliyunsms.cron;

import cn.shopex.ecshopx.aliyunsms.service.SmsService;
import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleQuerySendDetailHandler {

	private static final String HANDLER = "aliyunsms-query-sms-send-detail";

	private final SmsService smsService;
	private final ApplicationEventPublisher eventPublisher;

	@XxlJob(HANDLER)
	public void execute() {
		long start = System.currentTimeMillis();
		try {
			int dbUpdated = smsService.scheduleQuerySendDetail();
			log.info("[cron][{}] done, cost={}ms, dbUpdated={}", HANDLER, System.currentTimeMillis() - start, dbUpdated);
		} catch (Exception e) {
			log.error("[cron][{}] failed, costMs={}", HANDLER, System.currentTimeMillis() - start, e);
			eventPublisher.publishEvent(new CronAlertEvent(HANDLER, e));
			throw e;
		}
	}
}
