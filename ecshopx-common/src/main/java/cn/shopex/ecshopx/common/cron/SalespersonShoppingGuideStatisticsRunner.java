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
 * 导购（shopping_guide）日统计的同步执行入口；由 ecshopx-salesperson 实现，避免 ecshopx-companys
 * 对 salesperson 的 Maven 依赖成环。
 */
public interface SalespersonShoppingGuideStatisticsRunner {

	/**
	 * @return 导购行遍历条数 + 本段产生的 INSERT 条数，用于 handler processed 对账
	 */
	int runShoppingGuideBlock(int yesterdayYmd);
}
