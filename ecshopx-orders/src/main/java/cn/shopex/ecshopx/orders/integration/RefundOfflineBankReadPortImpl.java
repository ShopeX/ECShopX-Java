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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.common.port.order.RefundOfflineBankReadPort;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.service.offline.OfflinePaymentAdminListRowAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RefundOfflineBankReadPortImpl implements RefundOfflineBankReadPort {

	private final OfflinePaymentMapper offlinePaymentMapper;
	private final OfflinePaymentAdminListRowAssembler rowAssembler;

	public RefundOfflineBankReadPortImpl(
			OfflinePaymentMapper offlinePaymentMapper,
			OfflinePaymentAdminListRowAssembler rowAssembler) {
		this.offlinePaymentMapper = offlinePaymentMapper;
		this.rowAssembler = rowAssembler;
	}

	@Override
	public Object getApprovedOfflinePaymentByOrder(long companyId, String orderId) {
		String t = orderId.trim();
		long oid;
		try {
			oid = Long.parseLong(t);
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}
		OfflinePayment row =
				offlinePaymentMapper.selectOne(
						new LambdaQueryWrapper<OfflinePayment>()
								.eq(OfflinePayment::getCompanyId, companyId)
								.eq(OfflinePayment::getOrderId, oid)
								.eq(OfflinePayment::getCheckStatus, 1)
								.last("LIMIT 1"));
		if (row == null) {
			return Collections.emptyList();
		}
		Map<String, Object> map = rowAssembler.toListRowMap(row);
		return map;
	}
}
