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

package cn.shopex.ecshopx.espier.cron;

import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.espier.service.UploadFileService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleDeleteErrorFileHandler {

	private static final String SHORT = "delete-error-file";

	private final UploadFileService uploadFileService;
	private final ApplicationEventPublisher eventPublisher;

	@XxlJob(SHORT)
	public void execute() {
		long start = System.currentTimeMillis();
		try {
			int processed = uploadFileService.scheduleDeleteErrorFile();
			log.info("[cron][{}] done, costMs={}, processed={}", SHORT, System.currentTimeMillis() - start, processed);
		} catch (Exception e) {
			long costMs = System.currentTimeMillis() - start;
			log.error("[cron][{}] failed, costMs={}", SHORT, costMs, e);
			eventPublisher.publishEvent(new CronAlertEvent(SHORT, e));
			throw e;
		}
	}
}
