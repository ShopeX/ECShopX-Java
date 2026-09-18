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

package cn.shopex.ecshopx.orders.cron;

import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.orders.service.GroupsServiceOrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleCancelServiceGroupsOrdersHandler {

	private static final String HANDLER_SHORT_NAME = "cancel-service-group-orders";

	private final GroupsServiceOrderService groupsServiceOrderService;
	private final ApplicationEventPublisher eventPublisher;

	@XxlJob(HANDLER_SHORT_NAME)
	public void execute() {
		long start = System.currentTimeMillis();
		try {
			int cancelled = groupsServiceOrderService.scheduleCancelOrders();
			log.info("[cron][{}] done, cancelled={}, cost={}ms",
					HANDLER_SHORT_NAME, cancelled, System.currentTimeMillis() - start);
		} catch (Exception e) {
			log.error("[cron][{}] failed", HANDLER_SHORT_NAME, e);
			eventPublisher.publishEvent(new CronAlertEvent(HANDLER_SHORT_NAME, e));
			throw e;
		}
	}
}
