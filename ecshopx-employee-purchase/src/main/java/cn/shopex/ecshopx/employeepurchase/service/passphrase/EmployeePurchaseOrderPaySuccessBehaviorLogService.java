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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import cn.shopex.ecshopx.employeepurchase.domain.OrdersRelActivity;
import cn.shopex.ecshopx.employeepurchase.mapper.OrdersRelActivityMapper;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class EmployeePurchaseOrderPaySuccessBehaviorLogService {

	private final OrdersRelActivityMapper ordersRelActivityMapper;
	private final ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService;

	public EmployeePurchaseOrderPaySuccessBehaviorLogService(
			OrdersRelActivityMapper ordersRelActivityMapper,
			ActivityEnterpriseBehaviorLogService activityEnterpriseBehaviorLogService) {
		this.ordersRelActivityMapper = ordersRelActivityMapper;
		this.activityEnterpriseBehaviorLogService = activityEnterpriseBehaviorLogService;
	}

	public void recordEmployeePurchaseOrderPaid(long companyId, long orderId) {
		OrdersRelActivity rel = ordersRelActivityMapper.selectById(orderId);
		if (rel == null || !Objects.equals(rel.getCompanyId(), companyId)) {
			return;
		}
		Long activityId = rel.getActivityId();
		Long enterpriseId = rel.getEnterpriseId();
		Long userId = rel.getUserId();
		if (activityId == null || enterpriseId == null || userId == null) {
			return;
		}
		activityEnterpriseBehaviorLogService.recordOrder(
				companyId, activityId, enterpriseId, userId, orderId);
	}
}
