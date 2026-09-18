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

package cn.shopex.ecshopx.common.cron;

/**
 * 导购送券日统计块，由 ecshopx-salesperson 提供实现；段二不读 Redis 汇总键，仅对送券表做条件 COUNT。
 */
public interface SalespersonGiveCouponsRecordStatisticsRunner {

	/**
	 * @param yesterdayYmd 昨日 Ymd（Asia/Shanghai）
	 * @return 导购行数 + salespersonGiveCoupons/member 路径上实际 INSERT 条数
	 */
	int runGiveCouponsBlock(int yesterdayYmd);
}
