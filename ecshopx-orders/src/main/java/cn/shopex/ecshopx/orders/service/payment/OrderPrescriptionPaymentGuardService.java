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

package cn.shopex.ecshopx.orders.service.payment;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrdersPrescription;
import cn.shopex.ecshopx.orders.mapper.OrdersPrescriptionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderPrescriptionPaymentGuardService {

	private final OrdersPrescriptionMapper ordersPrescriptionMapper;

	public OrderPrescriptionPaymentGuardService(OrdersPrescriptionMapper ordersPrescriptionMapper) {
		this.ordersPrescriptionMapper = ordersPrescriptionMapper;
	}

	public void assertOrderPrescriptionAllowsPay(long companyId, String orderIdStr, Map<String, Object> orderInfo) {
		Object psRaw = orderInfo == null ? null : orderInfo.get("prescription_status");
		int ps = 0;
		if (psRaw instanceof Number n) {
			ps = n.intValue();
		} else if (psRaw != null) {
			try {
				ps = Integer.parseInt(String.valueOf(psRaw).trim());
			} catch (NumberFormatException ignored) {
				ps = 0;
			}
		}
		if (ps == 0) {
			return;
		}
		OrdersPrescription row =
				ordersPrescriptionMapper.selectOne(
						new LambdaQueryWrapper<OrdersPrescription>()
								.eq(OrdersPrescription::getCompanyId, companyId)
								.eq(OrdersPrescription::getOrderId, orderIdStr)
								.eq(OrdersPrescription::getIsDeleted, 0)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("处方药商品请先开方");
		}
		Integer status = row.getStatus();
		if (status == null || status != 1) {
			throw new ResourceException("处方单已作废");
		}
		Integer audit = row.getAuditStatus();
		if (audit != null && audit == 1) {
			throw new ResourceException("处方单待审核");
		}
		if (audit != null && audit == 3) {
			throw new ResourceException("处方单审核未通过");
		}
	}
}
