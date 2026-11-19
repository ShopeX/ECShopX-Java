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

import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单取消时的分销佣金冲销，对齐 PHP {@code BrokerageService::brokerageBycancelOrder}：
 * 不把原行标为已结算，而是插入负佣冲销行并回冲未结算统计，再刷新 plan_close_time。
 */
@Service
public class BrokerageCancelOnOrderService {

	private static final String REMARKS_CANCEL = "取消订单生成";

	private static final int PLAN_CLOSE_PLACEHOLDER_DAYS = 1000;

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final BrokerageMapper brokerageMapper;
	private final PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService;
	private final BrokeragePlanCloseTimeService brokeragePlanCloseTimeService;

	public BrokerageCancelOnOrderService(
			BrokerageMapper brokerageMapper,
			PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService,
			BrokeragePlanCloseTimeService brokeragePlanCloseTimeService) {
		this.brokerageMapper = brokerageMapper;
		this.popularizeBrokerageStatisticsIncrementService = popularizeBrokerageStatisticsIncrementService;
		this.brokeragePlanCloseTimeService = brokeragePlanCloseTimeService;
	}

	@Transactional
	public void brokerageByCancelOrder(long companyId, long orderId) {
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		String orderIdStr = String.valueOf(orderId);
		List<Brokerage> openRows =
				brokerageMapper.selectList(
						new LambdaQueryWrapper<Brokerage>()
								.eq(Brokerage::getCompanyId, companyId)
								.eq(Brokerage::getOrderId, orderIdStr)
								.eq(Brokerage::getIsClose, false));
		if (openRows == null || openRows.isEmpty()) {
			brokeragePlanCloseTimeService.updatePlanCloseTime(companyId, orderId);
			return;
		}

		int nowSec = (int) Instant.now().getEpochSecond();
		int placeholderPlanClose = nowSec + 3600 * 24 * PLAN_CLOSE_PLACEHOLDER_DAYS;

		for (Brokerage row : openRows) {
			Brokerage neg = buildNegativeOffsetRow(row, nowSec, placeholderPlanClose);
			brokerageMapper.insert(neg);
			applyCancelPopularizeStats(neg);
		}

		brokeragePlanCloseTimeService.updatePlanCloseTime(companyId, orderId);
	}

	private Brokerage buildNegativeOffsetRow(Brokerage row, int nowSec, int placeholderPlanClose) {
		Brokerage neg = new Brokerage();
		neg.setId(null);
		neg.setBrokerageType(row.getBrokerageType());
		neg.setOrderId(row.getOrderId());
		neg.setUserId(row.getUserId());
		neg.setBuyUserId(row.getBuyUserId());
		neg.setOrderType(row.getOrderType());
		neg.setSource(row.getSource());
		neg.setCompanyId(row.getCompanyId());
		neg.setPrice(negateInt(row.getPrice()));
		neg.setCommissionType(row.getCommissionType());
		neg.setRebate(negateInt(row.getRebate()));
		neg.setRebatePoint(negateRebatePoint(row.getRebatePoint()));
		neg.setDetail(detailWithCancelRemarks(row.getDetail()));
		neg.setIsClose(false);
		neg.setPlanCloseTime(placeholderPlanClose);
		neg.setCreated(nowSec);
		neg.setUpdated(nowSec);
		return neg;
	}

	private void applyCancelPopularizeStats(Brokerage neg) {
		if (neg.getUserId() == null || neg.getCompanyId() == null) {
			return;
		}
		long itemPrice =
				"order".equalsIgnoreCase(nullToEmpty(neg.getSource()))
						? (neg.getPrice() == null ? 0L : neg.getPrice().longValue())
						: 0L;
		String commissionType = nullToEmpty(neg.getCommissionType());
		if ("money".equalsIgnoreCase(commissionType)) {
			long rebate = neg.getRebate() == null ? 0L : neg.getRebate().longValue();
			popularizeBrokerageStatisticsIncrementService.add(
					Map.of(
							"item_total_price",
							itemPrice,
							"rebate_total",
							rebate,
							"no_close_rebate",
							rebate),
					neg.getUserId(),
					neg.getCompanyId());
			return;
		}
		long point = parseRebatePointSigned(neg.getRebatePoint());
		popularizeBrokerageStatisticsIncrementService.add(
				Map.of(
						"item_total_price",
						itemPrice,
						"point_total",
						point,
						"no_close_point",
						point),
				neg.getUserId(),
				neg.getCompanyId());
	}

	private static Integer negateInt(Integer v) {
		if (v == null) {
			return 0;
		}
		return -v;
	}

	private static String negateRebatePoint(String rebatePoint) {
		if (rebatePoint == null) {
			return "0";
		}
		String t = rebatePoint.trim();
		if (t.isEmpty() || "money".equalsIgnoreCase(t)) {
			return "0";
		}
		try {
			return Integer.toString(-Integer.parseInt(t));
		} catch (NumberFormatException e) {
			return "0";
		}
	}

	private static long parseRebatePointSigned(String rebatePoint) {
		if (rebatePoint == null) {
			return 0L;
		}
		String t = rebatePoint.trim();
		if (t.isEmpty() || "money".equalsIgnoreCase(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String detailWithCancelRemarks(String detail) {
		Map<String, Object> map = new LinkedHashMap<>();
		if (detail != null && !detail.isBlank()) {
			try {
				Map<String, Object> parsed =
						OBJECT_MAPPER.readValue(detail, new TypeReference<Map<String, Object>>() {});
				if (parsed != null) {
					map.putAll(parsed);
				}
			} catch (Exception ignored) {
				map.put("origin_detail", detail);
			}
		}
		map.put("remarks", REMARKS_CANCEL);
		try {
			return OBJECT_MAPPER.writeValueAsString(map);
		} catch (Exception e) {
			return "{\"remarks\":\"" + REMARKS_CANCEL + "\"}";
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
