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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.dto.SelfDeliveryStaffAccountListRow;
import cn.shopex.ecshopx.companys.mapper.EmployeeSelfDeliveryStaffMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminSelfDeliveryStaffFeeService {

	private final EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper;

	public AdminSelfDeliveryStaffFeeService(EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper) {
		this.employeeSelfDeliveryStaffMapper = employeeSelfDeliveryStaffMapper;
	}

	public int computeFeeFen(long companyId, NormalOrders order, long selfDeliveryOperatorId) {
		List<SelfDeliveryStaffAccountListRow> staffRows =
				employeeSelfDeliveryStaffMapper.selectAccountListRowsByCompanyIdAndOperatorIds(
						companyId, List.of(selfDeliveryOperatorId));
		if (staffRows == null || staffRows.isEmpty()) {
			throw new ResourceException("选择的配送员不存在，请确认");
		}
		SelfDeliveryStaffAccountListRow acc = staffRows.get(0);
		String paymentMethod = acc.getPaymentMethod();
		Integer paymentFee = acc.getPaymentFee();
		int paymentFeeInt = paymentFee == null ? 0 : paymentFee.intValue();

		int feeFen;
		if ("order".equalsIgnoreCase(paymentMethod)) {
			feeFen = paymentFeeInt;
		} else if ("amount".equalsIgnoreCase(paymentMethod)) {
			long totalFeeFen = 0L;
			String tf = order.getTotalFee();
			if (tf != null && !tf.isBlank()) {
				try {
					totalFeeFen = new BigDecimal(tf.trim()).longValue();
				} catch (NumberFormatException e) {
					throw new ResourceException("订单不存在");
				}
			}
			BigDecimal product = BigDecimal.valueOf((long) paymentFeeInt * totalFeeFen);
			BigDecimal yuan = product.divide(BigDecimal.valueOf(10000L), 2, RoundingMode.HALF_UP);
			feeFen = yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
		} else {
			throw new ResourceException("配送员结算方式无效");
		}
		return feeFen;
	}
}
