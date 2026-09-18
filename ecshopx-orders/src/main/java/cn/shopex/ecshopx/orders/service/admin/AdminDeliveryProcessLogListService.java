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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminDeliveryProcessLogListService {

	private final AdminDeliveryListsService adminDeliveryListsService;
	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final NormalOrdersRelDadaMapper normalOrdersRelDadaMapper;
	private final OrderDeliveryTimelineService orderDeliveryTimelineService;

	public AdminDeliveryProcessLogListService(
			AdminDeliveryListsService adminDeliveryListsService,
			NormalOrdersMapper normalOrdersMapper,
			TradeMapper tradeMapper,
			NormalOrdersRelDadaMapper normalOrdersRelDadaMapper,
			OrderDeliveryTimelineService orderDeliveryTimelineService) {
		this.adminDeliveryListsService = adminDeliveryListsService;
		this.normalOrdersMapper = normalOrdersMapper;
		this.tradeMapper = tradeMapper;
		this.normalOrdersRelDadaMapper = normalOrdersRelDadaMapper;
		this.orderDeliveryTimelineService = orderDeliveryTimelineService;
	}

	public LinkedHashMap<String, Object> processLogList(
			long companyId, String operatorType, long operatorId, String orderIdParam) {
		List<LinkedHashMap<String, Object>> deliveryList =
				adminDeliveryListsService.lists(
						companyId, operatorType == null ? "" : operatorType, operatorId, orderIdParam);

		String raw = orderIdParam == null ? "" : orderIdParam.trim();
		long dbOrderId;
		if (raw.isEmpty()) {
			throw new ResourceException("订单不存在");
		}
		try {
			dbOrderId = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			dbOrderId = 0L;
		}

		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, dbOrderId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("订单不存在");
		}

		LinkedHashMap<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("order_id", order.getOrderId());
		orderInfo.put("company_id", order.getCompanyId());
		orderInfo.put("receipt_type", order.getReceiptType() == null ? "" : order.getReceiptType());
		orderInfo.put("create_time", order.getCreateTime());
		orderInfo.put("end_time", order.getEndTime() == null ? 0 : order.getEndTime().intValue());

		LambdaQueryWrapper<Trade> tw =
				new LambdaQueryWrapper<Trade>()
						.eq(Trade::getCompanyId, String.valueOf(companyId))
						.eq(Trade::getOrderId, String.valueOf(dbOrderId))
						.orderByDesc(Trade::getTimeExpire)
						.orderByDesc(Trade::getTimeStart)
						.last("LIMIT 1");
		Trade trade = tradeMapper.selectOne(tw);
		Map<String, Object> tradeInfo;
		if (trade != null) {
			tradeInfo = new LinkedHashMap<>();
			tradeInfo.put("time_expire", trade.getTimeExpire());
			tradeInfo.put("time_start", trade.getTimeStart());
		} else {
			tradeInfo = null;
		}

		NormalOrdersRelDada dadaRow =
				normalOrdersRelDadaMapper.selectOne(
						new LambdaQueryWrapper<NormalOrdersRelDada>()
								.eq(NormalOrdersRelDada::getCompanyId, companyId)
								.eq(NormalOrdersRelDada::getOrderId, dbOrderId)
								.last("LIMIT 1"));
		Map<String, Object> dadaMap;
		if (dadaRow != null) {
			dadaMap = new LinkedHashMap<>();
			dadaMap.put("dada_status", dadaRow.getDadaStatus());
		} else {
			dadaMap = null;
		}

		List<Map<String, Object>> logs =
				orderDeliveryTimelineService.buildDeliveryLog(orderInfo, tradeInfo, dadaMap);

		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("delivery_list", deliveryList);
		body.put("logs", logs);
		return body;
	}
}
