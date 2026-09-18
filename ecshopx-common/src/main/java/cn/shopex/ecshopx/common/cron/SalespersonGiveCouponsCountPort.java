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
 * 对 {@code kaquan_salesperson_give_coupons} 做按日条件统计，避免 salesperson 模块直接依赖 kaquan 的 Mapper。
 */
public interface SalespersonGiveCouponsCountPort {

	/**
	 * @param dateYmd 业务日 8 位（Asia/Shanghai 日历）
	 * @return 满足 company/salesperson、当日 [0:00, 次日 0:00)、status=1 的**行数**
	 */
	int countSuccessRowsForDate(long companyId, long salespersonId, int dateYmd);
}
