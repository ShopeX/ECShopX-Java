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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.OrdersRelActivity;
import cn.shopex.ecshopx.employeepurchase.mapper.OrdersRelActivityMapper;
import cn.shopex.ecshopx.employeepurchase.support.PurchaseModeSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 预充点订单：按剩余未还还点，并累加 restored_prepaid_fee。 */
@Service
public class EmployeePurchasePrepaidRestoreService {

	private final OrdersRelActivityMapper ordersRelActivityMapper;
	private final MemberActivityAggregateService memberActivityAggregateService;

	public EmployeePurchasePrepaidRestoreService(
			OrdersRelActivityMapper ordersRelActivityMapper,
			MemberActivityAggregateService memberActivityAggregateService) {
		this.ordersRelActivityMapper = ordersRelActivityMapper;
		this.memberActivityAggregateService = memberActivityAggregateService;
	}

	/**
	 * @return 实际还点数；0 表示无需再还
	 */
	@Transactional(rollbackFor = Exception.class)
	public int restoreRemainingOrPartial(OrdersRelActivity rel, int requestedRestoreFee) {
		if (rel == null || !PurchaseModeSupport.isPrepaidPoint(rel.getPurchaseMode())) {
			return 0;
		}
		int payable = rel.getPrepaidPayableFee() == null ? 0 : rel.getPrepaidPayableFee();
		int restored = rel.getRestoredPrepaidFee() == null ? 0 : rel.getRestoredPrepaidFee();
		int remaining = Math.max(payable - restored, 0);
		if (remaining <= 0) {
			return 0;
		}
		int apply = requestedRestoreFee <= 0 ? remaining : Math.min(requestedRestoreFee, remaining);
		if (apply <= 0) {
			return 0;
		}
		Long companyId = rel.getCompanyId();
		Long enterpriseId = rel.getEnterpriseId();
		Long activityId = rel.getActivityId();
		Long userId = rel.getUserId();
		if (companyId == null || enterpriseId == null || activityId == null || userId == null) {
			return 0;
		}
		try {
			memberActivityAggregateService.minusAggregateFee(
					companyId, enterpriseId, activityId, userId, apply);
		} catch (RuntimeException e) {
			throw new ResourceException("额度返还失败");
		}
		rel.setRestoredPrepaidFee(restored + apply);
		ordersRelActivityMapper.updateById(rel);
		return apply;
	}
}
