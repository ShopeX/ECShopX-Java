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

package cn.shopex.ecshopx.datacube.cron.mock;

import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticJobEnqueuePort;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticsBatch;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下覆盖真实 GoodsStatistic job Bus 投递，避免向 Redis slow 真入队。
 */
@Slf4j
public class NoopGoodsStatisticJobEnqueuePort implements GoodsStatisticJobEnqueuePort {

	@Override
	public void enqueue(GoodsStatisticsBatch batch, LocalDate countDate) {
		log.info("[cron-mock][goods-statistic-job] noop enqueue countDate={} lines={}", countDate, batch);
	}

	@Override
	public void enqueueEmployeePurchase(GoodsStatisticsBatch batch, LocalDate countDate, long actId) {
		log.info(
				"[cron-mock][goods-statistic-job] noop enqueue employee_purchase countDate={} actId={} lines={}",
				countDate,
				actId,
				batch);
	}
}
