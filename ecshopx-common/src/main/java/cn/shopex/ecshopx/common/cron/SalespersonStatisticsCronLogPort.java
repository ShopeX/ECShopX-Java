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
 * 导购统计任务调试轨迹（开始 / 结束 / 异常），与 Job 中 debug 日志一致。
 */
public interface SalespersonStatisticsCronLogPort {

	void debugStart(SalespersonStatisticsCronLogKind kind, long companyId, long salespersonId);

	void debugError(
			SalespersonStatisticsCronLogKind kind, long companyId, long salespersonId, Throwable error);

	void debugEnd(SalespersonStatisticsCronLogKind kind, long companyId, long salespersonId);

	default void debugStart(long companyId, long salespersonId) {
		debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_DAILY, companyId, salespersonId);
	}

	default void debugError(long companyId, long salespersonId, Throwable error) {
		debugError(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_DAILY, companyId, salespersonId, error);
	}

	default void debugEnd(long companyId, long salespersonId) {
		debugEnd(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_DAILY, companyId, salespersonId);
	}
}
