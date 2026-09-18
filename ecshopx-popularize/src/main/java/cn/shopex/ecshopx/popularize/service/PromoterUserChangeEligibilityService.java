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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.popularize.PromoterChangePromoterDoneOrderCountPort;
import cn.shopex.ecshopx.common.popularize.PromoterChangePromoterVipGradeMatchPort;
import cn.shopex.ecshopx.members.service.stats.MemberTotalConsumptionReadService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromoterUserChangeEligibilityService {

	private final PopularizeSettingSaveService popularizeSettingSaveService;
	private final MemberTotalConsumptionReadService memberTotalConsumptionReadService;
	private final PromoterChangePromoterVipGradeMatchPort vipGradeMatchPort;
	private final PromoterChangePromoterDoneOrderCountPort doneOrderCountPort;

	public PromoterUserChangeEligibilityService(
			PopularizeSettingSaveService popularizeSettingSaveService,
			MemberTotalConsumptionReadService memberTotalConsumptionReadService,
			PromoterChangePromoterVipGradeMatchPort vipGradeMatchPort,
			PromoterChangePromoterDoneOrderCountPort doneOrderCountPort) {
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.memberTotalConsumptionReadService = memberTotalConsumptionReadService;
		this.vipGradeMatchPort = vipGradeMatchPort;
		this.doneOrderCountPort = doneOrderCountPort;
	}

	public boolean userIsChangePromoter(long companyId, long userId, boolean internalPromoter) {
		if (!"true".equals(popularizeSettingSaveService.getOpenPopularizeLiteral(companyId))) {
			return false;
		}
		Map<String, Object> changeRoot = popularizeSettingSaveService.resolveNormalizedChangePromoterBlock(companyId);
		String type = String.valueOf(changeRoot.getOrDefault("type", "no_threshold")).trim();
		Map<String, Object> filter = resolveMergedFilter(changeRoot);
		return switch (type) {
			case "no_threshold" -> true;
			case "internal" -> internalPromoter;
			case "vip_grade" -> {
				String configured = String.valueOf(filter.getOrDefault("vip_grade", "vip")).trim();
				yield vipGradeMatchPort.matchesConfiguredVipType(companyId, userId, configured);
			}
			case "consume_money" -> matchesConsumeMoneyThreshold(userId, filter);
			case "order_num" -> matchesOrderNumThreshold(companyId, userId, filter);
			default -> false;
		};
	}

	private static Map<String, Object> resolveMergedFilter(Map<String, Object> changeRoot) {
		Map<String, Object> mergedFilter = new LinkedHashMap<>();
		mergedFilter.put("no_threshold", 0);
		mergedFilter.put("vip_grade", "vip");
		mergedFilter.put("consume_money", 0);
		mergedFilter.put("order_num", 0);
		Object fObj = changeRoot.get("filter");
		if (fObj instanceof Map<?, ?> fm) {
			for (Map.Entry<?, ?> e : fm.entrySet()) {
				mergedFilter.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return mergedFilter;
	}

	private boolean matchesConsumeMoneyThreshold(long userId, Map<String, Object> filter) {
		BigDecimal totalRaw = memberTotalConsumptionReadService.getTotalConsumption(userId);
		BigDecimal consumptionYuan = totalRaw.movePointLeft(2).setScale(2, RoundingMode.DOWN);
		BigDecimal threshold = parseConsumeMoneyThreshold(filter.get("consume_money"));
		return consumptionYuan.compareTo(threshold) >= 0;
	}

	private static BigDecimal parseConsumeMoneyThreshold(Object raw) {
		if (raw == null) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
		}
		if (raw instanceof BigDecimal bd) {
			return bd.setScale(2, RoundingMode.DOWN);
		}
		if (raw instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.DOWN);
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
		}
		try {
			return new BigDecimal(s).setScale(2, RoundingMode.DOWN);
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
		}
	}

	private boolean matchesOrderNumThreshold(long companyId, long userId, Map<String, Object> filter) {
		long required = parseLongThreshold(filter.get("order_num"));
		long count = doneOrderCountPort.countDoneOrdersForBuyer(companyId, userId);
		return count >= required;
	}

	private static long parseLongThreshold(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
