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

package cn.shopex.ecshopx.orders.service.localdelivery;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LocalDeliveryOrderInfoLoadService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	public LocalDeliveryOrderInfoLoadService(
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
	}

	public Map<String, Object> loadOrderInfoForDadaFreight(long companyId, long orderId) {
		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("此订单不存在！");
		}
		String effective = effectiveOrderType(assoc);
		if (!isSupportedForNormalPhysicalOrder(effective)) {
			throw new ResourceException("同城配运费询价不支持该订单类型");
		}
		NormalOrders order =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getOrderId, orderId)
								.eq(NormalOrders::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (order == null) {
			throw new ResourceException("此订单不存在！");
		}
		Map<String, Object> orderInfo =
				new LinkedHashMap<>(normalOrdersServiceOrderDataAssembler.toServiceOrderData(order));
		orderInfo.put("order_id", Long.toString(orderId));
		orderInfo.put("company_id", companyId);

		List<NormalOrdersItems> itemRows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId)
								.orderByAsc(NormalOrdersItems::getId));
		List<Map<String, Object>> itemMaps = new ArrayList<>();
		for (NormalOrdersItems it : itemRows) {
			itemMaps.add(itemRowToDadaMap(it));
		}
		orderInfo.put("items", itemMaps);
		return orderInfo;
	}

	private static String effectiveOrderType(OrderAssociations assoc) {
		String ot = assoc.getOrderType() == null ? "" : assoc.getOrderType().toLowerCase(Locale.ROOT);
		String oc = assoc.getOrderClass() == null ? "" : assoc.getOrderClass().toLowerCase(Locale.ROOT);
		if (("normal".equals(ot) || "service".equals(ot))
				&& !ot.equals(oc)
				&& !"normal".equals(oc)
				&& !"service".equals(oc)) {
			return ot + "_" + oc;
		}
		return ot;
	}

	private static boolean isSupportedForNormalPhysicalOrder(String effective) {
		if (effective.isEmpty()) {
			return false;
		}
		if ("membercard".equals(effective) || "supplier_order".equals(effective)) {
			return false;
		}
		if ("normal".equals(effective)
				|| "normal_shopadmin".equals(effective)
				|| "service".equals(effective)
				|| effective.startsWith("service_")
				|| "bargain".equals(effective)
				|| "normal_bargain".equals(effective)) {
			return true;
		}
		return effective.startsWith("normal_");
	}

	private static Map<String, Object> itemRowToDadaMap(NormalOrdersItems it) {
		Map<String, Object> im = new LinkedHashMap<>();
		im.put("item_name", it.getItemName() == null ? "" : it.getItemName());
		im.put("item_bn", it.getItemBn() == null ? "" : it.getItemBn());
		im.put("num", it.getNum() == null ? 0 : it.getNum());
		im.put("item_unit", it.getItemUnit() == null ? "" : it.getItemUnit());
		float w = it.getWeight() == null ? 0.0f : it.getWeight();
		im.put("weight", w);
		return im;
	}
}
