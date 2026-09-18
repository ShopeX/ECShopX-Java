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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddOrderSumPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOrderSumRow;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 4 对拍；alias 与 plan §4 {@code youshu-analysis-order-sum} 一致。
 */
@Slf4j
public class NoopYoushuAnalysisAddOrderSumPort implements YoushuAnalysisAddOrderSumPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void addOrderSum(String dataSourceId, YoushuOrderSumRow orderSum, YoushuOpenApiCredentials credentials) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][youshu-analysis-order-sum] called#{}, args={}",
				n,
				Arrays.toString(
						new Object[] {
							dataSourceId,
							orderSum == null
									? null
									: String.format(
											"ref=%s giveAmt=%.2f giveNum=%d payAmt=%.2f payNum=%d",
											orderSum.refDate(),
											orderSum.giveOrderAmountSum(),
											orderSum.giveOrderNumSum(),
											orderSum.paymentAmountSum(),
											orderSum.payedNumSum()),
							credentials
						}));
	}
}
