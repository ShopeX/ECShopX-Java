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

package cn.shopex.ecshopx.orders.service.group;

import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.orders.port.NormalGroupsTradeFinishRefundBranchPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NormalGroupsTradeFinishRefundOrchestrator {

	private final NormalGroupsTradeFinishRefundBranchPort normalGroupsTradeFinishRefundBranchPort;
	private final TradeMapper tradeMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	public RefundBranchOutcome tryRefundDisabledMemberInFormedTeamAndPublishTradeUpdate(
			long companyId, long userId, long orderId) {
		if (!normalGroupsTradeFinishRefundBranchPort.shouldRefundDisabledMemberInFormedTeam(
				companyId, orderId, userId)) {
			return RefundBranchOutcome.NOT_APPLICABLE_CONTINUE_PAYED;
		}
		Trade successTrade =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getOrderId, String.valueOf(orderId))
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getTradeState, "SUCCESS")
								.last("LIMIT 1"));
		if (successTrade == null) {
			return RefundBranchOutcome.REFUND_SKIPPED;
		}
		normalOrdersMapper.update(
				null,
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId)
						.set(NormalOrders::getOrderStatus, "CANCEL"));
		int assocUpdated =
				orderAssociationsMapper.update(
						null,
						new LambdaUpdateWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.set(OrderAssociations::getOrderStatus, "CANCEL"));
		if (assocUpdated <= 0) {
			return RefundBranchOutcome.REFUND_SKIPPED;
		}
		OrderAssociations row =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderId)
								.last("LIMIT 1"));
		if (row == null) {
			return RefundBranchOutcome.REFUND_SKIPPED;
		}
		thirdPartyTradeUpdateDispatchPublisher.publish(
				Map.copyOf(associationRowToTradeUpdatePayload(row)));
		return RefundBranchOutcome.REFUND_PUBLISHED;
	}

	static Map<String, Object> associationRowToTradeUpdatePayload(OrderAssociations a) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		if (a.getCompanyId() != null) {
			m.put("company_id", a.getCompanyId());
		}
		if (a.getOrderId() != null) {
			m.put("order_id", String.valueOf(a.getOrderId()));
		}
		if (a.getUserId() != null) {
			m.put("user_id", a.getUserId());
		}
		if (a.getOrderClass() != null) {
			m.put("order_class", a.getOrderClass());
		}
		if (a.getOrderStatus() != null) {
			m.put("order_status", a.getOrderStatus());
		}
		if (a.getOrderType() != null) {
			m.put("order_type", a.getOrderType());
		}
		if (a.getShopId() != null) {
			m.put("shop_id", a.getShopId());
		}
		if (a.getMobile() != null) {
			m.put("mobile", a.getMobile());
		}
		if (a.getTitle() != null) {
			m.put("title", a.getTitle());
		}
		if (a.getTotalFee() != null) {
			m.put("total_fee", a.getTotalFee());
		}
		if (a.getCancelStatus() != null) {
			m.put("cancel_status", a.getCancelStatus());
		}
		if (a.getDeliveryStatus() != null) {
			m.put("delivery_status", a.getDeliveryStatus());
		}
		return m;
	}

	public enum RefundBranchOutcome {
		REFUND_PUBLISHED,
		REFUND_SKIPPED,
		NOT_APPLICABLE_CONTINUE_PAYED
	}
}
