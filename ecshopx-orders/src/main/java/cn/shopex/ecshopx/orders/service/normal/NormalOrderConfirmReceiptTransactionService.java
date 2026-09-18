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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NormalOrderConfirmReceiptTransactionService {

	private static <T> T firstRowOrNull(List<T> rows) {
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		return rows.get(0);
	}

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final SupplierOrderMapper supplierOrderMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;

	public NormalOrderConfirmReceiptTransactionService(
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			SupplierOrderMapper supplierOrderMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper) {
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.supplierOrderMapper = supplierOrderMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public NormalOrders applyConfirmReceiptInTransaction(
			long companyId,
			long orderIdNum,
			String trimmed,
			NormalOrders order,
			long autoClose,
			int nowSec) {
		if (order == null) {
			throw new ResourceException("订单号为" + trimmed + "的订单不存在");
		}
		List<NormalOrdersItems> lines =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderIdNum));

		Long supCount =
				supplierOrderMapper.selectCount(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getCompanyId, companyId)
								.eq(SupplierOrder::getOrderId, orderIdNum));
		if (supCount != null && supCount > 0) {
			LambdaUpdateWrapper<SupplierOrder> suw = new LambdaUpdateWrapper<>();
			suw.eq(SupplierOrder::getCompanyId, companyId)
					.eq(SupplierOrder::getOrderId, orderIdNum)
					.set(SupplierOrder::getOrderStatus, "DONE")
					.set(SupplierOrder::getEndTime, (long) nowSec);
			supplierOrderMapper.update(null, suw);
		}

		LambdaUpdateWrapper<OrderAssociations> au = new LambdaUpdateWrapper<>();
		au.eq(OrderAssociations::getCompanyId, companyId)
				.eq(OrderAssociations::getOrderId, orderIdNum)
				.set(OrderAssociations::getOrderStatus, "DONE")
				.set(OrderAssociations::getEndTime, (long) nowSec)
				.set(OrderAssociations::getUpdateTime, nowSec);
		orderAssociationsMapper.update(null, au);

		LambdaUpdateWrapper<NormalOrders> uw = new LambdaUpdateWrapper<>();
		uw.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getOrderId, orderIdNum)
				.eq(NormalOrders::getOrderStatus, "WAIT_BUYER_CONFIRM")
				.set(NormalOrders::getOrderStatus, "DONE")
				.set(NormalOrders::getEndTime, (long) nowSec)
				.set(NormalOrders::getOrderAutoCloseAftersalesTime, (int) autoClose);
		int updated = normalOrdersMapper.update(null, uw);
		if (updated == 0) {
			throw new ResourceException("没有需要完成的订单!");
		}

		if (autoClose > 0) {
			for (NormalOrdersItems line : lines) {
				if (line == null || line.getId() == null) {
					continue;
				}
				LambdaUpdateWrapper<NormalOrdersItems> iu = new LambdaUpdateWrapper<>();
				iu.eq(NormalOrdersItems::getId, line.getId())
						.set(NormalOrdersItems::getAutoCloseAftersalesTime, (int) autoClose);
				normalOrdersItemsMapper.update(null, iu);
			}
		}

		NormalOrders fresh =
				firstRowOrNull(
						normalOrdersMapper.selectList(
								new LambdaQueryWrapper<NormalOrders>()
										.eq(NormalOrders::getCompanyId, companyId)
										.eq(NormalOrders::getOrderId, orderIdNum)
										.last("LIMIT 1")));
		if (fresh == null) {
			throw new ResourceException("订单号为" + trimmed + "的订单不存在");
		}
		return fresh;
	}
}
