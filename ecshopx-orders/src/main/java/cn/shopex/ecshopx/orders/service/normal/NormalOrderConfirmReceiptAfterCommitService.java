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

import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.supplier.service.SupplierOrderConfirmReceiptSyncService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NormalOrderConfirmReceiptAfterCommitService {

	private final SupplierOrderConfirmReceiptSyncService supplierOrderConfirmReceiptSyncService;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService;
	private final PointMemberAddPointService pointMemberAddPointService;
	private final NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;
	private final OrderProfitPlanCloseTimeWriteService orderProfitPlanCloseTimeWriteService;

	public NormalOrderConfirmReceiptAfterCommitService(
			SupplierOrderConfirmReceiptSyncService supplierOrderConfirmReceiptSyncService,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService,
			PointMemberAddPointService pointMemberAddPointService,
			NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService,
			OrderProfitPlanCloseTimeWriteService orderProfitPlanCloseTimeWriteService) {
		this.supplierOrderConfirmReceiptSyncService = supplierOrderConfirmReceiptSyncService;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.ordersRelChinaumspayDivisionWriteService = ordersRelChinaumspayDivisionWriteService;
		this.pointMemberAddPointService = pointMemberAddPointService;
		this.normalOrderBrokerageOnFinishService = normalOrderBrokerageOnFinishService;
		this.orderProfitPlanCloseTimeWriteService = orderProfitPlanCloseTimeWriteService;
	}

	public void run(
			long companyId,
			long orderIdNum,
			NormalOrders fresh,
			int nowSec,
			long autoClose,
			String operatorType,
			long operatorId,
			Integer bonusPoints) {
		supplierOrderConfirmReceiptSyncService.confirmReceipt(companyId, orderIdNum);

		LinkedHashMap<String, Object> log = new LinkedHashMap<>();
		log.put("order_id", orderIdNum);
		log.put("company_id", companyId);
		log.put("operator_type", operatorType);
		log.put("operator_id", operatorId);
		log.put("remarks", "订单完成");
		String detailPrefix = "user".equals(operatorType) ? "订单单号：" : "订单号：";
		log.put("detail", detailPrefix + orderIdNum + "，订单完成");
		log.put("params", Map.of());
		orderProcessLogPublishPort.publish(log);

		if ("chinaums".equals(fresh.getPayType())
				&& fresh.getDistributorId() != null
				&& fresh.getDistributorId() > 0) {
			ordersRelChinaumspayDivisionWriteService.addRelChinaumsPayDivision(companyId, fresh);
		}

		if (bonusPoints != null
				&& bonusPoints > 0
				&& fresh.getUserId() != null
				&& fresh.getUserId() > 0) {
			pointMemberAddPointService.addPointForNormalOrderBonus(
					fresh.getUserId(), companyId, bonusPoints, orderIdNum);
		}

		normalOrderBrokerageOnFinishService.orderFinishBrokerage(companyId, orderIdNum, fresh);
		orderProfitPlanCloseTimeWriteService.orderProfitPlanCloseTime(companyId, orderIdNum);
	}
}
