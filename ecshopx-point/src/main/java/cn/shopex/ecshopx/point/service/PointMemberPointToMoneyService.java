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

package cn.shopex.ecshopx.point.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PointMemberPointToMoneyService {

	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final PointMemberMoneyToPointService pointMemberMoneyToPointService;

	public PointMemberPointToMoneyService(
			PointMemberRuleReadService pointMemberRuleReadService,
			PointMemberMoneyToPointService pointMemberMoneyToPointService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.pointMemberMoneyToPointService = pointMemberMoneyToPointService;
	}

	public int pointToMoney(long companyId, long points) {
		return pointToMoney(companyId, points, null);
	}

	public int pointToMoney(long companyId, long points, Long originalMoneyFen) {
		if (points <= 0L) {
			return 0;
		}
		Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId);
		if (!redisFlagTrue(rule.get("isOpenDeductPoint"))) {
			return 0;
		}
		BigDecimal deductPoint = parsePositiveDecimal(rule.get("deduct_point"));
		if (deductPoint.compareTo(BigDecimal.ZERO) <= 0) {
			return 0;
		}
		int calculated =
				BigDecimal.valueOf(points)
						.divide(deductPoint, 4, RoundingMode.DOWN)
						.multiply(BigDecimal.valueOf(100))
						.setScale(2, RoundingMode.DOWN)
						.intValue();
		if (originalMoneyFen != null) {
			int onePointMoney =
					BigDecimal.ONE.divide(deductPoint, 4, RoundingMode.DOWN)
							.multiply(BigDecimal.valueOf(100))
							.setScale(2, RoundingMode.DOWN)
							.intValue();
			long diff = (long) calculated - originalMoneyFen;
			if (diff > 0L && diff <= onePointMoney) {
				return originalMoneyFen.intValue();
			}
		}
		return calculated;
	}

	public long orderMaxMoneyToPoint(long companyId, long payFeeFen) {
		Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId);
		BigDecimal deductPoint = parsePositiveDecimal(rule.get("deduct_point"));
		if (deductPoint.compareTo(BigDecimal.ZERO) <= 0) {
			return 0L;
		}
		int limitPct = parsePercentInt(rule.get("deduct_proportion_limit"));
		BigDecimal maxMoney =
				BigDecimal.valueOf(limitPct)
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
						.multiply(BigDecimal.valueOf(payFeeFen));
		return pointMemberMoneyToPointService.moneyToPoint(companyId, maxMoney.longValue());
	}

	private static BigDecimal parsePositiveDecimal(Object raw) {
		if (raw == null) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private static int parsePercentInt(Object raw) {
		if (raw == null) {
			return 100;
		}
		try {
			int v = Integer.parseInt(String.valueOf(raw).trim());
			return v < 1 ? 100 : Math.min(v, 100);
		} catch (NumberFormatException e) {
			return 100;
		}
	}

	private static boolean redisFlagTrue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		return "true".equals(String.valueOf(v).trim());
	}
}
