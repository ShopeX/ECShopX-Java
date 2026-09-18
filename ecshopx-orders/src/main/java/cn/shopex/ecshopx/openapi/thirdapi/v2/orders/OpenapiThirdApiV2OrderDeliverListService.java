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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDeliverV2FailException;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.OrdersDeliveryItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2OrderDeliverListService {

	private static final DateTimeFormatter DELIVERY_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final ZoneId DELIVERY_TIME_ZONE = ZoneId.of("Asia/Shanghai");

	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final OrdersDeliveryItemsMapper ordersDeliveryItemsMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;

	public OpenapiThirdApiV2OrderDeliverListService(
			OrdersDeliveryMapper ordersDeliveryMapper,
			OrdersDeliveryItemsMapper ordersDeliveryItemsMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper) {
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.ordersDeliveryItemsMapper = ordersDeliveryItemsMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
	}

	public Map<String, Object> executeGetOrderDeliveryListV2(long companyId, String orderIdRaw) {
		if (!StringUtils.hasText(orderIdRaw)) {
			throw new OpenapiOrderDeliverV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "请填写订单编号");
		}

		Optional<Long> orderIdOpt = parseOrderIdForQuery(orderIdRaw);
		if (orderIdOpt.isEmpty()) {
			return emptyResult();
		}
		long orderId = orderIdOpt.get();

		int deliveryNum = 0;
		List<Map<String, Object>> list = new ArrayList<>();

		List<OrdersDelivery> deliveryList =
				ordersDeliveryMapper.selectList(
						new LambdaQueryWrapper<OrdersDelivery>()
								.eq(OrdersDelivery::getCompanyId, companyId)
								.eq(OrdersDelivery::getOrderId, orderId)
								.orderByAsc(OrdersDelivery::getOrdersDeliveryId));

		for (OrdersDelivery val : deliveryList) {
			List<OrdersDeliveryItems> deliveryItems =
					ordersDeliveryItemsMapper.selectList(
							new LambdaQueryWrapper<OrdersDeliveryItems>()
									.eq(
											OrdersDeliveryItems::getOrdersDeliveryId,
											val.getOrdersDeliveryId())
									.orderByAsc(OrdersDeliveryItems::getOrdersDeliveryItemsId));

			int itemsNum = 0;
			List<Map<String, Object>> items = new ArrayList<>();
			for (OrdersDeliveryItems row : deliveryItems) {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("order_items_id", row.getOrderItemsId());
				item.put("item_id", row.getItemId());
				item.put("num", nullToZero(row.getNum()));
				item.put("item_name", emptyIfNull(row.getItemName()));
				item.put("pic", emptyIfNull(row.getPic()));
				items.add(item);
				itemsNum += nullToZero(row.getNum());
			}

			Map<String, Object> line = new LinkedHashMap<>();
			line.put("delivery_id", val.getOrdersDeliveryId());
			line.put("delivery_corp", emptyIfNull(val.getDeliveryCorp()));
			line.put("delivery_corp_name", emptyIfNull(val.getDeliveryCorpName()));
			line.put("delivery_code", emptyIfNull(val.getDeliveryCode()));
			line.put("delivery_time", formatDeliveryTime(val.getDeliveryTime()));
			line.put("items", items);
			line.put("items_num", itemsNum);
			line.put("status_msg", "已发货");
			line.put("status", 1);
			list.add(line);
			deliveryNum++;
		}

		List<NormalOrdersItems> orderItems =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.orderByAsc(NormalOrdersItems::getId));

		List<Map<String, Object>> pendingItems = new ArrayList<>();
		int pendingItemsNum = 0;
		for (NormalOrdersItems row : orderItems) {
			if ("DONE".equals(row.getDeliveryStatus())) {
				continue;
			}
			int remain =
					nullToZero(row.getNum())
							- nullToZero(row.getCancelItemNum())
							- nullToZero(row.getDeliveryItemNum());
			if (remain <= 0) {
				continue;
			}
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("order_items_id", row.getId());
			item.put("item_id", row.getItemId());
			// items[].num 取订单行总 num；items_num 累加待发余量 remain
			item.put("num", nullToZero(row.getNum()));
			item.put("item_name", emptyIfNull(row.getItemName()));
			item.put("pic", emptyIfNull(row.getPic()));
			pendingItems.add(item);
			pendingItemsNum += remain;
		}

		if (!pendingItems.isEmpty()) {
			Map<String, Object> syn = new LinkedHashMap<>();
			syn.put("delivery_id", "");
			syn.put("delivery_corp", "");
			syn.put("delivery_corp_name", "");
			syn.put("delivery_code", "");
			syn.put("delivery_time", "");
			syn.put("items", pendingItems);
			syn.put("items_num", pendingItemsNum);
			syn.put("status_msg", "未发货");
			syn.put("status", 0);
			list.add(syn);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("delivery_num", deliveryNum);
		out.put("list", list);
		return out;
	}

	private static Optional<Long> parseOrderIdForQuery(String orderIdRaw) {
		try {
			return Optional.of(Long.parseLong(orderIdRaw.trim()));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static Map<String, Object> emptyResult() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("delivery_num", 0);
		out.put("list", List.of());
		return out;
	}

	private static String formatDeliveryTime(Integer epochSec) {
		int sec = epochSec == null ? 0 : epochSec;
		return Instant.ofEpochSecond(sec).atZone(DELIVERY_TIME_ZONE).format(DELIVERY_TIME_FMT);
	}

	private static int nullToZero(Integer v) {
		return v == null ? 0 : v;
	}

	private static String emptyIfNull(String s) {
		return s == null ? "" : s;
	}
}
