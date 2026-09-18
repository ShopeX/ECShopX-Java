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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.common.port.systemlink.JushuitanTradeFinishOrderUploadAssemblePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JushuitanTradeFinishOrderUploadAssemblePortImpl implements JushuitanTradeFinishOrderUploadAssemblePort {

	private static final int ITEMS_LIMIT = 200;
	private static final int GROUP_MEMBER_LIMIT = 50;

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;

	public JushuitanTradeFinishOrderUploadAssemblePortImpl(
			NormalOrdersMapper normalOrdersMapper, NormalOrdersItemsMapper normalOrdersItemsMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
	}

	@Override
	public List<Map<String, Object>> assembleOrderUploadPayloads(
			long companyId, Map<String, Object> tradeRowSnakeCase, String jstShopId) {
		String sourceType = str(tradeRowSnakeCase.get("trade_source_type"));
		if ("normal_groups".equals(sourceType) || "groups".equals(sourceType)) {
			return assembleGroupPayloads(companyId, tradeRowSnakeCase, jstShopId);
		}
		if ("normal".equals(sourceType)
				|| "normal_shopguide".equals(sourceType)
				|| (StringUtils.hasText(sourceType) && sourceType.startsWith("normal_"))) {
			long orderId = longOrZero(tradeRowSnakeCase.get("order_id"));
			if (orderId <= 0) {
				return List.of();
			}
			Map<String, Object> one = buildOrdersUploadBiz(companyId, orderId, jstShopId, tradeRowSnakeCase);
			return one.isEmpty() ? List.of() : List.of(one);
		}
		return List.of();
	}

	private List<Map<String, Object>> assembleGroupPayloads(
			long companyId, Map<String, Object> tradeRowSnakeCase, String jstShopId) {
		long orderId = longOrZero(tradeRowSnakeCase.get("order_id"));
		if (orderId <= 0) {
			return List.of();
		}
		NormalOrders self =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (self == null) {
			return List.of();
		}
		Long actId = self.getActId();
		if (actId == null || actId <= 0) {
			return List.of();
		}
		List<NormalOrders> members =
				normalOrdersMapper.selectList(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getActId, actId)
								.eq(NormalOrders::getOrderStatus, "PAYED")
								.last("LIMIT " + GROUP_MEMBER_LIMIT));
		if (members == null || members.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (NormalOrders m : members) {
			if (m.getOrderId() == null) {
				continue;
			}
			Map<String, Object> biz =
					buildOrdersUploadBiz(companyId, m.getOrderId(), jstShopId, tradeRowSnakeCase);
			if (!biz.isEmpty()) {
				out.add(biz);
			}
		}
		return out;
	}

	private Map<String, Object> buildOrdersUploadBiz(
			long companyId, long orderId, String jstShopId, Map<String, Object> tradeRowSnakeCase) {
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (order == null) {
			return Map.of();
		}
		List<NormalOrdersItems> itemRows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.last("LIMIT " + ITEMS_LIMIT));
		if (itemRows == null || itemRows.isEmpty()) {
			return Map.of();
		}
		List<Map<String, Object>> lines = new ArrayList<>();
		for (NormalOrdersItems it : itemRows) {
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("sku_id", it.getItemBn() != null ? it.getItemBn() : "");
			line.put("name", it.getItemName() != null ? it.getItemName() : "");
			line.put("qty", it.getNum() != null ? it.getNum() : 0);
			lines.add(line);
		}
		Map<String, Object> orderMap = new LinkedHashMap<>();
		orderMap.put("so_id", String.valueOf(orderId));
		orderMap.put("shop_id", jstShopId);
		orderMap.put("items", lines);
		long triggerOrderId = longOrZero(tradeRowSnakeCase.get("order_id"));
		Map<String, Object> pay = new LinkedHashMap<>();
		if (orderId == triggerOrderId) {
			pay.put("outer_pay_id", str(tradeRowSnakeCase.get("transaction_id")));
			pay.put("pay_fee", intOrZero(tradeRowSnakeCase.get("pay_fee")));
			pay.put("total_fee", intOrZero(tradeRowSnakeCase.get("total_fee")));
		} else {
			pay.put("outer_pay_id", "");
			int fee = parseMoneyFen(order.getTotalFee());
			pay.put("pay_fee", fee);
			pay.put("total_fee", fee);
		}
		orderMap.put("pay", pay);
		return Map.of("orders", List.of(orderMap));
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long longOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intOrZero(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int parseMoneyFen(String totalFee) {
		if (!StringUtils.hasText(totalFee)) {
			return 0;
		}
		try {
			return Integer.parseInt(totalFee.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
