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

package cn.shopex.ecshopx.employeepurchase.integration;

import cn.shopex.ecshopx.common.port.order.EmployeePurchasePrepaidAftersalesRestorePort;
import cn.shopex.ecshopx.employeepurchase.domain.OrdersRelActivity;
import cn.shopex.ecshopx.employeepurchase.mapper.OrdersRelActivityMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchasePrepaidRestoreService;
import cn.shopex.ecshopx.employeepurchase.service.MemberActivityItemsAggregateService;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EmployeePurchasePrepaidAftersalesRestorePortImpl
		implements EmployeePurchasePrepaidAftersalesRestorePort {

	private final OrdersRelActivityMapper ordersRelActivityMapper;
	private final EmployeePurchasePrepaidRestoreService employeePurchasePrepaidRestoreService;
	private final MemberActivityItemsAggregateService memberActivityItemsAggregateService;

	public EmployeePurchasePrepaidAftersalesRestorePortImpl(
			OrdersRelActivityMapper ordersRelActivityMapper,
			EmployeePurchasePrepaidRestoreService employeePurchasePrepaidRestoreService,
			MemberActivityItemsAggregateService memberActivityItemsAggregateService) {
		this.ordersRelActivityMapper = ordersRelActivityMapper;
		this.employeePurchasePrepaidRestoreService = employeePurchasePrepaidRestoreService;
		this.memberActivityItemsAggregateService = memberActivityItemsAggregateService;
	}

	@Override
	public void restoreOnRefundSuccess(
			long companyId,
			long orderId,
			long refundBn,
			int requestedRestoreFee,
			List<RefundLine> refundLines) {
		OrdersRelActivity rel = ordersRelActivityMapper.selectById(orderId);
		if (rel == null
				|| rel.getCompanyId() == null
				|| rel.getCompanyId().longValue() != companyId
				|| !PurchaseModeSupport.isPrepaidPoint(rel.getPurchaseMode())) {
			return;
		}
		employeePurchasePrepaidRestoreService.restoreRemainingOrPartial(rel, requestedRestoreFee);
		restoreItemAggregates(rel, refundLines);
	}

	private void restoreItemAggregates(OrdersRelActivity rel, List<RefundLine> refundLines) {
		if (refundLines == null || refundLines.isEmpty()) {
			return;
		}
		Long enterpriseId = rel.getEnterpriseId();
		Long activityId = rel.getActivityId();
		Long userId = rel.getUserId();
		Long companyId = rel.getCompanyId();
		if (companyId == null || enterpriseId == null || activityId == null || userId == null) {
			return;
		}
		for (RefundLine line : refundLines) {
			if (line.itemId() <= 0L || line.num() <= 0) {
				continue;
			}
			memberActivityItemsAggregateService.minusItemAggregate(
					companyId,
					enterpriseId,
					activityId,
					userId,
					line.itemId(),
					Math.max(0, line.itemFee()),
					line.num());
		}
	}
}
