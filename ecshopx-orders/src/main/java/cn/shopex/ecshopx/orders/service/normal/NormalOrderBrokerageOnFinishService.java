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
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.popularize.service.BrokeragePlanCloseTimeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;

	public NormalOrderBrokerageOnFinishService(
			NormalOrderFinishBrokerageCoordinator normalOrderFinishBrokerageCoordinator,
			BrokeragePlanCloseTimeService brokeragePlanCloseTimeService,
			DispatchFacade dispatchFacade,
			NormalOrdersItemsMapper normalOrdersItemsMapper) {
		this.normalOrderFinishBrokerageCoordinator = normalOrderFinishBrokerageCoordinator;
		this.brokeragePlanCloseTimeService = brokeragePlanCloseTimeService;
		this.dispatchFacade = dispatchFacade;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
	}

	/** 支付成功：仅写佣；计划结算时间在订单完成后刷新。 */
	public void orderFinishBrokerageOnTradePaySuccess(long companyId, long orderId, NormalOrders order) {
		if (companyId <= 0L || orderId <= 0L || order == null) {
			return;
		}
		NormalOrderBrokerageFinishInput finishInput = buildFinishInput(companyId, orderId, order);
		normalOrderFinishBrokerageCoordinator.onNormalOrderFinishBrokerage(companyId, orderId, finishInput);
	}

	/** 订单完成（确认收货、批量完成、自提核销等）：写佣幂等 + 按配置刷新 plan_close_time。 */
	public void orderFinishBrokerage(long companyId, long orderId, NormalOrders order) {
		if (companyId <= 0L || orderId <= 0L || order == null) {
			return;
		}
		NormalOrderBrokerageFinishInput finishInput = buildFinishInput(companyId, orderId, order);
		publishNormalOrderConfirmReceiptEvent(companyId, orderId);
		normalOrderFinishBrokerageCoordinator.onNormalOrderFinishBrokerage(companyId, orderId, finishInput);
		brokeragePlanCloseTimeService.updatePlanCloseTime(companyId, orderId);
	}

	private NormalOrderBrokerageFinishInput buildFinishInput(
			long companyId, long orderId, NormalOrders order) {
		return new NormalOrderBrokerageFinishInput(
				order.getUserId(),
				order.getOrderClass(),
				order.getTotalFee(),
				order.getCommissionFee(),
				loadOrderLines(companyId, orderId));
	}

	private List<NormalOrderBrokerageFinishInput.Line> loadOrderLines(long companyId, long orderId) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		if (items == null || items.isEmpty()) {
			return List.of();
		}
		List<NormalOrderBrokerageFinishInput.Line> lines = new ArrayList<>(items.size());
		for (NormalOrdersItems item : items) {
			if (item == null || item.getItemId() == null || item.getItemId() <= 0L) {
				continue;
			}
			int num = item.getNum() == null ? 0 : item.getNum();
			long totalFee = item.getTotalFee() == null ? 0L : item.getTotalFee().longValue();
			long costFee = item.getCostFee() == null ? 0L : item.getCostFee().longValue();
			double feeRate = item.getFeeRate() == null ? 1.0d : item.getFeeRate().doubleValue();
			lines.add(
					new NormalOrderBrokerageFinishInput.Line(
							item.getItemId(), num, totalFee, costFee, feeRate));
		}
		return lines;
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
