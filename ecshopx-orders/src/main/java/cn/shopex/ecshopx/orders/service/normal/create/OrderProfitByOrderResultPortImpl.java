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

package cn.shopex.ecshopx.orders.service.normal.create;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderProfitByOrderResultPort;
import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.mapper.OrderProfitMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderProfitByOrderResultPortImpl implements OrderProfitByOrderResultPort {

	private final OrderProfitMapper orderProfitMapper;
	private final ObjectMapper objectMapper;

	public OrderProfitByOrderResultPortImpl(OrderProfitMapper orderProfitMapper, ObjectMapper objectMapper) {
		this.orderProfitMapper = orderProfitMapper;
		this.objectMapper = objectMapper;
	}

	@Override
	public void profitByOrderResult(NormalOrderCreateParams p) {
		Map<String, Object> od = p.getOrderData();
		String orderClass = String.valueOf(od.getOrDefault("order_class", ""));
		if ("pointsmall".equals(orderClass) || "employee_purchase".equals(orderClass)) {
			return;
		}
		long orderId = longVal(od.get("order_id"), 0L);
		if (orderId <= 0L) {
			return;
		}
		OrderProfit existing = orderProfitMapper.selectOne(new LambdaQueryWrapper<OrderProfit>()
				.eq(OrderProfit::getOrderId, orderId)
				.last("LIMIT 1"));
		if (existing != null) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		long companyId = longVal(od.get("company_id"), 0L);
		long userId = longVal(od.get("user_id"), 0L);
		long totalOrderFen = longVal(od.get("total_fee"), 0L);
		int pointFee = intVal(od.get("point_fee"), 0);
		long payFen = Math.max(0L, totalOrderFen);
		long poolFen = Math.max(0L, payFen + (long) pointFee);
		long distributorId = longVal(od.get("distributor_id"), 0L);
		long salesmanId = longVal(od.get("salesman_id"), 0L);
		OrderProfit row = new OrderProfit();
		row.setOrderId(orderId);
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setOrderProfitStatus(0L);
		row.setProfitType(2);
		row.setPayFee(payFen);
		row.setTotalFee(poolFen);
		row.setDealerId(0L);
		row.setDistributorId(0L);
		row.setOrderDistributorId(distributorId);
		row.setDistributorNid(0L);
		row.setSellerId(0L);
		row.setPopularizeDistributorId(0L);
		row.setPopularizeSellerId(salesmanId);
		row.setProprietary(distributorId > 0L ? 1L : 0L);
		row.setPopularizeProprietary(2L);
		row.setDealers(0L);
		row.setSeller(0L);
		row.setDistributor(0L);
		row.setPopularizeSeller(0L);
		row.setPopularizeDistributor(0L);
		row.setCommission(0L);
		try {
			Map<String, Object> rule = new LinkedHashMap<>();
			rule.put("order_class", orderClass);
			rule.put("order_type", String.valueOf(od.getOrDefault("order_type", "")));
			rule.put("pay_type", String.valueOf(od.getOrDefault("pay_type", "")));
			rule.put("point_fee", pointFee);
			row.setRule(objectMapper.writeValueAsString(rule));
		} catch (JsonProcessingException e) {
			row.setRule("{}");
		}
		row.setCreated(now);
		row.setUpdated(now);
		orderProfitMapper.insert(row);
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
