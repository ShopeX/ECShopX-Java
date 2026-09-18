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

import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderBrokerageCancelOnRefundPassService {

	private final BrokerageMapper brokerageMapper;

	public AdminOrderBrokerageCancelOnRefundPassService(BrokerageMapper brokerageMapper) {
		this.brokerageMapper = brokerageMapper;
	}

	public void closeBrokerageForCanceledOrder(long companyId, long orderId) {
		if (companyId <= 0L || orderId <= 0L) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Brokerage> w = new LambdaUpdateWrapper<>();
		w.eq(Brokerage::getCompanyId, companyId).eq(Brokerage::getOrderId, String.valueOf(orderId));
		w.set(Brokerage::getIsClose, Boolean.TRUE).set(Brokerage::getUpdated, now);
		brokerageMapper.update(null, w);
	}
}
