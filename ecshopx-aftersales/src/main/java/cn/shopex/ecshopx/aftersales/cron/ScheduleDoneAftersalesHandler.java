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

import cn.shopex.ecshopx.common.cron.event.CronAlertEvent;
import cn.shopex.ecshopx.aftersales.service.AftersalesService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * XXL-Job handler for the legacy console daily batch: auto-close stale merchant-rejected aftersales
 * ({@code aftersales_status = 3} past threshold) by delegating to
 * {@link AftersalesService#scheduleAutoDoneAftersales()}.
 *
 * <p>SaaS ERP cancellation for {@code ThirdPartyBundle\Events\TradeAftersalesCancelEvent} is not emitted here and must
 * not rely on {@code ApplicationEventPublisher#publishEvent} with {@code SaasErpAftersalesSpringEvent} as the primary
 * path: the shared {@code AftersalesService} close transaction runs {@code afterCommit} hooks that publish through
 * {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher} (dispatch Bus),
 * matching post-commit queued dispatch semantics from the legacy monolith.
 *
 * <p>Operational scheduling frequency differs (XXL-Job admin vs legacy scheduler configuration); the handler shape and
 * service delegation align this scheduled batch close with the legacy daily batch closure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleDoneAftersalesHandler {

	private static final String HANDLER = "done-aftersales";

	private final AftersalesService aftersalesService;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * XXL-Job entry driving {@link AftersalesService#scheduleAutoDoneAftersales()} for batch auto-close. Each successful
	 * close publishes order-process-log data through {@link cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort}
	 * inside the close transaction; {@code afterCommit} then runs the same dispatch publishers as other close paths
	 * (including ThirdParty SaaS ERP Bus publish), plus cancel-notice scheduling via
	 * {@link cn.shopex.ecshopx.common.port.aftersales.AftersalesCancelNoticeJobPort}.
	 */
	@XxlJob(HANDLER)
	public void execute() {
		long start = System.currentTimeMillis();
		try {
			int processed = aftersalesService.scheduleAutoDoneAftersales();
			long costMs = System.currentTimeMillis() - start;
			log.info("[cron][done-aftersales] done, costMs={}, processed={}", costMs, processed);
		} catch (Exception e) {
			log.error(
					"[cron][done-aftersales] failed, costMs={}",
					System.currentTimeMillis() - start,
					e);
			eventPublisher.publishEvent(new CronAlertEvent(HANDLER, e));
			throw e;
		}
	}
}
