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

package cn.shopex.ecshopx.promotions.integration.order;

import cn.shopex.ecshopx.common.dispatch.RefundByOrderUpdateOrderStatusJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import org.springframework.stereotype.Service;

@Service("promotionGroupTeamOrderFailHandler")
public class RefundByOrderUpdateOrderStatusPromotionGroupTeamOrderFailHandler
		implements PromotionGroupTeamOrderFailHandler {

	private final RefundByOrderUpdateOrderStatusJobDispatchPublisher refundByOrderUpdateOrderStatusJobDispatchPublisher;

	public RefundByOrderUpdateOrderStatusPromotionGroupTeamOrderFailHandler(
			RefundByOrderUpdateOrderStatusJobDispatchPublisher refundByOrderUpdateOrderStatusJobDispatchPublisher) {
		this.refundByOrderUpdateOrderStatusJobDispatchPublisher = refundByOrderUpdateOrderStatusJobDispatchPublisher;
	}

	@Override
	public void onFailedTeamMember(PromotionGroupsTeam team, PromotionGroupsTeamMember member) {
		if (member == null || member.getOrderId() == null) {
			return;
		}
		String orderIdRaw = member.getOrderId().trim();
		if (orderIdRaw.isEmpty()) {
			return;
		}
		long orderId;
		try {
			orderId = Long.parseLong(orderIdRaw);
		} catch (NumberFormatException e) {
			return;
		}
		if (orderId <= 0) {
			return;
		}

		long companyId = member.getCompanyId() != null ? member.getCompanyId() : 0L;
		if (companyId <= 0 && team != null && team.getCompanyId() != null) {
			companyId = team.getCompanyId();
		}
		if (companyId <= 0) {
			return;
		}

		String groupGoodsType = "";
		if (member.getGroupGoodsType() != null) {
			groupGoodsType = member.getGroupGoodsType().trim();
		}
		if (groupGoodsType.isEmpty() && team != null && team.getGroupGoodsType() != null) {
			groupGoodsType = team.getGroupGoodsType().trim();
		}
		String orderType =
				"services".equalsIgnoreCase(groupGoodsType) ? "service_groups" : "normal_groups";

		refundByOrderUpdateOrderStatusJobDispatchPublisher.publish(orderId, companyId, orderType);
	}
}
