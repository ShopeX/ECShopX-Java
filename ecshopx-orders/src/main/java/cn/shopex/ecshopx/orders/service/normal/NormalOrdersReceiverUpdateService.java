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

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.freight.NormalOrderShippingFreightCountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NormalOrdersReceiverUpdateService {

	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrderShippingFreightCountService normalOrderShippingFreightCountService;
	private final NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	public NormalOrdersReceiverUpdateService(
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrderShippingFreightCountService normalOrderShippingFreightCountService,
			NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrderShippingFreightCountService = normalOrderShippingFreightCountService;
		this.normalOrdersServiceOrderDataAssembler = normalOrdersServiceOrderDataAssembler;
	}

	public Map<String, Object> updateReceiverForEmployeePurchase(
			long companyId,
			long userId,
			long orderId,
			String receiverName,
			String receiverMobile,
			String receiverZip,
			String receiverState,
			String receiverCity,
			String receiverDistrict,
			String receiverAddress) {
		LambdaQueryWrapper<NormalOrders> q = new LambdaQueryWrapper<>();
		q.eq(NormalOrders::getCompanyId, companyId).eq(NormalOrders::getOrderId, orderId);
		NormalOrders order = normalOrdersMapper.selectOne(q);
		if (order == null) {
			throw new ResourceException("订单号为" + orderId + "的订单不存在");
		}
		if (!Objects.equals(order.getUserId(), userId)) {
			throw new ForbiddenException("只能修改自己的订单");
		}
		if (!"employee_purchase".equals(order.getOrderClass())) {
			throw new ResourceException("只能修改内购订单的收货地址");
		}
		if (!"logistics".equals(order.getReceiptType())) {
			throw new ResourceException("非快递配送订单不能修改收货地址");
		}

		LambdaQueryWrapper<NormalOrdersItems> iq = new LambdaQueryWrapper<>();
		iq.eq(NormalOrdersItems::getCompanyId, companyId).eq(NormalOrdersItems::getOrderId, orderId);
		List<NormalOrdersItems> items = normalOrdersItemsMapper.selectList(iq);
		List<Map<String, Object>> freightRows = new ArrayList<>();
		for (NormalOrdersItems row : items) {
			Map<String, Object> fr = new HashMap<>();
			fr.put("templates_id", row.getTemplatesId());
			fr.put(
					"weight",
					row.getWeight() == null ? 0.0 : row.getWeight().doubleValue());
			fr.put("total_fee", Objects.requireNonNullElse(row.getTotalFee(), 0));
			fr.put("num", Objects.requireNonNullElse(row.getNum(), 0));
			fr.put("volume", Objects.requireNonNullElse(row.getVolume(), 0));
			freightRows.add(fr);
		}

		long newFreightCents =
				normalOrderShippingFreightCountService.countFreightFee(
						freightRows, companyId, receiverState, receiverCity, receiverDistrict, true);
		long storedFreightCents = Optional.ofNullable(order.getFreightFee()).orElse(0).longValue();
		if (newFreightCents != storedFreightCents) {
			throw new ResourceException("新地址的运费和原订单不一致，不能修改");
		}

		LambdaUpdateWrapper<NormalOrders> u =
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.set(NormalOrders::getReceiverName, receiverName)
						.set(NormalOrders::getReceiverMobile, receiverMobile)
						.set(NormalOrders::getReceiverZip, receiverZip)
						.set(NormalOrders::getReceiverState, receiverState)
						.set(NormalOrders::getReceiverCity, receiverCity)
						.set(NormalOrders::getReceiverDistrict, receiverDistrict)
						.set(NormalOrders::getReceiverAddress, receiverAddress);
		int updated = normalOrdersMapper.update(null, u);
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		NormalOrders fresh = normalOrdersMapper.selectOne(q);
		return normalOrdersServiceOrderDataAssembler.toServiceOrderData(fresh);
	}
}
