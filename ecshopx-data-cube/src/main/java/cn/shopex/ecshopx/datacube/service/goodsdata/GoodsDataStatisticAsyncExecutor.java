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

package cn.shopex.ecshopx.datacube.service.goodsdata;

import cn.shopex.ecshopx.datacube.service.GoodsDataService;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GoodsDataStatisticAsyncExecutor {

	private final GoodsDataService goodsDataService;
	private final GoodsStatisticJobEnqueuePort enqueuePort;
	private final boolean syncInline;
	private final boolean employeePurchaseSyncInline;

	public GoodsDataStatisticAsyncExecutor(
			@Lazy GoodsDataService goodsDataService,
			GoodsStatisticJobEnqueuePort enqueuePort,
			@Value("${datacube.cron.goods-daily-statistics.sync-inline:false}") boolean syncInline,
			@Value("${datacube.cron.employee-purchase-goods-daily-statistics.sync-inline:false}")
					boolean employeePurchaseSyncInline) {
		this.goodsDataService = goodsDataService;
		this.enqueuePort = enqueuePort;
		this.syncInline = syncInline;
		this.employeePurchaseSyncInline = employeePurchaseSyncInline;
	}

	/**
	 * 默认经 Dispatch Bus 投递 slow 队列异步执行；{@code datacube.cron.goods-daily-statistics.sync-inline=true}
	 * 时同线程内联执行 {@link GoodsDataService#runStatistics}，便于阶段 4 与 PHP 同步队列终态对账。
	 */
	public void runBatchAsync(GoodsStatisticsBatch batch, LocalDate countDate) {
		if (syncInline) {
			try {
				goodsDataService.runStatistics(batch, countDate);
			} catch (Exception e) {
				log.error("inline goods daily statistics failed, countDate={}", countDate, e);
			}
			return;
		}
		try {
			enqueuePort.enqueue(batch, countDate);
		} catch (Exception e) {
			log.error("goods statistic job enqueue failed, countDate={}", countDate, e);
		}
	}

	/**
	 * 内购活动商品日统计批调度：{@code datacube.cron.employee-purchase-goods-daily-statistics.sync-inline=true}
	 * 时同线程执行，与 {@code goods-daily-statistics.sync-inline} 无关。
	 */
	public void runEmployeePurchaseBatchAsync(GoodsStatisticsBatch batch, LocalDate countDate, long actId) {
		if (employeePurchaseSyncInline) {
			try {
				goodsDataService.runStatistics(batch, countDate, "employee_purchase", actId);
			} catch (Exception e) {
				log.error(
						"inline employee purchase goods daily statistics failed, countDate={}, actId={}",
						countDate,
						actId,
						e);
			}
			return;
		}
		try {
			enqueuePort.enqueueEmployeePurchase(batch, countDate, actId);
		} catch (Exception e) {
			log.error(
					"goods statistic job enqueue failed, countDate={}, actId={}, employeePurchase=true",
					countDate,
					actId,
					e);
		}
	}
}
