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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内购活动商品每人限购数、每人限额（含历史累计）统一校验。
 * 对齐 PHP {@code EmployeePurchaseItemLimitValidator}。
 */
public final class EmployeePurchaseItemLimitValidator {

	public static final String MSG_FEE = "商品购买金额已达限额";
	public static final String MSG_NUM = "商品购买数量已达限额";
	public static final String MSG_NOT_IN_ACTIVITY = "商品未参加内购活动";

	private EmployeePurchaseItemLimitValidator() {}

	public record ItemLimit(int limitNum, int limitFee) {}

	public record ItemAggregate(int aggregateNum, int aggregateFee) {}

	public record LimitLine(long itemId, int num, int itemFee) {}

	public static void assertWithPreloadedData(
			Map<Long, ItemLimit> activityItemByItemId,
			Map<Long, ItemAggregate> aggregateByItemId,
			List<LimitLine> linesOrdered) {
		List<LimitLine> merged = mergeLinesByFirstAppearance(linesOrdered);
		for (LimitLine line : merged) {
			long itemId = line.itemId();
			ItemLimit limit = activityItemByItemId.get(itemId);
			if (limit == null) {
				throw new ResourceException(MSG_NOT_IN_ACTIVITY);
			}
			ItemAggregate agg = aggregateByItemId.get(itemId);
			int aggNum = agg == null ? 0 : agg.aggregateNum();
			int aggFee = agg == null ? 0 : agg.aggregateFee();
			int newNum = aggNum + line.num();
			int newFee = aggFee + line.itemFee();
			boolean qtyExceed = limit.limitNum() > 0 && newNum > limit.limitNum();
			boolean feeExceed = limit.limitFee() > 0 && newFee > limit.limitFee();
			if (qtyExceed && feeExceed) {
				throw new ResourceException(MSG_FEE);
			}
			if (feeExceed) {
				throw new ResourceException(MSG_FEE);
			}
			if (qtyExceed) {
				throw new ResourceException(MSG_NUM);
			}
		}
	}

	static List<LimitLine> mergeLinesByFirstAppearance(List<LimitLine> linesOrdered) {
		LinkedHashMap<Long, LimitLine> bucket = new LinkedHashMap<>();
		if (linesOrdered == null) {
			return List.of();
		}
		for (LimitLine line : linesOrdered) {
			if (line == null || line.itemId() <= 0L) {
				continue;
			}
			LimitLine prev = bucket.get(line.itemId());
			if (prev == null) {
				bucket.put(line.itemId(), line);
			} else {
				bucket.put(
						line.itemId(),
						new LimitLine(line.itemId(), prev.num() + line.num(), prev.itemFee() + line.itemFee()));
			}
		}
		return new ArrayList<>(bucket.values());
	}
}
