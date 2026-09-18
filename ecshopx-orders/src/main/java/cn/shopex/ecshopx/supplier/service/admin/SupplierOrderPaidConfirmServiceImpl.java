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

package cn.shopex.ecshopx.supplier.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.constants.SupplierOrderStatusConstants;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.event.SupplierOrderPaidConfirmCommittedSpringEvent;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierOrderPaidConfirmServiceImpl implements SupplierOrderPaidConfirmService {

	private final SupplierOrderMapper supplierOrderMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final SupplierOfflineTradeWriteService supplierOfflineTradeWriteService;
	private final ApplicationEventPublisher publisher;

	public SupplierOrderPaidConfirmServiceImpl(
			SupplierOrderMapper supplierOrderMapper,
			NormalOrdersMapper normalOrdersMapper,
			SupplierOfflineTradeWriteService supplierOfflineTradeWriteService,
			ApplicationEventPublisher publisher) {
		this.supplierOrderMapper = supplierOrderMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.supplierOfflineTradeWriteService = supplierOfflineTradeWriteService;
		this.publisher = publisher;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void orderPaidConfirm(long companyId, long supplierId, long orderId) {
		if (supplierId > Integer.MAX_VALUE) {
			throw new BadRequestException("operator_id 无效");
		}
		int supplierIdInt = (int) supplierId;
		SupplierOrder row = supplierOrderMapper.selectOne(new LambdaQueryWrapper<SupplierOrder>()
				.eq(SupplierOrder::getCompanyId, companyId)
				.eq(SupplierOrder::getSupplierId, supplierIdInt)
				.eq(SupplierOrder::getOrderId, orderId)
				.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("订单不存在");
		}
		if (!SupplierOrderStatusConstants.WAIT_PAID_CONFIRM.equals(row.getOrderStatus())) {
			throw new ResourceException("订单不是待确认状态");
		}

		int u1 = supplierOrderMapper.update(
				null,
				new LambdaUpdateWrapper<SupplierOrder>()
						.eq(SupplierOrder::getCompanyId, companyId)
						.eq(SupplierOrder::getSupplierId, supplierIdInt)
						.eq(SupplierOrder::getOrderId, orderId)
						.eq(SupplierOrder::getOrderStatus, SupplierOrderStatusConstants.WAIT_PAID_CONFIRM)
						.set(SupplierOrder::getOrderStatus, SupplierOrderStatusConstants.PAYED));
		if (u1 == 0) {
			throw new ResourceException("订单不是待确认状态");
		}

		List<SupplierOrder> orders = supplierOrderMapper.selectList(new LambdaQueryWrapper<SupplierOrder>()
				.eq(SupplierOrder::getCompanyId, companyId)
				.eq(SupplierOrder::getOrderId, orderId));
		if (orders == null || orders.isEmpty()) {
			throw new ResourceException("订单不存在");
		}

		String orderStatus = SupplierOrderStatusConstants.PAYED;
		for (SupplierOrder o : orders) {
			String st = o.getOrderStatus();
			if (!SupplierOrderStatusConstants.NOTPAY.equals(st)
					&& !SupplierOrderStatusConstants.PART_PAYMENT.equals(st)
					&& !SupplierOrderStatusConstants.PAYED.equals(st)) {
				orderStatus = st;
				break;
			}
			if (!orderStatus.equals(st)) {
				orderStatus = SupplierOrderStatusConstants.PART_PAYMENT;
			}
		}

		int u2 = normalOrdersMapper.update(
				null,
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.set(NormalOrders::getOrderStatus, orderStatus));
		if (u2 == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		supplierOfflineTradeWriteService.createOfflineTradeAndMarkSuccess(companyId, supplierId, orderId);

		NormalOrders main = normalOrdersMapper.selectOne(new LambdaQueryWrapper<NormalOrders>()
				.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderId)
				.last("LIMIT 1"));
		Map<String, Object> orderInfoMap = main == null ? null : normalOrderEntityToSnakeMap(main);

		publisher.publishEvent(new SupplierOrderPaidConfirmCommittedSpringEvent(
				this, companyId, supplierId, orderId, orderInfoMap));
	}

	private static Map<String, Object> normalOrderEntityToSnakeMap(NormalOrders m) {
		Map<String, Object> map = new LinkedHashMap<>();
		putLong(map, "order_id", m.getOrderId());
		putLong(map, "company_id", m.getCompanyId());
		map.put("order_status", m.getOrderStatus());
		map.put("title", m.getTitle());
		putLong(map, "user_id", m.getUserId());
		putLong(map, "shop_id", m.getShopId());
		putLong(map, "distributor_id", m.getDistributorId());
		map.put("total_fee", m.getTotalFee());
		map.put("pay_status", m.getPayStatus());
		map.put("pay_type", m.getPayType());
		map.put("fee_type", m.getFeeType());
		map.put("fee_rate", m.getFeeRate());
		map.put("fee_symbol", m.getFeeSymbol());
		map.put("mobile", m.getMobile());
		map.put("receiver_mobile", m.getReceiverMobile());
		map.put("order_type", m.getOrderType());
		putInt(map, "create_time", m.getCreateTime());
		putInt(map, "update_time", m.getUpdateTime());
		putInt(map, "supplier_id", m.getSupplierId());
		putLong(map, "merchant_id", m.getMerchantId());
		map.put("order_class", m.getOrderClass());
		map.put("pay_channel", m.getPayChannel());
		map.put("order_source", m.getOrderSource());
		map.put("order_holder", m.getOrderHolder());
		putLong(map, "act_id", m.getActId());
		map.put("receipt_type", m.getReceiptType());
		map.put("delivery_status", m.getDeliveryStatus());
		map.put("cancel_status", m.getCancelStatus());
		return map;
	}

	private static void putLong(Map<String, Object> map, String key, Long v) {
		map.put(key, v == null ? 0L : v);
	}

	private static void putInt(Map<String, Object> map, String key, Integer v) {
		map.put(key, v == null ? 0 : v);
	}
}
