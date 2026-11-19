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

package cn.shopex.ecshopx.common.service.brokerage;

import java.util.List;

/**
 * Snapshot of normal-order fields required for brokerage persistence at trade finish.
 *
 * <p>{@code lines} is assembled by the orders module so popularize can write PHP-compatible
 * {@code detail.rebate_detail} without querying order tables.
 */
public record NormalOrderBrokerageFinishInput(
		Long userId,
		String orderClass,
		String totalFee,
		Integer commissionFee,
		List<Line> lines) {

	public NormalOrderBrokerageFinishInput {
		lines = lines == null ? List.of() : List.copyOf(lines);
	}

	/** Backward-compatible ctor used by tests and callers without line items. */
	public NormalOrderBrokerageFinishInput(
			Long userId, String orderClass, String totalFee, Integer commissionFee) {
		this(userId, orderClass, totalFee, commissionFee, List.of());
	}

	/**
	 * @param totalFeeMinor line {@code total_fee} in minor units (fen), before fee_rate
	 * @param costFeeMinor line {@code cost_fee} in minor units (fen), before fee_rate
	 * @param feeRate currency rate, default 1
	 */
	public record Line(long itemId, int num, long totalFeeMinor, long costFeeMinor, double feeRate) {
		public Line(long itemId, int num, long totalFeeMinor, long costFeeMinor) {
			this(itemId, num, totalFeeMinor, costFeeMinor, 1.0d);
		}
	}
}
