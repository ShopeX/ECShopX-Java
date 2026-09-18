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

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.service.brokerage.NormalOrderBrokerageFinishInput;
import cn.shopex.ecshopx.common.service.brokerage.NormalOrderFinishBrokerageCoordinator;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.popularize.service.BrokeragePlanCloseTimeService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 普通订单完成时的分销佣金处理入口（单笔核销流程内调用）。
 */
@Service
public class NormalOrderBrokerageOnFinishService {

	private static final DispatchOptions NORMAL_ORDER_CONFIRM_RECEIPT_PARENT_PUBLISH_OPTIONS =
			new DispatchOptions(
					DispatchMode.ASYNC,
					DispatchDriverType.REDIS,
					null,
					null,
					RetryPolicy.platformDefault());

	private final NormalOrderFinishBrokerageCoordinator normalOrderFinishBrokerageCoordinator;
	private final BrokeragePlanCloseTimeService brokeragePlanCloseTimeService;
	private final DispatchFacade dispatchFacade;

	public NormalOrderBrokerageOnFinishService(
			NormalOrderFinishBrokerageCoordinator normalOrderFinishBrokerageCoordinator,
			BrokeragePlanCloseTimeService brokeragePlanCloseTimeService,
			DispatchFacade dispatchFacade) {
		this.normalOrderFinishBrokerageCoordinator = normalOrderFinishBrokerageCoordinator;
		this.brokeragePlanCloseTimeService = brokeragePlanCloseTimeService;
		this.dispatchFacade = dispatchFacade;
	}

	public void orderFinishBrokerage(long companyId, long orderId, NormalOrders order) {
		if (companyId <= 0L || orderId <= 0L || order == null) {
			return;
		}
		NormalOrderBrokerageFinishInput finishInput =
				new NormalOrderBrokerageFinishInput(
						order.getUserId(), order.getOrderClass(), order.getTotalFee(), order.getCommissionFee());
		publishNormalOrderConfirmReceiptEvent(companyId, orderId);
		normalOrderFinishBrokerageCoordinator.onNormalOrderFinishBrokerage(companyId, orderId, finishInput);
		brokeragePlanCloseTimeService.updatePlanCloseTime(companyId, orderId);
	}

	private void publishNormalOrderConfirmReceiptEvent(long companyId, long orderId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		dispatchFacade.publishEvent(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_CONFIRM_RECEIPT,
				payload,
				NORMAL_ORDER_CONFIRM_RECEIPT_PARENT_PUBLISH_OPTIONS);
	}
}
