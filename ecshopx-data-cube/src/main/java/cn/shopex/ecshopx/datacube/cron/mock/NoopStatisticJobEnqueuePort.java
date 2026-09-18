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

import cn.shopex.ecshopx.datacube.service.companydata.StatisticJobEnqueuePort;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下覆盖真实全站商城日统计 Bus 投递，避免向 Redis slow 真入队。
 */
@Slf4j
public class NoopStatisticJobEnqueuePort implements StatisticJobEnqueuePort {

	@Override
	public void enqueue(long companyId, LocalDate countDate, String orderClass, long actId) {
		log.info(
				"[cron-mock][company-statistic-job] noop enqueue companyId={} countDate={} orderClass={} actId={}",
				companyId,
				countDate,
				orderClass,
				actId);
	}
}
