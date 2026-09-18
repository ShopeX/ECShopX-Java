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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.orders.repository.OrderItemsRelPointAccessReadRepository;
import cn.shopex.ecshopx.orders.service.point.ExtraPointActivityOrderBonusService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NormalOrderMemberGetPointsService {

	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final OrderItemsRelPointAccessReadRepository itemsRelPointAccessReadRepository;
	private final ExtraPointActivityOrderBonusService extraPointActivityOrderBonusService;

	public NormalOrderMemberGetPointsService(
			PointMemberRuleReadService pointMemberRuleReadService,
			OrderItemsRelPointAccessReadRepository itemsRelPointAccessReadRepository,
			ExtraPointActivityOrderBonusService extraPointActivityOrderBonusService) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.itemsRelPointAccessReadRepository = itemsRelPointAccessReadRepository;
		this.extraPointActivityOrderBonusService = extraPointActivityOrderBonusService;
	}

	@SuppressWarnings("unchecked")
	public void applyMemberGetPoints(long companyId, Map<String, Object> orderRow) {
		int pointFeeTotal = intVal(orderRow.get("point_fee"), 0);
		int freightFeeFull = intVal(orderRow.get("freight_fee"), 0);
		int pointFreightFee = 0;
		if (pointFeeTotal > 0 && freightFeeFull > 0) {
			List<Map<String, Object>> items = (List<Map<String, Object>>) orderRow.get("items");
			int sumLinePoint = 0;
			if (items != null) {
				for (Map<String, Object> it : items) {
					sumLinePoint += intVal(it.get("point_fee"), 0);
				}
			}
			pointFreightFee = pointFeeTotal - sumLinePoint;
		}

		Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId);
		if (!"true".equals(String.valueOf(rule.get("isOpenMemberPoint")).trim())) {
			orderRow.put("get_points", 0);
			orderRow.put("extra_points", 0);
			zeroLineGetPoints(orderRow);
			return;
		}

		String access = String.valueOf(rule.get("access")).trim();
		BigDecimal basePoint;
		if ("items".equals(access)) {
			basePoint = basePointItemsBranch(companyId, orderRow, rule);
		} else {
			basePoint = basePointOrderBranch(orderRow, rule, pointFreightFee);
		}

		int pointInt = basePoint.setScale(0, RoundingMode.DOWN).intValue();
		if (pointInt <= 0) {
			orderRow.put("get_points", 0);
			orderRow.put("extra_points", 0);
			zeroLineGetPoints(orderRow);
			return;
		}

		orderRow.put("get_points", basePoint.stripTrailingZeros());

		long totalFee = longVal(orderRow.get("total_fee"), 0L);
		int freightFee = intVal(orderRow.get("freight_fee"), 0);
		long totalFeeForExtra = totalFee - ((long) freightFee - pointFreightFee);

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("user_id", longVal(orderRow.get("user_id"), 0L));
		filter.put("distributor_id", longVal(orderRow.get("distributor_id"), 0L));
		filter.put("total_fee", totalFeeForExtra);
		filter.put("shop_id", longVal(orderRow.get("shop_id"), 0L));

		int extra =
				extraPointActivityOrderBonusService.computeExtraBonus(companyId, filter, basePoint);
		orderRow.put("extra_points", extra);

		if (!"items".equals(access)) {
			shareLineGetPoints(orderRow, rule, pointFreightFee, basePoint, extra);
		}
	}

	@SuppressWarnings("unchecked")
	private BigDecimal basePointItemsBranch(
			long companyId, Map<String, Object> orderRow, Map<String, Object> rule) {
		List<Map<String, Object>> items = (List<Map<String, Object>>) orderRow.get("items");
		if (items == null || items.isEmpty()) {
			return BigDecimal.ZERO;
		}
		List<Long> ids = new ArrayList<>();
		for (Map<String, Object> it : items) {
			long iid = longVal(it.get("item_id"), 0L);
			if (iid > 0L) {
				ids.add(iid);
			}
		}
		Map<Long, Long> pointByItem = itemsRelPointAccessReadRepository.mapPointByItemId(companyId, ids);
		BigDecimal sum = BigDecimal.ZERO;
		for (Map<String, Object> it : items) {
			long iid = longVal(it.get("item_id"), 0L);
			if (!pointByItem.containsKey(iid)) {
				continue;
			}
			BigDecimal rulePt = new BigDecimal(pointByItem.get(iid).toString());
			BigDecimal num = BigDecimal.valueOf(intVal(it.get("num"), 0));
			sum = sum.add(rulePt.multiply(num));
		}
		return sum;
	}

	private BigDecimal basePointOrderBranch(
			Map<String, Object> orderRow, Map<String, Object> rule, int pointFreightFee) {
		boolean excludeFreight =
				"false".equals(
						rule.get("include_freight") == null
								? null
								: String.valueOf(rule.get("include_freight")).trim());
		long totalFee = longVal(orderRow.get("total_fee"), 0L);
		int freightFee = intVal(orderRow.get("freight_fee"), 0);
		long baseFen;
		if (excludeFreight) {
			baseFen = totalFee - ((long) freightFee - pointFreightFee);
		} else {
			baseFen = totalFee;
		}
		BigDecimal gainPoint =
				new BigDecimal(String.valueOf(rule.getOrDefault("gain_point", "1")).trim());
		BigDecimal baseYuan =
				BigDecimal.valueOf(baseFen).divide(BigDecimal.valueOf(100L), 10, RoundingMode.HALF_UP);
		return gainPoint.multiply(baseYuan);
	}

	@SuppressWarnings("unchecked")
	private void shareLineGetPoints(
			Map<String, Object> orderRow,
			Map<String, Object> rule,
			int pointFreightFee,
			BigDecimal basePoint,
			int extraPts) {
		boolean excludeFreight =
				"false".equals(
						rule.get("include_freight") == null
								? null
								: String.valueOf(rule.get("include_freight")).trim());
		long totalFee = longVal(orderRow.get("total_fee"), 0L);
		int freightFee = intVal(orderRow.get("freight_fee"), 0);
		long totalForShare;
		if (excludeFreight) {
			totalForShare = totalFee - ((long) freightFee - pointFreightFee);
		} else {
			totalForShare = totalFee;
		}
		if (totalForShare <= 0L) {
			return;
		}
		List<Map<String, Object>> items = (List<Map<String, Object>>) orderRow.get("items");
		if (items == null || items.isEmpty()) {
			return;
		}
		BigDecimal totalForShareBd = BigDecimal.valueOf(totalForShare);
		BigDecimal sumPts = basePoint.add(BigDecimal.valueOf(extraPts));
		for (Map<String, Object> line : items) {
			BigDecimal lineTotal = BigDecimal.valueOf(intVal(line.get("total_fee"), 0));
			BigDecimal ratio =
					lineTotal.divide(totalForShareBd, 5, RoundingMode.HALF_UP);
			BigDecimal linePts =
					ratio.multiply(sumPts).setScale(1, RoundingMode.HALF_UP);
			int linePi = linePts.setScale(0, RoundingMode.HALF_UP).intValue();
			line.put("get_points", linePi);
		}
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	@SuppressWarnings("unchecked")
	private static void zeroLineGetPoints(Map<String, Object> orderRow) {
		Object rawItems = orderRow.get("items");
		if (!(rawItems instanceof List<?> list)) {
			return;
		}
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				((Map<String, Object>) m).put("get_points", 0);
			}
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
