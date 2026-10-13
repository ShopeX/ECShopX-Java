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

package cn.shopex.ecshopx.promotions.service.membertask;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Admin 概览/导出：归因消费（元）文案。 */
public final class MemberTaskAttributedMetricsDisplay {

	private MemberTaskAttributedMetricsDisplay() {}

	public static String spendYuanText(long spendFen) {
		if (spendFen <= 0L) {
			return "0";
		}
		BigDecimal yuan =
				BigDecimal.valueOf(spendFen).divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
		return yuan.stripTrailingZeros().toPlainString();
	}
}
