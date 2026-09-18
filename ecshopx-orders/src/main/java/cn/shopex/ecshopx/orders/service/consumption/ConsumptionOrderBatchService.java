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

package cn.shopex.ecshopx.orders.service.consumption;

import cn.shopex.ecshopx.members.service.stats.MemberAggregateConsumptionFromCronService;
import cn.shopex.ecshopx.members.service.stats.MemberAggregateConsumptionFromCronService.ConsumptionAggregate;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.domain.dto.OrderIdRefundSumRow;
import cn.shopex.ecshopx.orders.mapper.ConsumptionOrderDqlMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消费累加批处理：队列作业 handle 与 doNotAftersalesConsumption 同线程语义对齐。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumptionOrderBatchService {

	private static final int DEFAULT_PAGE_SIZE = 100;

	private final ConsumptionOrderDqlMapper consumptionOrderDqlMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final MemberAggregateConsumptionFromCronService memberAggregateConsumptionFromCronService;

	public int runEquivalentToHandle() {
		return runEquivalentToHandle(
				Map.of("orderType", "normal", "pageSize", "100"));
	}

	/**
	 * @param jobData payload: {@code orderType}（默认 {@code normal}）、{@code pageSize} 字符串页大小（默认
	 *     {@code 100}）
	 */
	public int runEquivalentToHandle(Map<String, Object> jobData) {
		Objects.requireNonNull(jobData, "jobData");
		String orderType = stringProp(jobData, "orderType", "normal");
		if (!"normal".equals(orderType)) {
			log.warn("[consumption-orders] unsupported orderType={}, skipping batch", orderType);
			return 0;
		}
		int pageSize = parsePageSize(stringProp(jobData, "pageSize", "100"));
		long timeSec = System.currentTimeMillis() / 1000L;
		List<Long> haveAftersales = consumptionOrderDqlMapper.selectHaveAftersalesOrderIds(timeSec);
		if (haveAftersales == null) {
			haveAftersales = new ArrayList<>();
		}
		int pageIndex = 0;
		int totalMarked = 0;
		while (true) {
			List<NormalOrders> page =
					normalOrdersMapper.selectConsumptionCandidatePage(
							timeSec, haveAftersales, pageSize);
			if (page == null || page.isEmpty()) {
				break;
			}
			List<Long> orderIds =
					page.stream().map(NormalOrders::getOrderId).filter(id -> id != null && id > 0L).toList();
			if (orderIds.isEmpty()) {
				break;
			}
			int n = doNotAftersalesConsumption(orderIds);
			totalMarked += n;
			log.info("[consumption-orders] batch i={} marked={}", pageIndex, n);
			pageIndex++;
		}
		return totalMarked;
	}

	private static String stringProp(Map<String, Object> m, String key, String defaultValue) {
		Object v = m.get(key);
		if (v == null) {
			return defaultValue;
		}
		return String.valueOf(v);
	}

	private static int parsePageSize(String raw) {
		try {
			int n = Integer.parseInt(raw.trim());
			if (n < 1) {
				return DEFAULT_PAGE_SIZE;
			}
			return Math.min(n, 10_000);
		} catch (NumberFormatException e) {
			return DEFAULT_PAGE_SIZE;
		}
	}

	int doNotAftersalesConsumption(List<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return 0;
		}
		List<Long> processedOrderIds = List.of();
		if (!orderIds.isEmpty()) {
			List<Long> raw = consumptionOrderDqlMapper.selectProcessedAftersalesOrderIds(orderIds);
			processedOrderIds = raw == null ? List.of() : raw;
		}
		Map<Long, BigDecimal> refundByOrder = new HashMap<>();
		if (!processedOrderIds.isEmpty()) {
			List<OrderIdRefundSumRow> sums =
					consumptionOrderDqlMapper.selectRefundSumsExcludingPoint(processedOrderIds);
			if (sums != null) {
				for (OrderIdRefundSumRow r : sums) {
					if (r.getOrderId() != null && r.getSumRefundedFee() != null) {
						refundByOrder.put(r.getOrderId(), r.getSumRefundedFee());
					}
				}
			}
		}
		List<String> orderIdStrs = orderIds.stream().map(String::valueOf).toList();
		List<Trade> trades =
				tradeMapper.selectList(
						new LambdaQueryWrapper<Trade>()
								.in(Trade::getOrderId, orderIdStrs)
								.eq(Trade::getTradeState, "SUCCESS")
								.ne(Trade::getPayType, "point"));
		Map<Long, ConsumptionAggregate> consumption = new HashMap<>();
		if (trades != null) {
			for (Trade t : trades) {
				if (t.getUserId() == null || t.getUserId().isEmpty()) {
					continue;
				}
				long userId;
				try {
					userId = Long.parseLong(t.getUserId().trim());
				} catch (NumberFormatException e) {
					continue;
				}
				long orderIdL = t.getOrderId() == null ? 0L : Long.parseLong(t.getOrderId().trim());
				long companyIdL =
						t.getCompanyId() == null || t.getCompanyId().isEmpty()
								? 0L
								: Long.parseLong(t.getCompanyId().trim());
				int payFen = t.getPayFee() == null ? 0 : t.getPayFee();
				BigDecimal add = new BigDecimal(payFen);
				BigDecimal sub = refundByOrder.getOrDefault(orderIdL, BigDecimal.ZERO);
				ConsumptionAggregate exist = consumption.get(userId);
				if (exist == null) {
					consumption.put(
							userId,
							new ConsumptionAggregate(userId, companyIdL, add.subtract(sub)));
				} else {
					BigDecimal next = exist.payFen().add(add).subtract(sub);
					consumption.put(
							userId, new ConsumptionAggregate(userId, exist.companyId(), next));
				}
			}
		}
		memberAggregateConsumptionFromCronService.applyAggregates(consumption);
		int n = orderIds.size();
		normalOrdersMapper.updateIsConsumptionWhenZero(orderIds);
		return n;
	}
}
